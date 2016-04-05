package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import io.github.jhg543.mellex.ASTHelper.plsql.ColumnDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ExprAnalyzeResult;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.plsql.SelectStmtData;
import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.ValueFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

public class InstFuncHelper {

	private static StateFunc applyState(StateFunc fn, State s) {
		Map<ObjectDefinition, ValueFunc> params = new HashMap<>();
		s.getVarState().forEach((k, v) -> params.put(k, v.getValueInfluence()));
		return fn.applyState(params);
	}

	private static List<Object> applyPossibleLiteralValue(List<Object> def, State s) {
		List<Object> result = new ArrayList<>();
		for (Object o : def) {
			if (o instanceof String) {
				result.add(o);
			} else if (o instanceof VariableDefinition) {
				VariableState v = s.getVarState().get(o);
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
		return result;
	}

	private static StateFunc metInsertColumn(ColumnDefinition cdef, StateFunc fn, State s,
			Map<VariableDefinition, VariableState> newVarState, AtomicBoolean stateModified) {

		s.getFuncState().addInsertOrUpdate(cdef, fn);

		if (!fn.getAssigns().isEmpty()) {
			if (!stateModified.get()) {
				newVarState.putAll(s.getVarState());
				stateModified.set(true);
				;
			}

			// this means variable value should not change and used
			// during one instruction
			// TODO check k is VariableDefinition
			fn.getAssigns().forEach((k, v) -> {
				newVarState.put((VariableDefinition) k, new VariableState(v, null));
			});
		}
		return fn;
	}

	/**
	 * merge assigns. record updates.
	 * 
	 * @param fn
	 * @param s
	 * @param newVarState
	 * @param stateModified
	 * @return
	 */
	private static StateFunc metFn(StateFunc fn, State s, Map<VariableDefinition, VariableState> newVarState,
			AtomicBoolean stateModified) {
		return metInsertColumn(null, fn, s, newVarState, stateModified);
	}

	public static Function<State, State> callExpression(StateFunc exprDefinition) {
		Function<State, State> fff = (State s) -> {
			Map<VariableDefinition, VariableState> newVarState = new HashMap<>();
			AtomicBoolean stateModified = new AtomicBoolean(false);

			StateFunc fn = applyState(exprDefinition, s);
			metFn(fn, s, newVarState, stateModified);

			if (stateModified.get()) {
				return new State(newVarState, s.getFuncState());
			} else {
				return s;
			}

		};

		return fff;
	}

	public static Function<State, State> assignExpression(VariableDefinition lvalue, ExprAnalyzeResult exprDefinition) {
		Function<State, State> fff = (State s) -> {
			Map<VariableDefinition, VariableState> newVarState = new HashMap<>(s.getVarState());
			AtomicBoolean stateModified = new AtomicBoolean(true);

			StateFunc fn = applyState(exprDefinition.getTransformation(), s);
			metFn(fn, s, newVarState, stateModified);

			// TODO possible literal

			newVarState.put(lvalue, new VariableState(fn.getValue().add(fn.getBranchCond()), applyPossibleLiteralValue(exprDefinition.getLiteralValue(),s)));

			if (stateModified.get()) {
				return new State(newVarState, s.getFuncState());
			} else {
				return s;
			}

		};

		return fff;
	}

	public static Function<State, State> insertOrUpdateFunc(List<ColumnDefinition> cdefs, List<StateFunc> subs) {

		Preconditions.checkArgument(cdefs.size() == subs.size(), "cdef & expr size mismatch %s %s", cdefs.size(), subs.size());
		Function<State, State> fff = (State s) -> {
			Map<VariableDefinition, VariableState> newVarState = new HashMap<>();
			AtomicBoolean stateModified = new AtomicBoolean(false);

			List<StateFunc> stateMods = subs.stream().map(fn -> applyState(fn, s)).collect(Collectors.toList());
			for (int i = 0; i < cdefs.size(); ++i) {
				StateFunc fn = stateMods.get(i);
				metInsertColumn(cdefs.get(i), fn, s, newVarState, stateModified);
			}

			if (stateModified.get()) {
				return new State(newVarState, s.getFuncState());
			} else {
				return s;
			}
		};
		return fff;
	}

	public static Function<State, State> NopFunc() {
		return Function.identity();
	}

	public static Function<State, State> SelectFunc(SelectStmtData ss) {

		Function<State, State> fff = (State s) -> {
			Map<VariableDefinition, VariableState> newVarState = new HashMap<>();
			AtomicBoolean stateModified = new AtomicBoolean(false);

			List<VariableDefinition> intos = null;
			// INTO CLAUSE
			if (ss.getIntos() != null) {
				if (!stateModified.get()) {
					newVarState.putAll(s.getVarState());
					stateModified.set(true);
				}
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
				metFn(fn, s, newVarState, stateModified);
				if (intos != null) {
					newVarState.put(intos.get(i), new VariableState(fn.getValue().add(fn.getBranchCond()), null));
				}
			}

			if (stateModified.get()) {
				return new State(newVarState, s.getFuncState());
			} else {
				return s;
			}
		};
		return fff;
	}
}
