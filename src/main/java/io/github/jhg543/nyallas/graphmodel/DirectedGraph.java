package io.github.jhg543.nyallas.graphmodel;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class DirectedGraph<V extends Vertex, E extends Edge> {

	private Set<V> vertexes = new HashSet<>();

	public Set<V> getVertexes() {
		return vertexes;
	}

	public V addVertex(V v) {
		vertexes.add(v);
		return v;
	}

	public V addVertex(Supplier<V> supplier) {
		return addVertex(supplier.get());
	}
	/**
	 * it's user's responsibility to ensure vertex added first AND initialize
	 * edge object FIRST
	 * 
	 * @param e
	 */
	public void addEdge(E e) {
		e.getSource().getOutgoingEdges().add(e);
		e.getTarget().getIncomingEdges().add(e);
	}
}
