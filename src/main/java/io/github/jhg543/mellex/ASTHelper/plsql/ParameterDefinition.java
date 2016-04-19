package io.github.jhg543.mellex.ASTHelper.plsql;

public class ParameterDefinition extends VariableDefinition {
    private boolean defaultValuePresent;
    private boolean in;
    private boolean out;

    public VariableDefinition getFunctionBodyVariable() {
        return functionBodyVariable;
    }

    public void setFunctionBodyVariable(VariableDefinition functionBodyVariable) {
        this.functionBodyVariable = functionBodyVariable;
    }

    private VariableDefinition functionBodyVariable;

    public boolean isDefaultValuePresent() {
        return defaultValuePresent;
    }

    public void setDefaultValuePresent(boolean defaultValuePresent) {
        this.defaultValuePresent = defaultValuePresent;
    }

    public boolean isIn() {
        return in;
    }

    public void setIn(boolean in) {
        this.in = in;
    }

    public boolean isOut() {
        return out;
    }

    public void setOut(boolean out) {
        this.out = out;
    }

}
