package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.LinkedHashMap;
import java.util.Map;

public class TableDefinition {
	private Map<String,ColumnDefinition> columns = new LinkedHashMap<>();

	public Map<String, ColumnDefinition> getColumns() {
		return columns;
	}

	public void setColumns(Map<String, ColumnDefinition> columns) {
		this.columns = columns;
	}
	
}
