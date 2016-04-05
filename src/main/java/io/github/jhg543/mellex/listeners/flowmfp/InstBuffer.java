package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.ArrayList;
import java.util.List;

public class InstBuffer {
	private List<Instruction> instbuffer = new ArrayList<>();

	public List<Instruction> getInstbuffer() {
		return instbuffer;
	}

	public void setInstbuffer(List<Instruction> instbuffer) {
		this.instbuffer = instbuffer;
	}
	
	public PatchList add(Instruction inst)
	{
		instbuffer.add(inst);
		return PatchList.singleInstrution(inst);
	}
	
}
