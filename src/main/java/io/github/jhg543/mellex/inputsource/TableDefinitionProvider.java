package io.github.jhg543.mellex.inputsource;

import io.github.jhg543.mellex.ASTHelper.CreateTableStmt;
import io.github.jhg543.mellex.ASTHelper.ObjectName;

import java.util.Map;

public interface TableDefinitionProvider {

	public abstract CreateTableStmt queryTable(ObjectName name2);
	
	public abstract CreateTableStmt queryTable(String name);

	public abstract void putTable(CreateTableStmt stmt, boolean isvolatile);

	public abstract Map<String, CreateTableStmt> getVolatileTables();
	
	public abstract Map<String, CreateTableStmt> getPermanentTables();

	public abstract void clearVolatileTables();

}