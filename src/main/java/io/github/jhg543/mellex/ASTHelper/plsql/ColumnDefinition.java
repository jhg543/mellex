package io.github.jhg543.mellex.ASTHelper.plsql;

public class ColumnDefinition extends ObjectDefinition {
	protected boolean infered = false;
	protected int position = 0; // starts from 0
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	
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

	public ColumnDefinition(String name, int position) {
		super();
		this.setName(name);
		this.setPosition(position);
		

	}

}
