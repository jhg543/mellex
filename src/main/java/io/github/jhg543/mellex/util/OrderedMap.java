package io.github.jhg543.mellex.util;

import java.util.*;

/**
 * Created by z089 on 2016/4/14.
 */
public class OrderedMap<K, V> {
    protected List<V> columns;
    protected Map<K, Integer> nameIndexMap; // index is 0 based

    private OrderedMap() {
    }

    public List<V> getColumns() {
        return columns;
    }

    public Map<K, Integer> getNameIndexMap() {
        return nameIndexMap;
    }

    public V getValue(K key)
    {
        return columns.get(nameIndexMap.get(key).intValue());
    }

    public static <K,V> Builder<K, V> builder() {
        return new Builder<>();
    }

    public static class Builder<K, V> {
        OrderedMap<K, V> m;

        protected Builder() {
            m = new OrderedMap<>();
            m.columns = new ArrayList<>();
            m.nameIndexMap = new HashMap<>();
        }

        /**
         * @param key
         * @param value
         * @return auto-assigned index
         */
        public int put(K key, V value) {
            int index = m.columns.size();
            m.columns.add(value);
            m.nameIndexMap.put(key, index);
            return index;
        }

        public OrderedMap<K, V> build() {
            m.columns = Collections.unmodifiableList(m.columns);
            m.nameIndexMap = Collections.unmodifiableMap(m.nameIndexMap);
            return m;
        }
    }
}
