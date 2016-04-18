package test.z01;

import io.github.jhg543.mellex.antlrparser.DefaultSQLPLexer;
import io.github.jhg543.mellex.antlrparser.DefaultSQLPParser;
import io.github.jhg543.mellex.listeners.PLSQLDataFlowVisitor;
import io.github.jhg543.mellex.listeners.flowmfp.InstBuffer;
import io.github.jhg543.mellex.util.DatabaseVendor;
import io.github.jhg543.mellex.util.Misc;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class DebugMain {

	private DatabaseVendor vendor;
	private boolean guessEnabled;
	private String sql;

	public static String GetSql(String inputfile) {
		try {
			Path f = (new File(DebugMain.class.getClassLoader().getResource("test/z01/sql/" + inputfile).toURI())).toPath();
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
		PLSQLDataFlowVisitor visitor = new PLSQLDataFlowVisitor(tokens, buffer, vendor, guessEnabled);
		visitor.visit(tree);
		return buffer;
	}

	private void printInstBuffer(InstBuffer buffer) {
//		for (Instruction inst : buffer.getInstbuffer()) {
//			Object obj = inst.getDebugInfo();
//			System.out.println(obj.toString());
//			// if (obj instanceof SelectStmtData) {
//			// SelectStmtData ss = (SelectStmtData) obj;
//			// System.out.println(ss.toString());
//			// }
//		}
	}

	public DebugMain(DatabaseVendor vendor, boolean guessEnabled, String sql) {
		super();
		this.vendor = vendor;
		this.guessEnabled = guessEnabled;
		this.sql = sql;
	}

	public void run() {
		printInstBuffer(generateInst(sql));
	}

	public static void main(String[] args) throws Exception {

		//DebugMain d = new DebugMain(DatabaseVendor.TERADATA, true, GetSql("ddl.sql") + GetSql("p001.sql"));
		DebugMain d = new DebugMain(DatabaseVendor.ORACLE, false, GetSql("p005.sql"));
		d.run();
	}
}
