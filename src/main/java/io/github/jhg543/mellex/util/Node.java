package io.github.jhg543.mellex.util;

import java.util.List;

public interface Node {
	List<HalfEdge> getOutEdges();
	int getVisitStatus();
}
