package io.github.jhg543.mellex.ASTHelper.symbol;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

import io.github.jhg543.mellex.ASTHelper.plsql.ControlBlock;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;

/**
 * variables or functions
 */
public class LocalObjectResolver {
	private LinkedList<ControlBlock> scopes = new LinkedList<>();
	private LinkedList<Boolean> parentScopeVisible = new LinkedList<>();
	private LinkedList<Map<String, ObjectDefinition>> content = new LinkedList<>();
	private Function<String, String> nameNormalizer;
	private CharMatcher dotmatcher = CharMatcher.is('.');

	public ObjectDefinition searchByName(String name) {
		if (dotmatcher.matchesAnyOf(name)) {
			// table.function ---> go to global or alias
			return null;
		}
		name = nameNormalizer.apply(name);
		for (int i = content.size() - 1; i > 0; --i) {
			Map<String, ObjectDefinition> m = content.get(i);
			ObjectDefinition d = m.get(name);
			if (d != null) {
				return d;
			}
			if (!parentScopeVisible.get(i)) {
				return null;
			}
		}
		return null;
	}

	public <T> T searchByName(String name, Class<T> expectedClass) {
		ObjectDefinition d = searchByName(name);
		if (expectedClass.isInstance(d)) {
			return (T) d;
		} else {
			return null;
		}

	}

	public void pushScope(ControlBlock scope, boolean parentScopeVisible) {
		this.scopes.add(scope);
		this.parentScopeVisible.add(parentScopeVisible);
		this.content.add(new HashMap<>());
	}

	public void popScope(ControlBlock expectedScope) {
		Preconditions.checkState(expectedScope == this.scopes.removeLast(), "popped scope is not as expected");
		this.parentScopeVisible.removeLast();
		this.content.removeLast();
	}

	public void addDefinition(String name, ObjectDefinition def) {
		name = nameNormalizer.apply(name);
		def.setName(name);
		Preconditions.checkState(this.content.getLast().put(name, def) == null, "duplicate object name %s", name);
	}

	public LocalObjectResolver(Function<String, String> nameNormalizer) {
		super();
		this.nameNormalizer = nameNormalizer;
	}

}
