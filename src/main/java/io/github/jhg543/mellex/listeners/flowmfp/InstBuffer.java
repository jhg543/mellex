package io.github.jhg543.mellex.listeners.flowmfp;

import com.google.common.base.Joiner;
import io.github.jhg543.mellex.ASTHelper.plsql.FunctionDefinition;
import io.github.jhg543.mellex.ASTHelper.symbol.LocalObjectResolver;
import io.github.jhg543.mellex.ASTHelper.symbol.LocalObjectStatusSnapshot;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class InstBuffer {

    private LinkedList<OneFunctionBuffer> stack = new LinkedList<>();
    private List<OneFunctionBuffer> finishedFunction = new ArrayList<>();

    public void enterFunctionDef(FunctionDefinition fndef) {
        OneFunctionBuffer one = new OneFunctionBuffer();
        one.fndef = fndef;
        stack.push(one);
    }

    public void exitFunctionDef(LocalObjectStatusSnapshot snapshot) {
        OneFunctionBuffer fb = stack.pop();

        // TODO this is a debug..
        finishedFunction.add(fb);
        PrintWriter w = new PrintWriter(System.out);

        if (fb.instbuffer.size() > 0) {
            StateExecutor exec = new StateExecutor(fb.getInstbuffer(),fb.fndef);

            exec.run(v-> LocalObjectResolver.isVariableLive(v,snapshot));
            printFunctionInst(fb, w, exec);
            w.flush();
        }


    }

    public PatchList add(Instruction inst) {
        return stack.peek().add(inst);
    }

    private static class OneFunctionBuffer {
        private FunctionDefinition fndef;

        private List<Instruction> instbuffer = new ArrayList<>();

        public List<Instruction> getInstbuffer() {
            return instbuffer;
        }

        public void setInstbuffer(List<Instruction> instbuffer) {
            this.instbuffer = instbuffer;
        }

        public PatchList add(Instruction inst) {
            inst.setId(instbuffer.size());
            instbuffer.add(inst);
            return PatchList.singleInstrution(inst);
        }

    }


    /**
     * used to debug
     *
     * @param b
     */
    public static void printFunctionInst(OneFunctionBuffer b, Writer w, StateExecutor exec) {
        try {
            if (b.fndef != null) {
                w.write(b.fndef.getName());
                w.write('\n');
            }
            for (int index = 0; index < b.instbuffer.size(); ++index) {
                Instruction i = b.instbuffer.get(index);
                State state = exec.getStates().get(index);

                w.write(Integer.valueOf(i.getId()).toString());
                w.write(" -- ");
                w.write(state.printDebugInfo());

                if (i.getDebugInfo() instanceof Object[]) {
                    w.write(Arrays.toString((Object[]) i.getDebugInfo()));
                } else {

                    w.write(i.getDebugInfo().toString());
                }
                w.write("\nNext :");
                w.write(Joiner.on(',').join(i.getNextPc().stream().map(s -> s.get().getId()).collect(Collectors.toList())));
                w.write('\n');
            }
            w.append("FUNCTION DEF = ");
            w.append(formatX(b.fndef.getDefinition().toString()));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String formatX(String a)
    {
        StringBuilder sb = new StringBuilder();
        int k = 0;
        for (int i=0;i<a.length();++i)
        {
            sb.append(a.charAt(i));
            if (a.charAt(i)=='[')
            {
                k++;
                sb.append('\n');
                for (int j=0;j<k;++j)
                {
                    sb.append("    ");
                }
            }
            if (a.charAt(i)==']')
            {
                k--;
                sb.append('\n');
                for (int j=0;j<k;++j)
                {
                    sb.append("    ");
                }
            }
        }
        return sb.toString();
    }
}
