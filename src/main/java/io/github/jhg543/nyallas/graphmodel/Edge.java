package io.github.jhg543.nyallas.graphmodel;

public interface Edge<VERTEX_DATA, EDGE_DATA> {

	EDGE_DATA getEdgeData();

	void setEdgeData(EDGE_DATA edgeData);

	Object getMarker();

	void setMarker(Object marker);

	Vertex<VERTEX_DATA, EDGE_DATA> getSource();

	Vertex<VERTEX_DATA, EDGE_DATA> getTarget();

}
