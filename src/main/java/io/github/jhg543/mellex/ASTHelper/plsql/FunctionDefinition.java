package io.github.jhg543.mellex.ASTHelper.plsql;

import io.github.jhg543.mellex.ASTHelper.ObjectName;
import io.github.jhg543.mellex.ASTHelper.SubQuery;

import java.util.List;

public class FunctionDefinition {
	
	List<VariableDefinition> parameters;
	VariableDefinition returnValue;
	List<SubQuery> mods;
	ObjectName name;
	ControlBlock controlBlock;
	
	public List<VariableDefinition> getParameters() {
		return parameters;
	}
	public void setParameters(List<VariableDefinition> parameters) {
		this.parameters = parameters;
	}
	public VariableDefinition getReturnValue() {
		return returnValue;
	}
	public void setReturnValue(VariableDefinition returnValue) {
		this.returnValue = returnValue;
	}
	public List<SubQuery> getMods() {
		return mods;
	}
	public void setMods(List<SubQuery> mods) {
		this.mods = mods;
	}
	public ObjectName getName() {
		return name;
	}
	public void setName(ObjectName name) {
		this.name = name;
	}
	public ControlBlock getControlBlock() {
		return controlBlock;
	}
	public void setControlBlock(ControlBlock controlBlock) {
		this.controlBlock = controlBlock;
	}
	
}
