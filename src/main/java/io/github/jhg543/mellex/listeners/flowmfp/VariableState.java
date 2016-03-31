package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.List;

import io.github.jhg543.mellex.ASTHelper.plsql.ValueFunc;

public class VariableState {
	private ValueFunc valueInfluence;
	private List<Object> possibleLiteralValue;
	public ValueFunc getValueInfluence() {
		return valueInfluence;
	}
	public List<Object> getPossibleLiteralValue() {
		return possibleLiteralValue;
	}
	public VariableState(ValueFunc valueInfluence, List<Object> possibleLiteralValue) {
		super();
		this.valueInfluence = valueInfluence;
		this.possibleLiteralValue = possibleLiteralValue;
	}
	
	
}
