package io.github.jhg543.mellex.util.tuple;

public class Tuple3<T, K, U> {
	private T field0;
	private K field1;
	private U field2;

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

	private Tuple3(T field0, K field1,U field2) {
		super();
		this.field0 = field0;
		this.field1 = field1;
		this.field2 = field2;
	}


	
	public static <T,K,U>  Tuple3<T,K,U>  of(T field0, K field1,U field2)
	{
		return new Tuple3<T,K,U>(field0,field1,field2);
	}

	public U getField2() {
		return field2;
	}

	public void setField2(U field2) {
		this.field2 = field2;
	}
}
