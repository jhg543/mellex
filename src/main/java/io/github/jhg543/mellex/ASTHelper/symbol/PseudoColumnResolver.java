package io.github.jhg543.mellex.ASTHelper.symbol;

import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;
import io.github.jhg543.mellex.util.DatabaseVendor;

import java.util.HashMap;
import java.util.Map;

public class PseudoColumnResolver {
    //TODO implement this.

    private Map<String, StateFunc> globalcs;

    public PseudoColumnResolver(DatabaseVendor vendor) {
        globalcs = new HashMap<>();
        globalcs.put("TRUE", StateFunc.of());
        globalcs.put("FALSE", StateFunc.of());
        globalcs.put("ROWNUM", StateFunc.of());
    }

    public StateFunc searchByName(String name) {
        if (name.endsWith(".NEXTVAL"))
        {
            return StateFunc.of();
        }
        return globalcs.get(name);
    }
}
