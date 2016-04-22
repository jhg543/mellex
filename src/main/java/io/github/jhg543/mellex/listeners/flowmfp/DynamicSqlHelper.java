package io.github.jhg543.mellex.listeners.flowmfp;

import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

import java.util.List;

/**
 * Created by zzz on 2016/4/20.
 */
public class DynamicSqlHelper {

    public static String DYNAMIC_VAR_HEADER = "DV_X_X_";
    public static String NON_PARSABLE_FUNC_OR_EXPR = "E_E_EF_F_F";

    public static boolean isDynamicVar(String name) {
        return name.startsWith(DYNAMIC_VAR_HEADER);
    }

    public static String removeDynamicVarHeader(String name) {
        if (name.startsWith(DYNAMIC_VAR_HEADER)) {
            return name.substring(DYNAMIC_VAR_HEADER.length());
        } else {
            return name;
        }
    }

    public static String literalsToString(List<Object> literals) {
        StringBuilder sb = new StringBuilder();
        for (Object o : literals) {
            if (o instanceof String) {
                sb.append(o.toString());
            } else {
                VariableDefinition vd = (VariableDefinition) o;
                sb.append(DYNAMIC_VAR_HEADER);
                sb.append(vd.getName());
            }
        }
        return sb.toString();
    }
}
