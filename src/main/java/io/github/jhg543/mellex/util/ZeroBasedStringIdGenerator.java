package io.github.jhg543.mellex.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Not thread safe
 * 
 * @author zzz
 *
 */
public class ZeroBasedStringIdGenerator {
	BiMap<String, Integer> map = HashBiMap.<String, Integer> create();

	public Integer queryNumber(String s) {
		Integer i = map.get(s);
		if (i != null) {
			return i;
		}
		Integer x = map.size();
		map.put(s, x);
		return x;
	}

	public String queryString(Integer i) {
		return map.inverse().get(i);
	}

}
