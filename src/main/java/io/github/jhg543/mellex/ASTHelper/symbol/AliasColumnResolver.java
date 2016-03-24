package io.github.jhg543.mellex.ASTHelper.symbol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import io.github.jhg543.mellex.ASTHelper.plsql.ColumnDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.SelectStmtData;
import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.TableDefinition;
import io.github.jhg543.mellex.util.tuple.Tuple2;
import io.github.jhg543.mellex.util.tuple.Tuple3;

public class AliasColumnResolver {

	private LinkedList<Scope> scopes = new LinkedList<>();

	/**
	 * Common table expression, bypass parentScope visible limit and columns can
	 * be used with cte alias.
	 */
	private Map<String, SelectStmtData> cte = new HashMap<>();

	/**
	 * for Oracle it's true, for other DB it's false
	 */
	private boolean parentScopeVisible;

	private Function<String, TableDefinition> tableResolver;

	private static Splitter dotsplitter = Splitter.on('.');

	public AliasColumnResolver(boolean parentScopeVisible, Function<String, TableDefinition> tableResolver) {
		super();
		this.parentScopeVisible = parentScopeVisible;
		this.tableResolver = tableResolver;
	}

	public void popCte(String alias) {
		Preconditions.checkNotNull(cte.remove(alias), "cte removal fail %s", alias);
	}

	public void pushCte(String alias, SelectStmtData data) {
		Preconditions.checkState(cte.put(alias, data) == null, "duplicate cte %s", alias);
	}

	public void pushScope(Object scopeId) {
		Scope s = new Scope();
		s.setId(scopeId);
		s.setSubqueries(new HashMap<>());
		s.setLiveTables(new HashMap<>());
		this.scopes.push(s);
	}

	public void popScope(Object expectedScopeId) {
		Preconditions.checkState(expectedScopeId == this.scopes.pop().getId(), "popped scope is not as expected");
	}

	public void addTable(String tableName) {
		// with ctetable1 as ... select * from ctetable1;
		if (cte.containsKey(tableName)) {
			return;
		}
		scopes.peek().getLiveTables().put(tableName, tableResolver.apply(tableName));
		// select * from sometable

	}

	public void addTable(String tableName, String alias) {
		// with ctetable1 as ... select * from ctetable1;
		if (cte.containsKey(tableName)) {
			return;
		}
		scopes.peek().getLiveTables().put(alias, tableResolver.apply(tableName));
		// select * from sometable

	}

	private SelectStmtData searchSubqueryOrCte(String alias) {

		for (Scope s : scopes) {
			SelectStmtData data = s.getSubqueries().get(alias);
			if (data != null) {
				return data;
			}

			if (!parentScopeVisible) {
				break;
			}
		}

		return cte.get(alias);
	}

	private Tuple3<String, ColumnDefinition, StateFunc> searchColumn(String name) {
		Tuple3<String, ColumnDefinition, StateFunc> result = null;

		for (Scope s : scopes) {
			for (Entry<String, SelectStmtData> e : s.getSubqueries().entrySet()) {
				StateFunc fc = e.getValue().getColumns().get(name);
				if (fc != null) {
					if (result != null) {
						throw new IllegalStateException(
								String.format("Duplicate column %s definition in %s %s", name, result.getField0(), e.getKey()));
					}

					result = Tuple3.of(e.getKey(), null, fc);
				}
			}

			for (Entry<String, TableDefinition> e : s.getLiveTables().entrySet()) {
				ColumnDefinition cd = e.getValue().getColumns().get(name);
				if (cd != null) {
					if (result != null) {
						throw new IllegalStateException(
								String.format("Duplicate column %s definition in %s %s", name, result.getField0(), e.getKey()));
					}
					result = Tuple3.of(e.getKey(), cd, null);
				}
			}

			if (!parentScopeVisible) {
				break;
			}
		}

		return result;
	}

	public Tuple2<ObjectDefinition, StateFunc> searchByName(String name) {
		if (scopes.isEmpty() && cte.isEmpty()) {
			return null;
		}
		List<String> namedotsplit = dotsplitter.splitToList(name);
		if (namedotsplit.size() == 2) {
			String tableName = namedotsplit.get(0);
			SelectStmtData ss = searchSubqueryOrCte(tableName);
			if (ss != null) {
				String columnName = namedotsplit.get(1);
				StateFunc inf = ss.getColumns().get(columnName);
				Preconditions.checkState(inf != null, "No column %s found in Subquery %s", columnName, tableName);
				return Tuple2.of(null, inf);
			} else {
				// if it's a table, leave it to globalObjectResolver
				return null;
			}

		} else if (namedotsplit.size() == 1) {
			Tuple3<String, ColumnDefinition, StateFunc> t = searchColumn(name);
			return Tuple2.of(t.getField1(), t.getField2());
		} else {
			// d.size>2
			// leave it to globalObjectResolver
			return null;
		}

	}

	public ObjectDefinition guessColumn(String name) {
		if (scopes.isEmpty()) {
			return null;
		}
		Scope s = scopes.peek();
		if (s.getSubqueries().size() == 0 && s.getLiveTables().size() == 1) {
			Entry<String, TableDefinition> e = s.getLiveTables().entrySet().iterator().next();
			ColumnDefinition cd = new ColumnDefinition();
			cd.setInfered(true);
			cd.setName(e.getKey());
			e.getValue().getColumns().put(e.getKey(), cd);
			return cd;
		} else {
			return null;
		}
	}

	private static class Scope {
		Object id;
		Map<String, SelectStmtData> subqueries;
		private Map<String, TableDefinition> liveTables;

		public Object getId() {
			return id;
		}

		public void setId(Object id) {
			this.id = id;
		}

		public Map<String, SelectStmtData> getSubqueries() {
			return subqueries;
		}

		public void setSubqueries(Map<String, SelectStmtData> subqueries) {
			this.subqueries = subqueries;
		}

		public Map<String, TableDefinition> getLiveTables() {
			return liveTables;
		}

		public void setLiveTables(Map<String, TableDefinition> liveTables) {
			this.liveTables = liveTables;
		}

	}
}
