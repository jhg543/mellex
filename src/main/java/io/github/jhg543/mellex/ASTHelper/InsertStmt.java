package io.github.jhg543.mellex.ASTHelper;

import java.util.List;

public class InsertStmt extends SubQuery {
	public boolean isEmptyInf() {
		for (ResultColumn c : columns) {
			if (!c.inf.isempty()) {
				return false;
			}
		}
		return true;
	}

	public void fromSubQuery(List<String> colnames, CreateTableStmt targetTable, SubQuery q, List<Influences> exprs,
			ObjectName tablename) {

		if (q != null) {
			this.copyRC(q);
			if (colnames.size() > 0) {
				// from insert (col1,col2..)
				if (colnames.size() != columns.size()) {
					throw new RuntimeException("insert column count mismatch " + columns + colnames);
				}
				for (int i = 0; i < colnames.size(); ++i) {
					columns.get(i).name = colnames.get(i);
				}
			} else {
				// copy from table meta
				if (targetTable.isInitialized()) {
					if (targetTable.columns.size() != columns.size()) {
						throw new RuntimeException("insert column count mismatch " + columns + colnames);
					}
					for (int i = 0; i < targetTable.columns.size(); ++i) {
						columns.get(i).name = targetTable.columns.get(i).name;
					}					
				}
			}
		} else {
			if (colnames.size() > 0) {
				if (colnames.size() != exprs.size()) {
					throw new RuntimeException("insert column count mismatch " + colnames);
				}
				for (int i = 0; i < exprs.size(); ++i) {
					Influences inf = exprs.get(i);
					if (!inf.isempty()) {
						ResultColumn c = new ResultColumn();
						c.inf.addAll(inf);
						c.name = colnames.get(i);
						columns.add(c);
					}
				}
			} else {
				// TODO
				// throw new RuntimeException("insert into " + tablename +
				// "values needs metadata ");
				for (int i = 0; i < exprs.size(); ++i) {
					Influences inf = exprs.get(i);
					if (!inf.isempty()) {
						ResultColumn c = new ResultColumn();
						c.inf.addAll(inf);
						c.name = "NO METADATA " + (i + 1);
						c.position = i + 1;
						columns.add(c);
					}
				}
			}

		}
	}
}
