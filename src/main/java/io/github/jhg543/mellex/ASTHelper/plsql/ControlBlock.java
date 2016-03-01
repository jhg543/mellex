package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.List;

import io.github.jhg543.mellex.ASTHelper.Influences;

public class ControlBlock {
	private List<ObjectDefinition> decls;
	private ControlBlock parentBlock;
	public List<ObjectDefinition> getDecls() {
		return decls;
	}
	public void setDecls(List<ObjectDefinition> decls) {
		this.decls = decls;
	}
	public ControlBlock getParentBlock() {
		return parentBlock;
	}
	public void setParentBlock(ControlBlock parentBlock) {
		this.parentBlock = parentBlock;
	}
	
}
