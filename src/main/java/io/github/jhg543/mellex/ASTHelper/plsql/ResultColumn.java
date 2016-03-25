package io.github.jhg543.mellex.ASTHelper.plsql;



public class ResultColumn {
	private String name; // name null = insert into xxx values ()
	private int position = 0; // starts from 0
	private StateFunc expr;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public StateFunc getExpr() {
		return expr;
	}
	public void setExpr(StateFunc expr) {
		this.expr = expr;
	}
	public ResultColumn(String name, int position, StateFunc expr) {
		super();
		this.name = name;
		this.position = position;
		this.expr = expr;
	}
	
}
