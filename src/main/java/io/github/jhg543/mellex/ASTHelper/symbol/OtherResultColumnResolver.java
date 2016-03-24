package io.github.jhg543.mellex.ASTHelper.symbol;

import java.util.LinkedList;

import com.google.common.base.CharMatcher;

import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.SelectStmtData;
import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;
import io.github.jhg543.mellex.util.tuple.Tuple2;

/**
 * select c1+c1 as c2, 'x' as c1; --- TD only
 */
public class OtherResultColumnResolver {
	private LinkedList<OneResultColumnResolver> stack = new LinkedList<>();
	private CharMatcher dotmatcher = CharMatcher.is('.');

	public void enterSelectStmt() {
		stack.push(new OneResultColumnResolver());
	}

	public void exitSelectStmt() {
		stack.pop();
	}

	public void newResultColumn(String alias) {
		stack.peek().newResultColumn(alias);
	}

	public SelectStmtData rewriteStateFunc(SelectStmtData tempResult) {
		return stack.peek().rewriteStateFunc(tempResult);
	}

	public Tuple2<ObjectDefinition, StateFunc> searchByName(String name) {
		if (stack.isEmpty())
		{
			return null;
		}
		if (dotmatcher.matchesAnyOf(name)) {
			return null;
		}
		return stack.peek().searchByName(name);
	}
}
