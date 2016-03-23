package io.github.jhg543.mellex.util.tuple;

public class Tuple2<T, K> {
	private T field0;
	private K field1;

	public T getField0() {
		return field0;
	}

	public void setField0(T field0) {
		this.field0 = field0;
	}

	public K getField1() {
		return field1;
	}

	public void setField1(K field1) {
		this.field1 = field1;
	}

	private Tuple2(T field0, K field1) {
		super();
		this.field0 = field0;
		this.field1 = field1;
	}


	
	public static <T,K>  Tuple2<T,K>  of(T field0, K field1)
	{
		return new Tuple2<T,K>(field0,field1);
	}
}
