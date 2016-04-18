package io.github.jhg543.mellex.listeners.flowmfp;

import com.google.common.collect.ImmutableList;
import io.github.jhg543.mellex.ASTHelper.plsql.*;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * 
 *
 */
public class FunctionStateRecorder extends StateFunc {
	public FunctionStateRecorder() {
		this.updates = new LinkedHashMap<ObjectDefinition, FilteredValueFunc>();
		this.branchCond = ValueFunc.of();
	}

	public void addBranch(StateFunc branchCond) {
		this.branchCond = ValueFunc.combine(ImmutableList.of(this.branchCond,branchCond.getValue(),branchCond.getBranchCond()));
	}

	public void addReturnValue() {

	}

	public void addOutParameterAssign() {

	}

	public void addInsertOrUpdate(ColumnDefinition cdef, StateFunc sub) {
		if (cdef != null) {
			FilteredValueFunc fc = new FilteredValueFunc(sub.getValue(), sub.getBranchCond());
			this.updates.merge(cdef, fc, (v1, v2) -> v1.add(v2));
		}
		for (Entry<ObjectDefinition, FilteredValueFunc> e : sub.getUpdates().entrySet()) {
			this.updates.merge(e.getKey(), e.getValue(), (v1, v2) -> v1.add(v2));
		}

		// Assigns of stateFunc goes to per-instruction state.
	}

}
