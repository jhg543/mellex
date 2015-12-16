package io.github.jhg543.nyallas.graphmodel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DirectedGraph<VERTEX_DATA,VERTEX_MARKER,EDGE_DATA,EDGE_MARKER> {
	public class Edge {
		private EDGE_DATA edgeData;
		private EDGE_MARKER marker;
		private Vertex source;
		private Vertex target;
		public EDGE_DATA getEdgeData() {
			return edgeData;
		}
		public void setEdgeData(EDGE_DATA edgeData) {
			this.edgeData = edgeData;
		}
		public EDGE_MARKER getMarker() {
			return marker;
		}
		public void setMarker(EDGE_MARKER marker) {
			this.marker = marker;
		}
		public Vertex getSource() {
			return source;
		}
		public Vertex getTarget() {
			return target;
		}
		public Edge(Vertex source,	Vertex target) {
			super();
			this.source = source;
			this.target = target;
		}
		
		
	}
	
	public class Vertex {
		private VERTEX_DATA vertexData;
		private VERTEX_MARKER marker;
		private List<Edge> outgoingEdges;
		private List<Edge> incomingEdges;
		public VERTEX_DATA getVertexData() {
			return vertexData;
		}
		public void setVertexData(VERTEX_DATA vertexData) {
			this.vertexData = vertexData;
		}
		public VERTEX_MARKER getMarker() {
			return marker;
		}
		public void setMarker(VERTEX_MARKER marker) {
			this.marker = marker;
		}
		public List<Edge> getOutgoingEdges() {
			return outgoingEdges;
		}
		public List<Edge> getIncomingEdges() {
			return incomingEdges;
		}
	}
	
	private Set<Vertex> vertexes = new HashSet<Vertex>();
	
	public Set<Vertex> getVertexes()
	{
		return vertexes;
	}
	
	/**
	 * it's user's responsbility to ensure vertex added first
	 * @param e
	 */
	public void addEdge(Edge e)
	{
		e.getSource().getOutgoingEdges().add(e);
		e.getTarget().getIncomingEdges().add(e);
	}
}
