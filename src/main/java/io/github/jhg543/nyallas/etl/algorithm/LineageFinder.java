package io.github.jhg543.nyallas.etl.algorithm;

import io.github.jhg543.nyallas.etl.ve.EdgeETL;
import io.github.jhg543.nyallas.etl.ve.VertexDBCol;
import io.github.jhg543.nyallas.graphmodel.DirectedGraph;
import io.github.jhg543.nyallas.graphmodel.Edge;
import io.github.jhg543.nyallas.graphmodel.Vertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class LineageFinder {

	DirectedGraph<Vertex<VertexDBCol, EdgeETL>,Edge<VertexDBCol, EdgeETL>> g;
	Predicate<Edge<VertexDBCol, EdgeETL>> test;
	List<Edge<VertexDBCol, EdgeETL>> results = new ArrayList<>();

	private void findA(Vertex<VertexDBCol, EdgeETL> v) {
		if (Boolean.TRUE.equals(v.getMarker())) {
			return;
		}

		v.setMarker(Boolean.TRUE);

		Set<Edge<VertexDBCol, EdgeETL>> es = v.getOutgoingEdges();
		for (Edge<VertexDBCol, EdgeETL> e : es) {
			if (test.test(e)) {
				results.add(e);
			}
			findA(e.getTarget());
		}

	}

	private void findB(Vertex<VertexDBCol, EdgeETL> v) {
		if (Boolean.TRUE.equals(v.getMarker())) {
			return;
		}

		v.setMarker(Boolean.TRUE);

		Set<Edge<VertexDBCol, EdgeETL>> es = v.getIncomingEdges();
		for (Edge<VertexDBCol, EdgeETL> e : es) {
			if (test.test(e)) {
				results.add(e);
			}
			findB(e.getSource());
		}

	}

	private void findBAfterA(Vertex<VertexDBCol, EdgeETL> v) {

		Set<Edge<VertexDBCol, EdgeETL>> es = v.getIncomingEdges();
		for (Edge<VertexDBCol, EdgeETL> e : es) {
			if (test.test(e)) {
				results.add(e);
			}
			findB(e.getSource());
		}

	}

	public static List<Edge<VertexDBCol, EdgeETL>> find(DirectedGraph<Vertex<VertexDBCol, EdgeETL>,Edge<VertexDBCol, EdgeETL>> g,
			List<Vertex<VertexDBCol, EdgeETL>> vs, Predicate<Edge<VertexDBCol, EdgeETL>> test, Boolean forward, Boolean backward) {
		LineageFinder d = new LineageFinder();
		d.g = g;
		d.test = test;
		g.getVertexes().forEach(x -> x.setMarker(Boolean.FALSE));

		if (forward && backward || !forward && !backward) {
			throw new RuntimeException("not implemented");
		}
		for (Vertex<VertexDBCol, EdgeETL> v : vs) {
			if (forward) {
				d.findA(v);
			}
			if (backward) {
				d.findB(v);
			}
			// d.findBAfterA(v);
		}
		return d.results;
	}
}
