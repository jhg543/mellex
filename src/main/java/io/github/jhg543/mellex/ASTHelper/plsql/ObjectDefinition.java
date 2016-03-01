package io.github.jhg543.mellex.ASTHelper.plsql;

public class ObjectDefinition {
	private String name;
	private ControlBlock controlBlock;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ControlBlock getControlBlock() {
		return controlBlock;
	}
	public void setControlBlock(ControlBlock controlBlock) {
		this.controlBlock = controlBlock;
	}
	
}
