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
            newState.writeOneVariable((VariableDefinition) k, new VariableState(v, null));
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

            ns.writeOneVariable(lvalue, new VariableState(fn.getValue().add(fn.getBranchCond()),
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

    public static Function<State, State> SelectFunc(SelectStmtData ss) {

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
                    ns.writeOneVariable(intos.get(i), new VariableState(fn.getValue().add(fn.getBranchCond()), null));
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


    public static Function<State, State> OpenCursorFuncStatic(CursorDefinition def, SelectStmtData ss) {

        // (1) apply parameter value (Done in visitOpenCursor)
        // (2) apply state
        /* for example DECLARE n1 number, n2 number, n3 number,cursor c1(sal number) is select n1*columnA-sal from t1
        begin open c1(n2+n3)
         */
        Function<State, State> fff = (State s) -> {
            State ns = s.copy();

            // SelectStmtData appliedss = new SelectStmtData();

            for (int i = 0; i < ss.getColumns().size(); ++i) {
                StateFunc fn = applyState(ss.getColumnExprFunc(i), s);
                metFn(fn, ns);
            }

            return ns;
        };
        return fff;
    }


    public static Function<State, State> OpenCursorFuncDynamic(CursorDefinition def, VariableDefinition dynamicSQL) {
        throw new UnsupportedOperationException("dynamic cursor not implemented");
    }
}
