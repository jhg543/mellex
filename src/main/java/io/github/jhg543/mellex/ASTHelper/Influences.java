package io.github.jhg543.mellex.ASTHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * It should be IMMUTABLE...
 *
 */
public class Influences {

	private List<InfSource> sources = new ArrayList<InfSource>();

	// private List<ObjectName> direct = new ArrayList<ObjectName>();
	// private List<ObjectName> indirect = new ArrayList<ObjectName>();

	public void addInResultExpression(ObjectName sourceObject) {
		sources.add(new InfSource(sourceObject, InfConnection.IN_RESULT_EXPRESSION));
	}

	public void addInClause(ObjectName sourceObject) {
		sources.add(new InfSource(sourceObject, InfConnection.IN_CLAUSE));
	}

	public void add(InfSource source) {
		sources.add(source);
	}
	
	public void remove(InfSource source) {
		sources.remove(source);
	}

	public void addAll(Influences other) {
		sources.addAll(other.sources);
	}

	public static Influences copyOf(Influences other) {
		Influences i = new Influences();
		i.addAll(other);
		return i;
	}

	public List<InfSource> getSources() {
		return Collections.unmodifiableList(sources);
	}

	public void addAllInClause(Influences other) {
		InfSource expander = new InfSource(null, InfConnection.IN_CLAUSE);
		sources.addAll(other.getSources().stream().map(expander::expand).collect(Collectors.toList()));
	}

	public void expand(Influences other, InfSource source) {
		sources.addAll(other.getSources().stream().map(source::expand).collect(Collectors.toList()));
	}

	public void copySelectScalar(SubQuery other) {
		addAllInClause(other.ci);
		for (ResultColumn c : other.columns) {
			addAll(c.inf);
		}
		unique();
	}

	public void copySelectStmtAsClause(SubQuery other) {
		addAllInClause(other.ci);
		for (ResultColumn c : other.columns) {
			addAllInClause(c.inf);
		}
		unique();
	}

	public static Influences ofdirect(ObjectName name) {
		Influences inf = new Influences();
		InfSource e = new InfSource(name, InfConnection.IN_RESULT_EXPRESSION);
		inf.sources.add(e);
		return inf;
	}

	public void unique() {
		sources = sources.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
	}

	public boolean isempty() {
		return sources.size() == 0;
	}

	@Override
	public String toString() {
		return sources.toString();
	}
}
