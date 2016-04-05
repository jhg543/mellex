package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.Collection;
import java.util.Collections;

public class PatchList {
	
	private Instruction startInstruction;
	private Collection<Instruction> nextList;
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
	public static  PatchList singleInstrution(Instruction instruction)
	{
		PatchList p = new PatchList();
		p.setStartInstruction(instruction);
		p.setNextList(Collections.singletonList(instruction));
		return p;
	}
	
}
