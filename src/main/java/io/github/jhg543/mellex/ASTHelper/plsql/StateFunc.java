package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import static java.util.stream.Collectors.*;

/**
 * variable value in a expression should not change. for example -- f(out x, y,
 * z) is { x:=x+y+z; return y;} expr := f(v1,v2,v3) --- OK { v = {v2} , a = { v1
 * := {v1,v2,v3} }} expr := f(v1,v2,v3) + v1 --- NO SUPPORT program result = { v
 * = { v1,v2 } v1:= {v1,v2,v3} } WRONG ANSWER
 *
 */
public class StateFunc {
	protected ValueFunc value;
	protected Map<ObjectDefinition, ValueFunc> updates;
	protected Map<ObjectDefinition, ValueFunc> assigns; // should be ordered map since used in result column
	protected ValueFunc branchCond;	
	
	public ValueFunc getValue() {
		return value;
	}

	public Map<ObjectDefinition, ValueFunc> getUpdates() {
		return updates;
	}

	public Map<ObjectDefinition, ValueFunc> getAssigns() {
		return assigns;
	}

	public ValueFunc getBranchCond() {
		return branchCond;
	}

	/**
	 * @param subs
	 * @return combined sets (immutable)
	 */
	private static ValueFunc combineValue(List<ValueFunc> subs) {

		subs.removeIf(v -> v.equals(ValueFunc.of()));

		if (subs.size() == 0) {
			return ValueFunc.of();
		}
		if (subs.size() == 1) {
			return subs.get(0);
		}
		ImmutableSet.Builder<ObjectReference> objs = ImmutableSet.builder();
		ImmutableSet.Builder<ObjectDefinition> params = ImmutableSet.builder();
		subs.forEach(s -> {
			objs.addAll(s.getObjects());
			params.addAll(s.getParameters());
		});
		ValueFunc result = new ValueFunc();
		result.setObjects(objs.build());
		result.setParameters(params.build());
		return result;

	}

	/**
	 * {k1:v1,k2:v2}+{k1:v4,k3:v3} = {k1: combineValue{v1,v4} , k2:v2, k3:v3 }
	 * 
	 * @param subs
	 * @return
	 */
	private static Map<ObjectDefinition, ValueFunc> combineAssigns(List<Map<ObjectDefinition, ValueFunc>> subs) {

		subs.removeIf(Map::isEmpty);

		if (subs.size() == 0) {
			return Collections.emptyMap();
		}

		if (subs.size() == 1) {
			return subs.get(0);
		}

		ImmutableMap.Builder<ObjectDefinition, ValueFunc> as = ImmutableMap.builder();
		subs.stream().flatMap(s -> s.entrySet().stream())
				.collect(groupingBy(e -> e.getKey(), mapping(e -> e.getValue(), toList())))
				.forEach((k, v) -> as.put(k, combineValue(v)));
		;

		return as.build();

	}

	public static StateFunc combine(List<StateFunc> subs) {

		subs.removeIf(s -> s.equals(StateFunc.of()));

		if (subs.size() == 1) {
			return subs.get(0);
		}
		StateFunc result = new StateFunc();
		result.value = combineValue(subs.stream().map(s -> s.value).collect(toList()));
		result.branchCond = combineValue(subs.stream().map(s -> s.branchCond).collect(toList()));
		result.assigns = combineAssigns(subs.stream().map(s -> s.assigns).collect(toList()));
		result.updates = combineAssigns(subs.stream().map(s -> s.updates).collect(toList()));
		return result;
	}

	public static StateFunc combine(StateFunc... subs) {
		return combine(Arrays.asList(subs));
	}

	private static StateFunc createEmpty() {
		StateFunc s = new StateFunc();
		s.assigns = Collections.emptyMap();
		s.updates = Collections.emptyMap();
		s.value = ValueFunc.of();
		s.branchCond = ValueFunc.of();
		return s;
	}

	private static StateFunc EMPTY = createEmpty();

	public static StateFunc of() {
		return EMPTY;
	}

	public static StateFunc ofValue(ValueFunc value) {
		StateFunc s = createEmpty();
		s.value = value;
		return s;
	}

	public static StateFunc ofBranchCond(ValueFunc branchCond) {
		StateFunc s = createEmpty();
		s.branchCond = branchCond;
		return s;
	}

	public static StateFunc ofUpdate(Map<ObjectDefinition, ValueFunc> updates) {
		StateFunc s = createEmpty();
		s.updates = updates;
		return s;
	}

	public static StateFunc ofAssign(Map<ObjectDefinition, ValueFunc> assigns) {
		StateFunc s = createEmpty();
		s.assigns = assigns;
		return s;
	}

	private static ValueFunc applyValue(ValueFunc v, Map<ObjectDefinition, StateFunc> parameterValues) {
		boolean nochange = true;
		ImmutableSet.Builder<ObjectReference> objs = ImmutableSet.builder();
		objs.addAll(v.getObjects());
		ImmutableSet.Builder<ObjectDefinition> params = ImmutableSet.builder();
		for (ObjectDefinition param : v.getParameters()) {
			StateFunc s = parameterValues.get(param);
			if (s == null) {
				params.add(param);
			} else {
				nochange = false;
				params.addAll(s.getValue().getParameters());
				objs.addAll(s.getValue().getObjects());
			}
		}

		if (nochange) {
			return v;
		} else {
			ValueFunc result = new ValueFunc();
			result.setParameters(params.build());
			Set<ObjectReference> so = objs.build();
			if (so.size() != v.getObjects().size()) {
				result.setObjects(objs.build());
			} else {
				result.setObjects(v.getObjects());
			}

			return result;
		}
	}

	private static Map<ObjectDefinition, ValueFunc> applyAssigns(Map<ObjectDefinition, ValueFunc> assigns,
			Map<ObjectDefinition, StateFunc> parameterValues) {
		boolean nochange = true;
		ImmutableMap.Builder<ObjectDefinition, ValueFunc> as = ImmutableMap.builder();
		for (Entry<ObjectDefinition, ValueFunc> e : assigns.entrySet()) {
			ValueFunc v = applyValue(e.getValue(), parameterValues);
			if (v != e.getValue()) {
				nochange = false;
			}
			// f(a,out b) = { b = a+b }  --> assigns = b: a,b   f(x,y) --->   x:x,y
			if (parameterValues.containsKey(e.getKey()))
			{
				StateFunc lvalue = parameterValues.get(e.getKey());
				nochange = false;
				// TODO check it is a lvalue
				Preconditions.checkState(lvalue.assigns.isEmpty());
				Preconditions.checkState( lvalue.updates.isEmpty());
				Preconditions.checkState( lvalue.value.getObjects().isEmpty());
				Preconditions.checkState( lvalue.value.getParameters().size() ==1);
				as.put(lvalue.getValue().getParameters().iterator().next(),v);
				
			}
			else
			{
				as.put(e.getKey(), v);
			}

		}
		if (nochange) {
			return assigns;
		} else {
			return as.build();
		}

	}

	public StateFunc apply(Map<ObjectDefinition, StateFunc> parameterValues) {
		// (1) mutate s(f) to s2(f) with param in ValueFunc transformed.
		// (2) return combine ( s2(f) s(param1) s(param2) s param(3) )
		StateFunc m1 = new StateFunc();
		m1.value = applyValue(this.value, parameterValues);
		m1.branchCond = applyValue(this.branchCond, parameterValues);
		m1.assigns = applyAssigns(this.getAssigns(), parameterValues);
		m1.updates = applyAssigns(this.getUpdates(), parameterValues);
		List<StateFunc> f = new ArrayList<>(parameterValues.values());
		f.add(m1);
		StateFunc m2 = combine(f);
		m2.value = m1.value;
		return m2;
	}

	@Override
	public String toString() {
		return "StateFunc [value=" + value + ", updates=" + updates + ", assigns=" + assigns + ", branchCond=" + branchCond
				+ "]";
	}
	
	
}
