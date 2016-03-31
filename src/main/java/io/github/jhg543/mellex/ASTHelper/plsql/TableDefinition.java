package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableDefinition {
	private boolean sessionScoped = false;

	public boolean isSessionScoped() {
		return sessionScoped;
	}

	public void setSessionScoped(boolean sessionScoped) {
		this.sessionScoped = sessionScoped;
	}

	protected String name;
	
	protected boolean infered = false;
	
	public TableDefinition(String name,boolean sessionScoped,  boolean infered) {
		super();
		this.sessionScoped = sessionScoped;
		this.name = name;
		this.infered = infered;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public ColumnDefinition addColumn(String name,boolean guessed) {
		int index = columns.size();
		ColumnDefinition def = new ColumnDefinition(name, index, this);
		def.setInfered(guessed);
		nameIndexMap.put(name, index);
		columns.add(def);
		
		return def;
	}
	
	public ColumnDefinition addColumn(String name) {
		return addColumn(name,false);
	}

}
