package io.github.jhg543.mellex.ASTHelper.plsql;

import com.google.common.collect.ImmutableSet;

import java.util.*;

public class ValueFunc {
	private Set<ObjectReference> objects;
	private Set<ObjectDefinition> parameters;

	public Set<ObjectReference> getObjects() {
		return objects;
	}

	public Set<ObjectDefinition> getParameters() {
		return parameters;
	}

	private static ValueFunc createEmpty() {
		ValueFunc v = new ValueFunc();
		v.objects = Collections.emptySet();
		v.parameters = Collections.emptySet();
		return v;
	}

	private static ValueFunc EMPTY = createEmpty();

	public static ValueFunc of() {
		return EMPTY;
	}

	public boolean isEmpty() {
		return objects.isEmpty() && parameters.isEmpty();
	}

	public static ValueFunc ofColumnReference(ObjectReference... columnReference) {
		ValueFunc v = new ValueFunc();
		v.objects = ImmutableSet.copyOf(columnReference);
		v.parameters = Collections.emptySet();
		return v;
	}

	public static ValueFunc ofVariableReference(ObjectDefinition... variableReference) {
		ValueFunc v = new ValueFunc();
		v.objects = Collections.emptySet();
		v.parameters = ImmutableSet.copyOf(variableReference);
		return v;
	}

	public static ValueFunc ofImmutable(Collection<? extends ObjectDefinition> variableReference,
										Collection<? extends ObjectReference> columnReference) {
		ValueFunc v = new ValueFunc();
		v.objects = ImmutableSet.copyOf(columnReference);
		v.parameters = ImmutableSet.copyOf(variableReference);
		return v;
	}

	public static ValueFunc buildDirect(Set<ObjectDefinition> variableReference,
										Set<ObjectReference> columnReference) {
		ValueFunc v = new ValueFunc();
		v.objects = columnReference;
		v.parameters = variableReference;
		return v;
	}

	public static class Builder {
		private int entries;
		private ValueFunc lastEntry;
		private Set<ObjectReference> objects;
		private Set<ObjectDefinition> parameters;

		public Builder() {
			entries = 0;
			lastEntry = null;
			objects = new HashSet<>();
			parameters = new HashSet<>();
		}

		public ValueFunc Build() {
			if (entries == 0) {
				return of();
			}
			if (entries == 1) {
				return lastEntry;
			}

			ValueFunc f = new ValueFunc();
			f.objects = Collections.unmodifiableSet(objects);
			f.parameters = Collections.unmodifiableSet(parameters);
			return f;
		}

		public void add(ValueFunc other) {
			if (!other.isEmpty() && lastEntry != other) {
				entries++;
				lastEntry = other;
				objects.addAll(other.getObjects());
				parameters.addAll(other.getParameters());
			}
		}
	}

	public static ValueFunc combine(List<ValueFunc> subs) {
		ValueFunc.Builder builder = new ValueFunc.Builder();
		for (ValueFunc fn : subs) {
			builder.add(fn);
		}

		return builder.Build();
	}

	public ValueFunc add(ValueFunc... others)
	{
		ValueFunc.Builder builder = new ValueFunc.Builder();
		builder.add(this);
		for (ValueFunc fn : others) {
			builder.add(fn);
		}
		return builder.Build();
	}


	@Override
	public String toString() {
		return "V [" + (objects.isEmpty() ? "" : ("obj=" + objects)) + (parameters.isEmpty() ? "" : (" param=" + parameters))
				+ "]";
	}

}
