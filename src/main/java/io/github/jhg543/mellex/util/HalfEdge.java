package io.github.jhg543.mellex.util;

public class HalfEdge {
	private int dest;
	private int type;
	private int statementid;

	public int getDest() {
		return dest;
	}

	public int getType() {
		return type;
	}

	public int getStatementid() {
		return statementid;
	}

	public HalfEdge(int dest, int type, int statementid) {
		super();
		this.dest = dest;
		this.type = type;
		this.statementid = statementid;
	}

	public static int TYPE_DIRECT = 0;
	public static int TYPE_INDIRECT = 1;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dest;
		result = prime * result + statementid;
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
		if (statementid != other.statementid)
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
	
}
