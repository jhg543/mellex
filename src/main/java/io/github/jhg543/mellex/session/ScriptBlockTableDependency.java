package io.github.jhg543.mellex.session;

import java.util.Set;

public class ScriptBlockTableDependency {
	private Set<String> provide;
	private Set<String> consume;
	private Set<String> consumeAsterisk;

	public ScriptBlockTableDependency(Set<String> provide, Set<String> consume, Set<String> consumeAsterisk) {
		super();
		this.provide = provide;
		this.consume = consume;
		this.consumeAsterisk = consumeAsterisk;
	}

	public Set<String> getProvide() {
		return provide;
	}

	public Set<String> getConsume() {
		return consume;
	}

	public Set<String> getConsumeAsterisk() {
		return consumeAsterisk;
	}
}
