package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.github.jhg543.mellex.ASTHelper.plsql.FunctionDefinition;

public class InstBuffer {

	private LinkedList<OneFunctionBuffer> stack = new LinkedList<>();

	public void enterFunctionDef(FunctionDefinition fndef) {
		OneFunctionBuffer one = new OneFunctionBuffer();
		one.fndef = fndef;
		stack.push(one);
	}

	public void exitFunctionDef() {
		stack.pop();
	}

	public PatchList add(Instruction inst) {
		return stack.peek().add(inst);
	}

	private static class OneFunctionBuffer {
		private FunctionDefinition fndef;

		private List<Instruction> instbuffer = new ArrayList<>();

		public List<Instruction> getInstbuffer() {
			return instbuffer;
		}

		public void setInstbuffer(List<Instruction> instbuffer) {
			this.instbuffer = instbuffer;
		}

		public PatchList add(Instruction inst) {
			inst.setId(instbuffer.size());
			instbuffer.add(inst);
			return PatchList.singleInstrution(inst);
		}

	}
}
