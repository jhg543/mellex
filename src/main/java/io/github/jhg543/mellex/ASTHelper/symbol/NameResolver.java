package io.github.jhg543.mellex.ASTHelper.symbol;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;

import io.github.jhg543.mellex.ASTHelper.plsql.FunctionDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.SelectStmtData;
import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.TableDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;
import io.github.jhg543.mellex.util.DatabaseVendor;
import io.github.jhg543.mellex.util.tuple.Tuple2;

public class NameResolver {
	private AliasColumnResolver alias;
	private LocalObjectResolver local;
	private GlobalObjectResolver global;
	private OtherResultColumnResolver ors;
	private PseudoColumnResolver pse;
	private DatabaseVendor vendor;
	private TableStorage tableStorage;
	private boolean guessEnabled;

	public NameResolver(DatabaseVendor vendor, TableStorage tableStorage, boolean guessEnabled) {
		super();
		this.vendor = vendor;
		this.tableStorage = tableStorage;
		this.guessEnabled = guessEnabled;
		this.alias = new AliasColumnResolver(vendor.equals(DatabaseVendor.ORACLE), tableStorage,guessEnabled);
		this.local = new LocalObjectResolver();
		this.global = new GlobalObjectResolver(true, tableStorage::getTable);
		this.pse = new PseudoColumnResolver(vendor);
		if (vendor.equals(DatabaseVendor.TERADATA)) {
			this.ors = new OtherResultColumnResolver();
		}

	}

	/**
	 * 
	 * @param name
	 * @return not found = null
	 */
	public FunctionDefinition searchFunction(String name) {
		ObjectDefinition result = local.searchByName(name);
		if (result != null && result instanceof FunctionDefinition) {
			return (FunctionDefinition) result;
		}

		result = global.searchByName(name);
		if (result != null && result instanceof FunctionDefinition) {
			return (FunctionDefinition) result;
		}
		return null;

	}

	public VariableDefinition searchVariable(String name)
	{
		ObjectDefinition result = local.searchByName(name);
		if (result != null && result instanceof VariableDefinition) {
			return (VariableDefinition) result;
		}
		return null;
	}
	/**
	 * 
	 * @param name
	 * @return not found = throw exception
	 */
	public Tuple2<ObjectDefinition, StateFunc> searchByName(String name) {
		Object result;

		result = pse.searchByName(name);
		if (result != null) {
			return Tuple2.of(null, null);
		}

		result = alias.searchByName(name);
		if (result != null) {
			return (Tuple2<ObjectDefinition, StateFunc>) result;
		}

		result = local.searchByName(name);
		if (result != null) {
			return Tuple2.of((ObjectDefinition) result, null);
		}

		result = global.searchByName(name);
		if (result != null) {
			return Tuple2.of((ObjectDefinition) result, null);
		}

		if (vendor.equals(DatabaseVendor.TERADATA)) {
			result = ors.searchByName(name);
			if (result != null) {
				return (Tuple2<ObjectDefinition, StateFunc>) result;
			}
		}

		if (isGuessEnabled()) {
			result = alias.guessColumn(name);
			if (result != null) {
				return Tuple2.of((ObjectDefinition) result, null);
			}
		}

		return null;
	}

	/**
	 * this is used in ”update a1 from sometable a1"
	 * 
	 * @param alias
	 * @return null if not found
	 */
	public TableDefinition getAliasTableDefinition(String alias) {
		return (TableDefinition) this.alias.searchSubqueryOrCteOrLiveTable(alias);
		
	}

	public List<Tuple2<String, StateFunc>> searchWildcardAll(String fileName, int lineNumber, int charPosition) {
		return alias.wildCardAll(fileName, lineNumber, charPosition);
	}

	public List<Tuple2<String, StateFunc>> searchWildcardOneTable(String tableName, String fileName, int lineNumber,
			int charPosition) {
		return alias.wildCardOneTable(tableName, fileName, lineNumber, charPosition);
	}

	public void enterFunctionDefinition(Object funcid) {
		local.pushScope(funcid, false);
	}

	public void metBegin(Object scopeId) {
		local.pushScope(scopeId, true);
	}

	public void metEnd(Object scopeId) {
		local.popScope(scopeId);
	}

	public void exitFunctionDefinition(Object funcid) {
		local.popScope(funcid);
	}

	public void popCte(String alias) {
		this.alias.popCte(alias);
	}

	public void pushCte(String alias, SelectStmtData data) {
		this.alias.pushCte(alias, data);
	}

	public void enterSelectStmt(Object scopeId) {
		alias.pushScope(scopeId);
		local.pushScope(scopeId, true);
		if (vendor.equals(DatabaseVendor.TERADATA)) {
			ors.enterSelectStmt();
		}
	}

	public void exitSelectStmt(Object scopeId) {
		local.popScope(scopeId);
		alias.popScope(scopeId);
		if (vendor.equals(DatabaseVendor.TERADATA)) {
			ors.exitSelectStmt();
		}
	}

	public void enterResultColumn(String alias) {
		if (vendor.equals(DatabaseVendor.TERADATA)) {
			ors.newResultColumn(alias);
		}
	}

	public void collectResultColumnAlias(List<String> aliasList) {
		if (vendor.equals(DatabaseVendor.TERADATA)) {
			ors.collectResultColumnAlias(aliasList);
		}
	}

	public SelectStmtData rewriteAfterResultColumns(SelectStmtData tempResult) {
		if (vendor.equals(DatabaseVendor.TERADATA)) {
			return ors.rewriteStateFunc(tempResult);
		}
		return tempResult;
	}

	public void defineTable(String name, TableDefinition def) {
		tableStorage.putTable(name, def);
	}

	/**
	 * null if not found
	 * 
	 * @param name
	 * @return
	 */
	public TableDefinition searchTable(String name) {
		return tableStorage.getTable(name);
	}

	public void addFromTable(String tableName, String alias) {
		this.alias.addFromTable(tableName, alias);
	}

	public void addFromSubQuery(String alias, SelectStmtData subquery) {
		this.alias.addFromSubQuery(alias, subquery);
	}

	public boolean isGuessEnabled() {
		return guessEnabled;
	}

	public void setGuessEnabled(boolean guessEnabled) {
		this.guessEnabled = guessEnabled;
	}
}
