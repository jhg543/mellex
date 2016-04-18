package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

public class Instruction {

    private int id;
    private Object debugInfo;
    private Object scopeInfo;
    private Function<State, State> func;
    private Collection<Supplier<Instruction>> nextPc;
    public Instruction(Function<State, State> func, Object debugInfo, Object scopeInfo) {
        super();
        this.func = func;
        this.debugInfo = debugInfo;
        this.scopeInfo = scopeInfo;
        this.nextPc = new ArrayList<>();
    }

    public Object getScopeInfo() {
        return scopeInfo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Function<State, State> getFunc() {
        return func;
    }

    public void setFunc(Function<State, State> func) {
        this.func = func;
    }

    public Collection<Supplier<Instruction>> getNextPc() {
        return nextPc;
    }

    public Object getDebugInfo() {
        return debugInfo;
    }

    public void setDebugInfo(Object debugInfo) {
        this.debugInfo = debugInfo;
    }

    /**
     * shortcut for add instruction
     *
     * @param i
     */
    public void addNextInstruction(Instruction i) {
        getNextPc().add(() -> i);
    }
}
