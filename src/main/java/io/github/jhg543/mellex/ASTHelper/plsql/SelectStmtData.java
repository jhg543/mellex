package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.Map;

public class SelectStmtData {
	/**
	 * should be an ordered map
	 */
	private Map<String, StateFunc> columns;

	public Map<String, StateFunc> getColumns() {
		return columns;
	}

	public SelectStmtData(Map<String, StateFunc> columns) {
		super();
		this.columns = columns;
	}

}
