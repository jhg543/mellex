package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.ArrayList;
import java.util.List;

public class InstBuffer {
	private List<Instruction<VariableUsageState>> instbuffer = new ArrayList<>();

	public List<Instruction<VariableUsageState>> getInstbuffer() {
		return instbuffer;
	}

	public void setInstbuffer(List<Instruction<VariableUsageState>> instbuffer) {
		this.instbuffer = instbuffer;
	}
	
	public void add(Instruction<VariableUsageState> inst)
	{
		instbuffer.add(inst);
	}
	
}
