package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class ExprAnalyzeResult {
	private StateFunc transformation;
	private List<Object> literalValue;

	public StateFunc getTransformation() {
		return transformation;
	}

	public List<Object> getLiteralValue() {
		return literalValue;
	}

	public ExprAnalyzeResult(StateFunc transformation, String literalValue) {
		super();
		this.transformation = transformation;
		this.literalValue = ImmutableList.of(literalValue);
	}

	public ExprAnalyzeResult(VariableDefinition vd) {
		super();
		this.transformation = StateFunc.ofValue(ValueFunc.of(vd));
		this.literalValue = ImmutableList.of(vd);
	}

	public ExprAnalyzeResult(StateFunc transformation, List<Object> values) {
		super();
		this.transformation = transformation;
		this.literalValue = values;
	}

	
	public ExprAnalyzeResult(StateFunc transformation) {
		super();
		this.transformation = transformation;
		this.literalValue = Collections.emptyList();
	}

	@Override
	public String toString() {
		return "E [v=" + literalValue + ", t=" + transformation + "]";
	}
	
	
}
