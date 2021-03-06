package io.github.jhg543.mellex.operation;

import io.github.jhg543.mellex.antlrparser.DefaultSQLLexer;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser;
import io.github.jhg543.mellex.inputsource.TableDefinitionProvider;
import io.github.jhg543.mellex.listeners.ColumnDataFlowListener;
import io.github.jhg543.mellex.listeners.PrintListener;
import io.github.jhg543.mellex.listeners.TableDataFlowListener;
import io.github.jhg543.mellex.listeners.TableDependencyListener;
import io.github.jhg543.mellex.session.OutputGraphSession;
import io.github.jhg543.mellex.session.ScriptBlockTableDependency;
import io.github.jhg543.mellex.session.SortDependencySession;
import io.github.jhg543.mellex.session.TableDependencySession;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class ObjectList {

	/**
	 * @param sql
	 * @param result
	 *            [0] = provide , [1] = consume , [2] = consume(Select *)
	 * @param errors
	 * @return
	 */
	public static void viewTableDependency(String sql, Consumer<ScriptBlockTableDependency> result, Consumer<Exception> errors) {

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
				errors.accept(new RuntimeException("line" + line + ":" + charPositionInLine + "at" + offendingSymbol + ":"
						+ msg));
				errorCount.incrementAndGet();
			}

		});
		try {
			ParseTree tree = parser.parse();
			ParseTreeWalker w = new ParseTreeWalker();
			TableDependencySession session = new TableDependencySession();
			TableDependencyListener s = new TableDependencyListener(session);
			w.walk(s, tree);
			result.accept(session.getRecord());

		} catch (Exception e) {
			errors.accept(e);
		}
	}

	public static List<String> sortTableDependency(Map<String, ScriptBlockTableDependency> source) {
		SortDependencySession s = new SortDependencySession();
		for (Entry<String, ScriptBlockTableDependency> e : source.entrySet()) {
			s.addScript(e.getKey(), e.getValue());
		}
		return s.sort(null);
	}

	public static void viewColumnImpact(String id, String sql, TableDefinitionProvider provider, OutputGraphSession outsession,
			PrintWriter out, Consumer<Exception> errors) {

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
				errors.accept(new RuntimeException("line" + line + ":" + charPositionInLine + "at" + offendingSymbol + ":"
						+ msg));
				errorCount.incrementAndGet();
			}

		});
		try {
			ParseTree tree = parser.parse();
			ParseTreeWalker w = new ParseTreeWalker();
			outsession.newVolatileNamespace(id);

			ColumnDataFlowListener s = new ColumnDataFlowListener(provider,tokens);
			w.walk(s, tree);
			provider.getVolatileTables().keySet().stream().forEach(x -> outsession.putVolatileTable(x));
			PrintListener p = new PrintListener(out, tokens, outsession);
			w.walk(p, tree);
			provider.clearVolatileTables();
		} catch (Exception e) {
			errors.accept(e);
		}
	}
	
	public static void viewTableImpact(String id, String sql, TableDefinitionProvider provider, OutputGraphSession outsession,
			PrintWriter out, Consumer<Exception> errors) {

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
				errors.accept(new RuntimeException("line" + line + ":" + charPositionInLine + "at" + offendingSymbol + ":"
						+ msg));
				errorCount.incrementAndGet();
			}

		});
		try {
			ParseTree tree = parser.parse();
			ParseTreeWalker w = new ParseTreeWalker();
			outsession.newVolatileNamespace(id);

			TableDataFlowListener s = new TableDataFlowListener(provider,tokens,outsession);
			w.walk(s, tree);
			provider.getVolatileTables().keySet().stream().forEach(x -> outsession.putVolatileTable(x));
			provider.clearVolatileTables();
		} catch (Exception e) {
			errors.accept(e);
		}
	}
}
