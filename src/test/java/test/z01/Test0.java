package test.z01;

import org.junit.Test;

import io.github.jhg543.mellex.util.DatabaseVendor;

public class Test0 {

	public static void runTest(DatabaseVendor vendor, boolean guessEnabled, String filename) {
		DebugMain d = new DebugMain(vendor, guessEnabled, DebugMain.GetSql("ddl.sql") + DebugMain.GetSql(filename));
		d.run();
	}

	@Test
	public void test0() {
		runTest(DatabaseVendor.TERADATA, false, "s010.sql");
	}

	@Test(expected = RuntimeException.class)
	public void testGuess() {
		runTest(DatabaseVendor.TERADATA, false, "guess001.sql");
	}
}