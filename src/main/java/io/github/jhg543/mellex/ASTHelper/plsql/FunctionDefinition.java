package io.github.jhg543.mellex.ASTHelper.plsql;

import io.github.jhg543.mellex.ASTHelper.InfSource;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class FunctionDefinition extends ObjectDefinition {
	
	private List<VariableDefinition> parameters;
	
	private StateTransformDefinition definition;
	
	public List<VariableDefinition> getParameters() {
		return parameters;
	}
	public void setParameters(List<VariableDefinition> parameters) {
		this.parameters = parameters;
	}
	public StateTransformDefinition getDefinition() {
		return definition;
	}
	public void setDefinition(StateTransformDefinition definition) {
		this.definition = definition;
	}
	
	
	
}
