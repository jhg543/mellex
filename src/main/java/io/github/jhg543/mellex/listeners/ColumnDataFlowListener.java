package io.github.jhg543.mellex.listeners;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.TokenStream;

import io.github.jhg543.mellex.ASTHelper.CreateTableStmt;
import io.github.jhg543.mellex.ASTHelper.GlobalSettings;
import io.github.jhg543.mellex.ASTHelper.Influences;
import io.github.jhg543.mellex.ASTHelper.InsertStmt;
import io.github.jhg543.mellex.ASTHelper.ObjectName;
import io.github.jhg543.mellex.ASTHelper.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.SubQuery;
import io.github.jhg543.mellex.ASTHelper.UpdateStmt;
import io.github.jhg543.mellex.antlrparser.DefaultSQLBaseListener;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Any_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Column_defContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Column_defsContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Common_table_expressionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Create_source_tableContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Create_table_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Create_view_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Expr1Context;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Expr2Context;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Expr2poiContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.ExprBetweenContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.ExprCaseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.ExprContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.ExprExistsContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.ExprFunctionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.ExprLiteralContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.ExprObjectContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.ExprSpecialFunctionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Grouping_by_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Having_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Insert_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Join_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Object_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Order_by_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Ordering_term_windowContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Qualify_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Result_columnAsteriskContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Result_columnContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Result_columnExprContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Result_columnTableAsteriskContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Select_coreContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Select_or_valuesSelectCoreContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Select_or_valuesSelectValueContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Select_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Special_function1Context;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Special_functionDateTimeContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Special_functionSubStringContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Sql_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Table_or_subqueryContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Table_or_subquerySubQueryContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Table_or_subqueryTableContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Update_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Update_stmt_fromContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Update_stmt_setContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Where_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.WindowContext;
import io.github.jhg543.mellex.inputsource.TableDefinitionProvider;
import io.github.jhg543.mellex.util.Misc;

public class ColumnDataFlowListener extends DefaultSQLBaseListener {

	private TableDefinitionProvider provider;
	private TokenStream stream;
	private String current_sql;

	@Override
	public void enterSql_stmt(Sql_stmtContext ctx) {

		super.enterSql_stmt(ctx);
		current_sql = stream.getText(ctx.getSourceInterval());
	}

	public ColumnDataFlowListener(TableDefinitionProvider provider, TokenStream stream) {
		super();
		this.provider = provider;
		this.stream = stream;
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
		if (ctx.st != null) {
			// has source table
			SubQuery sourceTable = ctx.st.q;
			if (ctx.def != null) {
				for (String colname : ctx.def.colnames) {
					ObjectName cn = new ObjectName();
					cn.ns.addAll(name.ns);
					cn.ns.add(colname);
					ResultColumn c = new ResultColumn();
					c.name = colname;
					c.inf.addInResultExpression(cn);
					stmt.columns.add(c);
				}

			} else {
				for (ResultColumn oc : sourceTable.columns) {
					String colname = oc.name;
					ObjectName cn = new ObjectName();
					cn.ns.addAll(name.ns);
					cn.ns.add(colname);
					ResultColumn c = new ResultColumn();
					c.name = colname;
					c.inf.addInResultExpression(cn);
					stmt.columns.add(c);
				}
			}

			if (!(ctx.st.nodata)) {
			// will copy source table data	
				InsertStmt ins = new InsertStmt();
				ins.copyResultColumnNames(stmt);
				for (int i = 0; i < ins.columns.size(); ++i) {
					ResultColumn c = ins.columns.get(i);
					
					if (sourceTable.columns == null) {
						ObjectName n = new ObjectName();
						n.ns.addAll(sourceTable.dbobj.ns);
						n.ns.add(c.name);
						c.inf.addInResultExpression(n);
					} else {
						// ResultColumn tc = q.searchcol(c.name); WTF TD 532
						ResultColumn tc = sourceTable.columns.get(i);
						if (tc == null) {
							throw new RuntimeException("col not found " + c.name);
						}
						c.inf.addAll(tc.inf);
					}
				}
				ins.dbobj = stmt.dbobj;
				ctx.insert = ins;
			}
		} else {
			// col def only
			for (String colname : ctx.def.colnames) {
				ObjectName cn = new ObjectName();
				cn.ns.addAll(name.ns);
				cn.ns.add(colname);
				ResultColumn c = new ResultColumn();
				c.name = colname;
				c.inf.addInResultExpression(cn);
				stmt.columns.add(c);
			}
		}
		if (Misc.isvolatile(stmt.dbobj)) {
			ctx.isvolatile = true;
		}
		stmt.setVolatile(ctx.isvolatile);

		provider.putTable(stmt, ctx.isvolatile);

	}

	@Override
	public void exitColumn_def(Column_defContext ctx) {
		ctx.colname = ctx.cn.getText();
		if (!GlobalSettings.isCaseSensitive()) {
			ctx.colname = ctx.colname.toUpperCase();
		}
	}

	@Override
	public void exitColumn_defs(Column_defsContext ctx) {
		List<String> colnames = new ArrayList<>();
		ctx.colnames = colnames;
		for (Column_defContext cd : ctx.cd) {
			colnames.add(cd.colname);
		}
	}

	@Override
	public void exitCreate_source_table(Create_source_tableContext ctx) {
		if (ctx.obj != null) {
			ctx.q = provider.queryTable(ctx.obj.objname);
		} else {
			ctx.q = new SubQuery();
			ctx.q.copyRC(ctx.ss.q);
		}
	}

	@Override
	public void exitCreate_view_stmt(Create_view_stmtContext ctx) {
		CreateTableStmt stmt = new CreateTableStmt();
		ctx.stmt = stmt;

		stmt.dbobj = ctx.obj.objname;

		SubQuery q = ctx.ss.q;

		if (q.columns.size() != ctx.cn.size() && ctx.cn.size() > 0) {
			throw new RuntimeException("column size mismatch " + ctx.obj.objname);
		}

		// write column names
		for (int i = 0; i < q.columns.size(); ++i) {
			String colname;
			if (ctx.cn.size() > 0) {
				colname = ctx.cn.get(i).getText();
			} else {
				colname = q.columns.get(i).name;
			}
			ObjectName coln = new ObjectName();
			coln.ns.addAll(ctx.obj.objname.ns);
			coln.ns.add(colname);
			ResultColumn c = new ResultColumn();
			c.name = colname;
			c.inf.addInResultExpression(coln);
			stmt.columns.add(c);
		}

		// mark data source
		InsertStmt ins = new InsertStmt();
		ins.copyResultColumnNames(stmt);
		for (int i = 0; i < ins.columns.size(); ++i) {
			ResultColumn c = ins.columns.get(i);
			c.inf.addAll(q.columns.get(i).inf);

		}
		ctx.insert = ins;
		ins.dbobj = stmt.dbobj;
		stmt.setViewDef(ins);

		provider.putTable(stmt, false);

	}

	@Override
	public void exitInsert_stmt(Insert_stmtContext ctx) {
		InsertStmt stmt = new InsertStmt();
		ctx.stmt = stmt;

		stmt.dbobj = ctx.obj.objname;
		// GlobalMeta.consumeTable(ctx.obj.objname);
		SubQuery q = null;
		if (ctx.ss != null) {
			q = ctx.ss.q;
		}

		List<String> colnames = new ArrayList<>(ctx.cn.size());
		for (int i = 0; i < ctx.cn.size(); ++i) {
			if (GlobalSettings.isCaseSensitive()) {
				colnames.add(ctx.cn.get(i).getText());
			} else {
				colnames.add(ctx.cn.get(i).getText().toUpperCase());
			}
		}
		
		CreateTableStmt targetTable = null;
		if (colnames.size()==0){
			targetTable = provider.queryTable(stmt.dbobj);
		}
		List<Influences> exprs = new ArrayList<>(ctx.ex.size());
		for (int i = 0; i < ctx.ex.size(); ++i) {
			exprs.add(ctx.ex.get(i).inf);
		}
		stmt.fromSubQuery(colnames, targetTable, q, exprs, ctx.obj.objname);

	}

	@Override
	public void exitSelect_stmt(Select_stmtContext ctx) {

		List<SubQuery> queries = new ArrayList<>();
		for (int i = 0; i < ctx.sv.size(); ++i) {
			queries.add(ctx.sv.get(i).q);
		}

		ctx.q.compound(queries);

		Sql_stmtContext stmt = getInvokingRule(ctx, Sql_stmtContext.class);

		for (int i = 0; i < ctx.c.size(); ++i) {
			stmt.cte_stack.pop();
		}

	}

	@Override
	public void exitSelect_or_valuesSelectCore(Select_or_valuesSelectCoreContext ctx) {
		ctx.q = ctx.sc.q;
	}

	@Override
	public void exitSelect_or_valuesSelectValue(Select_or_valuesSelectValueContext ctx) {
		ctx.q = ctx.ss.q;
	}

	@Override
	public void exitUpdate_stmt(Update_stmtContext ctx) {
		UpdateStmt q = new UpdateStmt();
		ctx.q = q;
		List<SubQuery> tables = new ArrayList<>();
		SubQuery targetTable = new SubQuery();

		// deal with "UPDATE A1 from REAL_TABLE_NAME a1,SOME_TABLE NAME a2, rewrite targettable.dbobj from A1 to REAL_TABLE_NAME
		if (ctx.tobj.objname.ns.size() == 1 && ctx.f != null) {
			String n = ctx.tobj.objname.ns.get(0);
			for (SubQuery query : ctx.f.tables) {
				if (n.equals(query.getAlias())) {
					if (query.dbobj == null) {
						throw new RuntimeException("update target is subquery " + n);
					}
					targetTable.dbobj = query.dbobj;
					
				}
				else
				{
					tables.add(query);
				}
			}
		}

		if (targetTable.dbobj == null) {
			targetTable.dbobj = ctx.tobj.objname;
		}
		targetTable.copyRC(provider.queryTable(targetTable.dbobj));

		if (ctx.ta != null) {
			targetTable.setAlias(ctx.ta.getText());
		}

		if (ctx.wex != null) {
			q.ci.addAllInClause(ctx.wex.inf);
		}

		q.columns = ctx.s.columns;
		List<SubQuery> temp = new ArrayList<>();
		temp.add(targetTable);
		q.resolvenames(tables, new ArrayList<Integer>(), temp);
		q.dbobj = targetTable.dbobj;
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
	public void exitUpdate_stmt_set(Update_stmt_setContext ctx) {
		List<ResultColumn> columns = new ArrayList<>();
		ctx.columns = columns;
		for (int i = 0; i < ctx.cn.size(); ++i) {
			ResultColumn c = new ResultColumn();
			c.name = ctx.cn.get(i).getText();
			c.inf.addAll(ctx.ex.get(i).inf);
			columns.add(c);
		}
	}

	@Override
	public void exitExprCase(ExprCaseContext ctx) {
		Influences cinf = new Influences();
		for (ExprContext param : ctx.ex) {
			cinf.addAllInClause(param.inf);
		}
		for (ExprContext param : ctx.ax) {
			cinf.addAll(param.inf);
		}
		ctx.inf = cinf;
	}

	@Override
	public void exitExprFunction(ExprFunctionContext ctx) {
		Influences cinf = new Influences();
		for (ExprContext param : ctx.ex) {
			cinf.addAll(param.inf);
		}
		if (ctx.wx != null) {
			cinf.addAllInClause(ctx.wx.inf);
		}
		ctx.inf = cinf;
	}

	@Override
	public void exitExprExists(ExprExistsContext ctx) {
		Influences cinf = new Influences();
		if (ctx.isexists != null) {
			cinf.copySelectStmtAsClause(ctx.ss.q);
		} else {
			cinf.copySelectScalar(ctx.ss.q);
		}
		ctx.inf = cinf;
	}

	@Override
	public void exitExprSpecialFunction(ExprSpecialFunctionContext ctx) {
		Influences cinf = new Influences();
		cinf.addAll(ctx.sp.inf);
		ctx.inf = cinf;
	}

	@Override
	public void exitExprObject(ExprObjectContext ctx) {
		Influences cinf = new Influences();
		cinf.addInResultExpression(ctx.obj.objname);
		ctx.objname = ctx.obj.objname;
		ctx.inf = cinf;
	}

	@Override
	public void exitExpr2(Expr2Context ctx) {
		Influences cinf = new Influences();
		cinf.addAll(ctx.operand1.inf);
		cinf.addAll(ctx.operand2.inf);
		ctx.inf = cinf;
	}

	@Override
	public void exitExpr1(Expr1Context ctx) {
		Influences cinf = new Influences();
		ctx.inf = cinf;
		cinf.addAll(ctx.operand1.inf);

	}

	@Override
	public void exitExprLiteral(ExprLiteralContext ctx) {
		Influences cinf = new Influences();
		ctx.inf = cinf;
	}

	@Override
	public void exitExpr2poi(Expr2poiContext ctx) {
		Influences cinf = new Influences();
		cinf.addAllInClause(ctx.operand1.inf);
		cinf.addAllInClause(ctx.operand2.inf);
		ctx.inf = cinf;
	}

	@Override
	public void exitExprBetween(ExprBetweenContext ctx) {
		Influences cinf = new Influences();
		cinf.addAllInClause(ctx.operand1.inf);
		cinf.addAllInClause(ctx.operand2.inf);
		cinf.addAllInClause(ctx.operand3.inf);
		ctx.inf = cinf;
	}

	@Override
	public void exitSpecial_function1(Special_function1Context ctx) {
		Influences cinf = new Influences();
		cinf.addAll(ctx.operand1.inf);
		ctx.inf = cinf;
	}

	@Override
	public void exitSpecial_functionSubString(Special_functionSubStringContext ctx) {
		Influences cinf = new Influences();
		cinf.addAll(ctx.operand1.inf);
		cinf.addAllInClause(ctx.operand2.inf);
		if (ctx.operand3 != null) {
			cinf.addAllInClause(ctx.operand3.inf);
		}
		ctx.inf = cinf;
	}

	@Override
	public void exitSpecial_functionDateTime(Special_functionDateTimeContext ctx) {
		Influences cinf = new Influences();
		ctx.inf = cinf;
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
	public void exitOrdering_term_window(Ordering_term_windowContext ctx) {
		Influences cinf = new Influences();
		cinf.addAllInClause(ctx.operand1.inf);
		ctx.inf = cinf;
	}

	@Override
	public void exitCommon_table_expression(Common_table_expressionContext ctx) {
		ctx.q = new SubQuery();
		ctx.q.copyRC(ctx.ss.q);
		if (ctx.tn != null) {
			ctx.q.setAlias(ctx.tn.getText());
		}
		if (ctx.cn.size() > 0) {
			if (ctx.cn.size() != ctx.q.columns.size()) {
				throw new RuntimeException("subquery column number mismatch, line" + ctx.ss.start.getLine());
			}

			for (int i = 0; i < ctx.cn.size(); ++i) {
				ctx.q.columns.get(i).name = ctx.cn.get(i).getText();
			}
		}
		Sql_stmtContext stmt = getInvokingRule(ctx, Sql_stmtContext.class);
		stmt.cte_stack.push(ctx.q);
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
		rc.setObjectName(true);
		ctx.rc = rc;
	}

	@Override
	public void exitResult_columnExpr(Result_columnExprContext ctx) {
		ResultColumn rc = new ResultColumn();
		rc.inf.addAll(ctx.ex.inf);
		if (ctx.ex.objname != null) {
			rc.setObjectName(true);
		}
		if (ctx.ca != null) {
			rc.name = ctx.ca.getText();
			rc.hasAlias = true;
		} else {

			rc.name = ctx.ex.getText();

		}
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
		q.copyRC(provider.queryTable(o));
		if (ctx.ta != null) {
			q.setAlias(ctx.ta.getText());
		}
	}

	@Override
	public void exitTable_or_subquerySubQuery(Table_or_subquerySubQueryContext ctx) {
		SubQuery q = new SubQuery();
		ctx.q = q;

		q.copyRC(ctx.ss.q);
		if (ctx.ta != null) {
			q.setAlias(ctx.ta.getText());
		}
		if (ctx.cn.size() > 0) {
			if (ctx.cn.size() != q.columns.size()) {
				throw new RuntimeException("subquery column number mismatch,line" + ctx.ss.start.getLine());
			}

			for (int i = 0; i < ctx.cn.size(); ++i) {

				if (GlobalSettings.isCaseSensitive()) {
					q.columns.get(i).name = ctx.cn.get(i).getText();
				} else {
					q.columns.get(i).name = ctx.cn.get(i).getText().toUpperCase();
				}
			}
		}
	}

	@Override
	public void exitJoin_clause(Join_clauseContext ctx) {
		List<SubQuery> tables = new ArrayList<>();
		Influences join_constraints = new Influences();
		ctx.tables = tables;
		ctx.join_constraints = join_constraints;

		for (Table_or_subqueryContext table : ctx.ts) {
			tables.add(table.q);
		}

		for (ExprContext expr : ctx.ex) {
			join_constraints.addAllInClause(expr.inf);
		}

	}

	@Override
	public void exitSelect_core(Select_coreContext ctx) {
		SubQuery q = new SubQuery();
		ctx.q = q;
		List<SubQuery> tables;
		List<Integer> groupbypositions = new ArrayList<>();
		for (Result_columnContext rc : ctx.r) {
			q.columns.add(rc.rc);
		}

		// join
		if (ctx.jc != null) {
			q.ci.addAllInClause(ctx.jc.join_constraints);
			tables = ctx.jc.tables;
		} else {
			tables = new ArrayList<SubQuery>();
		}

		// group by
		if (ctx.g1 != null) {
			q.ci.addAllInClause(ctx.g1.inf);
			groupbypositions.addAll(ctx.g1.positions);
		}

		if (ctx.g2 != null) {
			q.ci.addAllInClause(ctx.g2.inf);
			groupbypositions.addAll(ctx.g2.positions);
		}

		// where
		if (ctx.w1 != null) {
			q.ci.addAllInClause(ctx.w1.inf);
		}

		// having
		if (ctx.h1 != null) {
			q.ci.addAllInClause(ctx.h1.inf);
		}

		// qualify
		if (ctx.q1 != null) {
			q.ci.addAllInClause(ctx.q1.inf);
		}

		// order by
		if (ctx.o1 != null) {
			q.ci.addAllInClause(ctx.o1.inf);
			groupbypositions.addAll(ctx.o1.positions);
		}

		Sql_stmtContext stmt = getInvokingRule(ctx, Sql_stmtContext.class);
		q.resolvenames(tables, groupbypositions, stmt.cte_stack);

	}

	@Override
	public void exitOrder_by_clause(Order_by_clauseContext ctx) {
		Influences inf = new Influences();
		List<Integer> positions = new ArrayList<>();
		ctx.inf = inf;
		ctx.positions = positions;
		for (int i = 0; i < ctx.ex.size(); ++i) {
			inf.addAllInClause(ctx.ex.get(i).inf);
		}
		for (int i = 0; i < ctx.nx.size(); ++i) {
			positions.add(Integer.valueOf(ctx.nx.get(i).getText()));
		}
	}

	@Override
	public void exitWhere_clause(Where_clauseContext ctx) {
		Influences inf = new Influences();
		ctx.inf = inf;
		inf.addAllInClause(ctx.ex.inf);
	}

	@Override
	public void exitGrouping_by_clause(Grouping_by_clauseContext ctx) {
		Influences inf = new Influences();
		List<Integer> positions = new ArrayList<>();
		ctx.inf = inf;
		ctx.positions = positions;
		for (int i = 0; i < ctx.ex.size(); ++i) {
			inf.addAllInClause(ctx.ex.get(i).inf);
		}
		for (int i = 0; i < ctx.nx.size(); ++i) {
			positions.add(Integer.valueOf(ctx.nx.get(i).getText()));
		}
	}

	@Override
	public void exitHaving_clause(Having_clauseContext ctx) {
		Influences inf = new Influences();
		ctx.inf = inf;
		inf.addAllInClause(ctx.ex.inf);
	}

	@Override
	public void exitQualify_clause(Qualify_clauseContext ctx) {
		Influences inf = new Influences();
		ctx.inf = inf;
		inf.addAllInClause(ctx.ex.inf);
	}

	@Override
	public void exitWindow(WindowContext ctx) {
		Influences inf = new Influences();
		ctx.inf = inf;
		for (int i = 0; i < ctx.ex.size(); ++i) {
			inf.addAllInClause(ctx.ex.get(i).inf);
		}
		for (int i = 0; i < ctx.ox.size(); ++i) {
			inf.addAllInClause(ctx.ox.get(i).inf);
		}

	}

}
