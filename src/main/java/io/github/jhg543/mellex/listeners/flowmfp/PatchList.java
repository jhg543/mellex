package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.Collection;

public class PatchList {
	private Instruction<VariableUsageState> startInstruction;
	private Collection<Instruction<VariableUsageState>> breakList;
	private Collection<Instruction<VariableUsageState>> ContinueList;
	private Collection<Instruction<VariableUsageState>> nextList;
	public Collection<Instruction<VariableUsageState>> getBreakList() {
		return breakList;
	}
	public void setBreakList(Collection<Instruction<VariableUsageState>> breakList) {
		this.breakList = breakList;
	}
	public Collection<Instruction<VariableUsageState>> getContinueList() {
		return ContinueList;
	}
	public void setContinueList(Collection<Instruction<VariableUsageState>> continueList) {
		ContinueList = continueList;
	}
	public Collection<Instruction<VariableUsageState>> getNextList() {
		return nextList;
	}
	public void setNextList(Collection<Instruction<VariableUsageState>> nextList) {
		this.nextList = nextList;
	}
	public Instruction<VariableUsageState> getStartInstruction() {
		return startInstruction;
	}
	public void setStartInstruction(Instruction<VariableUsageState> startInstruction) {
		this.startInstruction = startInstruction;
	}
	
	
}
