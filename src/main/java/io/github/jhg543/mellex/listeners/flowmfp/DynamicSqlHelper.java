package io.github.jhg543.mellex.listeners.flowmfp;

import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

import java.util.List;

/**
 * Created by ccb on 2016/4/20.
 */
public class DynamicSqlHelper {
    public static String literalsToString(List<Object> literals) {
        StringBuilder sb = new StringBuilder();
        for (Object o : literals) {
            if (o instanceof String) {
                sb.append(o.toString());
            } else {
                VariableDefinition vd = (VariableDefinition) o;
                sb.append("@@VAR");
                sb.append(vd.getName());
            }
        }
        return sb.toString();
    }
}
