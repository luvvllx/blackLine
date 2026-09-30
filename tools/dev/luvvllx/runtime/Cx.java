package dev.luvvllx.runtime;

import java.security.MessageDigest;
import java.util.Arrays;

public final class Cx {

    private Cx() {
    }

    public static String table(byte[] salt) {
        return "q" + Integer.toHexString(((salt[0] & 0xff) << 8) | (salt[1] & 0xff));
    }

    public static int parts() {
        return 256;
    }

    public static int width() {
        return 4;
    }

    public static Object[] load(ClassLoader cl, byte[] salt) throws Exception {
        Class<?> tbl = Class.forName(table(salt).replace('/', '.'), true, cl);
        String[] names = (String[]) tbl.getMethod("a").invoke(null);
        int[] seeds = new int[names.length];
        for (int i = 0; i < names.length; i++) {
            seeds[i] = (Integer) Class.forName(names[i].replace('/', '.'), true, cl)
                    .getMethod("a").invoke(null);
        }
        return new Object[]{tbl.getMethod("p").invoke(null), tbl.getMethod("g").invoke(null), seeds};
    }

    public static byte[] slot(Object[] inputs, byte[] salt, int guard, int slot) throws Exception {
        java.lang.invoke.MethodHandles.Lookup lk = java.lang.invoke.MethodHandles.lookup();
        java.lang.invoke.MethodHandles.Lookup made = lk.defineHiddenClass(forgeBytes(), true);
        java.lang.invoke.MethodHandle derive = made.findStatic(made.lookupClass(), "derive",
                java.lang.invoke.MethodType.methodType(byte[].class,
                        int[].class, int[].class, int[].class, byte[].class, int.class, int.class));
        try {
            return (byte[]) derive.invokeWithArguments(inputs[0], inputs[1], inputs[2], salt, guard, slot);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        } finally {
            derive = null;
            made = null;
        }
    }

    private static byte[] forgeBytes() throws Exception {

        String here = Cx.class.getName();
        String pkg = here.substring(0, here.lastIndexOf('.') + 1).replace('.', '/');
        try (java.io.InputStream in = Cx.class.getResourceAsStream("/" + pkg + "fg.class")) {
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] t = new byte[4096];
            int n;
            while ((n = in.read(t)) > 0) {
                bo.write(t, 0, n);
            }
            return bo.toByteArray();
        }
    }

    public static byte[] digest(byte[] master, byte[] salt) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(master);
        md.update(salt);
        md.update((byte) 0x9e);
        return md.digest();
    }

public static byte[] crypt(byte[] key, byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] out = new byte[data.length];
        byte[] buf = new byte[key.length + 4];
        System.arraycopy(key, 0, buf, 0, key.length);
        for (int b = 0, off = 0; off < data.length; b++, off += 32) {
            buf[key.length] = (byte) (b >>> 24);
            buf[key.length + 1] = (byte) (b >>> 16);
            buf[key.length + 2] = (byte) (b >>> 8);
            buf[key.length + 3] = (byte) b;
            byte[] ks = md.digest(buf);
            for (int i = 0; i < 32 && off + i < data.length; i++) {
                out[off + i] = (byte) (data[off + i] ^ ks[i]);
            }
            Arrays.fill(ks, (byte) 0);
        }
        return out;
    }

    public static byte[] macOf(byte[] key, byte[] body, int tagLen) throws Exception {
        int bs = Math.max(64, key.length);
        byte[] k = new byte[bs];
        System.arraycopy(key, 0, k, 0, key.length);
        byte[] pad = new byte[bs];
        byte[] inner = new byte[bs + body.length];
        byte[] outer = new byte[bs + 32];
        for (int i = 0; i < bs; i++) {
            pad[i] = (byte) (k[i] ^ 0x36);
        }
        System.arraycopy(pad, 0, inner, 0, bs);
        System.arraycopy(body, 0, inner, bs, body.length);
        byte[] ih = MessageDigest.getInstance("SHA-256").digest(inner);
        for (int i = 0; i < bs; i++) {
            pad[i] = (byte) (k[i] ^ 0x5c);
        }
        System.arraycopy(pad, 0, outer, 0, bs);
        System.arraycopy(ih, 0, outer, bs, 32);
        byte[] oh = MessageDigest.getInstance("SHA-256").digest(outer);
        byte[] out = Arrays.copyOf(oh, tagLen);
        Arrays.fill(k, (byte) 0);
        Arrays.fill(pad, (byte) 0);
        Arrays.fill(inner, (byte) 0);
        Arrays.fill(outer, (byte) 0);
        return out;
    }

    public static String encode(String s, int key) {
        char[] out = new char[s.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (char) (s.charAt(i) ^ mix(key, i));
        }
        return new String(out);
    }

    public static String decode(String s, int key) {
        char[] out = new char[s.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (char) (s.charAt(i) ^ mix(key, i));
        }
        return new String(out);
    }

    public static int mix(int key, int i) {
        return key ^ (key * (i + 1)) ^ (key << (i & 15));
    }
}
