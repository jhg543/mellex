package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

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

	public static ValueFunc of(ObjectReference... columnReference) {
		ValueFunc v = new ValueFunc();
		v.objects = ImmutableSet.copyOf(columnReference);
		v.parameters = Collections.emptySet();
		return v;
	}

	public static ValueFunc of(ObjectDefinition... variableReference) {
		ValueFunc v = new ValueFunc();
		v.objects = Collections.emptySet();
		v.parameters = ImmutableSet.copyOf(variableReference);
		return v;
	}

	public static ValueFunc of(Collection<? extends ObjectDefinition> variableReference,
			Collection<? extends ObjectReference> columnReference) {
		ValueFunc v = new ValueFunc();
		v.objects = ImmutableSet.copyOf(columnReference);
		v.parameters = ImmutableSet.copyOf(variableReference);
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

	@Override
	public String toString() {
		return "V [" + (objects.isEmpty() ? "" : ("obj=" + objects)) + (parameters.isEmpty() ? "" : (" param=" + parameters))
				+ "]";
	}

}
