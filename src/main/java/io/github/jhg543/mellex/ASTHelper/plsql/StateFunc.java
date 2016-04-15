package io.github.jhg543.mellex.ASTHelper.plsql;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import static java.util.stream.Collectors.*;

/**
 * value of variable in a expression should not change. for example -- f(out x,
 * y, z) is { x:=x+y+z; return y;} expr := f(v1,v2,v3) --- OK { v = {v2} , a = {
 * v1 := {v1,v2,v3} }} expr := f(v1,v2,v3) + v1 --- NO SUPPORT program result =
 * { v = { v1,v2 } v1:= {v1,v2,v3} } WRONG ANSWER
 *
 */
public class StateFunc {
	protected ValueFunc value;
	protected Map<ObjectDefinition, FilteredValueFunc> updates;
	protected Map<ObjectDefinition, ValueFunc> assigns; // should be ordered map
														// since used in result
														// column
	/**
	 * For Select it's "where ... having... order...." clause, For expr it's
	 * only come from inline select stmt For Function def it's
	 * 
	 */
	protected ValueFunc branchCond;

	public ValueFunc getValue() {
		return value;
	}

	public Map<ObjectDefinition, FilteredValueFunc> getUpdates() {
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
		return ValueFunc.combine(subs);
	}

	private static FilteredValueFunc combineFilteredValue(List<FilteredValueFunc> subs) {
		return FilteredValueFunc.combine(subs);
	}

	/**
	 * {k1:v1,k2:v2}+{k1:v4,k3:v3} = {k1: combineValue{v1,v4} , k2:v2, k3:v3 }
	 * 
	 * @param subs
	 * @return
	 */
	private static Map<ObjectDefinition, ValueFunc> combineAssigns(List<Map<ObjectDefinition, ValueFunc>> subs) {
		return combineMap(subs, StateFunc::combineValue);
	}

	private static Map<ObjectDefinition, FilteredValueFunc> combineUpdates(
			List<Map<ObjectDefinition, FilteredValueFunc>> subs) {
		return combineMap(subs, StateFunc::combineFilteredValue);
	}

	private static <K, V> Map<K, V> combineMap(List<Map<K, V>> subs, Function<List<V>, V> valueCombiner) {
		subs = new ArrayList<>(subs);
		subs.removeIf(Map::isEmpty);

		if (subs.size() == 0) {
			return Collections.emptyMap();
		}

		if (subs.size() == 1) {
			return subs.get(0);
		}

		ImmutableMap.Builder<K, V> as = ImmutableMap.builder();
		subs.stream().flatMap(s -> s.entrySet().stream())
				.collect(groupingBy(e -> e.getKey(), mapping(e -> e.getValue(), toList())))
				.forEach((k, v) -> as.put(k, valueCombiner.apply(v)));

		return as.build();
	}

	public static StateFunc combine(List<StateFunc> subs) {
		subs = new ArrayList<>(subs);
		subs.removeIf(s -> s.equals(StateFunc.of()));

		if (subs.size() == 1) {
			return subs.get(0);
		}
		StateFunc result = new StateFunc();
		result.value = combineValue(subs.stream().map(s -> s.value).collect(toList()));
		result.branchCond = combineValue(subs.stream().map(s -> s.branchCond).collect(toList()));
		result.assigns = combineAssigns(subs.stream().map(s -> s.assigns).collect(toList()));
		result.updates = combineUpdates(subs.stream().map(s -> s.updates).collect(toList()));
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

	public static StateFunc ofUpdate(Map<ObjectDefinition, FilteredValueFunc> updates) {
		StateFunc s = createEmpty();
		s.updates = updates;
		return s;
	}

	public static StateFunc ofAssign(Map<ObjectDefinition, ValueFunc> assigns) {
		StateFunc s = createEmpty();
		s.assigns = assigns;
		return s;
	}

	private static ValueFunc applyValue(ValueFunc v, Map<ObjectDefinition, ValueFunc> parameterValues) {
		boolean nochange = true;
		ImmutableSet.Builder<ObjectReference> objs = ImmutableSet.builder();
		objs.addAll(v.getObjects());
		ImmutableSet.Builder<ObjectDefinition> params = ImmutableSet.builder();
		for (ObjectDefinition param : v.getParameters()) {
			ValueFunc s = parameterValues.get(param);
			if (s == null) {
				params.add(param);
			} else {
				nochange = false;
				params.addAll(s.getParameters());
				objs.addAll(s.getObjects());
			}
		}

		if (nochange) {
			return v;
		} else {
			Set<ObjectReference> so = objs.build();
			if (so.size() == v.getObjects().size()) {
				so = v.getObjects();
			}
			return ValueFunc.of(params.build(), so);
		}
	}

	private static FilteredValueFunc applyFilteredValue(FilteredValueFunc fv,
			Map<ObjectDefinition, ValueFunc> parameterValues) {
		ValueFunc v = applyValue(fv.getValue(), parameterValues);
		ValueFunc f = applyValue(fv.getFilter(), parameterValues);
		if (v == fv.getValue() && f == fv.getFilter()) {
			return fv;
		}
		return new FilteredValueFunc(v, f);
	}

	private static Map<ObjectDefinition, ValueFunc> applyAssigns(Map<ObjectDefinition, ValueFunc> assigns,
			Map<ObjectDefinition, ValueFunc> parameterValues) {
		boolean nochange = true;
		ImmutableMap.Builder<ObjectDefinition, ValueFunc> as = ImmutableMap.builder();
		for (Entry<ObjectDefinition, ValueFunc> e : assigns.entrySet()) {
			ValueFunc v = applyValue(e.getValue(), parameterValues);
			if (v != e.getValue()) {
				nochange = false;
			}
			// f(a,out b) = { b = a+b } --> assigns = b: a,b f(x,y) ---> x:x,y
			if (parameterValues.containsKey(e.getKey())) {
				ValueFunc lvalue = parameterValues.get(e.getKey());
				nochange = false;
				// TODO check it is a lvalue
//				Preconditions.checkState(lvalue.assigns.isEmpty());
//				Preconditions.checkState(lvalue.updates.isEmpty());
				Preconditions.checkState(lvalue.getObjects().isEmpty());
				Preconditions.checkState(lvalue.getParameters().size() == 1);
				as.put(lvalue.getParameters().iterator().next(), v);

			} else {
				as.put(e.getKey(), v);
			}

		}
		if (nochange) {
			return assigns;
		} else {
			return as.build();
		}

	}

	private static Map<ObjectDefinition, FilteredValueFunc> applyUpdates(Map<ObjectDefinition, FilteredValueFunc> updates,
			Map<ObjectDefinition, ValueFunc> parameterValues) {
		boolean nochange = true;
	
		ImmutableMap.Builder<ObjectDefinition, FilteredValueFunc>  as = ImmutableMap.builder();
		for (Entry<ObjectDefinition, FilteredValueFunc>  e : updates.entrySet()) {
			FilteredValueFunc v = applyFilteredValue(e.getValue(), parameterValues);
			if (v != e.getValue()) {
				nochange = false;
			}
			// f(a,out b) = { b = a+b } --> assigns = b: a,b f(x,y) ---> x:x,y
			if (parameterValues.containsKey(e.getKey())) {
				ValueFunc lvalue = parameterValues.get(e.getKey());
				nochange = false;
				// TODO check it is a lvalue
//				Preconditions.checkState(lvalue.assigns.isEmpty());
//				Preconditions.checkState(lvalue.updates.isEmpty());
				Preconditions.checkState(lvalue.getObjects().isEmpty());
				Preconditions.checkState(lvalue.getParameters().size() == 1);
				as.put(lvalue.getParameters().iterator().next(), v);

			} else {
				as.put(e.getKey(), v);
			}

		}
		if (nochange) {
			return updates;
		} else {
			return as.build();
		}

	}

	public StateFunc applyDefinition(Map<ObjectDefinition, StateFunc> parameterValues) {
		// (1) mutate s(f) to s2(f) with param value paramI.ValueFunc.
		// (2) return combine ( s2(f) s(param1) s(param2) s param(3) )
		if (parameterValues.isEmpty()) {
			return this;
		}
		
		Map<ObjectDefinition,ValueFunc> varp = new HashMap<>();
		parameterValues.forEach((k,v)->varp.put(k, v.getValue()));
		StateFunc m1 = new StateFunc();
		m1.value = applyValue(this.value, varp);
		m1.branchCond = applyValue(this.branchCond, varp);
		m1.assigns = applyAssigns(this.getAssigns(), varp);
		m1.updates = applyUpdates(this.getUpdates(), varp);
		List<StateFunc> f = new ArrayList<>(parameterValues.values());
		f.add(m1);
		StateFunc m2 = combine(f);
		m2.value = m1.value;
		return m2;
	}


	public StateFunc applyState(Map<ObjectDefinition, ValueFunc> parameterValues) {
		// (1) mutate s(f) to s2(f) with param value paramI.ValueFunc.
		// (2) return combine ( s2(f) s(param1) s(param2) s param(3) )
		if (parameterValues.isEmpty()) {
			return this;
		}
		
		StateFunc m1 = new StateFunc();
		m1.value = applyValue(this.value, parameterValues);
		m1.branchCond = applyValue(this.branchCond, parameterValues);
		m1.assigns = applyAssigns(this.getAssigns(), parameterValues);
		m1.updates = applyUpdates(this.getUpdates(), parameterValues);
		return m1;
	}	
	
	public StateFunc addWhereClause(StateFunc clause) {
		StateFunc m1 = new StateFunc();
		m1.value = this.value;
		m1.assigns = combineAssigns(ImmutableList.of(this.assigns, clause.assigns));
		m1.updates = combineUpdates(ImmutableList.of(this.updates, clause.updates));
		m1.branchCond = combineValue(ImmutableList.of(this.branchCond, clause.value, clause.branchCond));
		return m1;
	}

	/**
	 * used in "exists"
	 * 
	 * @param subs
	 * @return
	 */
	public static StateFunc combineNoValue(List<StateFunc> subs) {
		StateFunc m1 = combine(subs);
		m1.value = ValueFunc.of();
		return m1;
	}

	public static StateFunc combineInsertOrUpdate(List<ColumnDefinition> defs, List<StateFunc> subs) {
		throw new RuntimeException("not implemented");
	}

	public static StateFunc combineSelectInto(List<VariableDefinition> defs, List<StateFunc> subs) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public String toString() {
		String sb = "StateFunc [";
		if (!value.isEmpty()) {
			sb += " value=";
			sb += value;
		}

		if (!branchCond.isEmpty()) {
			sb += " branchCond=";
			sb += branchCond;
		}
		if (!updates.isEmpty()) {
			sb += " updates=";
			sb += updates;
		}

		if (!assigns.isEmpty()) {
			sb += " assigns=";
			sb += assigns;
		}

		sb += "]";
		return sb;
	}

}
