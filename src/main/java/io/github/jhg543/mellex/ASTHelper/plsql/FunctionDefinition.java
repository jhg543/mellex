package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;

public class FunctionDefinition extends ObjectDefinition {

	private List<ParameterDefinition> parameters;

	private StateFunc definition;

	public List<ParameterDefinition> getParameters() {
		return parameters;
	}

	public void setParameters(List<ParameterDefinition> parameters) {
		this.parameters = parameters;
	}

	public StateFunc getDefinition() {
		return definition;
	}

	public void setDefinition(StateFunc definition) {
		this.definition = definition;
	}

	public StateFunc applyDefinition(List<StateFunc> params) {
		if (parameters.size() != params.size()) {
			// TODO deal with default value
			throw new IllegalStateException("parameter count mismatch" + parameters.size() + " " + params.size());
		}

		Map<ObjectDefinition, StateFunc> paramValues = new HashMap<ObjectDefinition, StateFunc>();
		for (int i = 0; i < params.size(); ++i) {
			paramValues.put(parameters.get(i), params.get(i));
		}
		return definition.apply(paramValues);
	}

	private static Map<Integer, FunctionDefinition> unknown = new HashMap<>();

	private static FunctionDefinition makeUnknownFunction(int parameterCount) {
		FunctionDefinition d = new FunctionDefinition();
		List<ParameterDefinition> params = new ArrayList<>();
		for (int i = 0; i < parameterCount; ++i) {
			ParameterDefinition v = new ParameterDefinition();
			v.setName("ARG" + i);
			params.add(v);
		}
		d.setParameters(params);
		StateFunc func = StateFunc.ofValue(ValueFunc.of(params, Collections.emptyList()));
		d.setDefinition(func);
		return d;
	}

	public static FunctionDefinition unknownFunction(int parameterCount) {
		FunctionDefinition d = unknown.get(parameterCount);
		if (d != null) {
			return d;
		}
		d = makeUnknownFunction(parameterCount);
		unknown.put(parameterCount, d);
		return d;
	}

}
