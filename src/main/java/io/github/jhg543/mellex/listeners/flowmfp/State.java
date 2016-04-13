package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

public class State {
	public State copy() {
		this.locked = true;
		State s = new State(varState, funcState);
		return s;
	}

	/**
	 * mark
	 */
	private boolean varStateCopied = false;

	/**
	 * copy on write so no write after being copied.
	 */
	private boolean locked = false;

	private Map<VariableDefinition, VariableState> varState;
	private FunctionStateRecorder funcState;

	public Map<VariableDefinition, VariableState> readVarState() {
		return Collections.unmodifiableMap(varState);
	}

	public VariableState writeOneVariable(VariableDefinition def, VariableState s) {
		if (locked) {
			throw new RuntimeException("COW violated");
		}
		if (!varStateCopied) {
			varStateCopied = true;
			varState = new HashMap<>(varState);
		}
		return varState.put(def, s);

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
