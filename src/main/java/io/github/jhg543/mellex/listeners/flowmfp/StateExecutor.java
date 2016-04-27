package io.github.jhg543.mellex.listeners.flowmfp;

import io.github.jhg543.mellex.ASTHelper.plsql.*;
import io.github.jhg543.mellex.ASTHelper.symbol.LocalObjectResolver;
import io.github.jhg543.mellex.ASTHelper.symbol.LocalObjectStatusSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by z089 on 2016/4/15.
 */
public class StateExecutor {
    private static final Logger log = LoggerFactory.getLogger(StateExecutor.class);
    private List<Instruction> instructionList;
    private FunctionDefinition functionDefinition;

    private List<State> states;

    public List<State> getStates() {
        return states;
    }

    public StateExecutor(List<Instruction> list, FunctionDefinition functionDefinition) {
        this.instructionList = list;
        this.functionDefinition = functionDefinition;
    }


    public FunctionStateRecorder run(Predicate<VariableDefinition> variableLiveChecker) {
        states = Arrays.asList(new State[instructionList.size()]);
        FunctionStateRecorder recorder = new FunctionStateRecorder(functionDefinition.getParameters().stream().filter(pd -> pd.getFunctionBodyVariable() != null).collect(Collectors.toList()), variableLiveChecker);
        // empty state as 0
        State initialState = State.newInitialState(recorder);
        states.set(0, initialState);
        // priority queue, lower label first
        TreeSet<Instruction> queue = new TreeSet<>((i1, i2) -> i1.getId() - i2.getId());
        // put instruction 0 (start)
        queue.add(instructionList.get(0));
        Instruction currentInstruction;
        while ((currentInstruction = queue.pollFirst()) != null) {
            State currentState = states.get(currentInstruction.getId());
            State nextState = currentInstruction.getFunc().apply(currentState);
            for (Supplier<Instruction> nip : currentInstruction.getNextPc()) {
                Instruction nextInstruction = nip.get();
                State oldNextState = states.get(nextInstruction.getId());
                Predicate<VariableDefinition> variableScopeChecker = v -> Boolean.TRUE;
                if (currentInstruction.getScopeInfo() != nextInstruction.getScopeInfo()) {
                    variableScopeChecker = isVariableLive(nextInstruction.getScopeInfo());
                }

                State combinedState = compareAndCombineState(nextState, oldNextState, variableScopeChecker);
                if (combinedState != null) {
                    states.set(nextInstruction.getId(), combinedState);
                    queue.add(nextInstruction);
                }
            }
        }
        functionDefinition.setDefinition(recorder.outputDefinition());
        return recorder;
    }

    /**
     * @param whiteMan       newly generated X to be written
     * @param nativeAmerican the old X
     * @return combined value or null is not modified
     */
    private static VariableState compareAndCombineVariableState(VariableState whiteMan, VariableState nativeAmerican) {
        Set<ObjectDefinition> parameters = new HashSet<>(whiteMan.getValueInfluence().getParameters());
        Set<ObjectReference> objects = new HashSet<>(whiteMan.getValueInfluence().getObjects());
        boolean pChanged = parameters.addAll(nativeAmerican.getValueInfluence().getParameters());
        boolean oChanged = objects.addAll(nativeAmerican.getValueInfluence().getObjects());
        if (pChanged || oChanged) {
            ValueFunc vf = ValueFunc.buildDirect(pChanged ? Collections.unmodifiableSet(parameters) : nativeAmerican.getValueInfluence().getParameters(), oChanged ? Collections.unmodifiableSet(objects) : nativeAmerican.getValueInfluence().getObjects());
            // TODO should this be overwritten?
            List<Object> possibleLiteralValue = whiteMan.getPossibleLiteralValue();
            VariableState c = new VariableState(vf, possibleLiteralValue);
            return c;
        } else {
            return null;
        }
    }

    private static CursorState compareAndCombineCursorState(CursorState whiteMan, CursorState nativeAmerican) {
        // if name and size are equal then combine
        // else log and return null
        if (whiteMan.getNameIndexMap().equals(nativeAmerican.getNameIndexMap())) {
            AtomicBoolean modified = new AtomicBoolean(false);
            List<VariableState> v = new ArrayList<>();
            for (int i = 0; i < whiteMan.getColumns().size(); ++i) {
                VariableState cv = compareAndCombineVariableState(whiteMan.getColumns().get(i), nativeAmerican.getColumns().get(i));
                if (cv == null) {
                    cv = nativeAmerican.getColumns().get(i);
                } else {
                    modified.set(true);
                }
                v.add(cv);
            }
            if (modified.get()) {
                CursorState.Builder csbuilder = new CursorState.Builder();
                whiteMan.getNameIndexMap().forEach((name, index) -> csbuilder.put(name, v.get(index)));
                return csbuilder.build();
            } else {
                return null;
            }
        } else {
            log.debug("cursor dimension mismatch {} {}", whiteMan.getNameIndexMap(), nativeAmerican.getNameIndexMap());
            return null;
        }
    }

    /**
     * @param whiteMan             newly generated X to be written
     * @param nativeAmerican       the old X before Next instruction
     * @param variableScopeChecker whether a variable is live in " just before Next instruction"
     * @return combined value or null is not modified
     */
    private static State compareAndCombineState(State whiteMan, State nativeAmerican, Predicate<VariableDefinition> variableScopeChecker) {

        if (nativeAmerican == null) {
            AtomicBoolean modified = new AtomicBoolean(false);
            State c = whiteMan.copy();
            whiteMan.readVarState().forEach((variableDefinition, variableState) -> {
                if (!variableScopeChecker.test(variableDefinition)) {
                    c.removeVariable(variableDefinition);
                    modified.set(true);
                }
            });
            if (modified.get()) {
                return c;

            } else {
                return whiteMan;
            }
        } else {

            // Ignore cursor state combine since it can not be accumulated.
            // new cursor state is marked as changed.
            // remove dead variable first
            // possibleSQL is overwritten
            State c = nativeAmerican.copy();
            AtomicBoolean modified = new AtomicBoolean(false);
            whiteMan.readVarState().forEach((variableDefinition, variableState) -> {
                if (!variableScopeChecker.test(variableDefinition)) {
                    return;
                }
                VariableState oldVariableState = nativeAmerican.readVarState().get(variableDefinition);
                if (oldVariableState == null) {
                    modified.set(true);
                    c.writeVariable(variableDefinition, variableState);
                } else {
                    VariableState combined = compareAndCombineVariableState(variableState, oldVariableState);
                    if (combined != null) {
                        modified.set(true);
                        c.writeVariable(variableDefinition, combined);
                    } else {
                        // no change
                    }
                }
            });

            // TODO same as variable state  , remove duplicated code.
            whiteMan.readCursorState().forEach((cursorDefinition, cursorState) -> {
                // no need to check if def is live since closing def is explicit instruction
                CursorState oldCursorState = nativeAmerican.readCursorState().get(cursorDefinition);
                if (oldCursorState == null) {
                    modified.set(true);
                    c.writeCursorState(cursorDefinition, oldCursorState);
                } else {
                    CursorState combined = compareAndCombineCursorState(cursorState, oldCursorState);
                    if (combined != null) {
                        modified.set(true);
                        c.writeCursorState(cursorDefinition, combined);
                    } else {
                        // do nothing
                    }
                }
            });
            if (modified.get()) {
                return c;
            } else {
                return null;
            }
        }
    }

    /**
     * it is assumed that instruction.scopeinfo is LocalObjectResolver.Scope
     *
     * @param scope
     * @return
     */
    private static Predicate<VariableDefinition> isVariableLive(LocalObjectStatusSnapshot scope) {
        return v -> LocalObjectResolver.isVariableLive(v, scope);

    }


}
