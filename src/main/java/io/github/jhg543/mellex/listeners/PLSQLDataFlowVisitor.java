package io.github.jhg543.mellex.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.escape.Escapers;

import io.github.jhg543.mellex.ASTHelper.CreateTableStmt;
import io.github.jhg543.mellex.ASTHelper.GlobalSettings;
import io.github.jhg543.mellex.ASTHelper.Influences;
import io.github.jhg543.mellex.ASTHelper.InsertStmt;
import io.github.jhg543.mellex.ASTHelper.ObjectName;
import io.github.jhg543.mellex.ASTHelper.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.SubQuery;
import io.github.jhg543.mellex.ASTHelper.UpdateStmt;
import io.github.jhg543.mellex.ASTHelper.plsql.CursorDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ExprAnalyzeResult;
import io.github.jhg543.mellex.ASTHelper.plsql.FunctionDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ScopeStack;
import io.github.jhg543.mellex.ASTHelper.plsql.StateTransformDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableModification;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPBaseVisitor;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Any_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Case_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Column_defContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Column_defsContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Common_table_expressionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Create_procedureContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Create_source_tableContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Create_table_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Create_view_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Cursor_definitionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Declare_sectionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Declare_section_onelineContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Expr1Context;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Expr2Context;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Expr2poiContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.ExprBetweenContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.ExprCaseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.ExprContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.ExprExistsContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.ExprFunctionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.ExprLiteralContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.ExprORContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.ExprObjectContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.ExprSpecialFunctionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Grouping_by_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Having_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Insert_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Join_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.LabelContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Multiple_plsql_stmt_listContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Object_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Order_by_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Ordering_term_windowContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Parameter_declarationContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Parameter_declarationsContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Plsql_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Plsql_statement_nolabelContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Procedure_or_function_declarationContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Procedure_or_function_definitionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Qualify_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Result_columnAsteriskContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Result_columnContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Result_columnExprContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Result_columnTableAsteriskContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Select_coreContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Select_or_valuesSelectCoreContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Select_or_valuesSelectValueContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Select_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Special_function1Context;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Special_functionDateTimeContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Special_functionSubStringContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Sql_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Table_or_subqueryContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Table_or_subquerySubQueryContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Table_or_subqueryTableContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Update_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Update_stmt_fromContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Update_stmt_setContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Variable_declarationContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Where_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.WindowContext;
import io.github.jhg543.mellex.inputsource.TableDefinitionProvider;
import io.github.jhg543.mellex.listeners.flowmfp.Instruction;
import io.github.jhg543.mellex.listeners.flowmfp.PatchList;
import io.github.jhg543.mellex.listeners.flowmfp.VariableUsageState;
import io.github.jhg543.mellex.util.Misc;

@SuppressWarnings("unchecked")
public class PLSQLDataFlowVisitor extends DefaultSQLPBaseVisitor<Object> {

	private TableDefinitionProvider provider;
	private TokenStream stream;
	private String current_sql;
	private List<Instruction<VariableUsageState>> instbuffer;
	private Set<String> unmetFunctionNames;

	private ScopeStack scopeStack;

	private String getText(RuleContext ctx) {
		return stream.getText(ctx.getSourceInterval());
	}

	@Override
	public Object visitCreate_procedure(Create_procedureContext ctx) {
		/*
		 * parameter_declarations? (K_RETURN datatype)? ( K_IS | K_AS )
		 * declare_section? body
		 */

		FunctionDefinition functionDefinition = new FunctionDefinition();
		ctx.object_name().accept(this);
		functionDefinition.setName(ctx.object_name().getText());
		List<VariableDefinition> parameterDefinitions = (List<VariableDefinition>) ctx.parameter_declarations().accept(this);
		functionDefinition.setParameters(parameterDefinitions);

		// push new scope , all outer variable are not visible
		scopeStack.newBlock(ImmutableList.copyOf(parameterDefinitions), false);

		List<ObjectDefinition> objectDefinitions = Collections.EMPTY_LIST;
		if (ctx.declare_section() != null) {
			objectDefinitions = (List<ObjectDefinition>) ctx.declare_section().accept(this);
		}
		scopeStack.newBlock(objectDefinitions, true);

		// pop decl
		scopeStack.pop();

		// pop parameter~
		scopeStack.pop();
		return null;

	}

	@Override
	public PatchList visitCase_statement(Case_statementContext ctx) {
		PatchList p = new PatchList();
		if (ctx.selector != null && !ctx.selector.inf.isempty()) {
			Instruction<VariableUsageState> ins = new Instruction<VariableUsageState>();
			ins.setDebugInfo(ctx.getStart().getLine());
			Influences inf = ctx.selector.inf;
			ins.setFunc(state -> {
				VariableUsageState v = state.shallowCopy();

				return null;
			});
			p.setStartInstruction(ins);
		}

		return null;
	}

	@Override
	public PatchList visitMultiple_plsql_stmt_list(Multiple_plsql_stmt_listContext ctx) {
		PatchList patchList = new PatchList();
		patchList.setBreakList(new ArrayList<Instruction<VariableUsageState>>());
		patchList.setContinueList(new ArrayList<Instruction<VariableUsageState>>());

		PatchList prevpl = null;
		for (Plsql_statementContext plsqlctx : ctx.plsql_statement()) {
			PatchList currentpl = (PatchList) plsqlctx.accept(this);
			patchList.getBreakList().addAll(currentpl.getBreakList());
			patchList.getContinueList().addAll(currentpl.getContinueList());
			if (prevpl != null) {
				for (Instruction<VariableUsageState> ins : prevpl.getNextList()) {
					ins.getNextPc().add(() -> currentpl.getStartInstruction());
				}
			} else {
				patchList.setStartInstruction(currentpl.getStartInstruction());
			}
			prevpl = currentpl;
		}
		patchList.setNextList(prevpl.getNextList());
		if (patchList.getBreakList().isEmpty()) {
			patchList.setBreakList(Collections.EMPTY_LIST);
		}
		if (patchList.getContinueList().isEmpty()) {
			patchList.setContinueList(Collections.EMPTY_LIST);
		}
		return patchList;
	}

	@Override
	public PatchList visitPlsql_statement(Plsql_statementContext ctx) {
		PatchList patchList = (PatchList) ctx.plsql_statement_nolabel().accept(this);
		for (LabelContext labelctx : ctx.label()) {
			String labelName = (String) labelctx.label_name().getText();
			scopeStack.getLabels().put(labelName, patchList.getStartInstruction());
		}
		return patchList;
	}

	@Override
	public PatchList visitPlsql_statement_nolabel(Plsql_statement_nolabelContext ctx) {
		return (PatchList) visitChildren(ctx);
	}

	@Override
	public List<ObjectDefinition> visitDeclare_section(Declare_sectionContext ctx) {
		List<ObjectDefinition> decls = new ArrayList<ObjectDefinition>();
		for (Declare_section_onelineContext vd : ctx.declare_section_oneline()) {
			decls.add((ObjectDefinition) vd.accept(this));
		}
		return decls;
	}

	@Override
	public ObjectDefinition visitDeclare_section_oneline(Declare_section_onelineContext ctx) {
		return (ObjectDefinition) visitChildren(ctx);
	}

	@Override
	public Object visitProcedure_or_function_declaration(Procedure_or_function_declarationContext ctx) {
		throw new UnsupportedOperationException("FORWARD DECLARATION NOT SUPPORTED");
	}

	@Override
	public FunctionDefinition visitProcedure_or_function_definition(Procedure_or_function_definitionContext ctx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCursor_definition(Cursor_definitionContext ctx) {
		CursorDefinition cursorDefinition = new CursorDefinition();
		cursorDefinition.setName(ctx.any_name().getText());
		List<VariableDefinition> parameterDefinitions = Collections.EMPTY_LIST;
		if (ctx.parameter_declarations() != null) {
			parameterDefinitions = (List<VariableDefinition>) ctx.parameter_declarations().accept(this);

		}
		cursorDefinition.setParameters(parameterDefinitions);

		scopeStack.newBlock(ImmutableList.copyOf(parameterDefinitions), false);
		Select_stmtContext ssctx = ctx.select_stmt();
		scopeStack.pop();

		ssctx.accept(this);
		cursorDefinition.setSelectInf(ssctx.q);

		return cursorDefinition;
	}

	@Override
	public VariableDefinition visitVariable_declaration(Variable_declarationContext ctx) {
		// COPY FROM visitParameter_declaration

		VariableDefinition def = new VariableDefinition();
		def.setMods(new ArrayList<>(2));
		def.setName(ctx.any_name().getText());
		def.setConst(ctx.K_CONSTANT() != null);
		ExprContext exprContext = ctx.expr();
		if (exprContext != null) {
			exprContext.accept(this);
			VariableModification vm = new VariableModification();
			ResultColumn scalar = new ResultColumn();
			scalar.inf = exprContext.inf;
			vm.columns.add(scalar);
			def.setDefaultValue(vm);
		}
		return def;
	}

	@Override
	public List<VariableDefinition> visitParameter_declarations(Parameter_declarationsContext ctx) {
		List<VariableDefinition> defs = new ArrayList<VariableDefinition>();
		for (Parameter_declarationContext vd : ctx.parameter_declaration()) {
			defs.add((VariableDefinition) vd.accept(this));
		}
		return defs;
	}

	@Override
	public VariableDefinition visitParameter_declaration(Parameter_declarationContext ctx) {
		if (ctx.K_OUT() != null) {
			throw new UnsupportedOperationException("OUT PARAMETER NOT IMPLEMENTED");
		}
		VariableDefinition def = new VariableDefinition();
		def.setMods(new ArrayList<>(2));
		def.setName(ctx.any_name().getText());
		ExprContext exprContext = ctx.expr();
		if (exprContext != null) {
			exprContext.accept(this);
			VariableModification vm = new VariableModification();
			ResultColumn scalar = new ResultColumn();
			scalar.inf = exprContext.inf;
			vm.columns.add(scalar);
			def.setDefaultValue(vm);
		}
		return def;
	}

	@Override
	public Object visitSql_stmt(Sql_stmtContext ctx) {
		current_sql = stream.getText(ctx.getSourceInterval());
		visitChildren(ctx);
		return null;
	}

	public PLSQLDataFlowVisitor(TableDefinitionProvider provider, TokenStream stream) {
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
	public Object visitCreate_table_stmt(Create_table_stmtContext ctx) {
		visitChildren(ctx);
		exitCreate_table_stmt(ctx);
		return null;
	}

	private void exitCreate_table_stmt(Create_table_stmtContext ctx) {
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
	public Object visitColumn_def(Column_defContext ctx) {
		visitChildren(ctx);
		exitColumn_def(ctx);
		return null;
	}

	private void exitColumn_def(Column_defContext ctx) {
		ctx.colname = ctx.cn.getText();
		if (!GlobalSettings.isCaseSensitive()) {
			ctx.colname = ctx.colname.toUpperCase();
		}
	}

	@Override
	public Object visitColumn_defs(Column_defsContext ctx) {
		visitChildren(ctx);
		exitColumn_defs(ctx);
		return null;
	}

	private void exitColumn_defs(Column_defsContext ctx) {
		List<String> colnames = new ArrayList<>();
		ctx.colnames = colnames;
		for (Column_defContext cd : ctx.cd) {
			colnames.add(cd.colname);
		}
	}

	@Override
	public Object visitCreate_source_table(Create_source_tableContext ctx) {
		visitChildren(ctx);
		exitCreate_source_table(ctx);
		return null;
	}

	private void exitCreate_source_table(Create_source_tableContext ctx) {
		if (ctx.obj != null) {
			ctx.q = provider.queryTable(ctx.obj.objname);
		} else {
			ctx.q = new SubQuery();
			ctx.q.copyRC(ctx.ss.q);
		}
	}

	@Override
	public Object visitCreate_view_stmt(Create_view_stmtContext ctx) {
		visitChildren(ctx);
		exitCreate_view_stmt(ctx);
		return null;
	}

	private void exitCreate_view_stmt(Create_view_stmtContext ctx) {
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

	private void exitInsert_stmt(Insert_stmtContext ctx) {
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
		if (colnames.size() == 0) {
			targetTable = provider.queryTable(stmt.dbobj);
		}
		List<Influences> exprs = new ArrayList<>(ctx.ex.size());
		for (int i = 0; i < ctx.ex.size(); ++i) {
			exprs.add(ctx.ex.get(i).inf);
		}
		stmt.fromSubQuery(colnames, targetTable, q, exprs, ctx.obj.objname);

	}

	private void exitSelect_stmt(Select_stmtContext ctx) {

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

	private void exitSelect_or_valuesSelectCore(Select_or_valuesSelectCoreContext ctx) {
		ctx.q = ctx.sc.q;
	}

	private void exitSelect_or_valuesSelectValue(Select_or_valuesSelectValueContext ctx) {
		ctx.q = ctx.ss.q;
	}

	private void exitUpdate_stmt(Update_stmtContext ctx) {
		UpdateStmt q = new UpdateStmt();
		ctx.q = q;
		List<SubQuery> tables = new ArrayList<>();
		SubQuery targetTable = new SubQuery();

		// deal with "UPDATE A1 from REAL_TABLE_NAME a1,SOME_TABLE NAME a2,
		// rewrite targettable.dbobj from A1 to REAL_TABLE_NAME
		if (ctx.tobj.objname.ns.size() == 1 && ctx.f != null) {
			String n = ctx.tobj.objname.ns.get(0);
			for (SubQuery query : ctx.f.tables) {
				if (n.equals(query.getAlias())) {
					if (query.dbobj == null) {
						throw new RuntimeException("update target is subquery " + n);
					}
					targetTable.dbobj = query.dbobj;

				} else {
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

	private void exitUpdate_stmt_from(Update_stmt_fromContext ctx) {
		List<SubQuery> tables = new ArrayList<>();
		ctx.tables = tables;
		for (Table_or_subqueryContext table : ctx.ts) {
			tables.add(table.q);
		}
	}

	private void exitUpdate_stmt_set(Update_stmt_setContext ctx) {
		List<ResultColumn> columns = new ArrayList<>();
		ctx.columns = columns;
		for (int i = 0; i < ctx.cn.size(); ++i) {
			ResultColumn c = new ResultColumn();
			c.name = ctx.cn.get(i).getText();
			c.inf.addAll(ctx.ex.get(i).inf);
			columns.add(c);
		}
	}

	private void exitExprSpecialFunction(ExprSpecialFunctionContext ctx) {
		Influences cinf = new Influences();
		cinf.addAll(ctx.sp.inf);
		ctx.inf = cinf;
	}

	private void exitExprObject(ExprObjectContext ctx) {
		Influences cinf = new Influences();
		cinf.addInResultExpression(ctx.obj.objname);
		ctx.objname = ctx.obj.objname;
		ctx.inf = cinf;
	}

	private void exitExprLiteral(ExprLiteralContext ctx) {
		Influences cinf = new Influences();
		ctx.inf = cinf;
	}

	private void exitSpecial_function1(Special_function1Context ctx) {
		Influences cinf = new Influences();
		cinf.addAll(ctx.operand1.inf);
		ctx.inf = cinf;
	}

	private void exitSpecial_functionSubString(Special_functionSubStringContext ctx) {
		Influences cinf = new Influences();
		cinf.addAll(ctx.operand1.inf);
		cinf.addAllInClause(ctx.operand2.inf);
		if (ctx.operand3 != null) {
			cinf.addAllInClause(ctx.operand3.inf);
		}
		ctx.inf = cinf;
	}

	private void exitSpecial_functionDateTime(Special_functionDateTimeContext ctx) {
		Influences cinf = new Influences();
		ctx.inf = cinf;
	}

	private void exitObject_name(Object_nameContext ctx) {
		ObjectName name = new ObjectName();
		for (Any_nameContext s : ctx.d) {
			name.ns.add(s.getText());
		}
		ctx.objname = name;
	}

	private void exitOrdering_term_window(Ordering_term_windowContext ctx) {
		Influences cinf = new Influences();
		cinf.addAllInClause(ctx.operand1.inf);
		ctx.inf = cinf;
	}

	private void exitCommon_table_expression(Common_table_expressionContext ctx) {
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

	private void exitResult_columnAsterisk(Result_columnAsteriskContext ctx) {
		ResultColumn rc = new ResultColumn();
		rc.name = "*";
		ctx.rc = rc;
	}

	private void exitResult_columnTableAsterisk(Result_columnTableAsteriskContext ctx) {
		ResultColumn rc = new ResultColumn();
		rc.name = ctx.tn.getText() + ".*";
		rc.setObjectName(true);
		ctx.rc = rc;
	}

	private void exitResult_columnExpr(Result_columnExprContext ctx) {
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

	private void exitTable_or_subqueryTable(Table_or_subqueryTableContext ctx) {
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

	private void exitTable_or_subquerySubQuery(Table_or_subquerySubQueryContext ctx) {
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

	private void exitJoin_clause(Join_clauseContext ctx) {
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

	private void exitSelect_core(Select_coreContext ctx) {
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

	private void exitOrder_by_clause(Order_by_clauseContext ctx) {
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

	private void exitWhere_clause(Where_clauseContext ctx) {
		Influences inf = new Influences();
		ctx.inf = inf;
		inf.addAllInClause(ctx.ex.inf);
	}

	private void exitGrouping_by_clause(Grouping_by_clauseContext ctx) {
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

	private void exitHaving_clause(Having_clauseContext ctx) {
		Influences inf = new Influences();
		ctx.inf = inf;
		inf.addAllInClause(ctx.ex.inf);
	}

	private void exitQualify_clause(Qualify_clauseContext ctx) {
		Influences inf = new Influences();
		ctx.inf = inf;
		inf.addAllInClause(ctx.ex.inf);
	}

	private void exitWindow(WindowContext ctx) {
		Influences inf = new Influences();
		ctx.inf = inf;
		for (int i = 0; i < ctx.ex.size(); ++i) {
			inf.addAllInClause(ctx.ex.get(i).inf);
		}
		for (int i = 0; i < ctx.ox.size(); ++i) {
			inf.addAllInClause(ctx.ox.get(i).inf);
		}

	}

	@Override
	public Object visitInsert_stmt(Insert_stmtContext ctx) {
		visitChildren(ctx);
		exitInsert_stmt(ctx);
		return null;
	}

	@Override
	public Object visitSelect_stmt(Select_stmtContext ctx) {
		visitChildren(ctx);
		exitSelect_stmt(ctx);
		return null;
	}

	@Override
	public Object visitSelect_or_valuesSelectCore(Select_or_valuesSelectCoreContext ctx) {
		visitChildren(ctx);
		exitSelect_or_valuesSelectCore(ctx);
		return null;
	}

	@Override
	public Object visitSelect_or_valuesSelectValue(Select_or_valuesSelectValueContext ctx) {
		visitChildren(ctx);
		exitSelect_or_valuesSelectValue(ctx);
		return null;
	}

	@Override
	public Object visitUpdate_stmt(Update_stmtContext ctx) {
		visitChildren(ctx);
		exitUpdate_stmt(ctx);
		return null;
	}

	@Override
	public Object visitUpdate_stmt_from(Update_stmt_fromContext ctx) {
		visitChildren(ctx);
		exitUpdate_stmt_from(ctx);
		return null;
	}

	@Override
	public Object visitUpdate_stmt_set(Update_stmt_setContext ctx) {
		visitChildren(ctx);
		exitUpdate_stmt_set(ctx);
		return null;
	}

	@Override
	public ExprAnalyzeResult visitExprCase(ExprCaseContext ctx) {
		List<ExprAnalyzeResult> e = new ArrayList<ExprAnalyzeResult>();
		for (ExprContext param : ctx.ex) {
			e.add((ExprAnalyzeResult) param.accept(this));
		}
		for (ExprContext param : ctx.ax) {
			e.add((ExprAnalyzeResult) param.accept(this));
		}
		return new ExprAnalyzeResult(StateTransformDefinition.combine(e.stream().map(ExprAnalyzeResult::getTransformation)
				.collect(Collectors.toList())));
	}

	@Override
	public Object visitExprFunction(ExprFunctionContext ctx) {


//		for (ExprContext param : ctx.ex) {
//			cinf.addAll(param.inf);
//		}
//		if (ctx.wx != null) {
//			cinf.addAllInClause(ctx.wx.inf);
//		}
		FunctionDefinition fndef = null;
		
		if (ctx.function_name() != null) {
			String fn = ctx.function_name().getText();
			fndef = scopeStack.searchByName(fn, FunctionDefinition.class);
			if ( fndef == null) {
				unmetFunctionNames.add(fn);
			}
		}
		
		return null;
	}

	@Override
	public ExprAnalyzeResult visitExprExists(ExprExistsContext ctx) {
		StateTransformDefinition ss = (StateTransformDefinition) ctx.ss.accept(this);
		return new ExprAnalyzeResult(ss);
	}

	@Override
	public Object visitExprSpecialFunction(ExprSpecialFunctionContext ctx) {
		visitChildren(ctx);
		exitExprSpecialFunction(ctx);
		return null;
	}

	@Override
	public Object visitExprObject(ExprObjectContext ctx) {
		visitChildren(ctx);
		exitExprObject(ctx);
		return null;
	}

	@Override
	public ExprAnalyzeResult visitExpr2(Expr2Context ctx) {

		ExprAnalyzeResult e1 = (ExprAnalyzeResult) ctx.operand1.accept(this);
		ExprAnalyzeResult e2 = (ExprAnalyzeResult) ctx.operand2.accept(this);
		return new ExprAnalyzeResult(StateTransformDefinition.combine(e1.getTransformation(), e2.getTransformation()));

	}

	@Override
	public ExprAnalyzeResult visitExpr1(Expr1Context ctx) {
		ExprAnalyzeResult e = (ExprAnalyzeResult) ctx.operand1.accept(this);
		return new ExprAnalyzeResult(StateTransformDefinition.combine(e.getTransformation()));
	}

	private static String compressQuotes(String paramString1, String paramString2)
	  {
	    String str = paramString1;
	    for (int i = str.indexOf(paramString2); i != -1; i = str.indexOf(paramString2, i + 1))
	      str = str.substring(0, i + 1) + str.substring(i + 2);
	    return str;
     }

	private static String escape_sql_literal(String text)
	{
		return compressQuotes(text.substring(1, text.length()-1),"''");
	}
	
	@Override
	public ExprAnalyzeResult visitExprLiteral(ExprLiteralContext ctx) {
		String text = ctx.getText();
		if (ctx.literal_value().STRING_LITERAL() != null) {
			text = escape_sql_literal(text);

		}
		return new ExprAnalyzeResult(StateTransformDefinition.of(), text);

	}

	@Override
	public ExprAnalyzeResult visitExprOR(ExprORContext ctx) {
		ExprAnalyzeResult e1 = (ExprAnalyzeResult) ctx.operand1.accept(this);
		ExprAnalyzeResult e2 = (ExprAnalyzeResult) ctx.operand2.accept(this);
		String literalValue = null;
		if (e1.getLiteralValue() != null && e2.getLiteralValue() != null) {
			literalValue = e1.getLiteralValue() + e2.getLiteralValue();
		}
		return new ExprAnalyzeResult(StateTransformDefinition.combine(e1.getTransformation(), e2.getTransformation()),
				literalValue);

	}

	@Override
	public ExprAnalyzeResult visitExpr2poi(Expr2poiContext ctx) {
		ExprAnalyzeResult e1 = (ExprAnalyzeResult) ctx.operand1.accept(this);
		ExprAnalyzeResult e2 = (ExprAnalyzeResult) ctx.operand2.accept(this);
		return new ExprAnalyzeResult(StateTransformDefinition.combine(e1.getTransformation(), e2.getTransformation()));

	}

	@Override
	public ExprAnalyzeResult visitExprBetween(ExprBetweenContext ctx) {
		ExprAnalyzeResult e1 = (ExprAnalyzeResult) ctx.operand1.accept(this);
		ExprAnalyzeResult e2 = (ExprAnalyzeResult) ctx.operand2.accept(this);
		ExprAnalyzeResult e3 = (ExprAnalyzeResult) ctx.operand3.accept(this);
		return new ExprAnalyzeResult(StateTransformDefinition.combine(e1.getTransformation(), e2.getTransformation(),
				e3.getTransformation()));

	}

	@Override
	public Object visitSpecial_function1(Special_function1Context ctx) {
		visitChildren(ctx);
		exitSpecial_function1(ctx);
		return null;
	}

	@Override
	public Object visitSpecial_functionSubString(Special_functionSubStringContext ctx) {
		visitChildren(ctx);
		exitSpecial_functionSubString(ctx);
		return null;
	}

	@Override
	public Object visitSpecial_functionDateTime(Special_functionDateTimeContext ctx) {
		visitChildren(ctx);
		exitSpecial_functionDateTime(ctx);
		return null;
	}

	@Override
	public Object visitObject_name(Object_nameContext ctx) {
		visitChildren(ctx);
		exitObject_name(ctx);
		return null;
	}

	@Override
	public Object visitOrdering_term_window(Ordering_term_windowContext ctx) {
		visitChildren(ctx);
		exitOrdering_term_window(ctx);
		return null;
	}

	@Override
	public Object visitCommon_table_expression(Common_table_expressionContext ctx) {
		visitChildren(ctx);
		exitCommon_table_expression(ctx);
		return null;
	}

	@Override
	public Object visitResult_columnAsterisk(Result_columnAsteriskContext ctx) {
		visitChildren(ctx);
		exitResult_columnAsterisk(ctx);
		return null;
	}

	@Override
	public Object visitResult_columnTableAsterisk(Result_columnTableAsteriskContext ctx) {
		visitChildren(ctx);
		exitResult_columnTableAsterisk(ctx);
		return null;
	}

	@Override
	public Object visitResult_columnExpr(Result_columnExprContext ctx) {
		visitChildren(ctx);
		exitResult_columnExpr(ctx);
		return null;
	}

	@Override
	public Object visitTable_or_subqueryTable(Table_or_subqueryTableContext ctx) {
		visitChildren(ctx);
		exitTable_or_subqueryTable(ctx);
		return null;
	}

	@Override
	public Object visitTable_or_subquerySubQuery(Table_or_subquerySubQueryContext ctx) {
		visitChildren(ctx);
		exitTable_or_subquerySubQuery(ctx);
		return null;
	}

	@Override
	public Object visitJoin_clause(Join_clauseContext ctx) {
		visitChildren(ctx);
		exitJoin_clause(ctx);
		return null;
	}

	@Override
	public Object visitSelect_core(Select_coreContext ctx) {
		visitChildren(ctx);
		exitSelect_core(ctx);
		return null;
	}

	@Override
	public Object visitOrder_by_clause(Order_by_clauseContext ctx) {
		visitChildren(ctx);
		exitOrder_by_clause(ctx);
		return null;
	}

	@Override
	public Object visitWhere_clause(Where_clauseContext ctx) {
		visitChildren(ctx);
		exitWhere_clause(ctx);
		return null;
	}

	@Override
	public Object visitGrouping_by_clause(Grouping_by_clauseContext ctx) {
		visitChildren(ctx);
		exitGrouping_by_clause(ctx);
		return null;
	}

	@Override
	public Object visitHaving_clause(Having_clauseContext ctx) {
		visitChildren(ctx);
		exitHaving_clause(ctx);
		return null;
	}

	@Override
	public Object visitQualify_clause(Qualify_clauseContext ctx) {
		visitChildren(ctx);
		exitQualify_clause(ctx);
		return null;
	}

	@Override
	public Object visitWindow(WindowContext ctx) {
		visitChildren(ctx);
		exitWindow(ctx);
		return null;
	}
}

/*
 * 
 * A Graph–Free Approach to Data–Flow Analysis, Markus Mohnen
 * 
 * Define State s:
 * 
 * 
 * 
 * 
 * 
 * Define I(s) state transition I(s): Assign Call Select into (multiple assign)
 * Branch return Exception
 * 
 * 
 * Define <= of lattice:
 */
