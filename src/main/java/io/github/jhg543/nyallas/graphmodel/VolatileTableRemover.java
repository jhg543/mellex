package io.github.jhg543.nyallas.graphmodel;

import java.util.List;

public class VolatileTableRemover extends DirectedGraph<Integer, Integer, Integer, Integer> {
	public void remove(Vertex v) {
		List<Edge> incoming = v.getIncomingEdges();
		List<Edge> outgoing = v.getOutgoingEdges();

			outgoing.forEach(oe -> {
				oe.getTarget().getIncomingEdges().remove(oe);
			});

			incoming.forEach(ie -> {
				ie.getSource().getOutgoingEdges().remove(ie);
			});

		incoming.forEach(ie -> {
			outgoing.forEach(oe -> {
				Edge newedge = new Edge(ie.getSource(), oe.getTarget());
				addEdge(newedge);				
			});
		});
		getVertexes().remove(v);
	}
}
