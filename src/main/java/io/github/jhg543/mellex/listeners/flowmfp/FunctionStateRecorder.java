package io.github.jhg543.mellex.listeners.flowmfp;

import com.google.common.collect.ImmutableList;
import io.github.jhg543.mellex.ASTHelper.plsql.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 *
 *
 */
public class FunctionStateRecorder extends StateFunc {

    private List<ParameterDefinition> outParameters;
    private Predicate<VariableDefinition> variableLiveChecker;

    public FunctionStateRecorder(List<ParameterDefinition> outParameters, Predicate<VariableDefinition> variableLiveChecker) {
        this.updates = new LinkedHashMap<ObjectDefinition, FilteredValueFunc>();
        this.branchCond = ValueFunc.of();
        this.value = ValueFunc.of();
        this.assigns = new LinkedHashMap<>();
        this.outParameters = outParameters;
        this.variableLiveChecker = variableLiveChecker;
    }


    public void addBranch(StateFunc branchCond) {
        this.branchCond = ValueFunc.combine(ImmutableList.of(this.branchCond, branchCond.getValue(), branchCond.getBranchCond()));
    }

    public void addReturnValue(StateFunc returnValue) {
        this.value = ValueFunc.combine(ImmutableList.of(this.value, returnValue.getValue(), returnValue.getBranchCond()));
    }

    public void collectOutParameterAssign(State s) {
        outParameters.forEach(pd -> {
            VariableState vs = s.readVarState().get(pd.getFunctionBodyVariable());
            if (vs != null) {
                addOutParameterAssign(pd, vs.getValueInfluence());
            }
        });
        s.readVarState().forEach((variableDefinition, variableState) -> {
            if (variableLiveChecker.test(variableDefinition)) {
                addOutParameterAssign(variableDefinition, variableState.getValueInfluence());
            }
        });
    }

    private void addOutParameterAssign(VariableDefinition parameterDefinition, ValueFunc parameterValue) {
        this.assigns.merge(parameterDefinition, parameterValue, (v1, v2) -> v1.add(v2));
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

    public StateFunc outputDefinition() {
        this.updates = Collections.unmodifiableMap(this.updates);
        this.assigns = Collections.unmodifiableMap(this.assigns);
        this.outParameters = null;
        this.variableLiveChecker = null;
        return this;
    }

}
