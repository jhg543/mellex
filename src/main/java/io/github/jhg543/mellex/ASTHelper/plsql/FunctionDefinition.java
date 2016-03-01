package io.github.jhg543.mellex.ASTHelper.plsql;

import io.github.jhg543.mellex.ASTHelper.InfSource;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class FunctionDefinition extends ObjectDefinition {
	
	List<VariableDefinition> parameters;
	private HashMap<String, Set<InfSource>> globalObjectUsage;
	private Set<InfSource> branchCondUsage;
	private Set<InfSource> returnValueUsage;
	public List<VariableDefinition> getParameters() {
		return parameters;
	}
	public void setParameters(List<VariableDefinition> parameters) {
		this.parameters = parameters;
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
	public Set<InfSource> getReturnValueUsage() {
		return returnValueUsage;
	}
	public void setReturnValueUsage(Set<InfSource> returnValueUsage) {
		this.returnValueUsage = returnValueUsage;
	}
	
	
}
