package io.github.jhg543.mellex.session;

import io.github.jhg543.mellex.ASTHelper.ObjectName;
import io.github.jhg543.mellex.ASTHelper.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.SubQuery;
import io.github.jhg543.mellex.util.DAG;
import io.github.jhg543.mellex.util.NestedDAG;
import io.github.jhg543.mellex.util.HalfEdge;
import io.github.jhg543.mellex.util.Node;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class OutputGraphSession {
	private static final Logger log = LoggerFactory.getLogger(OutputGraphSession.class);
	private AtomicInteger groupIdGenerator = new AtomicInteger(0);
	private AtomicInteger stmtIdGenerator = new AtomicInteger(0);
	private List<String> tablenames = new ArrayList<String>();
	private Map<String, Integer> volatileTableIds = new HashMap<String, Integer>();
	private Map<String, Integer> tableIds = new HashMap<String, Integer>();;
	private String volatileNamespace;
	private List<List<String>> columns = new ArrayList<List<String>>();
	// private Map<Integer, List<HalfEdge>> edges = new HashMap<Integer,
	// List<HalfEdge>>();
	// private Map<Integer, List<HalfEdge>> groupToColumnEdges = new
	// HashMap<Integer, List<HalfEdge>>();
	private List<List<Integer>> groupedges = new ArrayList<>();

	private NestedDAG collapsedDag = new NestedDAG();
	private NestedDAG accDag = new NestedDAG();
	private DAG currentDag;

	// in case circular dependency, pick last table as root
	private int lastTableid;

	private static int COL_MULTIPILER = 1000;

	public OutputGraphSession() {
		convertTableName("000.000");
	}

	public void newVolatileNamespace(String namespace) {
		volatileTableIds.clear();
		volatileNamespace = namespace + ".";
		currentDag = new DAG();
	}

	public void putVolatileTable(String name) {
		if (!volatileTableIds.containsKey(name)) {
			int id = groupIdGenerator.getAndIncrement();
			volatileTableIds.put(name, id);
			tablenames.add("VT." + volatileNamespace + name);
			columns.add(new ArrayList<String>());
			groupedges.add(new ArrayList<Integer>());
		}
	}

	private Integer convertTableName(String name) {
		Integer id = volatileTableIds.get(name);
		if (id != null) {
			return id;
		}

		id = tableIds.get(name);
		if (id != null) {
			return id;
		}

		id = groupIdGenerator.getAndIncrement();
		tableIds.put(name, id);
		tablenames.add(name);
		columns.add(new ArrayList<String>());
		groupedges.add(new ArrayList<Integer>());
		return id;
	}

	private int resolveCol(String fullname) {
		int i = fullname.lastIndexOf('.');
		return resolveCol(fullname.substring(0, i), fullname.substring(i + 1));
	}

	private int resolveCol(String tablename, String colname) {
		Integer tableid = convertTableName(tablename);
		List<String> cols = columns.get(tableid);
		int colid = cols.indexOf(colname);
		if (colid == -1) {
			colid = cols.size();
			cols.add(colname);
		}
		return tableid * COL_MULTIPILER + colid;
	}

	private int resolveCol(ObjectName obj) {
		String tablename = null;
		if (obj.ns.size() == 2) {
			tablename = obj.ns.get(0);
		}
		if (obj.ns.size() == 3) {
			tablename = obj.ns.get(0) + '.' + obj.ns.get(1);
		}
		if (obj.ns.size() == 4) {
			tablename = obj.ns.get(0) + '.' + obj.ns.get(1) + '.' + obj.ns.get(2);
		}
		String colname = obj.ns.get(obj.ns.size() - 1);
		return resolveCol(tablename, colname);
	}

	public void printGroups(PrintWriter out) {
		for (int i = 0; i < tablenames.size(); ++i) {
			out.print(i);
			out.print(' ');
			out.print(tablenames.get(i));
			out.print(' ');

			List<String> colnames = columns.get(i);
			out.println(colnames.size());
			for (int j = 0; j < colnames.size(); ++j) {
				out.print(i * COL_MULTIPILER + j);
				out.print(' ');
				out.println(colnames.get(j));
			}
		}
	}

	public void addFlow(SubQuery q) {
		int stmtid = stmtIdGenerator.getAndIncrement();
		addFlow(q, stmtid);
	}

	public void addFlow(String tablename, List<String> colnames, List<List<String>> direct, List<List<String>> indirect,
			int stmtid) {
		Preconditions.checkNotNull(tablename, "Src table is null");
		Preconditions.checkNotNull(colnames, "Src column is null");
		Preconditions.checkNotNull(direct, "Dest column d is null");
		Preconditions.checkNotNull(indirect, "Dest column i is null");

		int leftTableid = convertTableName(tablename);
		setLastTableid(leftTableid);
		List<String> leftTableCols = columns.get(leftTableid);
		Integer[][] outColids;

		outColids = new Integer[colnames.size()][];
		Set<Integer> cis = new HashSet<Integer>();

		for (int i = 0; i < colnames.size(); ++i) {
			List<String> infs = indirect.get(i);
			List<String> dnfs = direct.get(i);
			outColids[i] = new Integer[infs.size() + dnfs.size()];
			for (int j = 0; j < infs.size(); ++j) {
				int cid = resolveCol(infs.get(j));
				outColids[i][j] = cid;
				int t = cid / COL_MULTIPILER;
				if (!groupedges.get(leftTableid).contains(t)) {
					groupedges.get(leftTableid).add(t);
				}
			}

			for (int j = 0; j < dnfs.size(); ++j) {
				int cid = resolveCol(dnfs.get(j));
				outColids[i][j + infs.size()] = cid;
				int t = cid / COL_MULTIPILER;
				if (!groupedges.get(leftTableid).contains(t)) {
					groupedges.get(leftTableid).add(t);
				}
			}

			if (i == 0) {
				cis.addAll(Arrays.asList(outColids[i]));
			} else {
				cis.retainAll(Arrays.asList(outColids[i]));
			}
		}

		if (colnames.size() == 1) {
			cis.clear();
		}

		for (Integer i : cis) {
			accDag.addGroupEdge(leftTableid, i, HalfEdge.TYPE_INDIRECT, stmtid);
		}
		for (int i = 0; i < colnames.size(); ++i) {
			String leftcolname = colnames.get(i);
			int leftcolid = leftTableCols.indexOf(leftcolname);
			if (leftcolid == -1) {

				leftcolid = leftTableCols.size();
				leftTableCols.add(leftcolname);
			}
			Integer leftid = leftTableid * COL_MULTIPILER + leftcolid;
			int indirectlen = indirect.get(i).size();

			for (int j = indirectlen; j < outColids.length; ++j) {
				int cid = outColids[i][j];
				HalfEdge hf = new HalfEdge(cid, HalfEdge.TYPE_DIRECT, stmtid);
				currentDag.addEdge(leftid, hf);
				if (!cis.contains(cid)) {
					accDag.addEdge(leftid, hf);
				}

			}
			for (int j = 0; j < indirectlen; ++j) {
				int cid = outColids[i][j];

				HalfEdge hf = new HalfEdge(cid, HalfEdge.TYPE_INDIRECT, stmtid);
				currentDag.addEdge(leftid, hf);
				if (!cis.contains(cid)) {
					accDag.addEdge(leftid, hf);
				}

			}

		}
	}

	public void addFlow(SubQuery q, int stmtid) {

		if (q == null) {
			throw new RuntimeException("Printing null subquery");
		}

		if (q.dbobj == null) {
			throw new RuntimeException("dbobj null");
		}
		int leftTableid = convertTableName(q.dbobj.toString());
		List<String> leftTableCols = columns.get(leftTableid);
		setLastTableid(leftTableid);
		Integer[][] outColids;

		outColids = new Integer[q.columns.size()][];
		Set<Integer> cis = new HashSet<Integer>();

		for (int i = 0; i < q.columns.size(); ++i) {
			List<ObjectName> infs = q.columns.get(i).inf.indirect;
			List<ObjectName> dnfs = q.columns.get(i).inf.direct;
			outColids[i] = new Integer[infs.size() + dnfs.size()];
			for (int j = 0; j < infs.size(); ++j) {
				ObjectName name = infs.get(j);
				int cid = resolveCol(name);
				outColids[i][j] = cid;
				int t = cid / COL_MULTIPILER;
				if (!groupedges.get(leftTableid).contains(t)) {
					groupedges.get(leftTableid).add(t);
				}
			}

			for (int j = 0; j < dnfs.size(); ++j) {
				ObjectName name = dnfs.get(j);
				int cid = resolveCol(name);
				outColids[i][j + infs.size()] = cid;
				int t = cid / COL_MULTIPILER;
				if (!groupedges.get(leftTableid).contains(t)) {
					groupedges.get(leftTableid).add(t);
				}
			}

			if (i == 0) {
				cis.addAll(Arrays.asList(outColids[i]));
			} else {
				cis.retainAll(Arrays.asList(outColids[i]));
			}

		}

		if (q.columns.size() == 1) {
			cis.clear();
		}

		for (Integer i : cis) {
			accDag.addGroupEdge(leftTableid, i, HalfEdge.TYPE_INDIRECT, stmtid);
		}
		for (int i = 0; i < q.columns.size(); ++i) {
			ResultColumn c = q.columns.get(i);
			int leftcolid = leftTableCols.indexOf(c.name);
			if (leftcolid == -1) {
				leftcolid = leftTableCols.size();
				leftTableCols.add(c.name);

			}
			Integer leftid = leftTableid * COL_MULTIPILER + leftcolid;

			int indirectlen = c.inf.indirect.size();

			for (int j = indirectlen; j < outColids[i].length; ++j) {
				int cid = outColids[i][j];
				currentDag.addEdge(leftid, cid, HalfEdge.TYPE_DIRECT, stmtid);
				if (!cis.contains(cid)) {
					accDag.addEdge(leftid, cid, HalfEdge.TYPE_DIRECT, stmtid);
				}

			}
			for (int j = 0; j < indirectlen; ++j) {
				int cid = outColids[i][j];
				currentDag.addEdge(leftid, cid, HalfEdge.TYPE_INDIRECT, stmtid);

				if (!cis.contains(cid)) {
					accDag.addEdge(leftid, cid, HalfEdge.TYPE_INDIRECT, stmtid);
				}

			}

		}
	}

	public void printFlow(NestedDAG gd, PrintWriter out) {
		for (int i = 1; i < tablenames.size(); ++i) {
			int colsize = columns.get(i).size();
			for (int j = 0; j < colsize; ++j) {
				int leftid = i * COL_MULTIPILER + j;
				List<HalfEdge> leftedges = gd.getOutEdges(leftid);
				if (leftedges != null) {
					out.print(leftid);
					out.print(' ');
					out.println(leftedges.size());
					for (HalfEdge e : leftedges) {
						out.print(e.getDest());
						out.print(' ');
						out.print(e.getType());
						out.print(' ');
						out.print(e.getStatementid());
						out.println();
					}
				}
			}
		}
	}

	public void printGroupJs(PrintWriter out) {
		String formatgroup = "g.setNode('G%d', {label2: '%s', style: 'fill: %s', width:%d, height:%d});";
		for (int i = 1; i < tablenames.size(); ++i) {
			String color = "#d3d7e8";
			if (tablenames.get(i).startsWith("VT.")) {
				color = "#e3a7a8";
			}

			if (groupedges.get(i).size() == 0) {
				color = "#a3a7e8";
			}
			// out.println(String.format(formatgroup, i, tablenames.get(i),
			// color, 150, 15 * columns.get(i).size()));
			out.println(String.format(formatgroup, i, tablenames.get(i), color, 15, 15));
		}

		String formatedge = "g.setEdge('G%d', 'G%d',{class:'z1-ed%d'});";
		for (int i = 0; i < tablenames.size(); ++i) {
			List<Integer> ge = groupedges.get(i);
			for (Integer dest : ge) {
				out.println(String.format(formatedge, i, dest, 0));
			}
		}
	}

	public void printJs(NestedDAG gd, PrintWriter out) {
		String formatgroup = "g.setNode('G%d', {label: '%s', clusterLabelPos: 'top', style: 'fill: #d3d7e8'});";
		String formatcol = "g.setNode(%d, {label: '%s',height:20});";
		String formatparent = "g.setParent(%d, 'G%d');";
		for (int i = 1; i < tablenames.size(); ++i) {
			out.println(String.format(formatgroup, i, tablenames.get(i)));
			List<String> colnames = columns.get(i);
			for (int j = 0; j < colnames.size(); ++j) {
				int cid = i * COL_MULTIPILER + j;
				out.println(String.format(formatcol, cid, colnames.get(j)));
				out.println(String.format(formatparent, cid, i));
			}

			List<HalfEdge> ge = gd.getGroupOutEdges(i);
			if (ge != null && colnames.size() > 1) {

				out.println(String.format("g.setNode('A%d', {label: 'Any Column',style: 'fill: #d3e7e8'});", i));
				out.println(String.format("g.setParent('A%d', 'G%d');", i, i));
				for (HalfEdge e : ge) {
					out.println(String.format("g.setEdge('A%d', %d, {class:'z1-aap'});", i, e.getDest()));
				}
			}

		}

		String formatedge = "g.setEdge(%d, %d,{class:'z1-ed%d'});";
		for (int i = 0; i < tablenames.size(); ++i) {
			int colsize = columns.get(i).size();
			for (int j = 0; j < colsize; ++j) {
				int leftid = i * COL_MULTIPILER + j;
				List<HalfEdge> leftedges = gd.getOutEdges(leftid);
				if (leftedges != null) {
					for (HalfEdge e : leftedges) {
						if (e.getType() == HalfEdge.TYPE_DIRECT || e.getType() == HalfEdge.TYPE_INDIRECT) {
							out.println(String.format(formatedge, leftid, e.getDest(), e.getType()));
						}
					}
				}
			}
		}
	}

	public void recordAndPrintCollapseJs(PrintWriter out) {
		int stmtid = stmtIdGenerator.get();
		volatileTableIds.values().forEach(x -> {
			List<String> colnames = columns.get(x);
			for (int i = 0; i < colnames.size(); ++i) {
				currentDag.setPerm(x * COL_MULTIPILER + i, false);
			}
		});
		Int2ObjectMap<Node> collapsedPart = currentDag.collapse(stmtid);
		Int2ObjectMap<IntSet> commonEdges = new Int2ObjectOpenHashMap<IntSet>();

		for (Entry<Node> entry : collapsedPart.int2ObjectEntrySet()) {

			IntSet s = new IntOpenHashSet(entry.getValue().getOutEdges().size());
			for (HalfEdge e : entry.getValue().getOutEdges()) {
				s.add(e.getDest());
			}

			int x = entry.getIntKey() / COL_MULTIPILER;
			if (commonEdges.containsKey(x)) {
				commonEdges.get(x).retainAll(s);
			} else {
				commonEdges.put(x, s);
			}

		}

		for (int i : commonEdges.keySet().toIntArray()) {
			if (columns.get(i).size() == 1) {
				commonEdges.remove(i);
			}
		}

		IntSet nodes = new IntOpenHashSet();
		Set<Integer> groups = collapsedPart.keySet().stream().map(x -> x / COL_MULTIPILER).collect(Collectors.toSet());
		String formatgroup = "g.setNode('G%d', {label: '%s', clusterLabelPos: 'top', style: 'fill: #d3d7e8'});";
		String formatcol = "g.setNode(%d, {label: '%s',height:20});";
		String formatparent = "g.setParent(%d, 'G%d');";
		if (out != null) {
			for (int i : groups) {
				out.println(String.format(formatgroup, i, tablenames.get(i)));
			}

			for (int i : collapsedPart.keySet()) {
				if (nodes.add(i)) {
					int xi = i / COL_MULTIPILER;
					int xj = i % COL_MULTIPILER;
					out.println(String.format(formatcol, i, columns.get(xi).get(xj)));
					out.println(String.format(formatparent, i, xi));
				}
			}
		}

		String formatedge = "g.setEdge(%d, %d,{class:'z1-ed%d'});";
		for (Entry<Node> entry : collapsedPart.int2ObjectEntrySet()) {
			int leftid = entry.getIntKey();
			IntSet ce = commonEdges.get(leftid / COL_MULTIPILER);
			for (HalfEdge e : entry.getValue().getOutEdges()) {
				if (e.getType() == HalfEdge.TYPE_DIRECT || e.getType() == HalfEdge.TYPE_INDIRECT) {
					if (e.getType() == HalfEdge.TYPE_DIRECT || !ce.contains(e.getDest())) {
						if (out != null) {
							int g = e.getDest() / COL_MULTIPILER;
							if (groups.add(g)) {
								out.println(String.format(formatgroup, g, tablenames.get(g)));

							}
							if (nodes.add(e.getDest())) {
								int i = e.getDest();
								int xi = i / COL_MULTIPILER;
								int xj = i % COL_MULTIPILER;
								out.println(String.format(formatcol, i, columns.get(xi).get(xj)));
								out.println(String.format(formatparent, i, xi));
							}
							out.println(String.format(formatedge, leftid, e.getDest(), e.getType()));
						}
						collapsedDag.addEdge(leftid, e);
					}
				}
			}

		}

		commonEdges.forEach((x, y) -> {
			if (out != null) {
				out.println(String.format("g.setNode('A%d', {label: 'Any Column',style: 'fill: #d3e7e8'});", x));
				out.println(String.format("g.setParent('A%d', 'G%d');", x, x));
			}
			y.forEach(z -> {
				if (out != null) {
					if (nodes.add(z)) {
						int i = z;
						int xi = i / COL_MULTIPILER;
						int xj = i % COL_MULTIPILER;
						out.println(String.format(formatcol, i, columns.get(xi).get(xj)));
						int g = z / COL_MULTIPILER;
						if (groups.add(g)) {
							out.println(String.format(formatgroup, g, tablenames.get(g)));
						}
						out.println(String.format(formatparent, i, xi));
					}
					out.println(String.format("g.setEdge('A%d', %d, {class:'z1-aap'});", x, z));
				}
				collapsedDag.addGroupEdge(x, z, HalfEdge.TYPE_INDIRECT, stmtid);
			});
		});
	}

	public void printNestedDAG(PrintWriter out) {
		NestedDAG nestedDAG = collapsedDag;
		Map<Integer, List<HalfEdge>> edges = nestedDAG.getAllEdges();
		Map<Integer, List<HalfEdge>> gcedges = nestedDAG.getAllGroupToColumnEdges();

		IntSet nodes = new IntOpenHashSet();
		IntSet groups = new IntOpenHashSet();
		String formatgroup = "g.setNode('G%d', {label: '%s', clusterLabelPos: 'top', style: 'fill: #d3d7e8'});";
		String formatcol = "g.setNode(%d, {label: '%s',height:20});";
		String formatparent = "g.setParent(%d, 'G%d');";

		gcedges.forEach((x, y) -> {
			groups.add(x.intValue());
			y.forEach(z -> {
				nodes.add(z.getDest());
				groups.add(z.getDest() / COL_MULTIPILER);
			});
		});

		edges.forEach((x, y) -> {
			nodes.add(x.intValue());
			groups.add(x.intValue() / COL_MULTIPILER);
			y.forEach(z -> {
				nodes.add(z.getDest());
				groups.add(z.getDest() / COL_MULTIPILER);
			});
		});

		for (int i : groups) {
			out.println(String.format(formatgroup, i, tablenames.get(i)));
		}

		for (int i : nodes) {
			int xi = i / COL_MULTIPILER;
			int xj = i % COL_MULTIPILER;
			out.println(String.format(formatcol, i, columns.get(xi).get(xj)));
			out.println(String.format(formatparent, i, xi));
		}

		String formatedge = "g.setEdge(%d, %d,{class:'z1-ed%d'});";
		edges.forEach((x, y) -> {
			y.forEach(z -> {
				out.println(String.format(formatedge, x, z.getDest(), z.getType()));
			});
		});

		gcedges.forEach((x, y) -> {
			out.println(String.format("g.setNode('A%d', {label: 'Any Column',style: 'fill: #d3e7e8'});", x));
			out.println(String.format("g.setParent('A%d', 'G%d');", x, x));
			y.forEach(z -> {
				out.println(String.format("g.setEdge('A%d', %d, {class:'z1-aap'});", x, z.getDest()));

			});
		});
	}

	public int getLastTableid() {
		return lastTableid;
	}

	private void setLastTableid(int lastTableid) {
		this.lastTableid = lastTableid;
	}
}
