package io.github.jhg543.mellex.listeners;

import java.io.PrintWriter;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.TokenStream;

import io.github.jhg543.mellex.ASTHelper.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.SubQuery;
import io.github.jhg543.mellex.antlrparser.DefaultSQLBaseListener;
import io.github.jhg543.mellex.antlrparser.DefaultSQLParser.Sql_stmtContext;

public class PrintListener extends DefaultSQLBaseListener {

	private PrintWriter out;
	private TokenStream stream;

	public PrintListener(PrintWriter out, TokenStream stream) {
		this.out = out;
		this.stream = stream;
	}

	private void printtext(RuleContext ctx) {
		out.println("\"\"\"");
		out.print(stream.getText(ctx.getSourceInterval()));
		out.println(";");
		out.println("\"\"\"");
	}

	@Override
	public void exitSql_stmt(Sql_stmtContext ctx) {

		super.exitSql_stmt(ctx);
		if (ctx.insert_stmt() != null) {
			out.println("SOI---------------");
			printtext(ctx.insert_stmt());
			printSubQuery(ctx.insert_stmt().stmt);
			out.println("EOI---------------");
		}

		if (ctx.create_table_stmt() != null) {
			out.println("SOT---------------");
			printtext(ctx.create_table_stmt());
			printSubQuery(ctx.create_table_stmt().stmt);
			if (ctx.create_table_stmt().insert != null) {
				out.println("IT+++++++++++++");
				printSubQuery(ctx.create_table_stmt().insert);
			} else {
				out.println("ET-------------");
			}
			out.println("EOT---------------");
		}

		if (ctx.create_view_stmt() != null) {
			out.println("SOV---------------");
			printtext(ctx.create_view_stmt());
			printSubQuery(ctx.create_view_stmt().stmt);
			out.println("IT+++++++++++++");
			printSubQuery(ctx.create_view_stmt().insert);
			out.println("EOV---------------");

		}

		if (ctx.update_stmt() != null) {
			out.println("SOU---------------");
			printtext(ctx.update_stmt());
			printSubQuery(ctx.update_stmt().q);
			out.println("EOU---------------");
		}
	}

	private void printSubQuery(SubQuery q) {
		if (q == null) {
			return;
		}
		out.println(q.dbobj);
		for (ResultColumn c : q.columns) {
			out.print(c.name);
			out.print('\t');
			out.println(c.inf);
		}
	}

}
