package io.github.jhg543.mellex.listeners.flowmfp;

import io.github.jhg543.mellex.ASTHelper.plsql.CursorDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class State {
    public State copy() {
        this.locked = true;
        State s = new State(variableStateMap, cursorStateMap, funcState);
        return s;
    }

    /**
     * mark
     */
    private boolean varStateCopied = false;
    private boolean cursorStateCopied = false;

    /**
     * copy on write so no write after being copied.
     */
    private boolean locked = false;

    private Map<VariableDefinition, VariableState> variableStateMap;

    //	TODO cursor may have Multiple state e.g if (x) cursor = selectStmt 1 else cursor = selectstmt2
    private Map<CursorDefinition, CursorState> cursorStateMap;

    private FunctionStateRecorder funcState;

    public Map<VariableDefinition, VariableState> readVarState() {
        return Collections.unmodifiableMap(variableStateMap);
    }

    public Map<CursorDefinition, CursorState> readCursorState() {
        return Collections.unmodifiableMap(cursorStateMap);
    }

    public VariableState writeVariable(VariableDefinition def, VariableState s) {
        if (locked) {
            throw new RuntimeException("COW violated");
        }
        if (!varStateCopied) {
            varStateCopied = true;
            variableStateMap = new HashMap<>(variableStateMap);
        }
        return variableStateMap.put(def, s);

    }

    public CursorState writeCursorState(CursorDefinition cursorDefinition, CursorState s) {
        if (locked) {
            throw new RuntimeException("COW violated");
        }
        if (!cursorStateCopied) {
            cursorStateCopied = true;
            cursorStateMap = new HashMap<>(cursorStateMap);
        }
        return cursorStateMap.put(cursorDefinition, s);

    }

    public VariableState removeVariable(VariableDefinition def) {
        if (locked) {
            throw new RuntimeException("COW violated");
        }
        if (!varStateCopied) {
            varStateCopied = true;
            variableStateMap = new HashMap<>(variableStateMap);
        }
        return variableStateMap.remove(def);

    }

    public CursorState removeCursorState(CursorDefinition cursorDefinition) {
        if (locked) {
            throw new RuntimeException("COW violated");
        }
        if (!cursorStateCopied) {
            cursorStateCopied = true;
            cursorStateMap = new HashMap<>(cursorStateMap);
        }
        return cursorStateMap.remove(cursorDefinition);
    }

    public FunctionStateRecorder getFuncState() {
        return funcState;
    }

    private State(Map<VariableDefinition, VariableState> variableStateMap, Map<CursorDefinition, CursorState> cursorStateMap, FunctionStateRecorder funcState) {
        this.variableStateMap = variableStateMap;
        this.cursorStateMap = cursorStateMap;
        this.funcState = funcState;
    }

    public static State newInitialState(FunctionStateRecorder recorder) {
        return new State(new HashMap<>(), new HashMap<>(), recorder);
    }

    public String printDebugInfo()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        readVarState().forEach((k,v)->{
            sb.append(k.getName());
            sb.append("-");
            v.getValueInfluence().getObjects().stream().map(o->o.getObjectDefinition().getName()).distinct().forEach(s->{sb.append(s);sb.append(" ");});
            v.getValueInfluence().getParameters().stream().map(o->o.getName()).distinct().forEach(s->{sb.append(s);sb.append(" ");});
            sb.append("/");
        });

        sb.append("]\n");
        return sb.toString();
    }
}
