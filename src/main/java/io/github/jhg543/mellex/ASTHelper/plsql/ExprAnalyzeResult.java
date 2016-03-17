package io.github.jhg543.mellex.ASTHelper.plsql;

public class ExprAnalyzeResult {
	private StateFunc transformation;
	private String literalValue;
	
	public StateFunc getTransformation() {
		return transformation;
	}
	public String getLiteralValue() {
		return literalValue;
	}
	public ExprAnalyzeResult(StateFunc transformation, String literalValue) {
		super();
		this.transformation = transformation;
		this.literalValue = literalValue;
	}

	public ExprAnalyzeResult(StateFunc transformation) {
		super();
		this.transformation = transformation;
	}
}
