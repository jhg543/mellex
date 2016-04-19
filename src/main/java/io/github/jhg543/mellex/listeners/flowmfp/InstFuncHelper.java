package io.github.jhg543.mellex.listeners.flowmfp;

import com.google.common.base.Preconditions;
import io.github.jhg543.mellex.ASTHelper.plsql.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InstFuncHelper {

    private static StateFunc applyState(StateFunc fn, State s) {
        // TODO what if varstate has R?
        Map<ObjectDefinition, ValueFunc> params = new HashMap<>();
        s.readVarState().forEach((k, v) -> params.put(k, v.getValueInfluence()));
        return fn.applyState(params);
    }

    private static List<Object> applyPossibleLiteralValue(List<Object> def, State s) {

        List<Object> result = new ArrayList<>();
        for (Object o : def) {
            if (o instanceof String) {
                result.add(o);
            } else if (o instanceof VariableDefinition) {
                VariableState v = s.readVarState().get(o);
                if (v == null) {
                    throw new IllegalStateException(o.toString() + " not found in state");
                }
                if (v.getPossibleLiteralValue() == null) {
                    // TODO ???????
                    result.add(v.getValueInfluence());
                } else {
                    // TODO what if add to self?
                    result.addAll(v.getPossibleLiteralValue());
                }
            } else {
                // ValueFunc...
                result.add(o);
            }
        }
        if (result.isEmpty()) {
            return Collections.emptyList();
        } else {
            return result;
        }
    }

    private static void metInsertColumn(ColumnDefinition cdef, StateFunc fn, State newState) {

        newState.getFuncState().addInsertOrUpdate(cdef, fn);
        // this means variable value should not change and used
        // during one instruction
        // TODO check k is VariableDefinition
        fn.getAssigns().forEach((k, v) -> {
            newState.writeVariable((VariableDefinition) k, new VariableState(v, null));
        });

    }

    /**
     * merge assigns. record updates.
     *
     * @param fn
     * @param newState
     * @return
     */
    private static void metFn(StateFunc fn, State newState) {
        metInsertColumn(null, fn, newState);
    }

    public static Function<State, State> callExpression(StateFunc exprDefinition) {
        Function<State, State> fff = (State s) -> {
            State ns = s.copy();

            StateFunc fn = applyState(exprDefinition, s);
            metFn(fn, ns);

            return ns;

        };

        return fff;
    }

    public static Function<State, State> assignExpression(VariableDefinition lvalue, ExprAnalyzeResult exprDefinition) {
        Function<State, State> fff = (State s) -> {
            State ns = s.copy();

            StateFunc fn = applyState(exprDefinition.getTransformation(), s);
            metFn(fn, ns);

            // TODO possible literal

            ns.writeVariable(lvalue, new VariableState(fn.getValue().add(fn.getBranchCond()),
                    applyPossibleLiteralValue(exprDefinition.getLiteralValue(), s)));

            return ns;

        };

        return fff;
    }

    public static Function<State, State> insertOrUpdateFunc(List<ColumnDefinition> cdefs, List<StateFunc> subs) {

        Preconditions.checkArgument(cdefs.size() == subs.size(), "cdef & expr size mismatch %s %s", cdefs.size(), subs.size());
        Function<State, State> fff = (State s) -> {
            State ns = s.copy();

            List<StateFunc> stateMods = subs.stream().map(fn -> applyState(fn, s)).collect(Collectors.toList());
            for (int i = 0; i < cdefs.size(); ++i) {
                StateFunc fn = stateMods.get(i);
                metInsertColumn(cdefs.get(i), fn, ns);
            }

            return ns;
        };
        return fff;
    }

    public static Function<State, State> NopFunc() {
        return Function.identity();
    }

    public static Function<State, State> selectFunc(SelectStmtData ss) {

        Function<State, State> fff = (State s) -> {
            State ns = s.copy();
            List<VariableDefinition> intos = null;
            // INTO CLAUSE
            if (ss.getIntos() != null) {
                intos = ss.getIntos();
                if (ss.getColumns().size() == intos.size()) {
                    // do nothing
                    // TODO what if variable is record with 1 column.
                } else {
                    if (intos.size() > 1) {
                        throw new IllegalStateException(
                                String.format("intos %s count mismatch %s", intos, ss.getNameIndexMap()));
                    }
                    // TODO analyze record structure
                    VariableDefinition recdef = intos.get(0);
                    intos = new ArrayList<>();
                    for (ResultColumn rc : ss.getColumns()) {
                        intos.add(recdef.getColumn(rc.getName()));
                    }
                    // a record
                }
            }

            for (int i = 0; i < ss.getColumns().size(); ++i) {
                StateFunc fn = applyState(ss.getColumnExprFunc(i), s);
                metFn(fn, ns);
                if (intos != null) {
                    ns.writeVariable(intos.get(i), new VariableState(fn.getValue().add(fn.getBranchCond()), null));
                }
            }

            return ns;
        };
        return fff;
    }

    public static Function<State, State> branchCondFunc(StateFunc branchCondDef) {
        Function<State, State> fff = (State s) -> {
            State ns = s.copy();
            StateFunc fn = applyState(branchCondDef, s);
            metFn(fn, ns);
            ns.getFuncState().addBranch(fn);
            return ns;
        };
        return fff;
    }


    public static Function<State, State> openCursorFuncStatic(CursorDefinition cursorDefinition, SelectStmtData ss) {

        // (1) apply parameter value (Done in visitOpenCursor)
        // (2) apply state
        /* for example DECLARE n1 number, n2 number, n3 number,cursor c1(sal number) is select n1*columnA-sal from t1
        begin open c1(n2+n3)
         */

        // it's very similar to "select into"
        Function<State, State> fff = (State s) -> {
            State ns = s.copy();
            CursorState.Builder csbuilder = new CursorState.Builder();
            for (ResultColumn rc : ss.getColumns()) {
                StateFunc fn = applyState(rc.getExpr(), s);
                metFn(fn, ns);
                csbuilder.put(rc.getName(), new VariableState(fn.getValue().add(fn.getBranchCond()), null));
            }
            ns.writeCursorState(cursorDefinition, csbuilder.build());
            return ns;
        };
        return fff;
    }

    public static Function<State, State> closeCursorFunc(CursorDefinition cursorDefinition) {
        Function<State, State> fff = (State s) -> {
            State ns = s.copy();
            ns.removeCursorState(cursorDefinition);
            return ns;
        };
        return fff;
    }

    public static Function<State, State> fetchCursorFunc(CursorDefinition cursorDefinition, List<VariableDefinition> intos) {

        // fetch c into a1,a2,a3
        // fetch c into record1
        // TODO c is 1 column and record1 is not scalar
        Function<State, State> fff = (State s) -> {
            State ns = s.copy();
            CursorState cs = s.readCursorState().get(cursorDefinition);
            if (intos.size() > 1 && intos.size() != cs.getColumns().size()) {
                throw new IllegalStateException(String.format("Fetch into size mismatch %d %d", intos.size(), cs.getColumns().size()));
            }

            if (intos.size() == cs.getColumns().size()) {
                // into multiple variables
                for (int i = 0; i < cs.getColumns().size(); ++i) {
                    ns.writeVariable(intos.get(i), cs.getColumns().get(i));
                }
            } else {
                // into a record
                cs.getNameIndexMap().forEach((name, index) -> ns.writeVariable(intos.get(0).getColumn(name), cs.getColumns().get(index)));
            }

            return ns;
        };
        return fff;
    }


    public static Function<State, State> OpenCursorFuncDynamic(CursorDefinition def, VariableDefinition dynamicSQL) {
        throw new UnsupportedOperationException("dynamic cursor not implemented");
    }

    public static Function<State, State> returnStmtFunc(StateFunc valueExpr) {

        Function<State, State> fff = (State s) -> {
            State ns = s.copy();

            if (valueExpr != null) {
                StateFunc fn = applyState(valueExpr, s);
                metFn(fn, ns);
                ns.getFuncState().addReturnValue(fn);
            }

            ns.getFuncState().collectOutParameterAssign(s);
            return ns;
        };
        return fff;
    }
}
