package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.Collection;

public class PatchList {
	private Instruction startInstruction;
	private Collection<Instruction> breakList;
	private Collection<Instruction> ContinueList;
	private Collection<Instruction> nextList;
	public Collection<Instruction> getBreakList() {
		return breakList;
	}
	public void setBreakList(Collection<Instruction> breakList) {
		this.breakList = breakList;
	}
	public Collection<Instruction> getContinueList() {
		return ContinueList;
	}
	public void setContinueList(Collection<Instruction> continueList) {
		ContinueList = continueList;
	}
	public Collection<Instruction> getNextList() {
		return nextList;
	}
	public void setNextList(Collection<Instruction> nextList) {
		this.nextList = nextList;
	}
	public Instruction getStartInstruction() {
		return startInstruction;
	}
	public void setStartInstruction(Instruction startInstruction) {
		this.startInstruction = startInstruction;
	}
	
	
}
