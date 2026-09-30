package dev.luvvllx.runtime;

public class Ex0 extends RuntimeException {

	public Ex0(String info) {
		super(info);
	}

	public Ex0(String info,Throwable e) {
		super(info,e);
	}

	public Ex0() {
		super();
	}

	public Ex0(Throwable e) {
		super(e);
	}

}
