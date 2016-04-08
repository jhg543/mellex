package test.z01;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import io.github.jhg543.mellex.ASTHelper.plsql.ColumnDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.FilteredValueFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.FunctionDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectReference;
import io.github.jhg543.mellex.ASTHelper.plsql.ParameterDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.ValueFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

public class StateTransformTest {

	private VariableDefinition[] v;
	private ColumnDefinition[] c;
	private ObjectReference[] r;

	@Before
	public void init() {
		int m = 6;
		v = new VariableDefinition[m];
		r = new ObjectReference[m];
		c = new ColumnDefinition[m];
		IntStream.range(0, m).forEach(i -> {
			v[i] = new VariableDefinition();
			v[i].setName("v" + i);
			r[i] = new ObjectReference(null,null,0,0);
			c[i] = new ColumnDefinition("c" + i,-1,null);
			r[i].setObjectDefinition(c[i]);
		});

	}

	private void checkEqual(ValueFunc v1, ValueFunc v2) {

		Assert.assertEquals(v1.getObjects(), v2.getObjects());
		Assert.assertEquals(v1.getParameters(), v2.getParameters());
	}

	private void checkEqual(FilteredValueFunc v1, FilteredValueFunc v2) {

		checkEqual(v1.getFilter(), v2.getFilter());
		checkEqual(v1.getValue(), v2.getValue());
	}

	
	private void checkEqual(StateFunc s1, StateFunc s2) {
		checkEqual(s1.getValue(), s2.getValue());
		checkEqual(s1.getBranchCond(), s2.getBranchCond());
		Assert.assertEquals(s1.getUpdates().size(), s2.getUpdates().size());
		s1.getUpdates().forEach((k, v) -> checkEqual(s2.getUpdates().get(k), v));
		s1.getAssigns().forEach((k, v) -> checkEqual(s2.getAssigns().get(k), v));
	}

	public static class MutableSD extends StateFunc {

		public void setValue(ValueFunc value) {
			this.value = value;
		}

		public void setUpdates(Map<ObjectDefinition, FilteredValueFunc> updates) {
			this.updates = updates;
		}

		public void setAssigns(Map<ObjectDefinition, ValueFunc> assigns) {
			this.assigns = assigns;
		}

		public void setAddedBranchConds(ValueFunc addedBranchConds) {
			this.branchCond = addedBranchConds;
		}

		public static MutableSD create() {
			MutableSD r = new MutableSD();
			r.setValue(ValueFunc.of());
			r.setAddedBranchConds(ValueFunc.of());
			r.setUpdates(new HashMap<>());
			r.setAssigns(new HashMap<>());
			return r;

		}
	}

	@Test
	public void combineValue() {
		StateFunc s1 = StateFunc.ofValue(ValueFunc.of(v[0], v[2]));
		StateFunc s2 = StateFunc.ofValue(ValueFunc.of(v[1], v[0]));
		StateFunc s3 = StateFunc.ofValue(ValueFunc.of(r[1]));
		StateFunc s4 = StateFunc.ofValue(ValueFunc.of(r[1], r[2]));
		StateFunc s5 = StateFunc.ofValue(ValueFunc.of(r[1], r[2], r[3]));
		StateFunc m1 = StateFunc.combine(s1, s2, s3);
		StateFunc m2 = StateFunc.combine(s4, s5, s3);
		StateFunc answer = StateFunc.combine(m1, m2);
		MutableSD expectedResult = MutableSD.create();
		expectedResult.setValue(ValueFunc.of(Arrays.asList(v[0], v[1], v[2]), Arrays.asList(r[1], r[2], r[3])));
		checkEqual(expectedResult, answer);
	}

	@Test
	public void combineUpdate() {
		ValueFunc vr0 = ValueFunc.of(r[0]);

		Map<ObjectDefinition, FilteredValueFunc> u1 = ImmutableMap.of(c[0], new FilteredValueFunc(ValueFunc.of(v[5]),vr0), c[1], new FilteredValueFunc(ValueFunc.of(v[4]),vr0));
		Map<ObjectDefinition, FilteredValueFunc> u2 = ImmutableMap.of(c[0], new FilteredValueFunc(ValueFunc.of(v[5], v[4]),vr0), c[2], new FilteredValueFunc(ValueFunc.of(r[1]),vr0));
		Map<ObjectDefinition, FilteredValueFunc> u3 = ImmutableMap.of(c[0], new FilteredValueFunc(ValueFunc.of(r[0]),vr0), c[1], new FilteredValueFunc(ValueFunc.of(v[0]),vr0));
	
			StateFunc s1 = StateFunc.ofUpdate(u1);
		StateFunc s2 = StateFunc.ofUpdate(u2);
		StateFunc s3 = StateFunc.ofUpdate(u3);

		Map<ObjectDefinition, ValueFunc> a1 = ImmutableMap.of(c[0], ValueFunc.of(v[5]), c[1], ValueFunc.of(v[4]));
		Map<ObjectDefinition, ValueFunc> a2 = ImmutableMap.of(c[0], ValueFunc.of(v[5], v[4]), c[2], ValueFunc.of(r[1]));
		Map<ObjectDefinition, ValueFunc> a3 = ImmutableMap.of(c[0], ValueFunc.of(r[0]), c[1], ValueFunc.of(v[0]));

		StateFunc s4 = StateFunc.ofAssign(a1);
		StateFunc s5 = StateFunc.ofAssign(a2);
		StateFunc s6 = StateFunc.ofAssign(a3);

		StateFunc answer = StateFunc.combine(s1, s2, s3, s4, s5, s6);
		MutableSD expectedResult = MutableSD.create();
		expectedResult.setUpdates(ImmutableMap.of(c[0], new FilteredValueFunc(ValueFunc.of(Arrays.asList(v[5], v[4]), Arrays.asList(r[0])),vr0), c[1],
				new FilteredValueFunc(ValueFunc.of(v[4], v[0]),vr0), c[2], new FilteredValueFunc(ValueFunc.of(r[1]),vr0)));
		expectedResult.setAssigns(ImmutableMap.of(c[0], ValueFunc.of(Arrays.asList(v[5], v[4]), Arrays.asList(r[0])), c[1],
				ValueFunc.of(v[4], v[0]), c[2], ValueFunc.of(r[1])));
		checkEqual(expectedResult, answer);
	}

	@Test
	public void functionCall() {
		// f(p0,p1,p2) { while (p2+col[2]) col[0] =p0 + p1 ; col[1] = r[1] ; p0
		// = p1 + p2; return p1+p2; }
		ValueFunc vr0 = ValueFunc.of(r[0]);
		FunctionDefinition fndef = new FunctionDefinition();
		List<ParameterDefinition> params = new ArrayList<ParameterDefinition>();
		IntStream.range(0, 3).forEach(i -> {
			params.add(new ParameterDefinition());
			params.get(i).setName("p" + i);
		});
		fndef.setParameters(params);
		VariableDefinition[] p = params.toArray(new VariableDefinition[0]);

		Map<ObjectDefinition, ValueFunc> as = ImmutableMap.of(p[1], ValueFunc.of(p[0], p[2]));
		Map<ObjectDefinition, FilteredValueFunc> us = ImmutableMap.of(c[0], new FilteredValueFunc(ValueFunc.of(p[0], p[1]),vr0), c[1], new FilteredValueFunc(ValueFunc.of(r[1]),vr0));
		ValueFunc v1 = ValueFunc.of(p[1], p[2]);
		ValueFunc b1 = ValueFunc.of(Arrays.asList(p[2]), Arrays.asList(r[2]));
		StateFunc fn = StateFunc.combine(StateFunc.ofValue(v1), StateFunc.ofAssign(as), StateFunc.ofUpdate(us),
				StateFunc.ofBranchCond(b1));
		fndef.setDefinition(fn);
		// f(v0+v1,v2,v3+v4+v5)
		StateFunc cc1 = StateFunc.ofValue(ValueFunc.of(v[0], v[1]));
		StateFunc cc2 = StateFunc.ofValue(ValueFunc.of(v[2]));
		StateFunc cc3 = StateFunc.ofValue(ValueFunc.of(v[3], v[4], v[5]));
		StateFunc answer = fndef.apply(Arrays.asList(cc1, cc2, cc3));
		// System.out.println(answer);

		MutableSD expectedResult = MutableSD.create();
		expectedResult.setValue(ValueFunc.of(v[2], v[3], v[4], v[5]));
		expectedResult.setAddedBranchConds(ValueFunc.of(Arrays.asList(v[3], v[4], v[5]), Arrays.asList(r[2])));
		expectedResult.setUpdates(ImmutableMap.of(c[0], new FilteredValueFunc(ValueFunc.of(v[0], v[1], v[2]),vr0), c[1], new FilteredValueFunc(ValueFunc.of(r[1]),vr0)));
		expectedResult.setAssigns(ImmutableMap.of(v[2], ValueFunc.of(v[0], v[3], v[4], v[5], v[1])));

		checkEqual(expectedResult, answer);
	}

}
