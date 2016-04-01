package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import io.github.jhg543.mellex.ASTHelper.plsql.ColumnDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.FilteredValueFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;

/**
 * 
 *
 */
public class FunctionStateRecorder extends StateFunc {
	public FunctionStateRecorder() {
		this.updates = new LinkedHashMap<ObjectDefinition, FilteredValueFunc>();
	}

	public void addBranch() {

	}

	public void addReturnValue() {

	}

	public void addOutParameterAssign() {

	}

	public void addInsertOrUpdate(ColumnDefinition cdef, StateFunc sub) {
		FilteredValueFunc fc = new FilteredValueFunc(sub.getValue(), sub.getBranchCond());
		this.updates.merge(cdef, fc, (v1, v2) -> v1.add(v2));
		for (Entry<ObjectDefinition, FilteredValueFunc> e : sub.getUpdates().entrySet()) {
			this.updates.merge(e.getKey(), e.getValue(), (v1, v2) -> v1.add(v2));
		}

		// Assigns of stateFunc goes to per-instruction state.
	}

}
