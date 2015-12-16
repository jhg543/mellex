package io.github.jhg543.mellex.ASTHelper;

public class CreateTableStmt extends SubQuery  {
	private SubQuery viewDef;
	
	private boolean isVolatile = false;

	public boolean isVolatile() {
		return isVolatile;
	}

	public void setVolatile(boolean isVolatile) {
		this.isVolatile = isVolatile;
	}

	public SubQuery getViewDef() {
		return viewDef;
	}
	
	public void setViewDef(SubQuery viewDef) {
		this.viewDef = viewDef;
	}
	
}
