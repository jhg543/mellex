package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.Arrays;
import java.util.List;

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

	public static FilteredValueFunc combine(List<FilteredValueFunc> subs) {
		if (subs.size() == 1) {
			return subs.get(0);
		}
		ValueFunc.Builder values = new ValueFunc.Builder();
		ValueFunc.Builder filters = new ValueFunc.Builder();
		for (FilteredValueFunc fn : subs) {
			values.add(fn.getValue());
			filters.add(fn.getFilter());
		}
		return new FilteredValueFunc(values.Build(), filters.Build());
	}

	public FilteredValueFunc add(FilteredValueFunc... other) {
		ValueFunc.Builder values = new ValueFunc.Builder();
		ValueFunc.Builder filters = new ValueFunc.Builder();
		values.add(this.value);
		filters.add(this.filter);
		for (FilteredValueFunc fn : other) {
			values.add(fn.getValue());
			filters.add(fn.getFilter());
		}
		ValueFunc newv = values.Build();
		ValueFunc newf = filters.Build();
		return new FilteredValueFunc(newv, newf);
	}

}
