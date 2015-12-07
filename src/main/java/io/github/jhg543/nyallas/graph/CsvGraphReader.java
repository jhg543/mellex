package io.github.jhg543.nyallas.graph;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.github.jhg543.mellex.util.Misc;
import io.github.jhg543.nyallas.etl.algorithm.LineageFinder;
import io.github.jhg543.nyallas.etl.algorithm.SimRank;
import io.github.jhg543.nyallas.etl.ve.EdgeETL;
import io.github.jhg543.nyallas.etl.ve.VertexDBCol;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DirectedPseudograph;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

public class CsvGraphReader {

	private Map<String, VertexDBCol> mp;
	DirectedGraph<VertexDBCol, EdgeETL> g;

	private VertexDBCol getTableVertex(String s, String t, String c) {
		s = s.intern();
		t = t.intern();
		c = c.intern();
		StringBuilder fqnsb = new StringBuilder();
		fqnsb.append(s);
		fqnsb.append('.');
		fqnsb.append(t);
		String fqn = fqnsb.toString().intern();
		VertexDBCol v = mp.get(fqn);
		if (v == null) {
			v = new VertexDBCol();
			v.setColumn(t);
			v.setTable(t);
			v.setSchema(s);
			v.setFqn(fqn);
			mp.put(fqn, v);
			g.addVertex(v);
		}
		return v;
	}

	public void readTableCsv(Path path) {
		g = new DefaultDirectedGraph<VertexDBCol, EdgeETL>(EdgeETL.class);
		mp = new HashMap<String, VertexDBCol>();
		try {
			Files.readAllLines(path).forEach(line -> {
				List<String> linelist = Splitter.on(',').splitToList(line);
				VertexDBCol end = getTableVertex(Misc.schemaSym(linelist.get(1)), linelist.get(2), linelist.get(2));
				VertexDBCol start = getTableVertex(Misc.schemaSym(linelist.get(4)), linelist.get(5), linelist.get(5));
				EdgeETL edge = g.getEdge(start, end);
				if (edge == null) {
					edge = g.addEdge(start, end);
					edge.setScriptname("");
				}
				edge.setScriptname(linelist.get(0));
				edge.setConntype(linelist.get(7));

			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private VertexDBCol getVertex(String s, String t, String c) {
		s = s.intern();
		t = t.intern();
		c = c.intern();
		StringBuilder fqnsb = new StringBuilder();
		fqnsb.append(s);
		fqnsb.append('.');
		fqnsb.append(t);
		fqnsb.append('.');
		fqnsb.append(c);
		String fqn = fqnsb.toString().intern();
		VertexDBCol v = mp.get(fqn);
		if (v == null) {
			v = new VertexDBCol();
			v.setColumn(c);
			v.setTable(t);
			v.setSchema(s);
			v.setFqn(fqn);
			mp.put(fqn, v);
			g.addVertex(v);
		}
		return v;
	}

	public void readCsv(Path path) {

		g = new DirectedPseudograph<VertexDBCol, EdgeETL>(EdgeETL.class);
		mp = new HashMap<String, VertexDBCol>();
		try {
			Files.readAllLines(path).forEach(line -> {
				List<String> linelist = Splitter.on(',').splitToList(line);
				VertexDBCol end = getVertex(Misc.schemaSym(linelist.get(1)), linelist.get(2), linelist.get(3));
				VertexDBCol start = getVertex(Misc.schemaSym(linelist.get(4)), linelist.get(5), linelist.get(6));
				EdgeETL edge = g.addEdge(start, end);
				edge.setScriptname(linelist.get(0));
				edge.setConntype(linelist.get(7));

			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void outputEdges(List<EdgeETL> es, Path path) {
		try (PrintWriter writer = new PrintWriter(path.toFile())) {

			es.stream().forEach(y -> {
				writer.append(y.getScriptname());
				writer.append(',');
				writer.append(y.getTarget().getSchema());
				writer.append(',');
				writer.append(y.getTarget().getTable());
				writer.append(',');
				writer.append(y.getTarget().getColumn());
				writer.append(',');
				writer.append(y.getSource().getSchema());
				writer.append(',');
				writer.append(y.getSource().getTable());
				writer.append(',');
				writer.append(y.getSource().getColumn());
				writer.append(',');
				writer.append(y.getConntype());
				writer.println();
			});
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	};

	public void gensvg() {

		try {
			CsvGraphReader r = new CsvGraphReader();
			Path src = Paths.get("d:/z1.csv");
			Path filteredfile = Paths.get("d:/zr.csv");
			Path layoutExecutable = Paths.get("d:/zrlayout.exe");
			Path layoutoutput = Paths.get("d:/manual_graph.svg");
			Path layoutoutputinversed = Paths.get("d:/manual_graph2.svg");
			r.readTableCsv(src);
			System.out.println("read complate");
			List<VertexDBCol> vs = r.mp.values().stream().filter(x -> x.getTable().equals("T80_LIAB_CORP_ACCT_IDX_DD"))
					.collect(Collectors.toList());
			Predicate<EdgeETL> test = x -> {
				// System.out.println(x.getSource().getFqn());
				// System.out.println(x.getTarget().getFqn());
				return !x.getSource().getSchema().equals(x.getTarget().getSchema());
			};

			System.out.println("find complate");
			List<EdgeETL> es = LineageFinder.find(r.g, vs, test);
			r.outputEdges(es, filteredfile);
			ProcessBuilder processbuilder = new ProcessBuilder(ImmutableList.of(layoutExecutable.toString(),
					filteredfile.toString(), layoutoutput.toString(), "1000"));
			processbuilder.redirectOutput(Redirect.INHERIT);
			Process process = processbuilder.start();
			boolean nottimeout = process.waitFor(300, TimeUnit.SECONDS);
			if (process.isAlive()) {
				process.destroyForcibly();
			}
			if (nottimeout) {
				SVGInverter.invertSVG(layoutoutput, layoutoutputinversed);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static long sizeOf(Object o)  throws Exception
	{
		Field ff = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		ff.setAccessible(true);
		sun.misc.Unsafe u = (sun.misc.Unsafe) ff.get(null);
		
		   
		    HashSet<Field> fields = new HashSet<Field>();
		    Class c = o.getClass();
		    while (c != Object.class) {
		        for (Field f : c.getDeclaredFields()) {
		            if ((f.getModifiers() & Modifier.STATIC) == 0) {
		                fields.add(f);
		            }
		        }
		        c = c.getSuperclass();
		    }

		    // get offset
		    long maxSize = 0;
		    for (Field f : fields) {
		        long offset = u.objectFieldOffset(f);
		        if (offset > maxSize) {
		            maxSize = offset;
		        }
		    }

		    return ((maxSize/8) + 1) * 8;   // padding
		
	}
	public static void main(String[] args) throws Exception {
		System.out.println(sizeOf(new Object()));
		CsvGraphReader r = new CsvGraphReader();
		Path src = Paths.get("d:/z1.csv");
		r.readTableCsv(src);

		SimRank ranker = new SimRank();
		
		ranker.convertG2(r.g);
		System.out.println(ranker.ec);
		ranker.doiteration(1);
		ranker.getSortedDvs().stream().filter(x->x.getA()!=x.getB()).limit(10).forEach(x->{
			System.out.print(x.getRanki1());
			System.out.print(x.getA().getInternal_id());
			System.out.print(x.getB().getInternal_id());
			System.out.println();
		});
	}
}
