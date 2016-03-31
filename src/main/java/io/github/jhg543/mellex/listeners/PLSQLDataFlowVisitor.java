package io.github.jhg543.mellex.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.TokenStream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import io.github.jhg543.mellex.ASTHelper.CreateTableStmt;
import io.github.jhg543.mellex.ASTHelper.GlobalSettings;
import io.github.jhg543.mellex.ASTHelper.Influences;
import io.github.jhg543.mellex.ASTHelper.InsertStmt;
import io.github.jhg543.mellex.ASTHelper.ObjectName;
import io.github.jhg543.mellex.ASTHelper.SubQuery;
import io.github.jhg543.mellex.ASTHelper.UpdateStmt;
import io.github.jhg543.mellex.ASTHelper.plsql.ColumnDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.CursorDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ExprAnalyzeResult;
import io.github.jhg543.mellex.ASTHelper.plsql.FunctionDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectReference;
import io.github.jhg543.mellex.ASTHelper.plsql.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.plsql.ScopeStack;
import io.github.jhg543.mellex.ASTHelper.plsql.SelectStmtData;
import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.TableDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ValueFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableModification;
import io.github.jhg543.mellex.ASTHelper.symbol.NameResolver;
import io.github.jhg543.mellex.ASTHelper.symbol.TableStorage;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPBaseVisitor;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Any_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Case_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Column_defContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Column_defsContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Column_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Common_table_expressionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Create_procedureContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Create_source_tableContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Create_table_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Create_view_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Cursor_definitionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Declare_sectionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Declare_section_onelineContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Delete_stmtContext;
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
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Non_subquery_select_stmtContext;
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
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Select_or_valuesContext;
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
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.While_loop_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.WindowContext;
import io.github.jhg543.mellex.inputsource.TableDefinitionProvider;
import io.github.jhg543.mellex.listeners.flowmfp.InstBuffer;
import io.github.jhg543.mellex.listeners.flowmfp.Instruction;
import io.github.jhg543.mellex.listeners.flowmfp.PatchList;
import io.github.jhg543.mellex.listeners.flowmfp.State;
import io.github.jhg543.mellex.util.DatabaseVendor;
import io.github.jhg543.mellex.util.Misc;
import io.github.jhg543.mellex.util.tuple.Tuple2;

@SuppressWarnings("unchecked")
public class PLSQLDataFlowVisitor extends DefaultSQLPBaseVisitor<Object> {

	private static String compressQuotes(String paramString1, String paramString2) {
		String str = paramString1;
		for (int i = str.indexOf(paramString2); i != -1; i = str.indexOf(paramString2, i + 1))
			str = str.substring(0, i + 1) + str.substring(i + 2);
		return str;
	}
	private static String escape_sql_literal(String text) {
		return compressQuotes(text.substring(1, text.length() - 1), "''");
	}
	private static StateFunc funcOfExpr(Object expr) {
		return ((ExprAnalyzeResult) expr).getTransformation();
	}
	private TableDefinitionProvider provider;
	private NameResolver nameResolver;
	private TokenStream stream;
	private String current_sql;
	private String current_file;
	private InstBuffer instbuffer;
	private boolean metaGuessEnabled = true;

	private ScopeStack scopeStack;

	public PLSQLDataFlowVisitor(TokenStream stream, InstBuffer instbuffer, DatabaseVendor vendor, boolean metaGuessEnabled) {
		super();

		this.stream = stream;
		this.instbuffer = instbuffer;
		this.metaGuessEnabled = metaGuessEnabled;
		this.nameResolver = new NameResolver(vendor, new TableStorage(), metaGuessEnabled);
	}

	private void exitObject_name(Object_nameContext ctx) {
		ObjectName name = new ObjectName();
		for (Any_nameContext s : ctx.d) {
			name.ns.add(s.getText());
		}
		ctx.objname = name;
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

	private String getText(RuleContext ctx) {
		return stream.getText(ctx.getSourceInterval());
	}

	@Override
	public PatchList visitCase_statement(Case_statementContext ctx) {
		PatchList p = new PatchList();
		if (ctx.selector != null && !ctx.selector.inf.isempty()) {
			Instruction<State> ins = new Instruction<State>("null");
			ins.setDebugInfo(ctx.getStart().getLine());
			Influences inf = ctx.selector.inf;
			ins.setFunc(state -> {
				State v = state.shallowCopy();

				return null;
			});
			p.setStartInstruction(ins);
		}

		return null;
	}

	@Override
	public String visitColumn_def(Column_defContext ctx) {
		return ctx.cn.getText();
	}

	@Override
	public List<String> visitColumn_defs(Column_defsContext ctx) {
		List<String> colnames = new ArrayList<>();

		for (Column_defContext cd : ctx.cd) {
			colnames.add((String) cd.accept(this));
		}

		return colnames;
	}

	@Override
	public Tuple2<String, SelectStmtData> visitCommon_table_expression(Common_table_expressionContext ctx) {
		SelectStmtData data = (SelectStmtData) ctx.ss.accept(this);
		if (ctx.cn.size() > 0) {
			Preconditions.checkState(data.getColumns().size() == ctx.cn.size(), "CTE column size mismatch");
			List<ResultColumn> results = new ArrayList<>();
			for (int i = 0; i < ctx.cn.size(); ++i) {
				results.add(new ResultColumn(ctx.cn.get(i).getText(), i, data.getColumnExprFunc(i)));
			}
			data = new SelectStmtData(results);
		}

		return Tuple2.of(ctx.tn.getText(), data);
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
	public Object visitCreate_source_table(Create_source_tableContext ctx) {
		if (ctx.ss != null) {
			return ctx.ss.accept(this);
		}

		return ctx.obj.getText();
	}

	@Override
	public Object visitCreate_table_stmt(Create_table_stmtContext ctx) {
		// TODO the source table data
		String tableName = ctx.obj.getText();
		TableDefinition def = new TableDefinition(tableName, ctx.isvolatile, false);

		if (Misc.isvolatile(tableName)) {
			def.setSessionScoped(true);
		}
		if (ctx.def != null) {
			List<String> colnames = (List<String>) ctx.def.accept(this);
			for (String colname : colnames) {
				def.addColumn(colname);

			}
			nameResolver.defineTable(tableName, def);
		} else {
			Object ss = ctx.st.accept(this);
			if (ss instanceof String) {
				TableDefinition sourceTableDef = nameResolver.searchTable((String) ss);
				List<StateFunc> subs = new ArrayList<>();
				for (ColumnDefinition srcColDef : sourceTableDef.getColumns()) {

					def.addColumn(srcColDef.getName());
					subs.add(StateFunc.ofValue(ValueFunc.of(new ObjectReference(srcColDef, current_file,
							ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine()))));
				}
				nameResolver.defineTable(tableName, def);
				if (!ctx.st.nodata) {
					instbuffer.add(new Instruction<>(StateFunc.combineInsertOrUpdate(def.getColumns(), subs)));

				}
			} else {
				SelectStmtData selectStmt = (SelectStmtData) ss;
				List<StateFunc> subs = new ArrayList<>();
				for (ResultColumn rc : selectStmt.getColumns()) {
					def.addColumn(rc.getName());
					subs.add(rc.getExpr());
				}
				nameResolver.defineTable(tableName, def);
				if (!ctx.st.nodata) {
					instbuffer.add(new Instruction<>(StateFunc.combineInsertOrUpdate(def.getColumns(), subs)));
				}
			}
		}
		return null;
	}

	@Override
	public Object visitCreate_view_stmt(Create_view_stmtContext ctx) {
		// TODO the source table data
		String tableName = ctx.obj.getText();

		TableDefinition def = new TableDefinition(tableName, false, false);
		def.setName(tableName);
		SelectStmtData selectStmt = (SelectStmtData) ctx.ss.accept(this);

		if (ctx.cn.size() > 0) {
			List<String> colnames = new ArrayList<>();
			for (Column_nameContext colctx : ctx.cn) {
				colnames.add(colctx.getText());
			}
			nameResolver.defineTable(tableName, def);
		} else {

			for (ResultColumn rc : selectStmt.getColumns()) {
				def.addColumn(rc.getName());
			}
			nameResolver.defineTable(tableName, def);

		}

		instbuffer.add(new Instruction<>(StateFunc.combineInsertOrUpdate(def.getColumns(),
				selectStmt.getColumns().stream().map(rc -> rc.getExpr()).collect(Collectors.toList()))));

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
	public Object visitDelete_stmt(Delete_stmtContext ctx) {
		return null; // TODO fill delete stmt
	}

	@Override
	public ExprAnalyzeResult visitExpr1(Expr1Context ctx) {
		ExprAnalyzeResult e = (ExprAnalyzeResult) ctx.operand1.accept(this);
		return new ExprAnalyzeResult(StateFunc.combine(e.getTransformation()));
	}

	@Override
	public ExprAnalyzeResult visitExpr2(Expr2Context ctx) {

		ExprAnalyzeResult e1 = (ExprAnalyzeResult) ctx.operand1.accept(this);
		ExprAnalyzeResult e2 = (ExprAnalyzeResult) ctx.operand2.accept(this);
		return new ExprAnalyzeResult(StateFunc.combine(e1.getTransformation(), e2.getTransformation()));

	}

	@Override
	public ExprAnalyzeResult visitExpr2poi(Expr2poiContext ctx) {
		ExprAnalyzeResult e1 = (ExprAnalyzeResult) ctx.operand1.accept(this);
		ExprAnalyzeResult e2 = (ExprAnalyzeResult) ctx.operand2.accept(this);
		return new ExprAnalyzeResult(StateFunc.combine(e1.getTransformation(), e2.getTransformation()));

	}

	@Override
	public ExprAnalyzeResult visitExprBetween(ExprBetweenContext ctx) {
		ExprAnalyzeResult e1 = (ExprAnalyzeResult) ctx.operand1.accept(this);
		ExprAnalyzeResult e2 = (ExprAnalyzeResult) ctx.operand2.accept(this);
		ExprAnalyzeResult e3 = (ExprAnalyzeResult) ctx.operand3.accept(this);
		return new ExprAnalyzeResult(StateFunc.combine(e1.getTransformation(), e2.getTransformation(), e3.getTransformation()));

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
		return new ExprAnalyzeResult(
				StateFunc.combine(e.stream().map(ExprAnalyzeResult::getTransformation).collect(Collectors.toList())));
	}

	@Override
	public ExprAnalyzeResult visitExprExists(ExprExistsContext ctx) {
		SelectStmtData ss = (SelectStmtData) ctx.ss.accept(this);
		StateFunc fn = StateFunc.combineNoValue(ss.getColumns().stream().map(rc -> rc.getExpr()).collect(Collectors.toList()));
		return new ExprAnalyzeResult(fn);
	}

	@Override
	public ExprAnalyzeResult visitExprFunction(ExprFunctionContext ctx) {

		FunctionDefinition fndef = null;

		if (ctx.function_name() != null) {
			String fn = ctx.function_name().getText();
			// TODO can return type cast to fndef?
			fndef = nameResolver.searchFunction(fn);
		}

		if (fndef == null) {
			fndef = FunctionDefinition.unknownFunction(ctx.ex.size());
		}

		List<StateFunc> params = new ArrayList<>();
		for (ExprContext param : ctx.ex) {
			ExprAnalyzeResult e = (ExprAnalyzeResult) param.accept(this);
			params.add(e.getTransformation());
		}

		StateFunc fs = fndef.apply(params);

		if (ctx.wx == null) {
			return new ExprAnalyzeResult(fs);
		} else {
			StateFunc windowsexpr = (StateFunc) ctx.wx.accept(this);
			return new ExprAnalyzeResult(StateFunc.combine(fs, windowsexpr));
		}

	}

	@Override
	public ExprAnalyzeResult visitExprLiteral(ExprLiteralContext ctx) {
		String text = ctx.getText();
		if (ctx.literal_value().STRING_LITERAL() != null) {
			text = escape_sql_literal(text);

		}
		return new ExprAnalyzeResult(StateFunc.of(), text);

	}

	@Override
	public ExprAnalyzeResult visitExprObject(ExprObjectContext ctx) {
		String name = ctx.getText();
		// TODO count(*)?
		if ("*".equals(name)) {
			return new ExprAnalyzeResult(StateFunc.of());
		}
		Tuple2<ObjectDefinition, StateFunc> dd = nameResolver.searchByName(name);
		if (dd == null) {
			throw new IllegalStateException(String.format("Can't resolve symbol %s, pos %d %d", name, ctx.getStart().getLine(),
					ctx.getStart().getCharPositionInLine()));
		}
		if (dd.getField0() != null) {
			ObjectDefinition def = dd.getField0();
			if (def instanceof ColumnDefinition) {
				ObjectReference r = new ObjectReference((ColumnDefinition) def, current_file, ctx.getStart().getLine(),
						ctx.getStart().getCharPositionInLine());
				return new ExprAnalyzeResult(StateFunc.ofValue(ValueFunc.of(r)));
			} else if (def instanceof VariableDefinition) {
				return new ExprAnalyzeResult(StateFunc.ofValue(ValueFunc.of((VariableDefinition) def)), name);
			}

			// if here , it's deferred resultcolumn resolution in
			// OtherResultColumnResolver
			return new ExprAnalyzeResult(StateFunc.ofValue(ValueFunc.of(def)));
		}

		return new ExprAnalyzeResult(dd.getField1());
	}

	@Override
	public ExprAnalyzeResult visitExprOR(ExprORContext ctx) {
		ExprAnalyzeResult e1 = (ExprAnalyzeResult) ctx.operand1.accept(this);
		ExprAnalyzeResult e2 = (ExprAnalyzeResult) ctx.operand2.accept(this);
		String literalValue = null;
		if (e1.getLiteralValue() != null && e2.getLiteralValue() != null) {
			literalValue = e1.getLiteralValue() + e2.getLiteralValue();
		}
		return new ExprAnalyzeResult(StateFunc.combine(e1.getTransformation(), e2.getTransformation()), literalValue);

	}

	@Override
	public ExprAnalyzeResult visitExprSpecialFunction(ExprSpecialFunctionContext ctx) {
		return (ExprAnalyzeResult) ctx.sp.accept(this);
	}

	@Override
	public Tuple2<List<StateFunc>, List<Integer>> visitGrouping_by_clause(Grouping_by_clauseContext ctx) {
		List<StateFunc> exprs = ctx.ex.stream().map(e -> funcOfExpr(e)).collect(Collectors.toList());
		List<Integer> indexes = ctx.nx.stream().map(t -> Integer.valueOf(t.getText()) - 1).collect(Collectors.toList());
		return Tuple2.of(exprs, indexes);
	}

	@Override
	public StateFunc visitHaving_clause(Having_clauseContext ctx) {
		return funcOfExpr(ctx.ex.accept(this));

	}

	@Override
	public Object visitInsert_stmt(Insert_stmtContext ctx) {
		String tableName = ctx.obj.getText();
		TableDefinition def = nameResolver.searchTable(tableName);
		if (def == null) {
			if (metaGuessEnabled) {
				def = new TableDefinition(tableName, false, true);
				nameResolver.defineTable(tableName, def);
			} else {
				throw new IllegalStateException(String.format("Table %s not found", tableName));
			}

		}
		List<ColumnDefinition> cdefs = new ArrayList<>();
		SelectStmtData ss = null;
		if (ctx.ss != null) {
			ss = (SelectStmtData) ctx.ss.accept(this);
		}
		if (ctx.cn.size() > 0) {

			for (Column_nameContext colnamectx : ctx.cn) {
				String colname = colnamectx.getText();
				ColumnDefinition cdef = def.getColumnByName(colname);
				if (cdef == null) {
					if (metaGuessEnabled) {
						cdef = def.addColumn(colname, true);
					} else {
						throw new IllegalStateException(String.format("Column to insert %s.%s not found", tableName, colname));
					}
				}

				cdefs.add(cdef);
			}
		}
		// else if (ss!=null)
		// {
		// for (ResultColumn rc:ss.getColumns())
		// {
		// String colname = rc.getName();
		// ColumnDefinition cdef = def.getColumnByName(colname);
		// Preconditions.checkState(cdef!=null,"Column to insert %s.%s not
		// found",tableName,colname);
		// cdefs.add(cdef);
		// }
		// }
		else {
			cdefs = def.getColumns();
		}

		List<StateFunc> exprs = new ArrayList<>();
		if (ss != null) {
			ss.getColumns().forEach(rc -> exprs.add(rc.getExpr()));
		} else {
			for (ExprContext exprctx : ctx.ex) {
				exprs.add(funcOfExpr(exprctx.accept(this)));
			}
		}

		Preconditions.checkState(cdefs.size() == exprs.size(), "Column size %d != expr size %d", cdefs.size(), exprs.size());
		// TODO REMOVE SELF ASSIGN FROM LIST

		instbuffer.add(new Instruction<>(StateFunc.combineInsertOrUpdate(cdefs, exprs)));
		return null;
	}

	@Override
	public Object visitJoin_clause(Join_clauseContext ctx) {
		// used directly by parent
		return null;
	}

	@Override
	public PatchList visitMultiple_plsql_stmt_list(Multiple_plsql_stmt_listContext ctx) {
		PatchList patchList = new PatchList();
		patchList.setBreakList(new ArrayList<Instruction<State>>());
		patchList.setContinueList(new ArrayList<Instruction<State>>());

		PatchList prevpl = null;
		for (Plsql_statementContext plsqlctx : ctx.plsql_statement()) {
			PatchList currentpl = (PatchList) plsqlctx.accept(this);
			patchList.getBreakList().addAll(currentpl.getBreakList());
			patchList.getContinueList().addAll(currentpl.getContinueList());
			if (prevpl != null) {
				for (Instruction<State> ins : prevpl.getNextList()) {
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
	public Object visitNon_subquery_select_stmt(Non_subquery_select_stmtContext ctx) {
		SelectStmtData ss = (SelectStmtData) ctx.select_stmt().accept(this);
		instbuffer.add(new Instruction<>(ss));
		return null;
	}

	@Override
	public Object visitObject_name(Object_nameContext ctx) {
		visitChildren(ctx);
		exitObject_name(ctx);
		return null;
	}

	@Override
	public Tuple2<List<StateFunc>, List<Integer>> visitOrder_by_clause(Order_by_clauseContext ctx) {
		List<StateFunc> exprs = ctx.ex.stream().map(e -> funcOfExpr(e)).collect(Collectors.toList());
		List<Integer> indexes = ctx.nx.stream().map(t -> Integer.valueOf(t.getText()) - 1).collect(Collectors.toList());
		return Tuple2.of(exprs, indexes);
	}

	@Override
	public StateFunc visitOrdering_term_window(Ordering_term_windowContext ctx) {
		return funcOfExpr(ctx.operand1.accept(this));
	}

	@Override
	public VariableDefinition visitParameter_declaration(Parameter_declarationContext ctx) {
		if (ctx.K_OUT() != null) {
			throw new UnsupportedOperationException("OUT PARAMETER NOT IMPLEMENTED");
		}
		VariableDefinition def = new VariableDefinition();
		def.setName(ctx.any_name().getText());
		ExprContext exprContext = ctx.expr();
		if (exprContext != null) {
			// TODO deal with expr
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
	public Object visitProcedure_or_function_declaration(Procedure_or_function_declarationContext ctx) {
		throw new UnsupportedOperationException("FORWARD DECLARATION NOT SUPPORTED");
	}

	@Override
	public FunctionDefinition visitProcedure_or_function_definition(Procedure_or_function_definitionContext ctx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StateFunc visitQualify_clause(Qualify_clauseContext ctx) {
		return funcOfExpr(ctx.ex.accept(this));

	}

	@Override
	public List<Tuple2<String, StateFunc>> visitResult_columnAsterisk(Result_columnAsteriskContext ctx) {
		List<Tuple2<String, StateFunc>> result = nameResolver.searchWildcardAll(current_file, ctx.getStart().getLine(),
				ctx.getStart().getCharPositionInLine());
		result.forEach(x -> nameResolver.enterResultColumn(null));
		return result;
	}

	@Override
	public List<Tuple2<String, StateFunc>> visitResult_columnExpr(Result_columnExprContext ctx) {

		String alias = null;
		if (ctx.ca != null) {
			alias = ctx.ca.getText();
		} else {
			alias = null;
		}
		nameResolver.enterResultColumn(alias);
		if (alias == null) {
			alias = ctx.ex.getText();
			// select p1.c1 from p1 , result col name is c1, not p1.c1
			if (ctx.ex instanceof ExprObjectContext) {
				int pos = alias.lastIndexOf('.');
				if (pos != -1) {
					alias = alias.substring(pos + 1);
				}
			}
		}
		ExprAnalyzeResult e = (ExprAnalyzeResult) ctx.ex.accept(this);
		return Collections.singletonList(Tuple2.of(alias, e.getTransformation()));
	}

	@Override
	public List<Tuple2<String, StateFunc>> visitResult_columnTableAsterisk(Result_columnTableAsteriskContext ctx) {
		String tableName = ctx.tn.getText();
		List<Tuple2<String, StateFunc>> result = nameResolver.searchWildcardOneTable(tableName, current_file,
				ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
		result.forEach(x -> nameResolver.enterResultColumn(null));
		return result;
	}

	@Override
	public Object visitSelect_core(Select_coreContext ctx) {
		// visitChildren(ctx);
		// exitSelect_core(ctx);
		// return null;
		nameResolver.enterSelectStmt(ctx);

		if (ctx.jc != null) {
			// visit all Table or subquery to construct name resolver
			for (Table_or_subqueryContext sub : ctx.jc.ts) {
				sub.accept(this);
			}
		}

		List<ResultColumn> columns = new ArrayList<>();

		List<String> aliasList = new ArrayList<>();

		for (Result_columnContext rcctx : ctx.r) {
			if (rcctx instanceof Result_columnExprContext) {
				Result_columnExprContext rcexpr = (Result_columnExprContext) rcctx;
				if (rcexpr.ca != null) {
					aliasList.add(rcexpr.ca.getText());
				}
			}
		}

		nameResolver.collectResultColumnAlias(aliasList);

		for (Result_columnContext rcctx : ctx.r) {
			List<Tuple2<String, StateFunc>> rc = (List<Tuple2<String, StateFunc>>) rcctx.accept(this);
			for (Tuple2<String, StateFunc> tuple : rc) {
				columns.add(new ResultColumn(tuple.getField0(), columns.size(), tuple.getField1()));
			}
		}

		SelectStmtData ss = nameResolver.rewriteAfterResultColumns(new SelectStmtData(columns));

		List<StateFunc> clauses = new ArrayList<>();

		// join clause
		if (ctx.jc != null) {
			// visit all Table or subquery to construct name resolver
			for (ExprContext sub : ctx.jc.ex) {
				clauses.add(funcOfExpr(sub.accept(this)));
			}
		}

		// where
		if (ctx.w1 != null) {
			clauses.add((StateFunc) ctx.w1.accept(this));
		}

		// having
		if (ctx.h1 != null) {
			clauses.add((StateFunc) ctx.h1.accept(this));
		}

		// qualify
		if (ctx.q1 != null) {
			clauses.add((StateFunc) ctx.q1.accept(this));
		}

		// group by
		if (ctx.g1 != null) {
			Tuple2<List<StateFunc>, List<Integer>> r = (Tuple2<List<StateFunc>, List<Integer>>) ctx.g1.accept(this);
			clauses.addAll(r.getField0());
			r.getField1().stream().map(i -> ss.getColumnExprFunc(i)).forEach(clauses::add);
		}

		if (ctx.g2 != null) {
			Tuple2<List<StateFunc>, List<Integer>> r = (Tuple2<List<StateFunc>, List<Integer>>) ctx.g2.accept(this);
			clauses.addAll(r.getField0());
			r.getField1().stream().map(i -> ss.getColumnExprFunc(i)).forEach(clauses::add);
		}

		// order by
		if (ctx.o1 != null) {
			Tuple2<List<StateFunc>, List<Integer>> r = (Tuple2<List<StateFunc>, List<Integer>>) ctx.o1.accept(this);
			clauses.addAll(r.getField0());
			r.getField1().stream().map(i -> ss.getColumnExprFunc(i)).forEach(clauses::add);
		}

		StateFunc combinedClause = StateFunc.combine(clauses);

		nameResolver.exitSelectStmt(ctx);
		ss.getColumns().forEach(rc -> rc.setExpr(rc.getExpr().addWhereClause(combinedClause)));
		return ss;

		// TODO implement "INTO" clause
	}

	@Override
	public Object visitSelect_or_valuesSelectCore(Select_or_valuesSelectCoreContext ctx) {
		return ctx.sc.accept(this);
	}

	@Override
	public Object visitSelect_or_valuesSelectValue(Select_or_valuesSelectValueContext ctx) {
		return ctx.ss.accept(this);
	}

	@Override
	public SelectStmtData visitSelect_stmt(Select_stmtContext ctx) {
		LinkedList<String> ctes = new LinkedList<>();
		for (Common_table_expressionContext ctectx : ctx.c) {
			Tuple2<String, SelectStmtData> cte = (Tuple2<String, SelectStmtData>) ctectx.accept(this);
			ctes.push(cte.getField0());
			nameResolver.pushCte(cte.getField0(), cte.getField1());
		}

		List<SelectStmtData> svs = new ArrayList<>();

		for (Select_or_valuesContext svctx : ctx.sv) {
			SelectStmtData sv = (SelectStmtData) svctx.accept(this);
			svs.add(sv);
			Preconditions.checkState(svs.get(0).getColumns().size() == sv.getColumns().size(), "Union Column size mismatch");
		}

		while (!ctes.isEmpty()) {
			nameResolver.popCte(ctes.pop());
		}

		SelectStmtData sv0 = svs.get(0);
		if (svs.size() == 1) {
			return sv0;
		}

		List<ResultColumn> columns = new ArrayList<>();
		for (int i = 0; i < sv0.getColumns().size(); ++i) {

			List<StateFunc> subs = new ArrayList<>();
			for (SelectStmtData sv : svs) {
				subs.add(sv.getColumnExprFunc(i));
			}
			columns.add(new ResultColumn(sv0.getColumns().get(i).getName(), i, StateFunc.combine(subs)));
		}

		return new SelectStmtData(columns);
	}

	@Override
	public ExprAnalyzeResult visitSpecial_function1(Special_function1Context ctx) {
		ExprAnalyzeResult e = (ExprAnalyzeResult) ctx.operand1.accept(this);
		return new ExprAnalyzeResult(StateFunc.combine(e.getTransformation()));
	}

	@Override
	public Object visitSpecial_functionDateTime(Special_functionDateTimeContext ctx) {
		return new ExprAnalyzeResult(StateFunc.of());
	}

	@Override
	public Object visitSpecial_functionSubString(Special_functionSubStringContext ctx) {
		ExprAnalyzeResult e1 = (ExprAnalyzeResult) ctx.operand1.accept(this);
		ExprAnalyzeResult e2 = (ExprAnalyzeResult) ctx.operand2.accept(this);
		ExprAnalyzeResult e3 = (ExprAnalyzeResult) ctx.operand3.accept(this);
		return new ExprAnalyzeResult(StateFunc.combine(e1.getTransformation(), e2.getTransformation(), e3.getTransformation()));
	}

	@Override
	public Object visitSql_stmt(Sql_stmtContext ctx) {
		current_sql = stream.getText(ctx.getSourceInterval());
		visitChildren(ctx);
		return null;
	}

	@Override
	public String visitTable_or_subquerySubQuery(Table_or_subquerySubQueryContext ctx) {
		SelectStmtData ss = (SelectStmtData) ctx.ss.accept(this);
		String alias = ctx.ta.getText();
		nameResolver.addFromSubQuery(alias, ss);
		return alias;
	}

	@Override
	public String visitTable_or_subqueryTable(Table_or_subqueryTableContext ctx) {
		String tableName = ctx.tn.getText();
		if (ctx.dn != null) {
			tableName = ctx.dn.getText() + "." + tableName;
		}
		String alias = null;
		if (ctx.ta != null) {
			alias = ctx.ta.getText();
		}

		nameResolver.addFromTable(tableName, alias);
		return tableName;
	}

	@Override
	public Object visitUpdate_stmt(Update_stmtContext ctx) {
		nameResolver.enterSelectStmt(ctx);

		String tableName = ctx.tobj.getText();
		TableDefinition targetTableDef;
		if (ctx.ta != null) {
			String alias = ctx.ta.getText();
			nameResolver.addFromTable(tableName, alias);
		}

		if (ctx.f != null) {
			ctx.f.accept(this);
		}

		targetTableDef = nameResolver.searchTable(tableName);
		if (targetTableDef == null) {
			targetTableDef = nameResolver.getAliasTableDefinition(tableName);
			// if (targetTableDef == null)
			// {
			// if (metaGuessEnabled)
			// {
			//
			// }
			// else
			// {
			// throw new IllegalStateException("");
			// }
			// }
			tableName = targetTableDef.getName();
		}

		StateFunc whereclause = StateFunc.of();
		if (ctx.wex != null) {
			whereclause = funcOfExpr(ctx.wex.accept(this));
		}
		List<ColumnDefinition> cdefs = new ArrayList<>();
		List<StateFunc> exprs = new ArrayList<>();
		for (int i = 0; i < ctx.s.cn.size(); ++i) {
			String columnName = ctx.s.cn.get(i).getText();
			ColumnDefinition cdef = targetTableDef.getColumnByName(columnName);
			if (cdef == null) {
				if (metaGuessEnabled) {
					cdef = targetTableDef.addColumn(columnName, true);
				} else {
					throw new IllegalStateException(String.format("Column to insert %s.%s not found", tableName, columnName));
				}
			}
			cdefs.add(cdef);
			exprs.add(StateFunc.combine(funcOfExpr(ctx.s.ex.get(i).accept(this)), whereclause));
		}

		nameResolver.exitSelectStmt(ctx);

		instbuffer.add(new Instruction<>(StateFunc.combineInsertOrUpdate(cdefs, exprs)));
		return null;
	}

	@Override
	public Object visitUpdate_stmt_from(Update_stmt_fromContext ctx) {
		visitChildren(ctx);
		return null;
	}

	@Override
	public PatchList visitWhile_loop_statement(While_loop_statementContext ctx) {
		StateFunc exprf = funcOfExpr(ctx.expr().accept(this));
		return null;
	}
	@Override
	public VariableDefinition visitVariable_declaration(Variable_declarationContext ctx) {
		// COPY FROM visitParameter_declaration

		VariableDefinition def = new VariableDefinition();
		def.setName(ctx.any_name().getText());
		// def.setConst(ctx.K_CONSTANT() != null);
		ExprContext exprContext = ctx.expr();
		if (exprContext != null) {
			// TODO deal with expr
		}

		return def;
	}

	@Override
	public StateFunc visitWhere_clause(Where_clauseContext ctx) {
		return funcOfExpr(ctx.ex.accept(this));

	}

	@Override
	public StateFunc visitWindow(WindowContext ctx) {
		List<StateFunc> s = new ArrayList<>();
		for (ExprContext p : ctx.ex) {
			s.add(funcOfExpr(p.accept(this)));
		}
		for (Ordering_term_windowContext p : ctx.ox) {
			s.add((StateFunc) p.accept(this));
		}
		return StateFunc.combine(s);
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
