package io.github.jhg543.mellex.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
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
import io.github.jhg543.mellex.ASTHelper.plsql.ParameterDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.plsql.SelectStmtData;
import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.TableDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ValueFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;
import io.github.jhg543.mellex.ASTHelper.symbol.NameResolver;
import io.github.jhg543.mellex.ASTHelper.symbol.TableStorage;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPBaseVisitor;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Any_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Assign_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Basic_loop_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.BodyContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Call_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Case_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Column_defContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Column_defsContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Column_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Common_table_expressionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Continue_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Create_procedureContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Create_source_tableContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Create_table_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Create_view_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Cursor_definitionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Declare_sectionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Declare_section_onelineContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Delete_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Exit_statementContext;
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
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.For_loop_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Grouping_by_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Having_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.If_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Insert_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Join_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.LabelContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Multiple_plsql_stmt_listContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Non_subquery_select_stmtContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Null_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Object_nameContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Order_by_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Ordering_term_windowContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Parameter_declarationContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Parameter_declarationsContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.ParseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Plsql_blockContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Plsql_statementContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Plsql_statement_nolabelContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Procedure_or_function_declarationContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Procedure_or_function_definitionContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Qualify_clauseContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Result_columnAsteriskContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Result_columnContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Result_columnExprContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Result_columnTableAsteriskContext;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser.Return_statementContext;
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
import io.github.jhg543.mellex.listeners.flowmfp.InstFuncHelper;
import io.github.jhg543.mellex.listeners.flowmfp.Instruction;
import io.github.jhg543.mellex.listeners.flowmfp.LabelRecorder;
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

	private static Object CollectDebugInfo(Object... objects) {
		return objects;
	}

	private TableDefinitionProvider provider;
	private NameResolver nameResolver;
	private TokenStream stream;
	private String current_sql;
	private String current_file;
	private InstBuffer instbuffer;
	private boolean metaGuessEnabled = true;
	private boolean debugPlsql = true;
	private LabelRecorder labelRecorder;

	public PLSQLDataFlowVisitor(TokenStream stream, InstBuffer instbuffer, DatabaseVendor vendor, boolean metaGuessEnabled) {
		super();

		this.stream = stream;
		this.instbuffer = instbuffer;
		this.metaGuessEnabled = metaGuessEnabled;
		this.labelRecorder = new LabelRecorder();
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
	public Object visitCreate_source_table(Create_source_tableContext ctx) {
		if (ctx.ss != null) {
			return ctx.ss.accept(this);
		}

		return ctx.obj.getText();
	}

	@Override
	public Object visitCreate_table_stmt(Create_table_stmtContext ctx) {
		String tableName = ctx.obj.getText();
		TableDefinition def = new TableDefinition(tableName, ctx.isvolatile, false);

		if (Misc.isvolatile(tableName)) {
			def.setSessionScoped(true);
		}
		if (ctx.def != null) {
			// create table t ( a number(2),b number 20)
			List<String> colnames = (List<String>) ctx.def.accept(this);
			for (String colname : colnames) {
				def.addColumn(colname);
			}
			nameResolver.defineTable(tableName, def);
		} else {
			Object ss = ctx.st.accept(this);
			if (ss instanceof String) {
				// create table t as schema.table;
				TableDefinition sourceTableDef = nameResolver.searchTable((String) ss);
				List<StateFunc> subs = new ArrayList<>();
				for (ColumnDefinition srcColDef : sourceTableDef.getColumns()) {

					def.addColumn(srcColDef.getName());
					subs.add(StateFunc.ofValue(ValueFunc.of(new ObjectReference(srcColDef, current_file,
							ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine()))));
				}
				nameResolver.defineTable(tableName, def);
				if (!ctx.st.nodata) {
					return instbuffer.add(new Instruction(InstFuncHelper.insertOrUpdateFunc(def.getColumns(), subs),
							CollectDebugInfo(ctx.getClass().getName(), ctx.getStart().getLine(), def.getColumns(), subs),
							nameResolver.getCurrentScopeId()));
				}
			} else {
				// create table t as select cc from tt;
				SelectStmtData selectStmt = (SelectStmtData) ss;
				List<StateFunc> subs = new ArrayList<>();
				for (ResultColumn rc : selectStmt.getColumns()) {
					def.addColumn(rc.getName());
					subs.add(rc.getExpr());
				}
				nameResolver.defineTable(tableName, def);
				if (!ctx.st.nodata) {
					return instbuffer.add(new Instruction(InstFuncHelper.insertOrUpdateFunc(def.getColumns(), subs),
							CollectDebugInfo(ctx.getClass().getName(), ctx.getStart().getLine(), def.getColumns(), subs),
							nameResolver.getCurrentScopeId()));
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

		List<StateFunc> subs = selectStmt.getColumns().stream().map(rc -> rc.getExpr()).collect(Collectors.toList());
		return instbuffer.add(new Instruction(InstFuncHelper.insertOrUpdateFunc(def.getColumns(), subs),
				CollectDebugInfo(ctx.getClass().getName(), ctx.getStart().getLine(), def.getColumns(), subs),
				nameResolver.getCurrentScopeId()));

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

		StateFunc fs = fndef.applyDefinition(params);

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
				return new ExprAnalyzeResult((VariableDefinition) def);
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

		if (!e1.getLiteralValue().isEmpty() && !e2.getLiteralValue().isEmpty()) {
			List<Object> literalValue = null;
			literalValue = new LinkedList<>();
			literalValue.addAll(e1.getLiteralValue());
			literalValue.addAll(e2.getLiteralValue());
			return new ExprAnalyzeResult(StateFunc.combine(e1.getTransformation(), e2.getTransformation()), literalValue);
		} else {
			return new ExprAnalyzeResult(StateFunc.combine(e1.getTransformation(), e2.getTransformation()));
		}

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

		return instbuffer.add(new Instruction(InstFuncHelper.insertOrUpdateFunc(cdefs, exprs),
				CollectDebugInfo(ctx.getClass().getName(), ctx.getStart().getLine(), cdefs, exprs),
				nameResolver.getCurrentScopeId()));

	}

	@Override
	public Object visitJoin_clause(Join_clauseContext ctx) {
		// used directly by parent
		return null;
	}

	@Override
	public Object visitNon_subquery_select_stmt(Non_subquery_select_stmtContext ctx) {
		SelectStmtData ss = (SelectStmtData) ctx.select_stmt().accept(this);
		return instbuffer.add(new Instruction(InstFuncHelper.SelectFunc(ss),
				CollectDebugInfo(ctx.getClass().getName(), ctx.getStart().getLine(), ss), nameResolver.getCurrentScopeId()));

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

		if (ctx.v.size() > 0) {
			List<VariableDefinition> intos = ctx.v.stream().map(vctx -> nameResolver.searchVariable(vctx.getText()))
					.collect(Collectors.toList());
			ss.setIntos(intos);
		}
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
			// must be here since it preserves "INTO" clause
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
	public PatchList visitSql_stmt(Sql_stmtContext ctx) {
		current_sql = stream.getText(ctx.getSourceInterval());
		Object o = visitChildren(ctx);
		if (o != null && o instanceof PatchList) {
			return (PatchList) o;
		}
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
			// if guessEnabled it will be created
			// if (targetTableDef == null) {
			// if (metaGuessEnabled) {
			// } else {
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

		return instbuffer.add(new Instruction(InstFuncHelper.insertOrUpdateFunc(cdefs, exprs),
				CollectDebugInfo(ctx.getClass().getName(), ctx.getStart().getLine(), cdefs, exprs),
				nameResolver.getCurrentScopeId()));

	}

	@Override
	public Object visitUpdate_stmt_from(Update_stmt_fromContext ctx) {
		visitChildren(ctx);
		return null;
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

	@Override
	public PatchList visitPlsql_statement(Plsql_statementContext ctx) {
		String current_statement = null;
		if (debugPlsql) {
			current_statement = getText(ctx);
		}
		PatchList patchList = (PatchList) ctx.plsql_statement_nolabel().accept(this);
		List<LabelContext> labels = ctx.label();
		if (labels.size() > 0) {
			if (patchList == null) {
				// a label so put a nop
				patchList = instbuffer.add(new Instruction(InstFuncHelper.NopFunc(),
						"NOP @" + ctx.getStart().getText() + ctx.getStart().getLine(), nameResolver.getCurrentScopeId()));
			}
			for (LabelContext labelctx : labels) {
				String labelName = (String) labelctx.label_name().getText();
				labelRecorder.getLabels().put(labelName, patchList.getStartInstruction());
			}
		}
		return patchList;
	}

	@Override
	public PatchList visitPlsql_statement_nolabel(Plsql_statement_nolabelContext ctx) {
		return (PatchList) visitChildren(ctx);
	}

	@Override
	public PatchList visitAssign_statement(Assign_statementContext ctx) {
		if (!(ctx.lvalue instanceof ExprObjectContext)) {
			throw new IllegalStateException(String.format("%s cannot be assigned", ctx.lvalue.getText()));
		}
		VariableDefinition lvalue = (VariableDefinition) funcOfExpr(ctx.lvalue.accept(this)).getValue().getParameters()
				.iterator().next();
		ExprAnalyzeResult rvalue = (ExprAnalyzeResult) ctx.rvalue.accept(this);
		return instbuffer.add(new Instruction(InstFuncHelper.assignExpression(lvalue, rvalue),
				CollectDebugInfo(ctx.getClass().getName(), ctx.getStart().getLine(), lvalue, rvalue),
				nameResolver.getCurrentScopeId()));
	}

	@Override
	public PatchList visitNull_statement(Null_statementContext ctx) {
		return instbuffer.add(new Instruction(InstFuncHelper.NopFunc(), "NOP NULL STMT@" + ctx.getStart().getLine(),
				nameResolver.getCurrentScopeId()));

	}

	@Override
	public PatchList visitCall_statement(Call_statementContext ctx) {
		StateFunc fn = funcOfExpr(ctx.expr().accept(this));
		return instbuffer.add(new Instruction(InstFuncHelper.callExpression(fn),
				CollectDebugInfo(ctx.getClass().getName(), ctx.getStart().getLine(), fn), nameResolver.getCurrentScopeId()));

	}

	@Override
	public PatchList visitMultiple_plsql_stmt_list(Multiple_plsql_stmt_listContext ctx) {
		PatchList patchList = new PatchList();
		PatchList prevpl = null;
		for (Plsql_statementContext plsqlctx : ctx.plsql_statement()) {
			PatchList currentpl = (PatchList) plsqlctx.accept(this);
			if (currentpl != null) {
				if (prevpl != null) {
					for (Instruction ins : prevpl.getNextList()) {
						ins.addNextInstruction(currentpl.getStartInstruction());
					}
				} else {
					patchList.setStartInstruction(currentpl.getStartInstruction());
				}
				prevpl = currentpl;
			}
		}
		if (prevpl != null) {
			patchList.setNextList(prevpl.getNextList());
			return patchList;
		} else {
			// no instruction put
			patchList = instbuffer.add(new Instruction(InstFuncHelper.NopFunc(),
					"NOP @" + ctx.getStart().getText() + ctx.getStart().getLine(), nameResolver.getCurrentScopeId()));
			return patchList;
		}
	}

	@Override
	public PatchList visitCase_statement(Case_statementContext ctx) {
		PatchList p = new PatchList();
		if (ctx.selector != null) {
			StateFunc fn = funcOfExpr(ctx.selector.accept(this));
			Instruction si = new Instruction(InstFuncHelper.branchCondFunc(fn),
					CollectDebugInfo(ctx.selector.getClass().getName(), ctx.selector.getStart().getLine(), fn),
					nameResolver.getCurrentScopeId());
			instbuffer.add(si);
			p.setStartInstruction(si);
		}

		Instruction prevBranch = null;
		for (int i = 0; i < ctx.selector_vals.size(); ++i) {
			StateFunc fn = funcOfExpr(ctx.selector_vals.get(i).accept(this));
			Instruction caseSelectorVal = new Instruction(InstFuncHelper.branchCondFunc(fn),
					CollectDebugInfo(ctx.selector_vals.get(i).getClass().getName(),
							ctx.selector_vals.get(i).getStart().getLine(), fn),
					nameResolver.getCurrentScopeId());
			instbuffer.add(caseSelectorVal);
			PatchList thenblock = (PatchList) ctx.then_stmts.get(i).accept(this);
			caseSelectorVal.addNextInstruction(thenblock.getStartInstruction());
			p.getNextList().addAll(thenblock.getNextList());
			if (prevBranch == null) {
				// first case
				if (p.getStartInstruction() == null) {
					// no selector
					p.setStartInstruction(caseSelectorVal);
				} else {
					p.getStartInstruction().addNextInstruction(caseSelectorVal);
				}
			} else {
				prevBranch.addNextInstruction(caseSelectorVal);
			}

			prevBranch = caseSelectorVal;
		}

		if (ctx.else_stmts != null) {
			PatchList elseblock = (PatchList) ctx.else_stmts.accept(this);
			prevBranch.addNextInstruction(elseblock.getStartInstruction());
			p.getNextList().addAll(elseblock.getNextList());
		} else {
			p.getNextList().add(prevBranch);
		}
		return p;
	}

	@Override
	public PatchList visitIf_statement(If_statementContext ctx) {
		// copied for "case" statement
		PatchList p = new PatchList();

		Instruction prevBranch = null;
		for (int i = 0; i < ctx.selector_vals.size(); ++i) {
			StateFunc fn = funcOfExpr(ctx.selector_vals.get(i).accept(this));
			Instruction caseSelectorVal = new Instruction(InstFuncHelper.branchCondFunc(fn),
					CollectDebugInfo(ctx.selector_vals.get(i).getClass().getName(),
							ctx.selector_vals.get(i).getStart().getLine(), fn),
					nameResolver.getCurrentScopeId());
			instbuffer.add(caseSelectorVal);
			PatchList thenblock = (PatchList) ctx.then_stmts.get(i).accept(this);
			caseSelectorVal.addNextInstruction(thenblock.getStartInstruction());
			p.getNextList().addAll(thenblock.getNextList());
			if (prevBranch == null) {
				p.setStartInstruction(caseSelectorVal);
			} else {
				prevBranch.addNextInstruction(caseSelectorVal);
			}

			prevBranch = caseSelectorVal;
		}

		if (ctx.else_stmts != null) {
			PatchList elseblock = (PatchList) ctx.else_stmts.accept(this);
			prevBranch.addNextInstruction(elseblock.getStartInstruction());
			p.getNextList().addAll(elseblock.getNextList());
		} else {
			p.getNextList().add(prevBranch);
		}
		return p;
	}

	@Override
	public PatchList visitBasic_loop_statement(Basic_loop_statementContext ctx) {
		labelRecorder.enterLoop();
		PatchList stmts = (PatchList) ctx.multiple_plsql_stmt_list().accept(this);
		Instruction first = stmts.getStartInstruction();
		for (Instruction topatch : stmts.getNextList()) {
			topatch.addNextInstruction(first);
		}
		PatchList p = new PatchList();
		p.setStartInstruction(first);

		p.getNextList().addAll(labelRecorder.getCurrentBreaks());
		labelRecorder.getCurrentContinues().forEach(i -> i.addNextInstruction(first));
		if (ctx.label_name() != null) {
			String label = ctx.label_name().getText();
			// TODO if duplicate label?
			Optional.ofNullable(labelRecorder.getBreakLabels().remove(label)).ifPresent(p.getNextList()::addAll);
			Optional.ofNullable(labelRecorder.getContinueLabels().remove(label))
					.ifPresent(list -> list.forEach(i -> i.addNextInstruction(first)));
		}

		labelRecorder.exitLoop();
		return p;
	}

	@Override
	public PatchList visitWhile_loop_statement(While_loop_statementContext ctx) {
		PatchList p = new PatchList();

		StateFunc whileCond = funcOfExpr(ctx.expr().accept(this));
		Instruction condInst = new Instruction(InstFuncHelper.branchCondFunc(whileCond),
				CollectDebugInfo(ctx.getClass().getName(), whileCond), nameResolver.getCurrentScopeId());
		instbuffer.add(condInst);
		p.setStartInstruction(condInst);

		labelRecorder.enterLoop();
		PatchList stmts = (PatchList) ctx.multiple_plsql_stmt_list().accept(this);

		condInst.addNextInstruction(stmts.getStartInstruction());
		for (Instruction topatch : stmts.getNextList()) {
			topatch.addNextInstruction(condInst);
		}

		p.getNextList().addAll(labelRecorder.getCurrentBreaks());
		p.getNextList().add(condInst);
		labelRecorder.getCurrentContinues().forEach(i -> i.addNextInstruction(condInst));
		if (ctx.label_name() != null) {
			String label = ctx.label_name().getText();
			// TODO if duplicate label?
			Optional.ofNullable(labelRecorder.getBreakLabels().remove(label)).ifPresent(p.getNextList()::addAll);
			Optional.ofNullable(labelRecorder.getContinueLabels().remove(label))
					.ifPresent(list -> list.forEach(i -> i.addNextInstruction(condInst)));
		}

		labelRecorder.exitLoop();
		return p;
	}

	@Override
	public Object visitContinue_statement(Continue_statementContext ctx) {
		PatchList p = null;
		Instruction inst = null;
		ExprContext exprctx = ctx.expr();
		if (exprctx != null) {
			StateFunc fn = funcOfExpr(exprctx.accept(this));
			inst = new Instruction(InstFuncHelper.branchCondFunc(fn),
					CollectDebugInfo("Continue WHEN @" + ctx.getStart().getLine(), fn), nameResolver.getCurrentScopeId());
			p = instbuffer.add(inst);

		} else {
			inst = new Instruction(InstFuncHelper.NopFunc(), "NOP Continue @" + ctx.getStart().getLine(),
					nameResolver.getCurrentScopeId());
			p = instbuffer.add(inst);
			p.setNextList(Collections.emptyList());
		}
		if (ctx.any_name() != null) {
			String label = ctx.any_name().getText();
			labelRecorder.getContinueLabels().computeIfAbsent(label, k -> new ArrayList<>()).add(inst);
		} else {
			labelRecorder.getCurrentContinues().add(inst);
		}
		return p;
	}

	@Override
	public PatchList visitExit_statement(Exit_statementContext ctx) {
		PatchList p = null;
		Instruction inst = null;
		ExprContext exprctx = ctx.expr();
		if (exprctx != null) {
			StateFunc fn = funcOfExpr(exprctx.accept(this));
			inst = new Instruction(InstFuncHelper.branchCondFunc(fn),
					CollectDebugInfo("EXIT WHEN @" + ctx.getStart().getLine(), fn), nameResolver.getCurrentScopeId());
			p = instbuffer.add(inst);

		} else {
			inst = new Instruction(InstFuncHelper.NopFunc(), "NOP EXIT @" + ctx.getStart().getLine(),
					nameResolver.getCurrentScopeId());
			p = instbuffer.add(inst);
			p.setNextList(Collections.emptyList());
		}
		if (ctx.any_name() != null) {
			String label = ctx.any_name().getText();
			labelRecorder.getBreakLabels().computeIfAbsent(label, k -> new ArrayList<>()).add(inst);
		} else {
			labelRecorder.getCurrentBreaks().add(inst);
		}
		return p;

	}

	@Override
	public Object visitFor_loop_statement(For_loop_statementContext ctx) {
		// TODO Auto-generated method stub
		return super.visitFor_loop_statement(ctx);
	}

	@Override
	public Object visitCreate_procedure(Create_procedureContext ctx) {
		/*
		 * parameter_declarations? (K_RETURN datatype)? ( K_IS | K_AS )
		 * declare_section? body
		 */

		FunctionDefinition functionDefinition = new FunctionDefinition();
		functionDefinition.setName(ctx.object_name().getText());
		List<ParameterDefinition> parameterDefinitions = (List<ParameterDefinition>) ctx.parameter_declarations().accept(this);
		functionDefinition.setParameters(parameterDefinitions);
		nameResolver.enterFunctionDefinition(ctx);
		instbuffer.enterFunctionDef(functionDefinition);
		parameterDefinitions.forEach(nameResolver::defineVariable);

		// for each out param create a same-name variable for it
		// for in out parameter put an assign . otherwise null;

		BodyContext bodyctx = ctx.body();
		nameResolver.startBlock(bodyctx);

		List<PatchList> instSeq = new ArrayList<>();

		for (ParameterDefinition pd : parameterDefinitions) {
			if (pd.isOut()) {
				VariableDefinition vd = new VariableDefinition();
				vd.setName(pd.getName());
				nameResolver.defineVariable(vd);
				if (pd.isIn()) {
					instSeq.add(instbuffer.add(new Instruction(InstFuncHelper.assignExpression(vd, new ExprAnalyzeResult(pd)),
							"INIT OUT PARAM " + pd.getName(), bodyctx)));
					// TODO parameter default value text.
				}
			}
		}

		if (ctx.declare_section() != null) {
			instSeq.addAll((List<PatchList>) ctx.declare_section().accept(this));
		}

		instSeq.add((PatchList) ctx.body().accept(this));

		for (int i = 0; i < instSeq.size() - 1; ++i) {
			for (Instruction inst : instSeq.get(i).getNextList()) {
				inst.addNextInstruction(instSeq.get(i + 1).getStartInstruction());
			}

		}

		Instruction programEnd = new Instruction(Function.identity(), "END OF FUNCTION", bodyctx);
		programEnd.setId(-1);

		instSeq.get(instSeq.size() - 1).getNextList().forEach(i -> i.addNextInstruction(programEnd));
		nameResolver.endBlock(bodyctx);
		nameResolver.exitFunctionDefinition(ctx);
		instbuffer.exitFunctionDef();
		return null;

	}


	@Override
	public List<PatchList> visitDeclare_section(Declare_sectionContext ctx) {
		List<PatchList> initSeqs = new ArrayList<>();
		for (Declare_section_onelineContext vd : ctx.declare_section_oneline()) {
			Object ret = vd.accept(this);
			if (ret != null && ret instanceof PatchList) {
				initSeqs.add((PatchList) ret);
			}
		}
		return initSeqs;
	}

	@Override
	public Object visitDeclare_section_oneline(Declare_section_onelineContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public ParameterDefinition visitParameter_declaration(Parameter_declarationContext ctx) {
		ParameterDefinition p = new ParameterDefinition();
		if (ctx.K_OUT() != null) {
			p.setOut(true);
			p.setIn(ctx.K_IN() != null);
		} else {
			p.setIn(true);
		}
		p.setName(ctx.any_name().getText());
		if (ctx.expr() != null) {
			p.setDefaultValuePresent(true);
		}

		return p;
	}

	@Override
	public List<ParameterDefinition> visitParameter_declarations(Parameter_declarationsContext ctx) {
		List<ParameterDefinition> defs = new ArrayList<>();
		for (Parameter_declarationContext vd : ctx.parameter_declaration()) {
			defs.add((ParameterDefinition) vd.accept(this));
		}
		return defs;
	}

	@Override
	public Object visitProcedure_or_function_declaration(Procedure_or_function_declarationContext ctx) {
		ctx.hashCode();
		throw new UnsupportedOperationException("FORWARD DECLARATION NOT SUPPORTED");
	}

	@Override
	public FunctionDefinition visitProcedure_or_function_definition(Procedure_or_function_definitionContext ctx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCursor_definition(Cursor_definitionContext ctx) {
		nameResolver.enterCursorDefinition(ctx);
		
		nameResolver.exitCursorDefinition(ctx);
		return null;
	}
	
	
	@Override
	public PatchList visitVariable_declaration(Variable_declarationContext ctx) {
		// COPY FROM visitParameter_declaration

		VariableDefinition def = new VariableDefinition();
		def.setName(ctx.any_name().getText());
		nameResolver.defineVariable(def);
		// def.setConst(ctx.K_CONSTANT() != null);
		ExprContext exprContext = ctx.expr();
		if (exprContext != null) {
			ExprAnalyzeResult e = (ExprAnalyzeResult) exprContext.accept(this);
			return instbuffer.add(new Instruction(InstFuncHelper.assignExpression(def, e),
					CollectDebugInfo(ctx.getClass().getName(), ctx.getStart().getLine(), def, e),
					nameResolver.getCurrentScopeId()));
		} else {
			return null;
		}
	}

	
	
	@Override
	public PatchList visitBody(BodyContext ctx) {
		return (PatchList) ctx.multiple_plsql_stmt_list().accept(this);
		// TODO exception handlers?
	}

	@Override
	public PatchList visitPlsql_block(Plsql_blockContext ctx) {

		if (ctx.declare_section() != null) {
			BodyContext bodyctx = ctx.body();
			nameResolver.startBlock(bodyctx);

			List<PatchList> instSeq = new ArrayList<>();

			instSeq.addAll((List<PatchList>) ctx.declare_section().accept(this));

			instSeq.add((PatchList) ctx.body().accept(this));
			for (int i = 0; i < instSeq.size() - 1; ++i) {
				for (Instruction inst : instSeq.get(i).getNextList()) {
					inst.addNextInstruction(instSeq.get(i + 1).getStartInstruction());
				}
			}

			nameResolver.endBlock(bodyctx);
			PatchList p = new PatchList();
			p.setStartInstruction(instSeq.get(0).getStartInstruction());
			p.getNextList().addAll(instSeq.get(instSeq.size() - 1).getNextList());
			return p;

		} else {
			return (PatchList) ctx.body().accept(this);
		}

	}

	@Override
	public Object visitParse(ParseContext ctx) {

		FunctionDefinition functionDefinition = new FunctionDefinition();
		functionDefinition.setName("ROOT");
		nameResolver.enterFunctionDefinition("ROOT");
		instbuffer.enterFunctionDef(functionDefinition);
		nameResolver.startBlock("ROOTBLOCK");
		visitChildren(ctx);
		nameResolver.endBlock("ROOTBLOCK");
		nameResolver.exitFunctionDefinition("ROOT");
		instbuffer.exitFunctionDef();
		return null;

	}

	@Override
	public Object visitReturn_statement(Return_statementContext ctx) {
		// TODO implement it
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
