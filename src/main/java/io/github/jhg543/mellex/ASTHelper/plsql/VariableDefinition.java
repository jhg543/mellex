package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.List;

public class VariableDefinition {
	private List<VariableModification> mods;
	private VariableModification defaultValue;
	private Boolean isScalar;
	private String name;
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
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public VariableModification getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(VariableModification defaultValue) {
		this.defaultValue = defaultValue;
	}
	
}
