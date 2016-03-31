package io.github.jhg543.mellex.ASTHelper.plsql;

public class FilteredValueFunc {
	private ValueFunc value;
	private ValueFunc filter;
	public ValueFunc getValue() {
		return value;
	}
	public ValueFunc getFilter() {
		return filter;
	}
	public FilteredValueFunc(ValueFunc value, ValueFunc filter) {
		super();
		this.value = value;
		this.filter = filter;
	}
	
	
	
}
