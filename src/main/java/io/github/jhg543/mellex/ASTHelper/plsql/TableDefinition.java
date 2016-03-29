package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableDefinition {
	private boolean sessionScoped;

	public boolean isSessionScoped() {
		return sessionScoped;
	}

	public void setSessionScoped(boolean sessionScoped) {
		this.sessionScoped = sessionScoped;
	}

	protected List<ColumnDefinition> columns = new ArrayList<>();
	protected Map<String, Integer> nameIndexMap = new HashMap<>(); // index is 0 based

	/**
	 * do not modify it.
	 * 
	 * @return
	 */
	public List<ColumnDefinition> getColumns() {
		return Collections.unmodifiableList(columns);
	}

	/**
	 * @param name
	 * @return null if not found
	 */
	public ColumnDefinition getColumnByName(String name) {
		Integer index = nameIndexMap.get(name);
		if (index == null) {
			return null;
		}
		return (ColumnDefinition) columns.get(index.intValue());
	}

	public ColumnDefinition addColumn(String name) {
		int index = columns.size();
		ColumnDefinition def = new ColumnDefinition(name, index);
		nameIndexMap.put(name, index);
		columns.add(def);
		return def;
	}

}
