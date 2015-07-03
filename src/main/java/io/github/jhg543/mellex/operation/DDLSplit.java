package io.github.jhg543.mellex.operation;

import io.github.jhg543.mellex.antlrparser.DefaultSQLLexer;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser;
import io.github.jhg543.mellex.listeners.DDLSplitListener;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * Statements in a DDL file exported from DBMS, will probably be out of order (
 * e.g. create view b as select * from a; create table a (...); ) so split DDLs
 * into smaller files and reorder them.
 * 
 * @author jhg543
 */
public class DDLSplit {

	/**
	 * @param ddl -- input ddl script
	 * @param result -- output parameter. accept method could be invoked mutiple times.
	 * @param err -- errors
	 * @return error count
	 */
	public static void splitDDL(String ddl, BiConsumer<String, String> results, Consumer<Exception> errors) {

		AtomicInteger errorCount = new AtomicInteger();
		ANTLRInputStream in = new ANTLRInputStream(ddl);
		DefaultSQLLexer lexer = new DefaultSQLLexer(in);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		DefaultSQLParser parser = new DefaultSQLParser(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
					String msg, RecognitionException e) {
				// List<String> stack = ((Parser)
				// recognizer).getRuleInvocationStack();
				// Collections.reverse(stack);
				// err.println("rulestack:" + stack);
				// err.println("line" + line + ":" + charPositionInLine + "at" +
				// offendingSymbol + ":" + msg);
				errors.accept(new RuntimeException("line" + line + ":" + charPositionInLine + "at" + offendingSymbol + ":"
						+ msg));
				errorCount.incrementAndGet();
			}

		});
		try {
			ParseTree tree = parser.parse();
			ParseTreeWalker w = new ParseTreeWalker();

			DDLSplitListener s = new DDLSplitListener(tokens, results);
			w.walk(s, tree);

		} catch (Exception e) {
			errors.accept(e);
		}
	}

}
