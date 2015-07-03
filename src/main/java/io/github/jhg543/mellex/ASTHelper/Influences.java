package io.github.jhg543.mellex.ASTHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Influences {
	public List<ObjectName> direct = new ArrayList<ObjectName>();
	public List<ObjectName> indirect = new ArrayList<ObjectName>();

	public void copy(Influences other) {
		direct.addAll(other.direct);
		indirect.addAll(other.indirect);
	}

	public void copypoi(Influences other) {
		indirect.addAll(other.direct);
		indirect.addAll(other.indirect);
	}

	public void copyselect(SubQuery other) {
		copypoi(other.ci);
		for (ResultColumn c : other.columns) {
			copy(c.inf);
		}
		unique();
	}

	public void copypoiselect(SubQuery other) {
		copypoi(other.ci);
		for (ResultColumn c : other.columns) {
			copypoi(c.inf);
		}
		unique();
	}

	public static Influences ofdirect(ObjectName name) {
		Influences inf = new Influences();
		inf.direct.add(name);
		return inf;
	}

	public void unique() {
		Set<ObjectName> d1 = new HashSet<ObjectName>(direct);
		Set<ObjectName> i1 = new HashSet<ObjectName>(indirect);
		i1.removeAll(d1);
		direct = new ArrayList<>(d1);
		indirect = new ArrayList<>(i1);
	}

	public boolean isempty() {
		return direct.size() + indirect.size() == 0;
	}

	@Override
	public String toString() {
		return direct + ", " + indirect;
	}
}
