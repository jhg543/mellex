package io.github.jhg543.mellex.listeners;

import io.github.jhg543.mellex.ASTHelper.CreateTableStmt;
import io.github.jhg543.mellex.ASTHelper.InsertStmt;
import io.github.jhg543.mellex.ASTHelper.ObjectName;
import io.github.jhg543.mellex.ASTHelper.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.SubQuery;
import io.github.jhg543.mellex.ASTHelper.UpdateStmt;
import io.github.jhg543.mellex.antlrparser.DefaultSQLBaseListener;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Any_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Create_source_tableContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Create_table_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Create_view_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Insert_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Object_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Sql_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Table_or_subqueryContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Table_or_subqueryTableContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Update_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Update_stmt_fromContext;
import io.github.jhg543.mellex.inputsource.TableDefinitionProvider;
import io.github.jhg543.mellex.session.OutputGraphSession;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.TokenStream;

public class TableDataFlowListener extends DefaultSQLBaseListener {

	private OutputGraphSession os;
	private TableDefinitionProvider provider;
	private TokenStream stream;
	private String current_sql;
	private List<ObjectName> p1 = new ArrayList<ObjectName>();
	private List<ObjectName> vt;

	@Override
	public void enterSql_stmt(Sql_stmtContext ctx) {
		super.enterSql_stmt(ctx);
		current_sql = stream.getText(ctx.getSourceInterval());
		p1.clear();
	}

	public TableDataFlowListener(TableDefinitionProvider provider, TokenStream stream, OutputGraphSession os) {
		super();
		this.provider = provider;
		this.stream = stream;
		this.os = os;
	}

	@SuppressWarnings("unchecked")
	private <T extends RuleContext> T getInvokingRule(RuleContext ctx, Class<T> clz) {
		RuleContext context = ctx;
		while (!(clz.isInstance(context))) {
			context = context.parent;
			// throwing NPE here = root reached it should not happen
		}
		return (T) context;

	}

	@Override
	public void exitCreate_table_stmt(Create_table_stmtContext ctx) {
		CreateTableStmt stmt = new CreateTableStmt();
		ctx.stmt = stmt;
		ObjectName name = ctx.obj.objname;
		stmt.dbobj = name;

		ObjectName n = new ObjectName();
		n.ns.addAll(stmt.dbobj.ns);
		n.ns.add("A");

		ResultColumn c = new ResultColumn();
		c.name = "A";
		c.inf.direct.addAll(p1);
		c.inf.unique();
		c.inf.direct.remove(n);
		stmt.columns.add(c);

		stmt.setVolatile(ctx.isvolatile);
		if (ctx.isvolatile)
		{
			os.putVolatileTable(stmt.dbobj.toDotString());
		}

		os.addFlow(stmt);
	}

	@Override
	public void exitCreate_source_table(Create_source_tableContext ctx) {
		if (ctx.obj != null) {
			ObjectName n = new ObjectName();
			n.ns.addAll(ctx.obj.objname.ns);
			n.ns.add("A");
			p1.add(n);
		}
	}

	@Override
	public void exitCreate_view_stmt(Create_view_stmtContext ctx) {
		CreateTableStmt stmt = new CreateTableStmt();
		ctx.stmt = stmt;
		stmt.dbobj = ctx.obj.objname;

		ObjectName n = new ObjectName();
		n.ns.addAll(stmt.dbobj.ns);
		n.ns.add("A");

		ResultColumn c = new ResultColumn();
		c.name = "A";
		c.inf.direct.addAll(p1);
		c.inf.unique();
		c.inf.direct.remove(n);
		stmt.columns.add(c);


		os.addFlow(stmt);
	}

	@Override
	public void exitInsert_stmt(Insert_stmtContext ctx) {
		InsertStmt stmt = new InsertStmt();
		ctx.stmt = stmt;

		stmt.dbobj = ctx.obj.objname;

		ObjectName n = new ObjectName();
		n.ns.addAll(stmt.dbobj.ns);
		n.ns.add("A");

		ResultColumn c = new ResultColumn();
		c.name = "A";
		c.inf.direct.addAll(p1);
		c.inf.unique();
		c.inf.direct.remove(n);
		stmt.columns.add(c);

		os.addFlow(stmt);
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
	public void exitUpdate_stmt(Update_stmtContext ctx) {
		UpdateStmt q = new UpdateStmt();
		ctx.q = q;
		List<SubQuery> tables = new ArrayList<>();
		SubQuery targetTable = new SubQuery();

		if (ctx.tobj.objname.ns.size() == 1 && ctx.f != null) {
			String n = ctx.tobj.objname.ns.get(0);
			for (SubQuery query : ctx.f.tables) {
				if (n.equals(query.getAlias())) {
					if (query.dbobj == null) {
						throw new RuntimeException("update target is subquery " + n);
					}
					targetTable.dbobj = query.dbobj;
					break;
				}
			}
		}

		if (targetTable.dbobj == null) {
			targetTable.dbobj = ctx.tobj.objname;
		}

		q.dbobj = targetTable.dbobj;

		ObjectName n = new ObjectName();
		n.ns.addAll(q.dbobj.ns);
		n.ns.add("A");

		ResultColumn c = new ResultColumn();
		c.name = "A";
		c.inf.direct.addAll(p1);
		c.inf.unique();
		c.inf.direct.remove(n);

		q.columns.add(c);

		os.addFlow(q);

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
	public void exitTable_or_subqueryTable(Table_or_subqueryTableContext ctx) {
		SubQuery q = new SubQuery();
		ctx.q = q;

		ObjectName o = new ObjectName();
		if (ctx.dn != null) {
			o.ns.add(ctx.dn.getText());
		}
		o.ns.add(ctx.tn.getText());
		q.dbobj = o;

		ObjectName n = new ObjectName();
		n.ns.addAll(o.ns);
		n.ns.add("A");
		p1.add(n);

		if (ctx.ta != null) {
			q.setAlias(ctx.ta.getText());
		}
	}

}
