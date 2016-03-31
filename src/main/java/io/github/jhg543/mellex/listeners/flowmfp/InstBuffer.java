package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.ArrayList;
import java.util.List;

public class InstBuffer {
	private List<Instruction<State>> instbuffer = new ArrayList<>();

	public List<Instruction<State>> getInstbuffer() {
		return instbuffer;
	}

	public void setInstbuffer(List<Instruction<State>> instbuffer) {
		this.instbuffer = instbuffer;
	}
	
	public void add(Instruction<State> inst)
	{
		instbuffer.add(inst);
	}
	
}
