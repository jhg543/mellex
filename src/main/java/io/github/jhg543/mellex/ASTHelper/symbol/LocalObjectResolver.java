package io.github.jhg543.mellex.ASTHelper.symbol;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import io.github.jhg543.mellex.ASTHelper.plsql.CursorDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.FunctionDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;
import io.github.jhg543.mellex.listeners.flowmfp.DynamicSqlHelper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * variables or functions
 */
public class LocalObjectResolver {

    private static Splitter dotsplitter = Splitter.on('.');
    private LinkedList<Scope> scopes = new LinkedList<>();

    public VariableDefinition searchVariable(String name) {
        return searchVariableInScope(name, scopes.peek());
    }

    private static VariableDefinition searchVariableInScope(String name, Scope s) {
        name = DynamicSqlHelper.removeDynamicVarHeader(name);
        List<String> namedotsplit = dotsplitter.splitToList(name);
        if (namedotsplit.size() == 1) {
            return searchVariableBySimpleName(name, s);
        } else if (namedotsplit.size() == 2) {
            VariableDefinition d = searchVariableBySimpleName(namedotsplit.get(0), s);
            if (d == null) {
                return null;
            }
            if (!(d instanceof VariableDefinition)) {
                // function does not have columns
                return null;
            }
            //throw new RuntimeException("TODO DEBUG MARKER variable.column" + name);
            return d.getColumn(namedotsplit.get(1));

        } else {
            // size>2, leave to global
            return null;
        }

    }

    public CursorDefinition searchCursor(String name) {
        for (Scope s : scopes) {
            Map<String, CursorDefinition> m = s.getCursors();
            CursorDefinition d = m.get(name);
            if (d != null) {
                return d;
            }
            if (s.getParentScope() == null) {
                return null;
            }
        }
        return null;
    }

    private static VariableDefinition searchVariableBySimpleName(String name, Scope s) {
        // assume no "." in name
        while (s != null) {
            Map<String, VariableDefinition> m = s.getVariables();
            VariableDefinition d = m.get(name);
            if (d != null) {
                return d;
            }
            s = s.getParentScope();
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
            if (s.getParentScope() == null) {
                return null;
            }
        }
        return null;
    }

    public void pushScope(Object scopeId, boolean parentScopeVisible) {
        Scope s = new Scope();
        s.setId(scopeId);
        if (parentScopeVisible) {
            s.setParentScope(this.scopes.peek());
        } else {
            s.setParentScope(null);
        }
        s.setFunctions(new HashMap<>());
        s.setVariables(new HashMap<>());
        s.setCursors(new HashMap<>());
        this.scopes.push(s);
    }

    private LocalObjectStatusSnapshot localObjectStatusSnapshot = new LocalObjectStatusSnapshot(null);

    public LocalObjectStatusSnapshot getCurrentScopeSnapshot() {
        // TODO I am lazy to implement the full copy of state
        if (localObjectStatusSnapshot.getScope() != this.scopes.peek()) {
            localObjectStatusSnapshot = new LocalObjectStatusSnapshot(this.scopes.peek());
        }
        return localObjectStatusSnapshot;
    }

    public static boolean isVariableLive(VariableDefinition variableDefinition, LocalObjectStatusSnapshot shot)
    {
        return searchVariableInScope(variableDefinition.getName(),shot.getScope())==variableDefinition;
    }

    public void popScope(Object expectedScopeId) {
        Preconditions.checkState(expectedScopeId == this.scopes.pop().getId(), "popped scope is not as expected");
    }

    public void addVariableDefinition(VariableDefinition def) {
        Preconditions.checkState(this.scopes.peek().getVariables().put(def.getName(), def) == null, "duplicate object name %s", def.getName());
    }

    public void addCursorDefinition(CursorDefinition def) {
        Preconditions.checkState(this.scopes.peek().getCursors().put(def.getName(), def) == null, "duplicate object name %s", def.getName());
    }

    public void addFunctionDefinition(FunctionDefinition def) {
        Preconditions.checkState(this.scopes.peek().getFunctions().put(def.getName(), def) == null, "duplicate object name %s", def.getName());
    }

    public static class Scope {
        Object id;
        Map<String, VariableDefinition> variables;
        Map<String, FunctionDefinition> functions;
        Map<String, CursorDefinition> cursors;
        Scope parentScope;

        private Scope() {
        }

        public Object getId() {
            return id;
        }

        public void setId(Object id) {
            this.id = id;
        }

        public Scope getParentScope() {
            return parentScope;
        }

        public void setParentScope(Scope parentScope) {
            this.parentScope = parentScope;
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
