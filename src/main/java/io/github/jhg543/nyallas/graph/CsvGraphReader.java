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


import io.github.jhg543.nyallas.graphmodel.DirectedGraph;
import io.github.jhg543.nyallas.graphmodel.Edge;
import io.github.jhg543.nyallas.graphmodel.Vertex;


import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

public class CsvGraphReader {
	
	private Map<String, Vertex<VertexDBCol, EdgeETL>> vertexNames;
	
	private DirectedGraph<VertexDBCol, EdgeETL> graph;

	public Map<String, Vertex<VertexDBCol, EdgeETL>> getVertexNames() {
		return vertexNames;
	}

	public DirectedGraph<VertexDBCol, EdgeETL> getGraph() {
		return graph;
	}

	private Vertex<VertexDBCol, EdgeETL> getTableVertex(String s, String t, String c) {
		s = s.intern();
		t = t.intern();
		c = c.intern();
		StringBuilder fqnsb = new StringBuilder();
		fqnsb.append(s);
		fqnsb.append('.');
		fqnsb.append(t);
		String fqn = fqnsb.toString().intern();
		Vertex<VertexDBCol, EdgeETL>  v = vertexNames.get(fqn);
		if (v == null) {
			v = graph.addVertex();
			VertexDBCol vc = new VertexDBCol();
			vc.setColumn(t);
			vc.setTable(t);
			vc.setSchema(s);
			vc.setFqn(fqn);
			vertexNames.put(fqn, v);
			v.setVertexData(vc);
		}
		return v;
	}

	public void readTableCsv(Path path) {
		EdgeETL dummyed = new EdgeETL();
		dummyed.setConntype("0");
		dummyed.setScriptname("0");
		graph = new DirectedGraph<VertexDBCol, EdgeETL>();
		vertexNames = new HashMap<>();
		try {
			Files.readAllLines(path).forEach(line -> {
				List<String> linelist = Splitter.on(',').splitToList(line);
				Vertex<VertexDBCol, EdgeETL> end = getTableVertex(Misc.schemaSym(linelist.get(1)), linelist.get(2), linelist.get(2));
				Vertex<VertexDBCol, EdgeETL> start = getTableVertex(Misc.schemaSym(linelist.get(4)), linelist.get(5), linelist.get(5));
				Edge<VertexDBCol, EdgeETL> edge = new Edge<>(start, end);
				if (!start.getOutgoingEdges().contains(edge)) {
					edge.setEdgeData(dummyed);
					graph.addEdge(edge);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Vertex<VertexDBCol, EdgeETL> getVertex(String s, String t, String c) {
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
		Vertex<VertexDBCol, EdgeETL> v = vertexNames.get(fqn);
		if (v == null) {
			v = graph.addVertex();
			VertexDBCol vc = new VertexDBCol();
			vc.setColumn(c);
			vc.setTable(t);
			vc.setSchema(s);
			vc.setFqn(fqn);
			v.setVertexData(vc);
			vertexNames.put(fqn, v);
		}
		return v;
	}

	public void readCsv(Path path) {
		graph = new DirectedGraph<VertexDBCol, EdgeETL>();
		vertexNames = new HashMap<>();
		try {
			Files.readAllLines(path).forEach(line -> {
				List<String> linelist = Splitter.on(',').splitToList(line);
				Vertex<VertexDBCol, EdgeETL> end = getVertex(Misc.schemaSym(linelist.get(1)), linelist.get(2), linelist.get(3));
				Vertex<VertexDBCol, EdgeETL> start = getVertex(Misc.schemaSym(linelist.get(4)), linelist.get(5), linelist.get(6));
				Edge<VertexDBCol, EdgeETL> edge = new Edge<>(start, end);
				if (!start.getOutgoingEdges().contains(edge)) {
					EdgeETL ed = new EdgeETL();
					ed.setScriptname(linelist.get(0));
					ed.setConntype(linelist.get(7));
					edge.setEdgeData(ed);
					graph.addEdge(edge);	
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void outputEdges(List<Edge<VertexDBCol, EdgeETL>> es, Path path) {
		try (PrintWriter writer = new PrintWriter(path.toFile())) {

			es.stream().forEach(y -> {
				writer.append(y.getEdgeData().getScriptname());
				writer.append(',');
				writer.append(y.getTarget().getVertexData().getSchema());
				writer.append(',');
				writer.append(y.getTarget().getVertexData().getTable());
				writer.append(',');
				writer.append(y.getTarget().getVertexData().getColumn());
				writer.append(',');
				writer.append(y.getSource().getVertexData().getSchema());
				writer.append(',');
				writer.append(y.getSource().getVertexData().getTable());
				writer.append(',');
				writer.append(y.getSource().getVertexData().getColumn());
				writer.append(',');
				writer.append(y.getEdgeData().getConntype());
				writer.println();
			});
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	};

	public static  void gensvg() {

		try {
			CsvGraphReader r = new CsvGraphReader();
			Path src = Paths.get("d:/z1.csv");
			Path filteredfile = Paths.get("d:/zr.csv");
			Path layoutExecutable = Paths.get("d:/zrlayout.exe");
			Path layoutoutput = Paths.get("d:/manual_graph.svg");
			Path layoutoutputinversed = Paths.get("d:/manual_graph2.svg");
			r.readTableCsv(src);
			System.out.println("read complate");
			List<Vertex<VertexDBCol, EdgeETL>> vs = r.vertexNames.values().stream().filter(x -> x.getVertexData().getTable().equals("T80_LIAB_CORP_ACCT_IDX_DD"))
					.collect(Collectors.toList());
			Predicate<Edge<VertexDBCol, EdgeETL>> test = x -> {
				// System.out.println(x.getSource().getFqn());
				// System.out.println(x.getTarget().getFqn());
				return !x.getSource().getVertexData().getSchema().equals(x.getTarget().getVertexData().getSchema());
			};

			System.out.println("find complate");
			List<Edge<VertexDBCol, EdgeETL>> es = LineageFinder.find(r.graph, vs, test,true,false);
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
		gensvg();
	}
}
