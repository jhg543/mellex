package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;

public class SelectStmtData {
	protected List<ResultColumn> columns;
	protected Map<String, Integer> nameIndexMap; // index is 0 based

	/**
	 * do not modify it.
	 * @return
	 */
	public List<ResultColumn> getColumns() {
		return Collections.unmodifiableList(columns);
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
	
	/**
	 * do not modify the result collection
	 * @return
	 */
	public Map<String, Integer> getNameIndexMap() {
		return Collections.unmodifiableMap(nameIndexMap);
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

	@Override
	public String toString() {
		String s = "SelectStmt [";
		for (ResultColumn rc:columns)
		{
			s+="\t";
			s+=rc.getName();
			s+=" = ";
			s+=rc.getExpr();
		}
		s+="]";		
		return s;
	}

}
