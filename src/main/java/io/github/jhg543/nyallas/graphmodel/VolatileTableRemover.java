package io.github.jhg543.nyallas.graphmodel;

import java.util.Set;
import java.util.stream.Collectors;

public class VolatileTableRemover extends DirectedGraph<Vertex<String, Integer>, Edge<String, Integer>> {
	private void remove(Vertex<String, Integer> v) {
		Set<Edge<String, Integer>> incoming = v.getIncomingEdges();
		Set<Edge<String, Integer>> outgoing = v.getOutgoingEdges();

		outgoing.forEach(oe -> {
			oe.getTarget().getIncomingEdges().remove(oe);
		});

		incoming.forEach(ie -> {
			ie.getSource().getOutgoingEdges().remove(ie);
		});

		incoming.forEach(ie -> {
			outgoing.forEach(oe -> {
				Edge<String, Integer> newedge = new BasicEdge<String, Integer>(ie.getSource(),
						oe.getTarget());
				newedge.setEdgeData(ie.getEdgeData() * oe.getEdgeData());
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
