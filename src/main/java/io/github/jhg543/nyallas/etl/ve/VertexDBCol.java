package io.github.jhg543.nyallas.etl.ve;

public class VertexDBCol {
	private int internal_id;
	private String fqn;
	private String schema;
	private String table;
	private String column;
	public String getFqn() {
		return fqn;
	}
	public void setFqn(String fqn) {
		this.fqn = fqn;
	}
	public String getSchema() {
		return schema;
	}
	public void setSchema(String schema) {
		this.schema = schema;
	}
	public String getTable() {
		return table;
	}
	public void setTable(String table) {
		this.table = table;
	}
	public String getColumn() {
		return column;
	}
	public void setColumn(String column) {
		this.column = column;
	}
	public int getInternal_id() {
		return internal_id;
	}
	public void setInternal_id(int internal_id) {
		this.internal_id = internal_id;
	}
}
