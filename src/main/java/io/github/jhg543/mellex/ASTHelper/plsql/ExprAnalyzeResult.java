package io.github.jhg543.mellex.ASTHelper.plsql;

public class ExprAnalyzeResult {
	private StateTransformDefinition transformation;
	private String literalValue;
	
	public StateTransformDefinition getTransformation() {
		return transformation;
	}
	public String getLiteralValue() {
		return literalValue;
	}
	public ExprAnalyzeResult(StateTransformDefinition transformation, String literalValue) {
		super();
		this.transformation = transformation;
		this.literalValue = literalValue;
	}

	public ExprAnalyzeResult(StateTransformDefinition transformation) {
		super();
		this.transformation = transformation;
	}
}
