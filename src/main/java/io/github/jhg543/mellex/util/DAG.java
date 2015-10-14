package io.github.jhg543.mellex.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.ArrayList;
import java.util.List;

public class DAG {

	private static class Node_internal implements Node {
		int visit = -1;
		boolean isperm = true;
		IntList in = new IntArrayList();
		List<HalfEdge> out = new ArrayList<HalfEdge>();

		@Override
		public List<HalfEdge> getOutEdges() {
			return out;
		}

		@Override
		public int getVisitStatus() {
			return visit;
		}
	}

	private void sortHalfEdge(Node_internal n) {
		List<HalfEdge> z = new ArrayList<HalfEdge>(n.out.size());
		for (HalfEdge e : n.out) {
			if (e.getType() == HalfEdge.TYPE_DIRECT) {
				z.add(e);
			}
		}

		for (HalfEdge e : n.out) {
			if (e.getType() == HalfEdge.TYPE_INDIRECT) {
				z.add(e);
			}
		}

		n.out = z;

	}

	Int2ObjectMap<Node_internal> edges = new Int2ObjectOpenHashMap<Node_internal>();

	public IntSet listNodes() {
		return edges.keySet();
	}

	public void setPerm(int node, boolean isperm) {
		Node_internal n = edges.get(node);
		if (n != null) {
			n.isperm = isperm;
		}
	}

	public void addEdge(int s, HalfEdge hf) {
		Node_internal sn = edges.get(s);
		if (sn == null) {
			sn = new Node_internal();
			edges.put(s, sn);
		}
		sn.out.add(hf);

		Node_internal en = edges.get(hf.getDest());
		if (en == null) {
			en = new Node_internal();
			edges.put(hf.getDest(), en);
		}
		en.in.add(s);

	}

	public void addEdge(int s, int e, int type, int stmtid) {
		Node_internal sn = edges.get(s);
		if (sn == null) {
			sn = new Node_internal();
			edges.put(s, sn);
		}
		sn.out.add(new HalfEdge(e, type, stmtid));

		Node_internal en = edges.get(e);
		if (en == null) {
			en = new Node_internal();
			edges.put(e, en);
		}
		en.in.add(s);

	}

	public List<HalfEdge> listOutEdges(int node) {
		Node_internal n = edges.get(node);
		if (n == null) {
			return null;
		}

		return n.out;

	}

	public Int2ObjectMap<Node> listAllEdges() {
		Int2ObjectMap<Node> result = new Int2ObjectOpenHashMap<Node>();
		for (Entry<Node_internal> entry : edges.int2ObjectEntrySet()) {
			result.put(entry.getIntKey(), entry.getValue());
		}
		return result;
	}

	// public IntList listInboundEdges(int node) {
	// Node n = edges.get(node);
	// if (n == null) {
	// return null;
	// }
	//
	// return n.in;
	// }

	public void dfs(int start) {
		edges.values().forEach(x -> x.visit = -1);
		dfs_internal(start, 0);
		// Int2IntMap result = new Int2IntOpenHashMap(edges.size());
		// edges.int2ObjectEntrySet().forEach(x -> {
		// result.put(x.getIntKey(), x.getValue().visit);
		// });
		// return null;
	}

	public Int2ObjectMap<Node> collapse(int collapsedEdgeid) {
		Int2ObjectMap<Node> result = new Int2ObjectOpenHashMap<Node>();

		edges.int2ObjectEntrySet().forEach(x -> {

			Node_internal newone = new Node_internal();
			if (x.getValue().isperm) {
				x.getValue().isperm = false;
				dfs(x.getIntKey());
				x.getValue().isperm = true;
				edges.int2ObjectEntrySet().forEach(y -> {
					Node_internal dest = y.getValue();
					if (!x.equals(y) && dest.isperm && dest.visit != -1) {
						newone.out.add(new HalfEdge(y.getIntKey(), dest.visit, collapsedEdgeid));
					}
				});
				if (newone.out.size() > 0) {
					result.put(x.getIntKey(), newone);
				}
			}
			// result.put(x.getIntKey(), x.getValue().visit);
			});
		return result;
	}

	private void dfs_internal(int node, int vt) {
		Node_internal n = edges.get(node);
		if (vt == n.visit) {
			return;
		}

		if (vt == 1 && n.visit == 0) {
			return;
		}

		if (n.visit == -1) {
			sortHalfEdge(n);
		}

		n.visit = vt;
		if (n.isperm) {
			return;
		}

		List<HalfEdge> e = n.out;
		if (vt == 1) {
			for (int i = 0; i < e.size(); ++i) {
				dfs_internal(e.get(i).getDest(), 1);
			}
		} else {
			for (int i = 0; i < e.size(); ++i) {
				dfs_internal(e.get(i).getDest(), e.get(i).getType());
			}
		}

	}
}
