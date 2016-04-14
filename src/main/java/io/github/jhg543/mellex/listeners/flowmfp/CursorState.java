package io.github.jhg543.mellex.listeners.flowmfp;

import io.github.jhg543.mellex.util.OrderedMap;

import java.util.List;
import java.util.Map;

/**
 * Created by z089 on 2016/4/14.
 */
public class CursorState {
    OrderedMap<String, VariableState> orderedMap;

    private CursorState() {
    }

    public List<VariableState> getColumns() {
        return orderedMap.getColumns();
    }

    public VariableState getValue(String key) {
        return orderedMap.getValue(key);
    }

    public Map<String, Integer> getNameIndexMap() {
        return orderedMap.getNameIndexMap();
    }

    public static class Builder {
        private OrderedMap.Builder<String, VariableState> b;

        public int put(String key, VariableState value) {
            return b.put(key, value);
        }

        public CursorState build() {
            CursorState s = new CursorState();
            s.orderedMap = b.build();
            return s;
        }
    }
}
