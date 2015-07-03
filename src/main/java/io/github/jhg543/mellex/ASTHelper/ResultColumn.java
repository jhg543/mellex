package io.github.jhg543.mellex.ASTHelper;

public class ResultColumn {
	public String name; // name null = insert into xxx values ()
	public int position = 0; // starts from 1
	public Influences inf = new Influences();
	public boolean isObjectName = false;
	public boolean hasAlias = false;
	public Influences unresolvedNames;

	public void copyof(ResultColumn other) {
		name = other.name;
		position = other.position;
		inf.copy(other.inf);
		isObjectName = other.isObjectName;
	}
}
