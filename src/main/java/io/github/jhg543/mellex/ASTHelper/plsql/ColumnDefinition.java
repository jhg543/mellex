package io.github.jhg543.mellex.ASTHelper.plsql;

public class ColumnDefinition extends ObjectDefinition {
	protected boolean infered = false;
	protected int position = 0; // starts from 0
	protected TableDefinition parent;

	public TableDefinition getParent() {
		return parent;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public String toString() {
		return "(C " + (parent == null ? "" : parent.getName()+".") + getName() + ")";
	}

	public boolean isInfered() {
		return infered;
	}

	public void setInfered(boolean infered) {
		this.infered = infered;
	}

	public ColumnDefinition(String name, int position, TableDefinition parent) {
		super();
		this.setName(name);
		this.setPosition(position);
		this.parent = parent;
	}

}
