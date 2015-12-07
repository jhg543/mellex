package io.github.jhg543.nyallas.etl.algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import io.github.jhg543.nyallas.etl.ve.EdgeETL;
import io.github.jhg543.nyallas.etl.ve.VertexDBCol;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DirectedPseudograph;

public class LineageFinder {

	DirectedGraph<VertexDBCol, EdgeETL> g;
	Set<VertexDBCol> visited = new HashSet<VertexDBCol>();
	Predicate<EdgeETL> test;
	List<EdgeETL> results = new ArrayList<EdgeETL>();
	private void findA(VertexDBCol v) {
		if (visited.contains(v)) {
			return;
		}

		visited.add(v);

		Set<EdgeETL> es = g.outgoingEdgesOf(v);
		for (EdgeETL e : es) {
			if (test.test(e)) {
				results.add(e);
			}
			findA(e.getTarget());
		}

	}

	private void findB(VertexDBCol v) {
		if (visited.contains(v)) {
			return;
		}

		visited.add(v);

		Set<EdgeETL> es = g.incomingEdgesOf(v);
		for (EdgeETL e : es) {
			if (test.test(e)) {
				results.add(e);
			}
			findB(e.getSource());
		}

	}
	
	private void findBAfterA(VertexDBCol v) {
		
		Set<EdgeETL> es = g.incomingEdgesOf(v);
		for (EdgeETL e : es) {
			if (test.test(e)) {
				results.add(e);
			}
			findB(e.getSource());
		}

	}

	public static List<EdgeETL> find(DirectedGraph<VertexDBCol, EdgeETL> g, List<VertexDBCol> vs, Predicate<EdgeETL> test) {
		LineageFinder d = new LineageFinder();
		d.g = g;
		d.test = test;
		for (VertexDBCol v : vs) {
			d.findA(v);
			d.findBAfterA(v);
		}
		return d.results;
	}
}
