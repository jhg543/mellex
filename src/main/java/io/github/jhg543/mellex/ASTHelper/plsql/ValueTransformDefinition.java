package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.Collections;
import java.util.Set;

public class ValueTransformDefinition {
	private Set<ObjectReference> objects;
	private Set<ObjectDefinition> parameters;

	public Set<ObjectReference> getObjects() {
		return objects;
	}

	void setObjects(Set<ObjectReference> objects) {
		this.objects = objects;
	}

	public Set<ObjectDefinition> getParameters() {
		return parameters;
	}

	void setParameters(Set<ObjectDefinition> parameters) {
		this.parameters = parameters;
	}

	private static ValueTransformDefinition createEmpty()
	{
		ValueTransformDefinition v = new ValueTransformDefinition();
		v.objects = Collections.emptySet();
		v.parameters = Collections.emptySet();
		return v;
	}
	
	private static ValueTransformDefinition EMPTY = createEmpty();
	
	public static ValueTransformDefinition of()
	{
		return EMPTY;
	}
	
	
}
