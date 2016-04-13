package io.github.jhg543.mellex.ASTHelper.plsql;

import io.github.jhg543.mellex.ASTHelper.SubQuery;

import java.util.List;

public class CursorDefinition extends ObjectDefinition {

	private List<ParameterDefinition> parameters;
	
	public List<ParameterDefinition> getParameters() {
		return parameters;
	}
	public void setParameters(List<ParameterDefinition> parameters) {
		this.parameters = parameters;
	}

	private SelectStmtData selectStmt;

	public SelectStmtData getSelectStmt() {
		return selectStmt;
	}
	public void setSelectStmt(SelectStmtData selectStmt) {
		this.selectStmt = selectStmt;
	}
	
	
}
