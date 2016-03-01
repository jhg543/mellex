package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.Collection;
import java.util.function.Function;

public class  Instruction<S> {
	private Function<S,S> func;
	private Collection<Instruction<S>> nextPc;
	
	public Function<S, S> getFunc() {
		return func;
	}
	public void setFunc(Function<S, S> func) {
		this.func = func;
	}
	public Collection<Instruction<S>> getNextPc() {
		return nextPc;
	}
	public void setNextPc(Collection<Instruction<S>> nextPc) {
		this.nextPc = nextPc;
	}
	
	
}
