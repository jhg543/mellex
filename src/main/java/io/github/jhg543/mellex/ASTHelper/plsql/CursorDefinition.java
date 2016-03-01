package io.github.jhg543.mellex.ASTHelper.plsql;

import io.github.jhg543.mellex.ASTHelper.SubQuery;

import java.util.List;

public class CursorDefinition extends ObjectDefinition {

	private List<VariableDefinition> parameters;
	private SubQuery selectInf;
	
	public List<VariableDefinition> getParameters() {
		return parameters;
	}
	public void setParameters(List<VariableDefinition> parameters) {
		this.parameters = parameters;
	}

	public SubQuery getSelectInf() {
		return selectInf;
	}
	public void setSelectInf(SubQuery selectInf) {
		this.selectInf = selectInf;
	}
}
