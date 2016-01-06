package io.github.jhg543.nyallas.graphmodel;

import java.util.Set;

public interface Vertex<VERTEX_DATA, EDGE_DATA> {
	VERTEX_DATA getVertexData();

	void setVertexData(VERTEX_DATA vertexData);

	Object getMarker();

	void setMarker(Object marker);

	Set<Edge<VERTEX_DATA, EDGE_DATA>> getOutgoingEdges();

	Set<Edge<VERTEX_DATA, EDGE_DATA>> getIncomingEdges();
}
