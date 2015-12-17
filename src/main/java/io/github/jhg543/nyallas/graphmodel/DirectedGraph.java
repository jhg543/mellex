package io.github.jhg543.nyallas.graphmodel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DirectedGraph<VERTEX_DATA, VERTEX_MARKER, EDGE_DATA, EDGE_MARKER> {
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

		public Edge(Vertex source, Vertex target) {
			super();
			this.source = source;
			this.target = target;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
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
			Edge other = (Edge) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
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

		private DirectedGraph getOuterType() {
			return DirectedGraph.this;
		}
		
		

	}

	public class Vertex {
		private VERTEX_DATA vertexData;
		private VERTEX_MARKER marker;
		private Set<Edge> outgoingEdges;
		private Set<Edge> incomingEdges;

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

		public Set<Edge> getOutgoingEdges() {
			return outgoingEdges;
		}

		public Set<Edge> getIncomingEdges() {
			return incomingEdges;
		}
	}

	private Set<Vertex> vertexes = new HashSet<Vertex>();

	public Set<Vertex> getVertexes() {
		return vertexes;
	}

	/**
	 * it's user's responsibility to ensure vertex added first
	 * 
	 * @param e
	 */
	public Vertex addVertex() {
		Vertex v = new Vertex();
		v.incomingEdges = new HashSet<>();
		v.outgoingEdges = new HashSet<>();
		vertexes.add(v);
		return v;
	}

	public Edge newEdge(Vertex source, Vertex target) {
		return new Edge(source, target);
	}

	public void addEdge(Edge e) {
		e.getSource().getOutgoingEdges().add(e);
		e.getTarget().getIncomingEdges().add(e);
	}
}
