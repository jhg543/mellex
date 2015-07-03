package io.github.jhg543.mellex.ASTHelper;

import java.util.ArrayList;

public class CaseInsensitiveList extends ArrayList<String> {

	public CaseInsensitiveList(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public boolean add(String e) {
		return super.add(e.toUpperCase());
	}

}
