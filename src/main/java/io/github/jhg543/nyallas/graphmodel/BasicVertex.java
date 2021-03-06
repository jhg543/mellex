package io.github.jhg543.nyallas.graphmodel;

import java.util.HashSet;
import java.util.Set;

public class BasicVertex <VERTEX_DATA, EDGE_DATA> implements Vertex<VERTEX_DATA, EDGE_DATA> {
	VERTEX_DATA vertexData;
	Object marker;
	Set<Edge<VERTEX_DATA, EDGE_DATA>> outgoingEdges = new HashSet<>();
	Set<Edge<VERTEX_DATA, EDGE_DATA>> incomingEdges = new HashSet<>();

	public VERTEX_DATA getVertexData() {
		return vertexData;
	}

	public void setVertexData(VERTEX_DATA vertexData) {
		this.vertexData = vertexData;
	}

	public Object getMarker() {
		return marker;
	}

	public void setMarker(Object marker) {
		this.marker = marker;
	}

	public Set<Edge<VERTEX_DATA, EDGE_DATA>> getOutgoingEdges() {
		return outgoingEdges;
	}

	public Set<Edge<VERTEX_DATA, EDGE_DATA>> getIncomingEdges() {
		return incomingEdges;
	}
}
