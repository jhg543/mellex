package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import io.github.jhg543.mellex.ASTHelper.plsql.ColumnDefinition;
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

	public static Function<State, State> insertOrUpdateFunc(List<ColumnDefinition> cdefs, List<StateFunc> subs) {

		Preconditions.checkArgument(cdefs.size() == subs.size(), "cdef & expr size mismatch %s %s", cdefs.size(), subs.size());
		Function<State, State> fff = (State s) -> {

			Map<VariableDefinition, VariableState> newVarState = new HashMap<>();
			boolean stateModified = false;
			List<StateFunc> stateMods = subs.stream().map(fn -> applyState(fn, s)).collect(Collectors.toList());
			for (int i = 0; i < cdefs.size(); ++i) {
				StateFunc fn = stateMods.get(i);
				s.getFuncState().addInsertOrUpdate(cdefs.get(i), fn);
				if (!fn.getAssigns().isEmpty()) {
					if (!stateModified) {
						newVarState.putAll(s.getVarState());
						stateModified = true;
					}

					// this means variable value should not change and used
					// during one instruction
					// TODO check k is VariableDefinition
					fn.getAssigns().forEach((k, v) -> {
						newVarState.put((VariableDefinition) k, new VariableState(v, null));
					});
				}
			}
			if (stateModified) {
				return new State(newVarState, s.getFuncState());
			} else {
				return s;
			}
		};
		return fff;
	}


	public static Function<State, State> SelectFunc(SelectStmtData ss) {
		
		Function<State, State> fff = (State s) -> {
			Map<VariableDefinition, VariableState> newVarState = new HashMap<>();
			boolean stateModified = false;

			List<VariableDefinition> intos = null;
			// INTO CLAUSE
			if (ss.getIntos()!=null)
			{
				if (!stateModified) {
					newVarState.putAll(s.getVarState());
					stateModified = true;
				}
				intos = ss.getIntos();
				if (ss.getColumns().size()==intos.size())
				{
					// do nothing
					// TODO what if variable is record with 1 column.
				}
				else
				{
					if (intos.size()>1)
					{
						throw new IllegalStateException(String.format("intos %s count mismatch %s",intos,ss.getNameIndexMap()));
					}
					// TODO analyze record structure
					VariableDefinition recdef = intos.get(0);
					intos = new ArrayList<>();
					for (ResultColumn rc:ss.getColumns())
					{
					intos.add(recdef.getColumn(rc.getName()));
					}
					// a record
				}
			}
			
			for (int i = 0; i < ss.getColumns().size(); ++i) {
				StateFunc fn = applyState(ss.getColumnExprFunc(i),s);
				s.getFuncState().addInsertOrUpdate(null, fn);
				if (!fn.getAssigns().isEmpty()) {
					if (!stateModified) {
						newVarState.putAll(s.getVarState());
						stateModified = true;
					}

					// this means variable value should not change and used
					// during one instruction
					// TODO check k is VariableDefinition
					fn.getAssigns().forEach((k, v) -> {
						newVarState.put((VariableDefinition) k, new VariableState(v, null));
					});
					
					
				}
				if (intos!=null)
				{
					
				}
			}
			
			if (stateModified) {
				return new State(newVarState, s.getFuncState());
			} else {
				return s;
			}
		};
		return fff;
	}	
}
