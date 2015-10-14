package io.github.jhg543.mellex.operation;

import io.github.jhg543.mellex.ASTHelper.GlobalSettings;
import io.github.jhg543.mellex.ASTHelper.ObjectName;
import io.github.jhg543.mellex.ASTHelper.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.SubQuery;
import io.github.jhg543.mellex.antlrparser.DefaultSQLBaseListener;
import io.github.jhg543.mellex.antlrparser.DefaultSQLLexer;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Sql_stmtContext;
import io.github.jhg543.mellex.inputsource.BasicTableDefinitionProvider;
import io.github.jhg543.mellex.inputsource.TableDefinitionProvider;
import io.github.jhg543.mellex.listeners.ColumnDataFlowListener;
import io.github.jhg543.mellex.util.Misc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringEdgePrinter {
	private static final Logger log = LoggerFactory.getLogger(StringEdgePrinter.class);
	private static int ERR_NOSQL = 1;
	private static int ERR_PARSE = 2;
	private static int ERR_SEMANTIC = 3;
	private static int ERR_OK = 0;
	private static int printSingleFile(Path srcdir, Path dstdir, int scriptNumber, TableDefinitionProvider tp) {
		String srcHash = Integer.toHexString(srcdir.hashCode());
		try {
			Files.createDirectories(dstdir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try (PrintWriter err = new PrintWriter(dstdir.resolve("log").toAbsolutePath().toString(), "utf-8")) {

			String sql = Misc.trimPerlScript(srcdir);
			if (sql == null) {
				err.println("Can't extract sql from file " + srcdir.toString());
				return ERR_NOSQL;
			}
			try (PrintWriter writer = new PrintWriter(dstdir.resolve("sql").toAbsolutePath().toString(), "utf-8")) {
				writer.append(sql);
			}

			AtomicInteger errorCount = new AtomicInteger();
			ANTLRInputStream in = new ANTLRInputStream(sql);
			DefaultSQLLexer lexer = new DefaultSQLLexer(in);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			DefaultSQLParser parser = new DefaultSQLParser(tokens);
			parser.removeErrorListeners();
			parser.addErrorListener(new BaseErrorListener() {
				@Override
				public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
						String msg, RecognitionException e) {
					err.println("line" + line + ":" + charPositionInLine + "at" + offendingSymbol + ":" + msg);
					errorCount.incrementAndGet();
				}

			});
			err.println("-------Parse start---------");
			ParseTree tree = null;
			try {
				tree = parser.parse();
				if (errorCount.get() > 0) {
					return ERR_PARSE;
				}
			} catch (Exception e) {
				e.printStackTrace(err);
				return ERR_PARSE;
			}

			err.println("-------Parse OK, Semantic Analysis start --------");
			ParseTreeWalker w = new ParseTreeWalker();

			try {
				ColumnDataFlowListener s = new ColumnDataFlowListener(tp, tokens);
				w.walk(s, tree);
			} catch (Exception e) {
				e.printStackTrace(err);
				return ERR_SEMANTIC;
			}

			err.println("-------Semantic OK, Writing result --------");
			try (PrintWriter out = new PrintWriter(dstdir.resolve("out").toAbsolutePath().toString(), "utf-8")) {
				out.println("ScriptID StmtID StmtType DestCol SrcCol ConnectionType");
				String template = "%d %d %s %s.%s %s.%s %d\n";
				DefaultSQLBaseListener pr = new DefaultSQLBaseListener() {

					int stmtNumber = 0;

					@Override
					public void exitSql_stmt(Sql_stmtContext ctx) {

						super.exitSql_stmt(ctx);
						String stmtType = null;
						SubQuery q = null;
						if (ctx.insert_stmt() != null) {
							stmtType = "I";
							q = ctx.insert_stmt().stmt;
						}

						if (ctx.create_table_stmt() != null) {
							if (ctx.create_table_stmt().insert != null) {
								stmtType = "C";
								q = ctx.create_table_stmt().insert;
							}
						}

						if (ctx.create_view_stmt() != null) {
							stmtType = "V";
							q = ctx.create_view_stmt().insert;
						}

						if (ctx.update_stmt() != null) {
							stmtType = "U";
							q = ctx.update_stmt().q;
						}
						if (q != null) {

							Set<String> vts = tp.getVolatileTables().keySet();
							String dstTable = q.dbobj.toDotString();
							if (vts.contains(dstTable)) {
								dstTable = "VT_" + srcHash + "_" + dstTable;
							}

							for (ResultColumn c : q.columns) {
								for (ObjectName srcname : c.inf.direct) {
									String srcTable = srcname.toDotStringExceptLast();
									if (vts.contains(srcTable)) {
										srcTable = "VT_" + srcHash + "_" + srcTable;
									}
									out.append(String.format(template, scriptNumber, stmtNumber, stmtType, dstTable, c.name,
											srcTable, srcname.toDotStringLast(), 1));
								}
								for (ObjectName srcname : c.inf.indirect) {
									String srcTable = srcname.toDotStringExceptLast();
									if (vts.contains(srcTable)) {
										srcTable = "VT_" + srcHash + "_" + srcTable;
									}
									out.append(String.format(template, scriptNumber, stmtNumber, stmtType, dstTable, c.name,
											srcTable, srcname.toDotStringLast(), 0));
								}

							}
						} else {
							log.warn("query null for sm " + stmtNumber);
						}

						stmtNumber++;
					}

				};

				w.walk(pr, tree);
			}

			tp.clearVolatileTables();
			err.println("-------Success --------");
			return 0;
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static void printStringEdge(Path srcdir, Path dstdir, int scriptNumberStart, boolean caseSensitive) {

		GlobalSettings.setCaseSensitive(caseSensitive);
		AtomicInteger scriptNumber = new AtomicInteger(scriptNumberStart);
		TableDefinitionProvider tp = new BasicTableDefinitionProvider(Misc::nameSym);
		try {
			Files.walk(srcdir)
					.filter(x -> Files.isRegularFile(x)
							&& (x.getFileName().toString().toLowerCase().endsWith(".sql") || x.getFileName().toString()
									.toLowerCase().endsWith(".pl"))
							&& x.toString().toUpperCase().endsWith("BIN\\" + x.getFileName().toString().toUpperCase()))
					.sorted().forEach(path -> {
						String srcHash = Integer.toHexString(path.hashCode());
						Path workdir = dstdir.resolve(path.getFileName()).resolve(srcHash);
						printSingleFile(path, workdir, scriptNumber.getAndIncrement(), tp);
					});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Exception {
		printStringEdge(Paths.get("d:/dataflow/work2/script"), Paths.get("d:/dataflow/work2/res"), 0, false);
	}
}