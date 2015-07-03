package io.github.jhg543.mellex.listeners;

import io.github.jhg543.mellex.ASTHelper.CreateTableStmt;
import io.github.jhg543.mellex.ASTHelper.GlobalSettings;
import io.github.jhg543.mellex.ASTHelper.InsertStmt;
import io.github.jhg543.mellex.ASTHelper.ObjectName;
import io.github.jhg543.mellex.ASTHelper.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.SubQuery;
import io.github.jhg543.mellex.antlrparser.DefaultSQLBaseListener;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Any_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Create_source_tableContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Create_table_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Create_view_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Insert_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Join_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Object_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Result_columnAsteriskContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Result_columnContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Result_columnTableAsteriskContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Select_coreContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Table_or_subqueryContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Table_or_subqueryTableContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Update_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Update_stmt_fromContext;
import io.github.jhg543.mellex.session.TableDependencySession;

import java.util.ArrayList;
import java.util.List;

public class TableDependencyListener extends DefaultSQLBaseListener {

	public TableDependencyListener(TableDependencySession session) {
		super();
		this.session = session;
	}

	private TableDependencySession session;
	@Override
	public void exitCreate_table_stmt(Create_table_stmtContext ctx) {
		CreateTableStmt stmt = new CreateTableStmt();
		ctx.stmt = stmt;
		ObjectName name = ctx.obj.objname;
		stmt.dbobj = name;
		session.putTable(stmt, ctx.isvolatile);

	}

	@Override
	public void exitCreate_source_table(Create_source_tableContext ctx) {
		if (ctx.obj != null) {
			session.consumeTable(ctx.obj.objname);
		}
	}

	@Override
	public void exitCreate_view_stmt(Create_view_stmtContext ctx) {
		CreateTableStmt stmt = new CreateTableStmt();
		ctx.stmt = stmt;
		stmt.dbobj = ctx.obj.objname;
		session.putTable(stmt, false);

	}

	@Override
	public void exitUpdate_stmt_from(Update_stmt_fromContext ctx) {
		List<SubQuery> tables = new ArrayList<>();
		ctx.tables = tables;
		for (Table_or_subqueryContext table : ctx.ts) {
			tables.add(table.q);
		}
	}

	@Override
	public void exitInsert_stmt(Insert_stmtContext ctx) {
		InsertStmt stmt = new InsertStmt();
		ctx.stmt = stmt;

		stmt.dbobj = ctx.obj.objname;
		session.consumeTable(ctx.obj.objname);

	}

	@Override
	public void exitUpdate_stmt(Update_stmtContext ctx) {
		if (ctx.tobj.objname.ns.size() == 1) {
			String n = ctx.tobj.objname.ns.get(0);
			boolean isalias = false;
			if (ctx.f != null) {
				for (SubQuery q : ctx.f.tables) {
					if (n.equals(q.getAlias())) {
						isalias = true;
						break;
					}
				}
			}
			if (!isalias) {
				session.consumeTable(ctx.tobj.objname);
			}
		} else {
			session.consumeTable(ctx.tobj.objname);
		}
	}

	@Override
	public void exitObject_name(Object_nameContext ctx) {
		ObjectName name = new ObjectName();
		for (Any_nameContext s : ctx.d) {
			name.ns.add(s.getText());
		}
		ctx.objname = name;
	}

	@Override
	public void exitResult_columnAsterisk(Result_columnAsteriskContext ctx) {
		ResultColumn rc = new ResultColumn();
		rc.name = "*";
		ctx.rc = rc;
	}

	@Override
	public void exitResult_columnTableAsterisk(Result_columnTableAsteriskContext ctx) {
		ResultColumn rc = new ResultColumn();
		rc.name = ctx.tn.getText() + ".*";
		rc.isObjectName = true;
		ctx.rc = rc;
	}

	@Override
	public void exitTable_or_subqueryTable(Table_or_subqueryTableContext ctx) {
		SubQuery q = new SubQuery();
		ctx.q = q;

		ObjectName o = new ObjectName();
		if (ctx.dn != null) {
			o.ns.add(ctx.dn.getText());
		}
		o.ns.add(ctx.tn.getText());
		q.dbobj = o;
		//q.copyRC(sessionRecorder.queryTable(o));
		if (ctx.ta != null) {
			q.setAlias(ctx.ta.getText());
		}
	}

	@Override
	public void exitJoin_clause(Join_clauseContext ctx) {
		List<SubQuery> tables = new ArrayList<>();
		ctx.tables = tables;
		for (Table_or_subqueryContext table : ctx.ts) {
			if (table.q != null) {
				tables.add(table.q);
			}
		}

	}

	@Override
	public void exitSelect_core(Select_coreContext ctx) {
		SubQuery q = new SubQuery();
		List<SubQuery> tables;
		for (Result_columnContext rc : ctx.r) {
			if (rc.rc != null) {
				q.columns.add(rc.rc);
			}
		}

		// join
		if (ctx.jc != null) {
			tables = ctx.jc.tables;
		} else {
			tables = new ArrayList<SubQuery>();
		}

		for (ResultColumn c : q.columns) {
			if (c.name.contains("*")) {
				if ("*".equals(c.name)) {
					// select *
					for (SubQuery table : tables) {
						// no need to check dbobj is null ( "real" subquery wont have SubQuery object created )
						session.mustConsumeTable(table.dbobj);
					}
				} else if (c.isObjectName) {
					// select p1.*
					String tablename = c.name.substring(0, c.name.lastIndexOf('.'));
					for (SubQuery table : tables) {
						if (tablename.equals(table.getAlias())) {
							session.mustConsumeTable(table.dbobj);
						}
					}
				}
			}

		}
	}

}
