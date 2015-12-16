package io.github.jhg543.mellex.ASTHelper;

public class ResultColumn {
	public String name; // name null = insert into xxx values ()
	public int position = 0; // starts from 1
	public Influences inf = new Influences();
	private boolean isObjectName = false;
	
	public boolean isObjectName() {
		return isObjectName;
	}

	public void setObjectName(boolean isObjectName) {
		this.isObjectName = isObjectName;
	}

	public boolean hasAlias = false;
	public Influences unresolvedNames;

	public void copyof(ResultColumn other) {
		name = other.name;
		position = other.position;
		inf.addAll(other.inf);
		isObjectName = other.isObjectName;
	}
}
