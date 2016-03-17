package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.List;

public class FunctionDefinition extends ObjectDefinition {
	
	private List<VariableDefinition> parameters;
	
	private StateFunc definition;
	
	public List<VariableDefinition> getParameters() {
		return parameters;
	}
	public void setParameters(List<VariableDefinition> parameters) {
		this.parameters = parameters;
	}
	public StateFunc getDefinition() {
		return definition;
	}
	public void setDefinition(StateFunc definition) {
		this.definition = definition;
	}
	
}
