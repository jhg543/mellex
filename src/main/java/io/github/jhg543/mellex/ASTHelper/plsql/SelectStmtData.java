package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;

public class SelectStmtData {
	private List<ResultColumn> columns;
	private Map<String, Integer> nameIndexMap; // index is 0 based

	public List<ResultColumn> getColumns() {
		return columns;
	}

	/**
	 * @param i
	 *            zero based order
	 * @return
	 */
	public StateFunc getColumnExprFunc(int i) {
		return columns.get(i).getExpr();
	}

	public StateFunc getColumnExprFunc(String name) {
		return getColumnExprFunc(nameIndexMap.get(name));
	}

	public Map<String, Integer> getNameIndexMap() {
		return nameIndexMap;
	}

	public SelectStmtData(List<ResultColumn> columns) {
		super();
		this.columns = columns;
		this.nameIndexMap = new HashMap<>();
		// SELECT A,A,A FROM X ---> names: A,A_1,A_2
		IntStream.range(0, columns.size()).forEach(i -> {
			String k = columns.get(i).getName();
			String l = k;
			int c = 0;
			while (this.nameIndexMap.get(l) != null) {
				c++;
				l = k + "_" + c;
			}
			this.nameIndexMap.put(l, i);
			if (c > 0) {
				columns.get(i).setName(l);
			}
		});

	}

}
