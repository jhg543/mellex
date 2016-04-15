package io.github.jhg543.mellex.listeners.flowmfp;

import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by z089 on 2016/4/15.
 */
public class StateExecutor {
    private FunctionStateRecorder state;
    private List<Instruction> instructionList;
    private List<State> states;

    public StateExecutor(List<Instruction> list)
    {
        this.instructionList = list;
    }

    private State compareAndCombineState(State from, State oldOne, Function<VariableDefinition,Boolean> variableScopeChecker)
    {
        // Ignore cursor state since it can not be accumulated.
        // remove dead variable first
        // possibleSQL is overwritten
        State c = oldOne.copy();
        from.readVarState().forEach((variableDefinition, variableState) -> {
            if (!variableScopeChecker.apply(variableDefinition))
            {
                return;
            }

        });
        return null;
    }

    public FunctionStateRecorder run()
    {
        states = new ArrayList<>(instructionList.size());
        FunctionStateRecorder recorder = new FunctionStateRecorder();
        State initialState = State.newInitialState(recorder);
        states.set(0,initialState);
        TreeSet<Instruction> queue = new TreeSet<>((i1,i2)->{return i1.getId()-i2.getId();});
        queue.add(instructionList.get(0));
        Instruction currentInstruction;
        while ((currentInstruction=queue.pollFirst())!=null)
        {
            State currentState = states.get(currentInstruction.getId());
            State nextState = currentInstruction.getFunc().apply(currentState);
            for (Supplier<Instruction> nip:currentInstruction.getNextPc())
            {
                Instruction nextInstruction = nip.get();
                State oldNextState = states.get(nextInstruction.getId());
                if (oldNextState==null)
                {
                    states.set(nextInstruction.getId(),nextState);
                    queue.add(nextInstruction);
                    // TODO remove dead variables
                }else
                {

                }
            }
        }
        return recorder;
    }

}
