package io.github.jhg543.mellex.ASTHelper.plsql;

public class ColumnDefinition extends ObjectDefinition {
	private boolean infered = false;
	@Override
	public String toString() {
		return "Cdef [" + getName() + "]";
	}
	
	public boolean isInfered() {
		return infered;
	}
	public void setInfered(boolean infered) {
		this.infered = infered;
	}

	public ColumnDefinition(String name) {
		super();
		this.setName(name);
	}
	
}
