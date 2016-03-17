package io.github.jhg543.mellex.ASTHelper.plsql;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import static java.util.stream.Collectors.*;

public class StateTransformDefinition {
	private ValueTransformDefinition value;
	private Map<ObjectDefinition, ValueTransformDefinition> updates;
	private Map<ObjectDefinition, ValueTransformDefinition> assigns;
	private ValueTransformDefinition addedBranchConds;

	/**
	 * @param subs
	 * @return combined sets (immutable)
	 */
	private static ValueTransformDefinition combineValue(List<ValueTransformDefinition> subs) {

		if (subs.size() == 1) {
			return subs.get(0);
		}
		ImmutableSet.Builder<ObjectReference> objs = ImmutableSet.builder();
		ImmutableSet.Builder<ObjectDefinition> params = ImmutableSet.builder();
		subs.forEach(s -> {
			objs.addAll(s.getObjects());
			params.addAll(s.getParameters());
		});
		ValueTransformDefinition result = new ValueTransformDefinition();
		result.setObjects(objs.build());
		result.setParameters(params.build());
		return result;

	}

	
	/**
	 * {k1:v1,k2:v2}+{k1:v4,k3:v3} = {k1: combineValue{v1,v4}  , k2:v2, k3:v3 }
	 * @param subs
	 * @return
	 */
	private static Map<ObjectDefinition, ValueTransformDefinition> combineAssigns(
			List<Map<ObjectDefinition, ValueTransformDefinition>> subs) {
		if (subs.size() == 1) {
			return subs.get(0);
		}
		ImmutableMap.Builder<ObjectDefinition, ValueTransformDefinition> as = ImmutableMap.builder();
		subs.stream().flatMap(s -> s.entrySet().stream())
				.collect(groupingBy(e -> e.getKey(), mapping(e -> e.getValue(), toList())))
				.forEach((k, v) -> as.put(k, combineValue(v)));
		;

		return as.build();

	}

	public static StateTransformDefinition combine(List<StateTransformDefinition> subs) {
		if (subs.size() == 1) {
			return subs.get(0);
		}
		StateTransformDefinition result = new StateTransformDefinition();
		result.value = combineValue(subs.stream().map(s -> s.value).collect(toList()));
		result.addedBranchConds = combineValue(subs.stream().map(s -> s.addedBranchConds).collect(toList()));
		result.assigns = combineAssigns(subs.stream().map(s -> s.assigns).collect(toList()));
		result.updates = combineAssigns(subs.stream().map(s -> s.updates).collect(toList()));
		return result;
	}
	
	public static StateTransformDefinition combine(StateTransformDefinition... subs) {
		return combine(Arrays.asList(subs));
	}
	
	private static StateTransformDefinition createEmpty(){
		StateTransformDefinition s = new StateTransformDefinition();
		s.assigns = Collections.emptyMap();
		s.updates= Collections.emptyMap();
		s.value = ValueTransformDefinition.of();
		s.addedBranchConds = ValueTransformDefinition.of();
		return s;
	}
	
	private static StateTransformDefinition EMPTY = createEmpty();
	
	public static StateTransformDefinition of(){
		return EMPTY;
	}
}
