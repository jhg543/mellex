package io.github.jhg543.nyallas.etl.ve;

import org.jgrapht.graph.DefaultEdge;

public class EdgeETL extends DefaultEdgeWithSE<VertexDBCol> {
	private String scriptname;
	private String conntype;

	public String getScriptname() {
		return scriptname;
	}

	public void setScriptname(String scriptname) {
		this.scriptname = scriptname;
	}

	public String getConntype() {
		return conntype;
	}

	public void setConntype(String conntype) {
		this.conntype = conntype;
	}

}
