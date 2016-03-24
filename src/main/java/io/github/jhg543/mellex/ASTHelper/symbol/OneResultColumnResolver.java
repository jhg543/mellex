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
import io.github.jhg543.mellex.ASTHelper.plsql.SelectStmtData;
import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;
import io.github.jhg543.mellex.util.tuple.Tuple2;

public class OneResultColumnResolver {

	private Map<String, ObjectDefinition> aliases;
	private Set<String> currentColumnDependencies;
	private String currentalias;

	/**
	 * @param alias
	 *            pass null if no alias
	 */
	public void newResultColumn(String alias) {
		if (alias != null) {
			ObjectDefinition d = new ObjectDefinition();
			d.setName(alias);
			aliases.put(alias, d);
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
		return result.getColumn(name);
	}

	public Tuple2<ObjectDefinition, StateFunc> searchByName(String name) {
		if (result == null) {
			return Tuple2.of(searchNameTemp(name), null);
		} else {
			return Tuple2.of(null, searchNameAfterRewrite(name));
		}
	}

	private List<Set<String>> outedges;
	private SelectStmtData result;

	public SelectStmtData rewriteStateFunc(SelectStmtData tempResult) {
		Map<String, Integer> colNameToNumber = new HashMap<>();
		List<String> colNameList = tempResult.getColumnOrder();
		IntStream.range(0, colNameList.size()).forEach(i -> colNameToNumber.put(colNameList.get(i), i));

		List<Set<Integer>> outEdgeNumber = outedges.stream()
				.map(s -> s.stream().map(x -> colNameToNumber.get(x)).collect(Collectors.toSet())).collect(Collectors.toList());
		List<StateFunc> newres = new ArrayList<>();
		IntStream.range(0, colNameList.size()).forEach(i -> newres.add(tempResult.getColumn(i)));

		Solver solver = new Solver(outEdgeNumber, newres, i -> aliases.get(colNameList.get(i)));
		solver.solve();

		ImmutableMap.Builder<String, StateFunc> b = ImmutableMap.builder();
		IntStream.range(0, colNameList.size()).forEach(i -> b.put(colNameList.get(i), newres.get(i)));

		result = new SelectStmtData(b.build(), ImmutableList.copyOf(colNameList));
		return result;
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
