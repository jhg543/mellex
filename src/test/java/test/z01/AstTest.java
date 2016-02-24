package test.z01;

import io.github.jhg543.mellex.operation.StringEdgePrinter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AstTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { { "s009.sql", new int[] { 1, 0, 0, 0 } } });
	}

	@Parameter(value = 0)
	public String inputfile;

	@Parameter(value = 1)
	public int[] expected;

	@Test
	public void test() throws IOException, URISyntaxException {
		// do not run test in jar since getresource.touri don't work when in jar
		Path f = (new File(this.getClass().getClassLoader().getResource("test/z01/sql/" + inputfile).toURI())).toPath();
		Path srcdir = folder.newFolder().toPath();
		Path dstdir = folder.newFolder().toPath();
		Files.copy(f, srcdir.resolve(f.getFileName()));
		int[] result = StringEdgePrinter.printStringEdge(srcdir, dstdir, x -> Files.isRegularFile(x)
				&& x.getFileName().toString().toLowerCase().endsWith(".sql"), 0, false);
		Files.walk(dstdir).filter(x -> x.getFileName().toString().equals("log")).forEach(x -> {
			try {
				System.out.println(x);
				System.out.write(Files.readAllBytes(x));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		Assert.assertArrayEquals(result, expected);

	}
}
