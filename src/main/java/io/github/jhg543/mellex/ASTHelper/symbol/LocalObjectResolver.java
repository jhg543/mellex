package io.github.jhg543.mellex.ASTHelper.symbol;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import io.github.jhg543.mellex.ASTHelper.plsql.ControlBlock;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

/**
 * variables or functions
 */
public class LocalObjectResolver {

	private LinkedList<Scope> scopes = new LinkedList<>();
	private static Splitter dotsplitter = Splitter.on('.');

	public ObjectDefinition searchByName(String name) {

		List<String> namedotsplit = dotsplitter.splitToList(name);
		if (namedotsplit.size() == 1) {
			return searchBySimpleName(name);
		} else if (namedotsplit.size() == 2) {
			ObjectDefinition d = searchBySimpleName(namedotsplit.get(0));
			if (d == null) {
				return null;
			}
			if (!(d instanceof VariableDefinition))
			{
				// function does not have columns
				return null;
			}
			throw new RuntimeException("TODO DEBUG MARKER variable.column" + name);
			//return ((VariableDefinition)d).getColumn(namedotsplit.get(1)); 

		} else {
			// size>2, leave to global
			return null;
		}

	}

	private ObjectDefinition searchBySimpleName(String name) {
		// assume no "." in name
		for (Scope s : scopes) {
			Map<String, ObjectDefinition> m = s.getContent();
			ObjectDefinition d = m.get(name);
			if (d != null) {
				return d;
			}
			if (!s.isParentScopeVisible()) {
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

	public void pushScope(Object scopeId, boolean parentScopeVisible) {
		Scope s = new Scope();
		s.setId(scopeId);
		s.setParentScopeVisible(parentScopeVisible);
		s.setContent(new HashMap<>());
		this.scopes.push(s);
	}

	public void popScope(Object expectedScopeId) {
		Preconditions.checkState(expectedScopeId == this.scopes.pop().getId(), "popped scope is not as expected");
	}

	public void addDefinition(String name, ObjectDefinition def) {
		def.setName(name);
		Preconditions.checkState(this.scopes.peek().getContent().put(name, def) == null, "duplicate object name %s", name);
	}

	private static class Scope {
		Object id;
		boolean parentScopeVisible;
		Map<String, ObjectDefinition> content;

		public Object getId() {
			return id;
		}

		public void setId(Object id) {
			this.id = id;
		}

		public boolean isParentScopeVisible() {
			return parentScopeVisible;
		}

		public void setParentScopeVisible(boolean parentScopeVisible) {
			this.parentScopeVisible = parentScopeVisible;
		}

		public Map<String, ObjectDefinition> getContent() {
			return content;
		}

		public void setContent(Map<String, ObjectDefinition> content) {
			this.content = content;
		}

	}
}
