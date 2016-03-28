package io.github.jhg543.mellex.ASTHelper.symbol;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import io.github.jhg543.mellex.ASTHelper.plsql.TableDefinition;

public class TableStorage {
	private Map<String, TableDefinition> tables = new LinkedHashMap<>();

	public TableDefinition getTable(String name) {
		return tables.get(name);

	}

	public void putTable(String name, TableDefinition def) {
		Preconditions.checkState(tables.put(name, def)==null, "redefine table %s", name);
	}

}
