package dev.luvvllx.runtime;

import java.io.*;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;

import dev.luvvllx.runtime.Ex0;
import dev.luvvllx.bline.Main;
import sun.misc.Unsafe;

import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class B1 implements Serializable {

	private static final int consts = 5000;
	private static final long serialVersionUID = 7345893312951L;
	public static final boolean _________;
	public static final String slotOne,slotTwo,slotThree;
	public static final String luvvllx = "made vith love by luvvllx :3";

	static {

        byte[] a = new byte[11];
		boolean b = false;
		try(InputStream is = B1.class.getResourceAsStream("/b.marker")) {
			is.read(a);
			b = new String(a).equals("B1");
		} catch (Exception e) { }

		String s1 = "jullySlot0";
		String s2 = "jullySlot1";
		String s3 = "jullySlot2";

		try {
			InputStream stream = B1.class.getResourceAsStream("/b.blob");
			if (stream != null) {

				byte[] bytes = new byte[stream.available()];
				stream.read(bytes);
				stream.close();
				for (int i = 0; i < bytes.length; i++) {
					bytes[i] ^= (byte)(new Random(i).nextInt(256));
				}

				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
				B1 ii = i = (B1) ois.readObject();
				ois.close();
				ii.init();
			}
		} catch (Throwable e) {
		}

		boolean isErrored = false;
        try {

			Field f = s1.getClass().getDeclaredField("value");
			f.setAccessible(true);
			Object obj = f.get(s1);
			if (obj instanceof byte[]) {
				Field f2 = s1.getClass().getDeclaredField("coder");
				f2.setAccessible(true);

				f2.set(s1, f2.get(s2));
				f.set(s1, f.get(s2));
			} else if (obj instanceof char[]) {
				f.set(s1, s2.toCharArray());
			} else throw new RuntimeException();

		} catch (Throwable e) {

			isErrored = true;
        }

		slotOne = s1;
		slotTwo = isErrored ? s1 : s2;
		slotThree = s3;

		_________ = b;
	}

	public static Object dynBootstrap(Object lookup, Object idk, Object mt, Object type, Object key, Object clazz, Object method, Object des) {

		try {

			int key1 = (Integer) key;
			char[] charKey = new char[key1 % 10 + 10];
			for(int i = 0;i < charKey.length;i++) {
				key1 += (key1 * (key1 >> 5) + (key1 << 5)) + 928374;
				charKey[i] = (char) (key1 % 0x10000);
			}

			char[] charOwner = clazz.toString().toCharArray();
			char[] charName = method.toString().toCharArray();
			char[] charDescriptor = des.toString().toCharArray();
			for(int i = 0;i < charOwner.length;i++) {
				charOwner[i] = (char) (charOwner[i] ^ charKey[i % charKey.length]);
			}
			for(int i = 0;i < charName.length;i++) {
				charName[i] = (char) (charName[i] ^ charKey[i % charKey.length]);
			}
			for(int i = 0;i < charDescriptor.length;i++) {
				charDescriptor[i] = (char) (charDescriptor[i] ^ charKey[i % charKey.length]);
			}

			MethodHandle mh = null;

			switch(type.hashCode()) {
				case -2051695357:
					mh = ((Lookup) lookup).findStatic(
							Class.forName(new String(charOwner)),
							new String(charName),
							MethodType.fromMethodDescriptorString(
									new String(charDescriptor),
									B1.class.getClassLoader()
							)
					);
					break;
				case 859384035:
					mh = ((Lookup) lookup).findVirtual(
							Class.forName(new String(charOwner)),
							new String(charName),
							MethodType.fromMethodDescriptorString(
									new String(charDescriptor),
									B1.class.getClassLoader()
							)
					);
					break;
				default:
					throw new Ex0("bad bootstrap");
			}

			mh = mh.asType((MethodType) mt);

			return new ConstantCallSite(mh);

		} catch(Throwable e) {
			throw new Ex0("blackLine bootstrap fault", e);
		}

	}

	public static Object dynBootstrapPlain(Object lookup, Object idk, Object mt, Object type, Object clazz, Object method, Object des) {

		try {

			MethodHandle mh = null;

			switch(type.hashCode()) {
				case -2051695357:
					mh = ((Lookup) lookup).findStatic(
							Class.forName(clazz.toString()),
							new String(method.toString()),
							MethodType.fromMethodDescriptorString(
									new String(des.toString()),
									B1.class.getClassLoader()
							)
					);
					break;
				case 859384035:
					mh = ((Lookup) lookup).findVirtual(
							Class.forName(clazz.toString()),
							new String(method.toString()),
							MethodType.fromMethodDescriptorString(
									new String(des.toString()),
									B1.class.getClassLoader()
							)
					);
					break;
				default:
					throw new Ex0("bad bootstrap");
			}

			mh = mh.asType((MethodType) mt);

			return new ConstantCallSite(mh);
		} catch(Throwable e) {
			throw new Ex0("blackLine bootstrap fault", e);
		}

	}

	public static HashMap<Object, Object> hashMap;

	public static String decKey(Object s) {
		if(hashMap == null) hashMap = new HashMap<>();
		if(hashMap.containsKey(s)) {
            return hashMap.get(s).toString();
        }
		char[] newchars;
		if(s instanceof Throwable) {
			int key = ((Throwable) s).getStackTrace()[0].getLineNumber();
			int key2 = 0;
			int key3 = 0;
			char[] chars = ((Throwable) s).getLocalizedMessage().toCharArray();
			char[] chars2 = s.getClass().getName().toCharArray();

			newchars = new char[chars.length - 1];

			for(int i = 0;i < chars.length;i++) {
				if(i == 0) key2 = chars[i];
				else {
					key2 <<= key2 + (key2 % 4) + "dynamicStringKey".hashCode();
					key3 = (key2 / 9) >> 2;
					newchars[i - 1] = (char) (chars[i] ^ ((((key * (i)) ^ (key2 + key3)) ^ chars2[(i- 1) % chars2.length]) % 0x10000));
				}
			}
		} else {
			char key = 0;
			char[] chars = s.toString().toCharArray();

			newchars = new char[chars.length - 1];

			for(int i = 0;i < chars.length;i++) {
				if(i == 0) key = chars[i];
				else {
					newchars[i - 1] = (char) (chars[i] ^ (key * i));
				}
			}
		}

		synchronized (hashMap) {
			return hashMap.computeIfAbsent(s, k -> new String(newchars)).toString();
		}
	}

	public static long decKey(long l1, long l2, long l3) {
		if(_________) return l1 ^ l2 ^ l3 ^ -1;
		return (l1 ^ l2) ^ (~l3);
	}

	public static int decKey(int i1, int i2, int i3) {
		if(_________) return i1 ^ i2 ^ i3 ^ -1;
		return (i1 ^ i2) ^ (~i3);
	}

	public static void log(Object obj) {
		if(!_________) System.out.println(obj);
	}

	public static String test(String s) {
		System.out.println(s);
		return s;
	}

	private transient final Object type, clazz, method, des;
	private transient Object cache;

	public B1(Object type, Object clazz, Object method, Object des) {
		this.type = type;
		this.clazz = clazz;
		this.method = method;
		this.des = des;
	}

	public B1() {
		this(null, null, null, null);
	}

	public static final ConcurrentHashMap<String, Map.Entry<B1[], Integer>> map = new ConcurrentHashMap<>();
	public static Set<Map.Entry<String, Map.Entry<B1[], Integer>>> entries = map.entrySet();

	public static Object invoke(Object lookup, Object idk, Object mt, Object var1, Object var2, Object var3, Object var4) throws Throwable {

		String var11 = decKey(var1);
		String var22 = decKey(var2);

		Map.Entry<B1[], Integer> x = map.computeIfAbsent(var11, k -> {
			try {
				Class<?> clazz = Class.forName(var11);
				MethodHandle mh = ((Lookup) lookup).findStaticGetter(
						clazz,
                        var22,
						B1[].class
				);
				B1[] value = (B1[]) mh.invokeExact();
				int m = 0;

				return new AbstractMap.SimpleEntry<>(value, m);
			} catch (Throwable e) {

				throw new Ex0();
			}
		});

        return (x.getKey())[var11.hashCode() ^ var22.hashCode() ^ var3.hashCode() ^ var4.hashCode() ^ x.getValue()].invoke0(lookup, mt);
	}

	public Object invoke0(Object lookup, Object mt) {

		if(cache == null) {

			try {

				Throwable t = (Throwable)  clazz;

				char cc = 0;
				char[] chars = t.getLocalizedMessage().toCharArray();
				int key = t.getStackTrace()[0].getLineNumber();
				for(int i = 0;i < chars.length;i++) {
					chars[i] = (char) (chars[i] ^ ((cc ^ (key * (chars.length - i)) ^ t.getClass().getName().charAt(i % t.getClass().getName().length())) % 0x10000));
					cc = chars[i];
				}
				String charOwner = new String(chars);

				t = (Throwable) method;
				cc = 0;
				chars = t.getLocalizedMessage().toCharArray();
				key = t.getStackTrace()[0].getLineNumber();
				for(int i = 0;i < chars.length;i++) {
					chars[i] = (char) (chars[i] ^ ((cc ^ (key * (chars.length - i)) ^ t.getClass().getName().charAt(i % t.getClass().getName().length())) % 0x10000));
					cc = chars[i];
				}
				String charName = new String(chars);

				t = (Throwable) des;
				cc = 0;
				chars = t.getLocalizedMessage().toCharArray();
				key = t.getStackTrace()[0].getLineNumber();
				for(int i = 0;i < chars.length;i++) {
					chars[i] = (char) (chars[i] ^ ((cc ^ (key * (chars.length - i)) ^ t.getClass().getName().charAt(i % t.getClass().getName().length())) % 0x10000));
					cc = chars[i];
				}
				String charDescriptor = new String(chars);

				MethodHandle mh = null;

				switch(type.hashCode()) {
					case -2051695357:
						mh = ((Lookup) lookup).findStatic(
								Class.forName(new String(charOwner)),
								new String(charName),
								MethodType.fromMethodDescriptorString(
										new String(charDescriptor),
										B1.class.getClassLoader()
								)
						);
						break;
					case 859384035:
						mh = ((Lookup) lookup).findVirtual(
								Class.forName(new String(charOwner)),
								new String(charName),
								MethodType.fromMethodDescriptorString(
										new String(charDescriptor),
										B1.class.getClassLoader()
								)
						);
						break;
					default:
						throw new Ex0("bad bootstrap");
				}

				try {
					mh = mh.asType((MethodType) mt);
				} catch(Exception e) {
					e.printStackTrace();
				}

				cache = new ConstantCallSite(mh);

			} catch(Throwable e) {
				throw new Ex0("blackLine bootstrap fault", e);
			}

		}
		return cache;
	}

	public static final boolean unusedObject = false;

	public static B1 i;

	public void init() throws Exception {

		Field[] f = getClass().getFields();
		int hash = 0;
		boolean a = false;
		String fieldName = "unusedObject";

		for(Field field : f) {
			if(field.getName().endsWith("b9f") && !fieldName.equals(field.getName())) {

				int hash1 = field.get(this).hashCode();

				List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
				if(arguments != ManagementFactory.getRuntimeMXBean().getInputArguments()) {

					hash1 ^= arguments.hashCode();
					hash += hash1;
					hash1 *= 2;
					a = true;
				}
				try {
					arguments.add("");

					hash1 ^= arguments.hashCode();
					hash += hash1;
					hash1 *= 2;
					a = true;
				} catch (Exception ignored) {

				}

				for (String string : arguments) {
					if (string.contains("-javaagent:")) {

						hash1 ^= arguments.hashCode();
						hash += hash1;
						hash1 *= 2;
						a = true;
					}
					if (string.contains("-Xrunjdwp:")) {

						hash1 ^= arguments.hashCode();
						hash += hash1;
						hash1 *= 2;
						a = true;
					}
					if (string.contains("-agentlib:jdwp")) {

						hash1 ^= arguments.hashCode();
						hash += hash1;
						hash1 *= 2;
						a = true;
					}
					if (string.contains("-Xdebug")) {

						hash1 ^= arguments.hashCode();
						hash += hash1;
						hash1 *= 2;
						a = true;
					}
				}
                if(a) field.set(this, new Random(fieldName.hashCode()).nextInt() ^ hash1 ^ ((Boolean) a).hashCode() ^ 1231);

				hash ^= hash1;
			}
		}

		getClass().getField(fieldName).set(this, hash);

		try {
			init2();
		} catch(Throwable e) {
			if(e instanceof UnsupportedOperationException) throw new Ex0("Please use < java 17 to run");
		}

		if(a) {
			Thread thread = new Thread(() -> {
				try {
					Thread.sleep(consts);
					Field field;
					field = Unsafe.class.getDeclaredField("theUnsafe");
					field.setAccessible(true);
					((Unsafe) field.get(null)).putAddress(0, 0);
				} catch (Throwable ignored) {
				}
			});
			thread.setDaemon(true);
			thread.start();
		}

	}

	public void init2() {

		System.setSecurityManager(new B2());
		Thread thread1 = new Thread(() -> {
			try {
				while (true) {
					Thread.sleep(1000);
					if(System.getSecurityManager() instanceof B2) {
						System.setSecurityManager(new B2());
					}
				}
			} catch (Exception ignored) {

			}
			i = null;
		});
		thread1.setDaemon(true);
		thread1.start();

	}

}
