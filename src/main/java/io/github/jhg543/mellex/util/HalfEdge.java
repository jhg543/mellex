package io.github.jhg543.mellex.util;

public class HalfEdge {
	private int dest;
	private int type;
	private int marker;

	public int getDest() {
		return dest;
	}

	public int getType() {
		return type;
	}

	public int getMarker() {
		return marker;
	}

	public HalfEdge(int dest, int type, int marker) {
		super();
		this.dest = dest;
		this.type = type;
		this.marker = marker;
	}

	public static int TYPE_DIRECT = 0;
	public static int TYPE_INDIRECT = 1;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dest;
		result = prime * result + marker;
		result = prime * result + type;
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
		HalfEdge other = (HalfEdge) obj;
		if (dest != other.dest)
			return false;
		if (marker != other.marker)
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
	
}
