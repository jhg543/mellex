package io.github.jhg543.nyallas.graphmodel;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VolatileTableRemover extends DirectedGraph<String, Integer, Integer, Integer> {
	private void remove(Vertex v) {
		Set<Edge> incoming = v.getIncomingEdges();
		Set<Edge> outgoing = v.getOutgoingEdges();

		outgoing.forEach(oe -> {
			oe.getTarget().getIncomingEdges().remove(oe);
		});

		incoming.forEach(ie -> {
			ie.getSource().getOutgoingEdges().remove(ie);
		});

		incoming.forEach(ie -> {
			outgoing.forEach(oe -> {
				Edge newedge = new Edge(ie.getSource(), oe.getTarget());
				newedge.setEdgeData(ie.getEdgeData()*oe.getEdgeData());
				addEdge(newedge);
				// TODO ...
				
			});
		});
		getVertexes().remove(v);
	}

	public void remove() {
		getVertexes().stream().filter(v -> v.getMarker() != null).collect(Collectors.toList()).forEach(v -> remove(v));
	}
}
