package io.github.jhg543.nyallas.graphmodel;

import java.util.HashSet;
import java.util.Set;

public class DirectedGraph<VERTEX_DATA, EDGE_DATA> {

	private Set<Vertex<VERTEX_DATA, EDGE_DATA>> vertexes = new HashSet<>();

	public Set<Vertex<VERTEX_DATA, EDGE_DATA>> getVertexes() {
		return vertexes;
	}

	public Vertex<VERTEX_DATA, EDGE_DATA> addVertex() {
		Vertex<VERTEX_DATA, EDGE_DATA> v = new Vertex<VERTEX_DATA, EDGE_DATA>();
		v.incomingEdges = new HashSet<>();
		v.outgoingEdges = new HashSet<>();
		vertexes.add(v);
		return v;
	}

	public Edge<VERTEX_DATA, EDGE_DATA> newEdge(Vertex<VERTEX_DATA, EDGE_DATA> source, Vertex<VERTEX_DATA, EDGE_DATA> target) {
		return new Edge<VERTEX_DATA, EDGE_DATA>(source, target);
	}

	/**
	 * it's user's responsibility to ensure vertex added first AND initialize
	 * edge object FIRST
	 * 
	 * @param e
	 */
	public void addEdge(Edge<VERTEX_DATA, EDGE_DATA> e) {
		e.getSource().getOutgoingEdges().add(e);
		e.getTarget().getIncomingEdges().add(e);
	}
}
