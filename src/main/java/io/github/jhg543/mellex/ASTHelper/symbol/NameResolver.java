package io.github.jhg543.mellex.ASTHelper.symbol;

import com.google.common.base.Preconditions;

import io.github.jhg543.mellex.ASTHelper.plsql.SelectStmtData;
import io.github.jhg543.mellex.util.DatabaseVendor;

public class NameResolver {
	private AliasColumnResolver alias;
	private LocalObjectResolver local;
	private GlobalObjectResolver global;
	private OtherResultColumnResolver ors;
	private PseudoColumnResolver pse;
	private DatabaseVendor vendor;

	public Object searchByName(String name) {
		Object result;

		result = pse.searchByName(name);
		if (result != null) {
			return result;
		}

		result = alias.searchByName(name);
		if (result != null) {
			return result;
		}

		result = local.searchByName(name);
		if (result != null) {
			return result;
		}

		result = global.searchByName(name);
		if (result != null) {
			return result;
		}

		if (vendor.equals(DatabaseVendor.TERADATA)) {
			result = ors.searchByName(name);
			if (result != null) {
				return result;
			}
		}

		result = alias.guessColumn(name);
		if (result != null) {
			return result;
		}

		return null;
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
		local.pushScope(scopeId, vendor.equals(DatabaseVendor.ORACLE));
		if (vendor.equals(DatabaseVendor.TERADATA)) {
			ors.enterSelectStmt();
		}
	}

	public void exitSelectStmt(Object scopeId) {
		local.popScope(scopeId);
		if (vendor.equals(DatabaseVendor.TERADATA)) {
			ors.exitSelectStmt();
		}
	}

	public void enterResultColumn(String alias) {
		if (vendor.equals(DatabaseVendor.TERADATA)) {
			ors.newResultColumn(alias);
		}
	}

	public SelectStmtData rewriteAfterResultColumns(SelectStmtData tempResult) {
		if (vendor.equals(DatabaseVendor.TERADATA)) {
			return ors.rewriteStateFunc(tempResult);
		}
		return tempResult;
	}
}
