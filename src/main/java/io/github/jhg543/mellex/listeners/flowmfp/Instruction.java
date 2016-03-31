package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

public class Instruction {
	private Object debugInfo;

	private Function<State, State> func;
	private Collection<Supplier<Instruction>> nextPc;

	public Function<State, State> getFunc() {
		return func;
	}

	public void setFunc(Function<State, State> func) {
		this.func = func;
	}

	public Collection<Supplier<Instruction>> getNextPc() {
		return nextPc;
	}

	public void setNextPc(Collection<Supplier<Instruction>> nextPc) {
		this.nextPc = nextPc;
	}

	public Object getDebugInfo() {
		return debugInfo;
	}

	public void setDebugInfo(Object debugInfo) {
		this.debugInfo = debugInfo;
	}

	public Instruction(Object debugInfo) {
		super();
		this.debugInfo = debugInfo;
	}

}
