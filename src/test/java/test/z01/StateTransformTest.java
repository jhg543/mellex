package test.z01;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.github.jhg543.mellex.ASTHelper.plsql.ColumnDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectReference;
import io.github.jhg543.mellex.ASTHelper.plsql.StateFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.ValueFunc;
import io.github.jhg543.mellex.ASTHelper.plsql.VariableDefinition;

public class StateTransformTest {

	private VariableDefinition[] v;
	private ObjectReference[] r;

	@Before
	public void init() {
		v = new VariableDefinition[6];
		r = new ObjectReference[6];
		IntStream.range(0, 5).forEach(i -> {
			v[i] = new VariableDefinition();
			v[i].setName("v" + i);
			r[i] = new ObjectReference();
		});

	}

	private void checkEqual(ValueFunc v1, ValueFunc v2) {

		Assert.assertEquals(v1.getObjects(), v2.getObjects());
		Assert.assertEquals(v1.getParameters(), v2.getParameters());
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

		public void setUpdates(Map<ObjectDefinition, ValueFunc> updates) {
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
	public void test1() {
		StateFunc s1 = StateFunc.ofValue(ValueFunc.of(v[0], v[2]));
		StateFunc s2 = StateFunc.ofValue(ValueFunc.of(v[1], v[0]));
		StateFunc s3 = StateFunc.ofValue(ValueFunc.of(r[1]));
		StateFunc answer = StateFunc.combine(s1, s2, s3);
		MutableSD expectedResult = MutableSD.create();
		expectedResult.setValue(ValueFunc.of(Arrays.asList(v[0], v[1], v[2]), Arrays.asList(r[1])));

		checkEqual(expectedResult, answer);

	}
}
