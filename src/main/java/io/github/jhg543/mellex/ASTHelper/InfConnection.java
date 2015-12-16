package io.github.jhg543.mellex.ASTHelper;

public enum InfConnection {
	IN_RESULT_EXPRESSION(1), IN_CLAUSE(0);

	InfConnection(int marker) {
		this.marker = marker;
	}

	private final int marker;

	public int getMarker() {
		return marker;
	}
}
