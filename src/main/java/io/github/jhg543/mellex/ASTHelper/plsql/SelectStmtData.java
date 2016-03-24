package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.List;
import java.util.Map;

public class SelectStmtData {
	private Map<String, StateFunc> columns;
	
	/**
	 * Zero based
	 */
	private List<String> columnOrder;
	
	public Map<String, StateFunc> getColumns() {
		return columns;
	}

	public List<String> getColumnOrder() {
		return columnOrder;
	}

	public void setColumnOrder(List<String> columnOrder) {
		this.columnOrder = columnOrder;
	}

	/**
	 * @param i zero based order
	 * @return
	 */
	public StateFunc getColumn(int i)
	{
		return getColumn(columnOrder.get(i));
	}
	
	public StateFunc getColumn(String name)
	{
		return columns.get(name);
	}

	public SelectStmtData(Map<String, StateFunc> columns, List<String> columnOrder) {
		super();
		this.columns = columns;
		this.columnOrder = columnOrder;
	}
	
	
}
