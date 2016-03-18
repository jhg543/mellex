package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.List;

public class VariableDefinition extends ObjectDefinition {
	private List<VariableModification> mods;
	private VariableModification defaultValue;
	private Boolean isScalar;
	private boolean isConst = false;
	public List<VariableModification> getMods() {
		return mods;
	}
	public void setMods(List<VariableModification> mods) {
		this.mods = mods;
	}
	public Boolean getIsScalar() {
		return isScalar;
	}
	public void setIsScalar(Boolean isScalar) {
		this.isScalar = isScalar;
	}
	public VariableModification getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(VariableModification defaultValue) {
		this.defaultValue = defaultValue;
	}
	public boolean isConst() {
		return isConst;
	}
	public void setConst(boolean isConst) {
		this.isConst = isConst;
	}
	@Override
	public String toString() {
		return "Vdef [" + getName() + "]";
	}
	
}
