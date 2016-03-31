package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.Collections;
import java.util.Map;

import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

public class State {
	private Map<VariableDefinition,VariableState> varState;
	private FunctionStateRecorder funcState;
	
	public Map<VariableDefinition, VariableState> getVarState() {
		return Collections.unmodifiableMap(varState);
	}
	public FunctionStateRecorder getFuncState() {
		return funcState;
	}
	public State(Map<VariableDefinition, VariableState> varState, FunctionStateRecorder funcState) {
		super();
		this.varState = varState;
		this.funcState = funcState;
	}
	
}
