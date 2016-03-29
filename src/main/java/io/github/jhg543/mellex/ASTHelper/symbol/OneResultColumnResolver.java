package io.github.jhg543.mellex.ASTHelper.symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.plsql.SelectStmtData;
import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;
import io.github.jhg543.mellex.util.tuple.Tuple2;

public class OneResultColumnResolver {

	private Map<String, ObjectDefinition> aliases = new HashMap<>();
	private Set<String> currentColumnDependencies;
	private String currentalias;

	private List<Set<String>> outedges = new ArrayList<>();
	private SelectStmtData result;

	public void collectAlias(List<String> aliasList) {
		aliasList.forEach(s -> {
			ObjectDefinition def = new ObjectDefinition();
			def.setName(this.toString() + "_" + s);
			aliases.put(s, def);
		});
	}

	/**
	 * @param alias
	 *            pass null if no alias
	 */
	public void newResultColumn(String alias) {
		if (alias != null) {
			currentalias = alias;
		}
		currentColumnDependencies = new HashSet<>();
		outedges.add(currentColumnDependencies);
	}

	private ObjectDefinition searchNameTemp(String name) {
		if (name.equals(currentalias)) {
			return null;
		}

		ObjectDefinition d = aliases.get(name);
		if (d != null) {
			currentColumnDependencies.add(name);
		}
		return d;
	}

	private StateFunc searchNameAfterRewrite(String name) {
		return result.getColumnExprFunc(name);
	}

	public Tuple2<ObjectDefinition, StateFunc> searchByName(String name) {
		if (result == null) {
			ObjectDefinition def = searchNameTemp(name);
			if (def == null) {
				return null;
			}
			return Tuple2.of(def, null);
		} else {
			StateFunc func = searchNameAfterRewrite(name);
			if (func == null) {
				return null;
			}
			return Tuple2.of(null, func);
		}
	}

	public SelectStmtData rewriteStateFunc(SelectStmtData tempResult) {

		List<ResultColumn> columns = (List<ResultColumn>) tempResult.getColumns();
		List<Set<Integer>> outEdgeNumber = outedges.stream()
				.map(s -> s.stream().map(tempResult.getNameIndexMap()::get).collect(Collectors.toSet()))
				.collect(Collectors.toList());
		List<StateFunc> newres = columns.stream().map(rc -> rc.getExpr()).collect(Collectors.toList());
		Solver solver = new Solver(outEdgeNumber, newres, i -> aliases.get(columns.get(i).getName()));
		solver.solve();

		List<ResultColumn> newrc = new ArrayList<>();
		IntStream.range(0, columns.size()).forEach(
				i -> newrc.add(new ResultColumn(columns.get(i).getName(), columns.get(i).getPosition(), newres.get(i))));
		return new SelectStmtData(newrc);
	}

	/**
	 * Do topological sort to resolve
	 * "select c3+c3 as c4,c2+c2 as c3,c1+c1 as c2,realcolumn as c1 from sometable"
	 * 
	 */
	private static class Solver {
		// 0 = no 1 = in process 2 = finished
		private int[] resolveprocess;
		private List<Set<Integer>> out;
		private List<StateFunc> result;
		private Function<Integer, ObjectDefinition> t;

		public Solver(List<Set<Integer>> out, List<StateFunc> result, Function<Integer, ObjectDefinition> t) {
			super();
			this.out = out;
			this.result = result;
			this.t = t;
			Preconditions.checkArgument(out.size() == result.size(), "out and result size not equal");
			resolveprocess = new int[out.size()];
		}

		public void solve() {
			for (int i = 0; i < out.size(); ++i) {
				solve(i);
			}
		}

		private void solve(int i) {
			if (resolveprocess[i] == 2) {
				return;
			}
			Preconditions.checkState(resolveprocess[i] != 1, "circular column name reference %d", i);
			resolveprocess[i] = 1;

			for (Integer c : out.get(i)) {
				solve(c);
			}

			StateFunc s = result.get(i);
			Map<ObjectDefinition, StateFunc> applyParam = new HashMap<>();
			out.get(i).forEach(n -> applyParam.put(t.apply(n), result.get(n)));
			result.set(i, s.apply(applyParam));

			resolveprocess[i] = 2;
		}
	}
}
