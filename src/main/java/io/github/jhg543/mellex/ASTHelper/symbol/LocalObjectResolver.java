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
import io.github.jhg543.mellex.ASTHelper.plsql.CursorDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.FunctionDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

/**
 * variables or functions
 */
public class LocalObjectResolver {

	private LinkedList<Scope> scopes = new LinkedList<>();
	private static Splitter dotsplitter = Splitter.on('.');

	public VariableDefinition searchVariable(String name) {

		List<String> namedotsplit = dotsplitter.splitToList(name);
		if (namedotsplit.size() == 1) {
			return searchVariableBySimpleName(name);
		} else if (namedotsplit.size() == 2) {
			VariableDefinition d = searchVariableBySimpleName(namedotsplit.get(0));
			if (d == null) {
				return null;
			}
			if (!(d instanceof VariableDefinition))
			{
				// function does not have columns
				return null;
			}
			//throw new RuntimeException("TODO DEBUG MARKER variable.column" + name);
			return ((VariableDefinition)d).getColumn(namedotsplit.get(1)); 

		} else {
			// size>2, leave to global
			return null;
		}

	}
	
	public CursorDefinition searchCursor(String name)
	{
		for (Scope s : scopes) {
			Map<String, CursorDefinition> m = s.getCursors();
			CursorDefinition d = m.get(name);
			if (d != null) {
				return d;
			}
			if (!s.isParentScopeVisible()) {
				return null;
			}
		}
		return null;		
	}

	private VariableDefinition searchVariableBySimpleName(String name) {
		// assume no "." in name
		for (Scope s : scopes) {
			Map<String, VariableDefinition> m = s.getVariables();
			VariableDefinition d = m.get(name);
			if (d != null) {
				return d;
			}
			if (!s.isParentScopeVisible()) {
				return null;
			}
		}
		return null;
	}



	public FunctionDefinition searchFunction(String name) {
		for (Scope s : scopes) {
			Map<String, FunctionDefinition> m = s.getFunctions();
			FunctionDefinition d = m.get(name);
			if (d != null) {
				return d;
			}
			if (!s.isParentScopeVisible()) {
				return null;
			}
		}
		return null;
	}
	
	public void pushScope(Object scopeId, boolean parentScopeVisible) {
		Scope s = new Scope();
		s.setId(scopeId);
		s.setParentScopeVisible(parentScopeVisible);
		s.setFunctions(new HashMap<>());
		s.setVariables(new HashMap<>());
		s.setCursors(new HashMap<>());
		this.scopes.push(s);
	}
	
	public Object getCurrentScopeId()
	{
		return this.scopes.peek().getId();
	}

	public void popScope(Object expectedScopeId) {
		Preconditions.checkState(expectedScopeId == this.scopes.pop().getId(), "popped scope is not as expected");
	}

	public void addVariableDefinition( VariableDefinition def) {
		Preconditions.checkState(this.scopes.peek().getVariables().put(def.getName(), def) == null, "duplicate object name %s", def.getName());
	}

	public void addCursorDefinition( CursorDefinition def) {
		Preconditions.checkState(this.scopes.peek().getCursors().put(def.getName(), def) == null, "duplicate object name %s", def.getName());
	}
	
	private static class Scope {
		Object id;
		boolean parentScopeVisible;
		Map<String, VariableDefinition> variables;
		Map<String, FunctionDefinition> functions;
		Map<String, CursorDefinition> cursors;
		

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

		public Map<String, VariableDefinition> getVariables() {
			return variables;
		}

		public void setVariables(Map<String, VariableDefinition> variables) {
			this.variables = variables;
		}

		public Map<String, FunctionDefinition> getFunctions() {
			return functions;
		}

		public void setFunctions(Map<String, FunctionDefinition> functions) {
			this.functions = functions;
		}

		public Map<String, CursorDefinition> getCursors() {
			return cursors;
		}

		public void setCursors(Map<String, CursorDefinition> cursors) {
			this.cursors = cursors;
		}


	}
}
