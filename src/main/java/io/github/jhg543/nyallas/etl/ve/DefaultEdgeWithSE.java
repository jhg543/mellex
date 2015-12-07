package io.github.jhg543.nyallas.etl.ve;

import org.jgrapht.graph.DefaultEdge;

public class DefaultEdgeWithSE<V> extends DefaultEdge {

	public V getSource() {
		return (V) super.getSource();
	}

	public V getTarget() {
		return (V) super.getTarget();
	}
}
