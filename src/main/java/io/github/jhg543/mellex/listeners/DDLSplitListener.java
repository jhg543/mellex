package io.github.jhg543.mellex.listeners;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.function.BiConsumer;

import org.antlr.v4.runtime.TokenStream;

import io.github.jhg543.mellex.antlrparser.DefaultSQLBaseListener;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Sql_stmtContext;

public class DDLSplitListener extends DefaultSQLBaseListener {

	private TokenStream stream;
	private BiConsumer<String, String> results;

	public DDLSplitListener(TokenStream stream, BiConsumer<String, String> results) {
		this.stream = stream;
		this.results = results;
	}

	@Override
	public void exitSql_stmt(Sql_stmtContext ctx) {

		if (ctx.create_table_stmt() != null || ctx.create_view_stmt() != null) {
			String tablename = null;
			if (ctx.create_table_stmt() != null) {
				tablename = ctx.create_table_stmt().obj.getText();
			}
			if (ctx.create_view_stmt() != null) {
				tablename = ctx.create_view_stmt().obj.getText();
			}

			results.accept(tablename, stream.getText(ctx.getSourceInterval()));

		}

	}

}
