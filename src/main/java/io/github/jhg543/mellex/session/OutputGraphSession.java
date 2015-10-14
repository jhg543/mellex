package io.github.jhg543.mellex.session;

import io.github.jhg543.mellex.ASTHelper.CreateTableStmt;
import io.github.jhg543.mellex.ASTHelper.ObjectName;
import io.github.jhg543.mellex.ASTHelper.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.SubQuery;
import io.github.jhg543.mellex.util.DAG;
import io.github.jhg543.mellex.util.Misc;
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
	private Map<Integer,Integer> tableprop = new HashMap<Integer,Integer>();
	private Map<String, Integer> volatileTableIds = new HashMap<String, Integer>();
	private Map<String, Integer> tableIds = new HashMap<String, Integer>();;
	private String volatileNamespace;
	private List<List<String>> columns = new ArrayList<List<String>>();
	// private Map<Integer, List<HalfEdge>> edges = new HashMap<Integer,
	// List<HalfEdge>>();
	// private Map<Integer, List<HalfEdge>> groupToColumnEdges = new
	// HashMap<Integer, List<HalfEdge>>();
	private List<List<Integer>> groupedges = new ArrayList<>();

	private NestedDAG overallCollapsedDag = new NestedDAG();
	private NestedDAG overallDag = new NestedDAG();
	private DAG currentSessionDag;

	// in case circular dependency, pick last table as root
	private int lastTableid;

	private static int COL_MULTIPILER = 1000;

	public OutputGraphSession() {
		convertTableName("000.000");
	}

	public void newVolatileNamespace(String namespace) {
		volatileTableIds.clear();
		volatileNamespace = namespace + ".";
		currentSessionDag = new DAG();
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
		name = Misc.nameSym(name);
		Integer id = volatileTableIds.get(name);
		if (id != null) {
			return id;
		}

		if (Misc.isvolatile(name)) {
			putVolatileTable(name);
			return volatileTableIds.get(name);
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

	/*
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
			overallDag.addGroupEdge(leftTableid, i, HalfEdge.TYPE_INDIRECT, stmtid);
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
				currentSessionDag.addEdge(leftid, hf);
				if (!cis.contains(cid)) {
					overallDag.addEdge(leftid, hf);
				}

			}
			for (int j = 0; j < indirectlen; ++j) {
				int cid = outColids[i][j];

				HalfEdge hf = new HalfEdge(cid, HalfEdge.TYPE_INDIRECT, stmtid);
				currentSessionDag.addEdge(leftid, hf);
				if (!cis.contains(cid)) {
					overallDag.addEdge(leftid, hf);
				}

			}

		}
	}
*/
	public void addFlow(SubQuery q, int stmtid) {

		if (q == null) {
			throw new RuntimeException("Printing null subquery");
		}

		if (q.dbobj == null) {
			throw new RuntimeException("dbobj null");
		}
		int leftTableid = convertTableName(q.dbobj.toString());
		if (q instanceof CreateTableStmt)
		{
			CreateTableStmt stmt = (CreateTableStmt)q;
			if (stmt.getViewDef()!=null)
			{
				tableprop.put(leftTableid, 1);
			}
		}
		
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
			overallDag.addGroupEdge(leftTableid, i, HalfEdge.TYPE_INDIRECT, stmtid);
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
				currentSessionDag.addEdge(leftid, cid, HalfEdge.TYPE_DIRECT, stmtid);
				if (!cis.contains(cid)) {
					overallDag.addEdge(leftid, cid, HalfEdge.TYPE_DIRECT, stmtid);
				}

			}
			for (int j = 0; j < indirectlen; ++j) {
				int cid = outColids[i][j];
				currentSessionDag.addEdge(leftid, cid, HalfEdge.TYPE_INDIRECT, stmtid);

				if (!cis.contains(cid)) {
					overallDag.addEdge(leftid, cid, HalfEdge.TYPE_INDIRECT, stmtid);
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
						out.print(e.getMarker());
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

	public void printOverallJs(PrintWriter out) {
		printJs(overallDag, out);
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
							if (i != e.getDest() / COL_MULTIPILER) {
								out.println(String.format(formatedge, leftid, e.getDest(), e.getType()));
							}
						}
					}
				}
			}
		}
	}


	public void printCurrentSessionGroupJs(PrintWriter out,int edgemarker)
	{
		Int2ObjectMap<Node> edges = currentSessionDag.listAllEdges();
		printEdgeJs(edges, out,false,edgemarker);
	}
	
	public void recordAndPrintCollapseJs(PrintWriter out,int edgemarker) {
		//int stmtid = stmtIdGenerator.get();

		// Mark volatile tables
		volatileTableIds.values().forEach(x -> {
			List<String> colnames = columns.get(x);
			for (int i = 0; i < colnames.size(); ++i) {
				currentSessionDag.setPerm(x * COL_MULTIPILER + i, false);
			}
		});
		Int2ObjectMap<Node> collapsedPart = currentSessionDag.collapse(edgemarker);
		printEdgeJs(collapsedPart, out,true,edgemarker);
	}

	private void printEdgeJs(Int2ObjectMap<Node> edges, PrintWriter out, boolean record,int commonEdgeIDtoWrite) {
		
		Int2ObjectMap<IntSet> commonEdges = new Int2ObjectOpenHashMap<IntSet>();

		for (Entry<Node> entry : edges.int2ObjectEntrySet()) {

			// get destination list
			IntSet currentNodeDestinations = new IntOpenHashSet(entry.getValue().getOutEdges().size());
			for (HalfEdge e : entry.getValue().getOutEdges()) {
				currentNodeDestinations.add(e.getDest());
			}

			int groupid = entry.getIntKey() / COL_MULTIPILER;
			if (commonEdges.containsKey(groupid)) {
				commonEdges.get(groupid).retainAll(currentNodeDestinations);
			} else {
				commonEdges.put(groupid, currentNodeDestinations);
			}

		}

		// single column = no "ALL COLUMN"
		for (int i : commonEdges.keySet().toIntArray()) {
			if (columns.get(i).size() == 1) {
				commonEdges.remove(i);
			}
		}

		IntSet nodes = new IntOpenHashSet();
		Set<Integer> groups = edges.keySet().stream().map(x -> x / COL_MULTIPILER).collect(Collectors.toSet());
		String stringTemplate_group = "g.setNode('G%d', {label: '%s', clusterLabelPos: 'top', style: 'fill: #d3d7e8'});";
		String stringTemplate_column = "g.setNode(%d, {label: '%s',height:20});";
		String stringTemplate_setparent = "g.setParent(%d, 'G%d');";

		if (out != null) {
			// create left group nodes
			for (int i : groups) {
				out.println(String.format(stringTemplate_group, i, tablenames.get(i)));
			}

			// create left columns node
			for (int i : edges.keySet()) {
				if (nodes.add(i)) {
					int xi = i / COL_MULTIPILER;
					int xj = i % COL_MULTIPILER;
					out.println(String.format(stringTemplate_column, i, columns.get(xi).get(xj)));
					out.println(String.format(stringTemplate_setparent, i, xi));
				}
			}
		}

		String stringTemplate_edge = "g.setEdge(%d, %d,{class:'z1-ed%d'});";
		for (Entry<Node> entry : edges.int2ObjectEntrySet()) {

			int leftid = entry.getIntKey();
			int leftcolgroupid = leftid / COL_MULTIPILER;
			IntSet commonEdgesofLeft = commonEdges.get(leftcolgroupid);
			for (HalfEdge e : entry.getValue().getOutEdges()) {
				if (e.getType() == HalfEdge.TYPE_DIRECT || e.getType() == HalfEdge.TYPE_INDIRECT) {

					if (e.getType() == HalfEdge.TYPE_DIRECT || !commonEdgesofLeft.contains(e.getDest())) {
						if (out != null) {
							int rightgroupid = e.getDest() / COL_MULTIPILER;

							// lazy creation of right groups
							if (groups.add(rightgroupid)) {
								out.println(String.format(stringTemplate_group, rightgroupid, tablenames.get(rightgroupid)));
							}

							// lazy creation of right columns
							if (nodes.add(e.getDest())) {
								int i = e.getDest();
								int xi = i / COL_MULTIPILER;
								int xj = i % COL_MULTIPILER;
								out.println(String.format(stringTemplate_column, i, columns.get(xi).get(xj)));
								out.println(String.format(stringTemplate_setparent, i, xi));
							}

							// print edge
							if (rightgroupid != leftcolgroupid) {
								out.println(String.format(stringTemplate_edge, leftid, e.getDest(), e.getType()));
							}
						}

						// add to all
						if (record) {
							overallCollapsedDag.addEdge(leftid, e);
						}
					}
				}
			}

		}

		commonEdges.forEach((srcnodeid, outedges) -> {
			if (out != null) {
				out.println(String.format("g.setNode('A%d', {label: 'Any Column',style: 'fill: #d3e7e8'});", srcnodeid));
				out.println(String.format("g.setParent('A%d', 'G%d');", srcnodeid, srcnodeid));
			}
			outedges.forEach(destnodeid -> {
				if (out != null) {
					if (nodes.add(destnodeid)) {
						int i = destnodeid;
						int xi = i / COL_MULTIPILER;
						int xj = i % COL_MULTIPILER;
						out.println(String.format(stringTemplate_column, i, columns.get(xi).get(xj)));
						int destnodegroup = destnodeid / COL_MULTIPILER;
						if (groups.add(destnodegroup)) {
							out.println(String.format(stringTemplate_group, destnodegroup, tablenames.get(destnodegroup)));
						}
						out.println(String.format(stringTemplate_setparent, i, xi));
					}
					out.println(String.format("g.setEdge('A%d', %d, {class:'z1-aap'});", srcnodeid, destnodeid));
				}
				if (record) {
					overallCollapsedDag.addGroupEdge(srcnodeid, destnodeid, HalfEdge.TYPE_INDIRECT, commonEdgeIDtoWrite);
				}
			});
		});
	}

	public void printNestedDAG(PrintWriter out) {
		NestedDAG nestedDAG = overallCollapsedDag;
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
				if (x / COL_MULTIPILER != z.getDest() / COL_MULTIPILER) {
					out.println(String.format(formatedge, x, z.getDest(), z.getType()));
				}
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

	public void printTableNestedDAG(PrintWriter out) {
		NestedDAG nestedDAG = overallCollapsedDag;
		Map<Integer, List<HalfEdge>> edges = nestedDAG.getAllEdges();

		IntSet nodes = new IntOpenHashSet();
		//String formatcol = "g.setNode(%d, {label: '%s',height:20});";
		String formatcol = "%d=%s %s";
		
		edges.forEach((x, y) -> {
			nodes.add(x.intValue());
			y.forEach(z -> {
				nodes.add(z.getDest());
			});
		});

		for (int i : nodes) {
			int xi = i / COL_MULTIPILER;
			int xj = i % COL_MULTIPILER;
			out.println(String.format(formatcol, i, tablenames.get(xi),tableprop.get(xi) == null ? "NONVIEW" : "VIEW"));
		}

		//String formatedge = "g.setEdge(%d, %d,{class:'z1-ed%d'});";
		String formatedge = "%d - %d %d";
		edges.forEach((x, y) -> {
			y.forEach(z -> {
				if (x / COL_MULTIPILER != z.getDest() / COL_MULTIPILER) {
					out.println(String.format(formatedge, x, z.getDest(), z.getType()));
				}
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
