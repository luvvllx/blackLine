package dev.luvvllx.runtime;

public class Ex1 extends Error {

	public Ex1(String info) {
		super(info);
	}

	public Ex1(String info,Throwable e) {
		super(info,e);
	}

	public Ex1() {
		super();
	}

	public Ex1(Throwable e) {
		super(e);
	}

}
