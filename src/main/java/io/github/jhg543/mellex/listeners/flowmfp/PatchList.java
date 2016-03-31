package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.Collection;

public class PatchList {
	private Instruction<State> startInstruction;
	private Collection<Instruction<State>> breakList;
	private Collection<Instruction<State>> ContinueList;
	private Collection<Instruction<State>> nextList;
	public Collection<Instruction<State>> getBreakList() {
		return breakList;
	}
	public void setBreakList(Collection<Instruction<State>> breakList) {
		this.breakList = breakList;
	}
	public Collection<Instruction<State>> getContinueList() {
		return ContinueList;
	}
	public void setContinueList(Collection<Instruction<State>> continueList) {
		ContinueList = continueList;
	}
	public Collection<Instruction<State>> getNextList() {
		return nextList;
	}
	public void setNextList(Collection<Instruction<State>> nextList) {
		this.nextList = nextList;
	}
	public Instruction<State> getStartInstruction() {
		return startInstruction;
	}
	public void setStartInstruction(Instruction<State> startInstruction) {
		this.startInstruction = startInstruction;
	}
	
	
}
