package io.github.jhg543.mellex.ASTHelper.symbol;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;

/**
 * select c1+c1 as c2, 'x' as c1; --- TD only just record the dependency. the
 * "real work" is left
 */
public class TempOtherResultColumnResolver {
	private Map<String, ObjectDefinition> aliases;
	private Set<String> dependencies;
	private String currentalias;

	public void addAlias(String alias) {
		ObjectDefinition d = new ObjectDefinition();
		d.setName(alias);
		aliases.put(alias, d);
	}

	public ObjectDefinition searchName(String name) {
		if (name.equals(currentalias)) {
			return null;
		}

		ObjectDefinition d = aliases.get(name);
		if (d != null) {
			dependencies.add(name);
		}
		return d;
	}
}
