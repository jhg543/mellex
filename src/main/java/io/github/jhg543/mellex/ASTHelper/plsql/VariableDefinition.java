package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.HashMap;
import java.util.Map;

public class VariableDefinition extends ObjectDefinition {

	private Map<String, VariableDefinition> columns;

	@Override
	public String toString() {
		return "Vdef [" + getName() + "]";
	}

	public Map<String, VariableDefinition> getColumns() {
		return columns;
	}

	public VariableDefinition getColumn(String name) {
		if (columns == null) {
			columns = new HashMap<>();
		}

		VariableDefinition v = columns.get(name);
		if (v == null) {

			v = new VariableDefinition();
			v.setControlBlock(getControlBlock());
			v.setName(getName() + "." + name);
			columns.put(name, v);
		}
		return v;

	}

}
