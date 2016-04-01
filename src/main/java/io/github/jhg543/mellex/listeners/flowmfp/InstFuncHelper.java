package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.base.Preconditions;

import io.github.jhg543.mellex.ASTHelper.plsql.ColumnDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

public class InstFuncHelper {
	
	private static StateFunc applyState(StateFunc fn,State s)
	{
		Map<VariableDefinition, VariableState>  params = s.getVarState();
		
	}
	
	public Function<State,State> insertOrUpdateFunc(List<ColumnDefinition> cdefs, List<StateFunc> subs)
	{
		Preconditions.checkArgument(cdefs.size()==subs.size(), "cdef & expr size mismatch %s %s", cdefs.size(),subs.size());
		Function<State,State> fff = (State s)->{
			for (int i=0;i<cdefs.size();++i)
			{
				s.getFuncState().addInsertOrUpdate(cdefs.get(i), subs.get(i));
				// TODO assigns?
			}
			s=null;
			return s;
		};
		return fff;
	}
	
	
}
