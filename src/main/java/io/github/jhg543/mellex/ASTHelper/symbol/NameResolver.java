package io.github.jhg543.mellex.ASTHelper.symbol;

import io.github.jhg543.mellex.util.DatabaseVendor;

public class NameResolver {
	private AliasColumnResolver alias;
	private LocalObjectResolver local;
	private GlobalObjectResolver global;
	private TempOtherResultColumnResolver tcol;
	private OtherResultColumnResolver rcol;
	private DatabaseVendor vendor;
	
	public Object searchByName(String name) {
		Object result;
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

	
}
