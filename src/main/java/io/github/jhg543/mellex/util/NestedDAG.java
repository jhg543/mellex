package io.github.jhg543.mellex.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NestedDAG {
	private Map<Integer, List<HalfEdge>> edges = new HashMap<Integer, List<HalfEdge>>();
	private Map<Integer, List<HalfEdge>> groupToColumnEdges = new HashMap<Integer, List<HalfEdge>>();

	private static final Logger log = LoggerFactory.getLogger(NestedDAG.class);

	public void addEdge(int s, int e, int type, int stmtid) {
		if (s == e) {
			log.warn("s=e " + s);
			return;
		}
		List<HalfEdge> sl = edges.get(s);
		if (sl == null) {
			sl = new ArrayList<HalfEdge>();
			edges.put(s, sl);
		}
		sl.add(new HalfEdge(e, type, stmtid));
	}

	public void addEdge(int s, HalfEdge hf) {
		if (s == hf.getDest()) {
			log.warn("s=e " + s);
			return;
		}
		List<HalfEdge> sl = edges.get(s);
		if (sl == null) {
			sl = new ArrayList<HalfEdge>();
			edges.put(s, sl);
		}
		sl.add(hf);
	}

	public void addGroupEdge(int s, int e, int type, int stmtid) {
		List<HalfEdge> sl = groupToColumnEdges.get(s);
		if (sl == null) {
			sl = new ArrayList<HalfEdge>();
			groupToColumnEdges.put(s, sl);
		}
		sl.add(new HalfEdge(e, type, stmtid));
	}

	public List<HalfEdge> getOutEdges(Integer node) {
		return edges.get(node);
	}

	public List<HalfEdge> getGroupOutEdges(Integer node) {
		return groupToColumnEdges.get(node);
	}

	public Map<Integer, List<HalfEdge>> getAllEdges() {
		// TODO unmodifiable map
		return edges;
	}

	public Map<Integer, List<HalfEdge>> getAllGroupToColumnEdges() {
		// TODO unmodifiable map
		return groupToColumnEdges;
	}
}
