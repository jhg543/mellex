package io.github.jhg543.mellex.listeners.flowmfp;

import io.github.jhg543.mellex.ASTHelper.InfSource;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

import java.util.HashMap;
import java.util.Set;

public class VariableUsageState {
	// as branchCondUsage 
	private HashMap<VariableDefinition, Set<InfSource>> variableUsage;
	private HashMap<String, Set<InfSource>> globalObjectUsage;
	private Set<InfSource> branchCondUsage;
	
	public HashMap<VariableDefinition, Set<InfSource>> getVariableUsage() {
		return variableUsage;
	}
	public void setVariableUsage(HashMap<VariableDefinition, Set<InfSource>> variableUsage) {
		this.variableUsage = variableUsage;
	}
	public HashMap<String, Set<InfSource>> getGlobalObjectUsage() {
		return globalObjectUsage;
	}
	public void setGlobalObjectUsage(HashMap<String, Set<InfSource>> globalObjectUsage) {
		this.globalObjectUsage = globalObjectUsage;
	}
	public Set<InfSource> getBranchCondUsage() {
		return branchCondUsage;
	}
	public void setBranchCondUsage(Set<InfSource> branchCondUsage) {
		this.branchCondUsage = branchCondUsage;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((branchCondUsage == null) ? 0 : branchCondUsage.hashCode());
		result = prime * result + ((globalObjectUsage == null) ? 0 : globalObjectUsage.hashCode());
		result = prime * result + ((variableUsage == null) ? 0 : variableUsage.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VariableUsageState other = (VariableUsageState) obj;
		if (branchCondUsage == null) {
			if (other.branchCondUsage != null)
				return false;
		} else if (!branchCondUsage.equals(other.branchCondUsage))
			return false;
		if (globalObjectUsage == null) {
			if (other.globalObjectUsage != null)
				return false;
		} else if (!globalObjectUsage.equals(other.globalObjectUsage))
			return false;
		if (variableUsage == null) {
			if (other.variableUsage != null)
				return false;
		} else if (!variableUsage.equals(other.variableUsage))
			return false;
		return true;
	}
	
	
	public VariableUsageState shallowCopy()
	{
		VariableUsageState s = new VariableUsageState();
		s.branchCondUsage = this.branchCondUsage;
		s.globalObjectUsage = this.globalObjectUsage;
		s.variableUsage = this.variableUsage;
		return s;
	}
	
}
