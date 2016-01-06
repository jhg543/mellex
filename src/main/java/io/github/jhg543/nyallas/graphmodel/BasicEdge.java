package io.github.jhg543.nyallas.graphmodel;

public class BasicEdge<VERTEX_DATA, EDGE_DATA> implements Edge<VERTEX_DATA, EDGE_DATA>{
	EDGE_DATA edgeData;
	Object marker;
	Vertex<VERTEX_DATA, EDGE_DATA> source;
	Vertex<VERTEX_DATA, EDGE_DATA> target;

	public EDGE_DATA getEdgeData() {
		return edgeData;
	}

	public void setEdgeData(EDGE_DATA edgeData) {
		this.edgeData = edgeData;
	}

	public Object getMarker() {
		return marker;
	}

	public void setMarker(Object marker) {
		this.marker = marker;
	}

	public Vertex<VERTEX_DATA, EDGE_DATA> getSource() {
		return source;
	}

	public Vertex<VERTEX_DATA, EDGE_DATA> getTarget() {
		return target;
	}

	public BasicEdge(Vertex<VERTEX_DATA, EDGE_DATA> source, Vertex<VERTEX_DATA, EDGE_DATA> target) {
		super();
		this.source = source;
		this.target = target;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((edgeData == null) ? 0 : edgeData.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BasicEdge other = (BasicEdge) obj;
		if (edgeData == null) {
			if (other.edgeData != null)
				return false;
		} else if (!edgeData.equals(other.edgeData))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

}
