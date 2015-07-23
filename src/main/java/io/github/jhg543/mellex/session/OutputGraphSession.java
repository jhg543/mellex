package io.github.jhg543.mellex.session;

import io.github.jhg543.mellex.ASTHelper.Influences;
import io.github.jhg543.mellex.ASTHelper.ObjectName;
import io.github.jhg543.mellex.ASTHelper.ResultColumn;
import io.github.jhg543.mellex.ASTHelper.SubQuery;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;

public class OutputGraphSession {

	private AtomicInteger groupIdGenerator = new AtomicInteger(0);
	private AtomicInteger stmtIdGenerator = new AtomicInteger(0);
	private List<String> tablenames = new ArrayList<String>();
	private Map<String, Integer> volatileTableIds = new HashMap<String, Integer>();
	private Map<String, Integer> tableIds = new HashMap<String, Integer>();;
	private String volatileNamespace;
	private List<List<String>> columns = new ArrayList<List<String>>();
	private Map<Integer, List<Edge>> edges = new HashMap<Integer, List<Edge>>();
	private Map<Integer, List<Edge>> allColumnEdges = new HashMap<Integer, List<Edge>>();
	private List<List<Integer>> groupedges = new ArrayList<>();

	private static int COL_MULTIPILER = 1000;
	public static int TYPE_DIRECT = 0;
	public static int TYPE_INDIRECT = 1;

	public static class Edge {
		private int dest;
		private int type;
		private int statementid;

		public int getDest() {
			return dest;
		}

		public int getType() {
			return type;
		}

		public int getStatementid() {
			return statementid;
		}

		public Edge(int dest, int type, int statementid) {
			super();
			this.dest = dest;
			this.type = type;
			this.statementid = statementid;
		}

	}

	public void newVolatileNamespace(String namespace) {
		volatileTableIds.clear();
		volatileNamespace = namespace + ".";
	}

	public void putVolatileTable(String name) {
		if (!volatileTableIds.containsKey(name)) {
			int id = groupIdGenerator.getAndIncrement();
			volatileTableIds.put(name, id);
			tablenames.add(volatileNamespace + name);
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
		int tableid = convertTableName(tablename);
		String colname = obj.ns.get(obj.ns.size() - 1);
		List<String> cols = columns.get(tableid);
		int colid = cols.indexOf(colname);
		if (colid == -1) {
			colid = cols.size();
			cols.add(colname);
		}
		return tableid * COL_MULTIPILER + colid;
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

	private void addEdge(int s, int e, int type, int stmtid) {
		List<Edge> sl = edges.get(s);
		if (sl == null) {
			sl = new ArrayList<Edge>();
			edges.put(s, sl);
		}
		sl.add(new Edge(e, type, stmtid));
	}

	private void addGroupEdge(int s, int e, int type, int stmtid) {
		List<Edge> sl = allColumnEdges.get(s);
		if (sl == null) {
			sl = new ArrayList<Edge>();
			allColumnEdges.put(s, sl);
		}
		sl.add(new Edge(e, type, stmtid));
	}

	public void addFlow(SubQuery q) {
		int stmtid = stmtIdGenerator.getAndIncrement();
		addFlow(q, stmtid);
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
		Integer[][] indirectColids;

		indirectColids = new Integer[q.columns.size()][];
		Set<Integer> cis = new HashSet<Integer>();

		for (int i = 0; i < q.columns.size(); ++i) {
			List<ObjectName> infs = q.columns.get(i).inf.indirect;
			List<ObjectName> dnfs = q.columns.get(i).inf.direct;
			indirectColids[i] = new Integer[infs.size() + dnfs.size()];
			for (int j = 0; j < infs.size(); ++j) {
				ObjectName name = infs.get(j);
				int cid = resolveCol(name);
				indirectColids[i][j] = cid;
				int t = cid % COL_MULTIPILER;
				if (!groupedges.get(leftTableid).contains(t)) {
					groupedges.get(leftTableid).add(t);
				}
			}

			for (int j = 0; j < dnfs.size(); ++j) {
				ObjectName name = dnfs.get(j);
				int cid = resolveCol(name);
				indirectColids[i][j + infs.size()] = cid;
				int t = cid % COL_MULTIPILER;
				if (!groupedges.get(leftTableid).contains(t)) {
					groupedges.get(leftTableid).add(t);
				}
			}

			if (i == 0) {
				cis.addAll(Arrays.asList(indirectColids[i]));
			} else {
				cis.retainAll(Arrays.asList(indirectColids[i]));
			}
		}

		for (Integer i : cis) {
			addGroupEdge(leftTableid, i, TYPE_INDIRECT, stmtid);
		}
		for (int i = 0; i < q.columns.size(); ++i) {
			ResultColumn c = q.columns.get(i);
			int leftcolid = leftTableCols.indexOf(c.name);
			if (leftcolid == -1) {
				leftTableCols.add(c.name);
				leftcolid = leftTableCols.size();
			}
			Integer leftid = leftTableid * COL_MULTIPILER + leftcolid;
			for (ObjectName name : c.inf.direct) {
				addEdge(leftid, resolveCol(name), TYPE_DIRECT, stmtid);
			}

			for (int j = 0; j < indirectColids[i].length; ++j) {
				int cid = indirectColids[i][j];
				if (!cis.contains(cid)) {
					addEdge(leftid, cid, TYPE_INDIRECT, stmtid);
				}

			}

		}
	}

	public void printFlow(PrintWriter out) {
		for (int i = 0; i < tablenames.size(); ++i) {
			int colsize = columns.get(i).size();
			for (int j = 0; j < colsize; ++j) {
				int leftid = i * COL_MULTIPILER + j;
				List<Edge> leftedges = edges.get(leftid);
				if (leftedges != null) {
					out.print(leftid);
					out.print(' ');
					out.println(leftedges.size());
					for (Edge e : leftedges) {
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
		String formatgroup = "g.setNode('G%d', {label: '%s', style: 'fill: #d3d7e8', height:%d, width:%d});";
		for (int i = 0; i < tablenames.size(); ++i) {
			out.println(String.format(formatgroup, i, tablenames.get(i), 150, 15 * columns.get(i).size()));
		}

		String formatedge = "g.setEdge(%d, %d,{class:'z1-ed%d'});";
		for (int i = 0; i < tablenames.size(); ++i) {
			List<Integer> ge = groupedges.get(i);
			for (Integer dest : ge) {
				out.println(String.format(formatedge, i, dest, 0));
			}
		}
	}

	public void printJs(PrintWriter out) {
		String formatgroup = "g.setNode('G%d', {label: '%s', clusterLabelPos: 'top', style: 'fill: #d3d7e8'});";
		String formatcol = "g.setNode(%d, {label: '%s',height:20});";
		String formatparent = "g.setParent(%d, 'G%d');";
		for (int i = 0; i < tablenames.size(); ++i) {
			out.println(String.format(formatgroup, i, tablenames.get(i)));
			List<String> colnames = columns.get(i);
			for (int j = 0; j < colnames.size(); ++j) {
				int cid = i * COL_MULTIPILER + j;
				out.println(String.format(formatcol, cid, colnames.get(j)));
				out.println(String.format(formatparent, cid, i));
			}

			List<Edge> ge = allColumnEdges.get(i);
			if (ge != null && false) {

				out.println(String.format("g.setNode('A%d', {label: 'AllColumn'});", i));
				out.println(String.format("g.setParent('A%d', 'G%d');", i, i));
				for (Edge e : ge) {
					out.println(String.format("g.setEdge('A%d', %d, {class:'z1-eap'});", i, e.dest));
				}
			}

		}

		String formatedge = "g.setEdge(%d, %d,{class:'z1-ed%d'});";
		for (int i = 0; i < tablenames.size(); ++i) {
			int colsize = columns.get(i).size();
			for (int j = 0; j < colsize; ++j) {
				int leftid = i * COL_MULTIPILER + j;
				List<Edge> leftedges = edges.get(leftid);
				if (leftedges != null) {
					for (Edge e : leftedges) {
						if (e.type == TYPE_DIRECT) {
							out.println(String.format(formatedge, leftid, e.dest, e.type));
						}
					}
				}
			}
		}
	}
}
