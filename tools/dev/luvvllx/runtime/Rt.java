package dev.luvvllx.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Rt {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private Rt() {
    }

    public static String a(String enc, int key) {
        String hit = CACHE.get(enc);
        if (hit != null) {
            return hit;
        }
        String plain = Cx.decode(enc, key);
        String prev = CACHE.putIfAbsent(enc, plain);
        return prev == null ? plain : prev;
    }

    public static String x(String[] pool, int idx, int mask) {
        return pool[idx ^ mask];
    }

    public static int p() {
        int v = (int) System.nanoTime();
        v ^= v >>> 7;
        v *= 0x9E3779B1;
        v ^= v >>> 15;
        v *= 0x85EBCA6B;
        v ^= v >>> 13;
        return v | 1;
    }

    public static int q(int a, int b) {
        return (a ^ (b << 3)) + (a >>> 2);
    }

    public static CallSite b(MethodHandles.Lookup l, String tag, MethodType site,
                             String eo, int k1, String en, int k2, String ed, int k3, int kind) {
        try {
            ClassLoader cl = l.lookupClass().getClassLoader();

            Class<?> owner = Class.forName(Cx.decode(eo, k1).replace('/', '.'), false, cl);
            String name = Cx.decode(en, k2);
            MethodType mt = MethodType.fromMethodDescriptorString(Cx.decode(ed, k3), cl);
            MethodHandle mh = kind == 0
                    ? l.findStatic(owner, name, mt)
                    : l.findVirtual(owner, name, mt);
            return new ConstantCallSite(mh.asType(site));
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }
}
