package test.z01;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import io.github.jhg543.mellex.ASTHelper.plsql.SelectStmtData;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPLexer;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser;
import io.github.jhg543.mellex.listeners.PLSQLDataFlowVisitor;
import io.github.jhg543.mellex.listeners.flowmfp.InstBuffer;
import io.github.jhg543.mellex.listeners.flowmfp.Instruction;
import io.github.jhg543.mellex.listeners.flowmfp.VariableUsageState;
import io.github.jhg543.mellex.util.DatabaseVendor;
import io.github.jhg543.mellex.util.Misc;

public class DebugMain {

	private String GetSql(String inputfile) {
		try {
			Path f = (new File(this.getClass().getClassLoader().getResource("test/z01/sql/" + inputfile).toURI())).toPath();
			return Misc.trimPerlScript(f).toUpperCase();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private InstBuffer generateInst(String sql) {
		InstBuffer buffer = new InstBuffer();
		// antlr parse
		AtomicInteger errorCount = new AtomicInteger();
		ANTLRInputStream in = new ANTLRInputStream(sql);
		DefaultSQLPLexer lexer = new DefaultSQLPLexer(in);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		DefaultSQLPParser parser = new DefaultSQLPParser(tokens);
		ParseTree tree = null;
		tree = parser.parse();
		PLSQLDataFlowVisitor visitor = new PLSQLDataFlowVisitor(tokens, buffer, DatabaseVendor.TERADATA);
		visitor.visit(tree);

		return buffer;
	}

	private void printInstBuffer(InstBuffer buffer) {
		for (Instruction<VariableUsageState> inst : buffer.getInstbuffer()) {
			Object obj = inst.getDebugInfo();
			if (obj instanceof SelectStmtData) {
				SelectStmtData ss = (SelectStmtData) obj;
				System.out.println(ss.toString());
			}
		}
	}

	
	public void debug() {
		printInstBuffer(generateInst(GetSql("s010.sql")));
	}
	
	public static void main(String[] args) throws Exception {
		DebugMain d= new DebugMain();
		d.debug();
	}
}
