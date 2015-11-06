package io.github.jhg543.mellex.ASTHelper;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

public class ObjectName {
	// TODO remove Global settings here
	public List<String> ns = GlobalSettings.isCaseSensitive() ? new ArrayList<String>(4) : new CaseInsensitiveList(4);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ns == null) ? 0 : ns.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectName other = (ObjectName) obj;
		if (ns == null) {
			if (other.ns != null)
				return false;
		} else if (!ns.equals(other.ns))
			return false;
		return true;
	}

	public String toDotString() {
		return Joiner.on('.').join(ns.toArray());
	}

	public String toDotStringExceptLast() {
		if (ns.size() == 2) {
			return ns.get(0);
		}
		if (ns.size() == 1) {
			return null;
		}
		StringBuilder b = new StringBuilder(ns.get(0));
		for (int i = 1; i < ns.size()-1; ++i) {
			b.append('.');
			b.append(ns.get(i));
		}
		return b.toString();
	}
	
	public String toDotStringLast() {
		return ns.get(ns.size()-1);
	}

	public static ObjectName fromString(String name) {
		ObjectName result = new ObjectName();
		result.ns.addAll(Splitter.on('.').splitToList(name));
		return result;
	}

	@Override
	public String toString() {
		return Joiner.on('.').join(ns.toArray());
	}
}
