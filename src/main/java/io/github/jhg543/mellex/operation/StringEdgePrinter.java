package io.github.jhg543.mellex.operation;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import io.github.jhg543.mellex.ASTHelper.*;
import io.github.jhg543.mellex.antlrparser.DefaultSQLBaseListener;
import io.github.jhg543.mellex.antlrparser.DefaultSQLLexer;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Sql_stmtContext;
import io.github.jhg543.mellex.inputsource.BasicTableDefinitionProvider;
import io.github.jhg543.mellex.inputsource.TableDefinitionProvider;
import io.github.jhg543.mellex.listeners.ColumnDataFlowListener;
import io.github.jhg543.mellex.util.Misc;
import io.github.jhg543.nyallas.graphmodel.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class StringEdgePrinter {
	private static final Logger log = LoggerFactory.getLogger(StringEdgePrinter.class);
	private static int ERR_NOSQL = 1;
	private static int ERR_PARSE = 2;
	private static int ERR_SEMANTIC = 3;
	private static int ERR_OK = 0;

	private static int printSingleFile(Path srcdir, Path dstdir, int scriptNumber, TableDefinitionProvider tp) {
		
		// generate a hash to mark vt table names
		String srcHash = Integer.toHexString(srcdir.hashCode());
	
		// create destination dir
		try {
			Files.createDirectories(dstdir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		

		try (PrintWriter err = new PrintWriter(dstdir.resolve("log").toAbsolutePath().toString(), "utf-8")) {

			// trim perl code
			String sql = Misc.trimPerlScript(srcdir, StandardCharsets.UTF_8);
			if (sql == null) {
				err.println("Can't extract sql from file " + srcdir.toString());
				return ERR_NOSQL;
			}
			
			// log actual sql statement ( for corrent line number ..)
			try (PrintWriter writer = new PrintWriter(dstdir.resolve("sql").toAbsolutePath().toString(), "utf-8")) {
				writer.append(sql);
			}

			// antlr parse
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

			// Remove volatile tables
			
			VolatileTableRemover graph = new VolatileTableRemover();
			// DAG dag = new DAG();
			// ZeroBasedStringIdGenerator ids = new
			// ZeroBasedStringIdGenerator();
			Map<String, Vertex<String, Integer>> vmap = new HashMap<>();
			
			// Output result and initialize volatile tables removal process
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

							// what's vt's scope?
							Set<String> vts = tp.getVolatileTables().keySet();
							String dstTable = q.dbobj.toDotString();
							boolean isDstVT = vts.contains(dstTable);
							if (isDstVT) {
								dstTable = "VT_" + srcHash + "_" + dstTable;
							}

							for (ResultColumn c : q.columns) {
								for (InfSource source : c.inf.getSources()) {
									ObjectName srcname = source.getSourceObject();
									String srcTable = srcname.toDotStringExceptLast();

									boolean isSrcVT = vts.contains(srcTable);
									if (isSrcVT) {
										srcTable = "VT_" + srcHash + "_" + srcTable;
									}
									out.append(String.format(template, scriptNumber, stmtNumber, stmtType, dstTable, c.name,
											srcTable, srcname.toDotStringLast(), source.getConnectionType().getMarker()));

									// collapse volatile table
									String dst = dstTable + "." + c.name;
									String src = srcTable + "." + srcname.toDotStringLast();
									// Integer dstnum = ids.queryNumber(dst);
									// Integer srcnum = ids.queryNumber(src);
									Vertex<String, Integer> srcv;
									srcv = vmap.get(src);
									if (srcv == null) {
										srcv = graph.addVertex(BasicVertex::new);
										vmap.put(src, srcv);
										srcv.setVertexData(src);

										if (isSrcVT) {
											srcv.setMarker(0);
										}
									}
									Vertex<String, Integer> dstv;
									dstv = vmap.get(dst);
									if (dstv == null) {
										dstv = graph.addVertex(BasicVertex::new);
										vmap.put(dst, dstv);
										dstv.setVertexData(dst);

										if (isDstVT) {
											dstv.setMarker(0);
										}
									}
									Edge<String, Integer> edge = new BasicEdge<String, Integer>(srcv, dstv);
									edge.setEdgeData(source.getConnectionType().getMarker());
									graph.addEdge(edge);

								}

							}
						} else {
							// log.warn("query null for sm " + stmtNumber);
						}

						stmtNumber++;
					}

				};

				w.walk(pr, tree);
			}

			// Int2ObjectMap<Node> collapsed = dag.collapse(scriptNumber);
			
			graph.remove();
			
			// write result (with volatile tables removed)
			try (PrintWriter out = new PrintWriter(dstdir.resolve("novt").toAbsolutePath().toString(), "utf-8")) {
				out.println("scriptid,dstsch,dsttbl,dstcol,srcsch,srctbl,srccol,contype");
				String template = "%d,%s,%s,%s,%s,%s,%s,%d\n";
				for (Vertex<String, Integer> v : graph.getVertexes()) {
					for (Edge<String, Integer> e : v.getOutgoingEdges()) {
						String dst = e.getTarget().getVertexData();
						String src = e.getSource().getVertexData();
						List<String> t1 = Splitter.on('.').splitToList(dst);
						if (t1.size() == 2) {
							t1 = new ArrayList<String>(t1);
							t1.add(0, "3X_NOSCHEMA_" + scriptNumber);
						}
						List<String> t2 = Splitter.on('.').splitToList(src);
						if (t2.size() == 2) {
							t2 = new ArrayList<String>(t1);
							t2.add(0, "3X_NOSCHEMA_" + scriptNumber);
						}
						out.append(String.format(template, scriptNumber, t1.get(0), t1.get(1), t1.get(2), t2.get(0), t2.get(1),
								t2.get(2), e.getEdgeData()));
					}
				}
			}

			tp.clearVolatileTables();
			err.println("-------Success --------");
			return 0;
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static int[] printStringEdge(Path srcdir, Path dstdir, Predicate<Path> filefilter, int scriptNumberStart,
			boolean caseSensitive) {
		// ensure directories exist
		Preconditions.checkState(Files.isDirectory(srcdir));
		try {
			Files.createDirectories(dstdir);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
		
		// set up variables
		GlobalSettings.setCaseSensitive(caseSensitive);
		AtomicInteger scriptNumber = new AtomicInteger(scriptNumberStart);
		TableDefinitionProvider tp = new BasicTableDefinitionProvider(Misc::nameSym);
		int[] stats = new int[10];
		
		// open global output files
		try (PrintWriter out = new PrintWriter(dstdir.resolve("stats").toAbsolutePath().toString(), "utf-8");
				PrintWriter cols = new PrintWriter(dstdir.resolve("cols").toAbsolutePath().toString(), "utf-8");
				PrintWriter numbers = new PrintWriter(dstdir.resolve("number").toAbsolutePath().toString(), "utf-8")) {
			
			// for each file
			Files.walk(srcdir).filter(filefilter).sorted().forEach(path -> {
				int sn = scriptNumber.getAndIncrement();
				numbers.println("" + sn + " " + path.toString());
				String srcHash = Integer.toHexString(path.hashCode());
				Path workdir = dstdir.resolve(path.getFileName()).resolve(srcHash);
				
				// deal with single files.
				int retcode = printSingleFile(path, workdir, sn, tp);
				if (retcode > 0) {
					out.println(String.format("%s %d %d", path.toString(), retcode, sn));
				}
				stats[retcode]++;
			});

			out.println("OK=" + stats[ERR_OK]);
			out.println("NOSQL=" + stats[ERR_NOSQL]);
			out.println("PARSE=" + stats[ERR_PARSE]);
			out.println("SEMANTIC=" + stats[ERR_SEMANTIC]);
			tp.getPermanentTables().forEach((name, stmt) -> {
				stmt.columns.forEach(colname -> cols.println(name + "." + colname.name));
			});
			return stats;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Exception {
		Predicate<Path> filefilter = x -> Files.isRegularFile(x)
				&& (x.getFileName().toString().toLowerCase().endsWith(".sql") || x.getFileName().toString().toLowerCase()
						.endsWith(".pl"))
				&& x.toString().toUpperCase().endsWith("BIN\\" + x.getFileName().toString().toUpperCase());
		// printStringEdge(Paths.get("d:/dataflow/work1/script/mafixed"),
		// Paths.get("d:/dataflow/work2/mares"), filefilter, 0, false);
		printStringEdge(Paths.get("d:/dataflow/work1/debug"), Paths.get("d:/dataflow/work2/debugres"), filefilter, 0, false);
		// printStringEdge(Paths.get("d:/dataflow/work1/f1/sor"),
		// Paths.get("d:/dataflow/work2/result2/sor"), filefilter, 0, false);
	}
}
