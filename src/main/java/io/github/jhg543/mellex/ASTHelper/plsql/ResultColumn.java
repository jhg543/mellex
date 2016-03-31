package io.github.jhg543.mellex.ASTHelper.plsql;



public class ResultColumn extends ColumnDefinition {
	protected StateFunc expr;
	

	public StateFunc getExpr() {
		return expr;
	}
	public void setExpr(StateFunc expr) {
		this.expr = expr;
	}
	public ResultColumn(String name, int position, StateFunc expr) {
		super(name,position,null);
		this.expr = expr;
	}
	
}
