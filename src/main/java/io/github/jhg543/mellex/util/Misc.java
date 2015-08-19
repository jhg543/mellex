package io.github.jhg543.mellex.util;

import java.util.List;

import com.google.common.base.Splitter;

import io.github.jhg543.mellex.ASTHelper.ObjectName;

public class Misc {
	public static boolean isvolatile(ObjectName n) {
		if (n != null && n.ns.size() == 2) {
			String a = n.ns.get(1);
			if (a.endsWith("_NEW_DATA") || a.startsWith("TMP_")) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isvolatile(String name) {
		List<String> n = Splitter.on('.').splitToList(name);
		if (n != null && n.size() == 2) {
			String a = n.get(1);
			if (a.endsWith("_NEW_DATA") || a.startsWith("TMP_")) {
				return true;
			}
		}
		return false;
	}
}
