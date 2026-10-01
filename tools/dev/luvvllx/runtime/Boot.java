package dev.luvvllx.runtime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class Boot {

    private static final int TAG = 16;
    private static final int SALT = 8;
    private static final int STRIDE = 64;
    private static final int FILL = 4;
    private static final byte[] PREF = bytes(0xff, 0xfe, 0x00, 0x00, 0x00, 0x00,
            0xe2, 0x80, 0xae, 0xed, 0xa0, 0x80, 0xc0, 0x80, 0x00, 0x00,
            0x1b, 0x5b, 0x30, 0x3b, 0x33, 0x31, 0x6d, 0x07, 0x08, 0x08, 0x08,
            0xfe, 0xff, 0x00, 0x1a);
    private static final byte[] SUFF = bytes(0x00, 0x00, 0x00, 0x00,
            0xe2, 0x80, 0xab, 0xef, 0xbf, 0xbe, 0xf8, 0x3f, 0x3f,
            0x00, 0x0c, 0x00, 0x0d, 0x00, 0x0a, 0x04, 0x04, 0x04, 0x1a);

    private static byte[] bytes(int... v) {
        byte[] b = new byte[v.length];
        for (int i = 0; i < v.length; i++) {
            b[i] = (byte) v[i];
        }
        return b;
    }

    private static byte[] dearmor(byte[] blob) {
        int over=PREF.length+SUFF.length;
        if(blob==null||blob.length<=over+FILL+1) return null;
        for(int i=0;i<PREF.length;i++){
            if(blob[i]!=PREF[i]) return null;
        }
        for(int i=0;i<SUFF.length;i++){
            if(blob[blob.length-SUFF.length+i]!=SUFF[i]) return null;
        }
        int data=blob.length-over;
        int full=data/(STRIDE+FILL);
        int rest=data%(STRIDE+FILL);
        if(rest!=0&&(rest<=FILL||rest>STRIDE+FILL)) return null;
        int blocks=full+(rest==0?0:1);
        int sealed=data-blocks*FILL;
        byte[] out=new byte[sealed];
        for(int p=PREF.length,o=0,left=sealed;o<sealed;left-=STRIDE){
            int len=Math.min(STRIDE,left);
            System.arraycopy(blob,p,out,o,len);
            p+=len+FILL;
            o+=len;
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        File self = self();
        byte[] cfg = config(self);

        byte[] salt = Arrays.copyOfRange(cfg, 0, SALT);
        int folderLen = cfg[SALT] & 0xff;
        String folder = new String(cfg, SALT + 1, folderLen);
        int guard = cfg[SALT + 1 + folderLen] & 0xff;

        if (inspect()) {
            guard ^= 0xff;
        }

        INPUTS = dev.luvvllx.runtime.Cx.load(Boot.class.getClassLoader(), salt);

        BOOTREF = bootBytes(self);
        ROOT = dev.luvvllx.runtime.Cx.self(salt, guard, INPUTS, BOOTREF);
        dev.luvvllx.runtime.Rt.ROOT = ROOT;
        CHAIN = dev.luvvllx.runtime.Cx.chain0(ROOT);

        List<byte[]> files = read(self, folder);
        if (files.size() < 3) {
            die(1);
        }

        byte[] head = take(files, salt, guard);
        if (head == null) {
            die(2);
        }
        int a = num(head, 0);
        if (a <= 0 || a > head.length - 9) {
            die(3);
        }
        String entryClass = new String(head, 4, a);
        int b = num(head, 4 + a);
        if (b <= 0 || 4 + a + b > head.length) {
            die(3);
        }
        String entryMethod = new String(head, 8 + a, b);
        Arrays.fill(head, (byte) 0);

        byte[] idx = take(files, salt, guard);
        if (idx == null) {
            die(3);
        }
        int count = num(idx, 0);
        if (count <= 0 || count > 4096) {
            die(3);
        }
        String[] names = new String[count];
        int[] widths = new int[count];
        int[] totals = new int[count];
        int p = 4;
        for (int c = 0; c < count; c++) {
            int nameLen = num(idx, p);
            if (nameLen <= 0 || p + 12 + nameLen > idx.length) {
                Arrays.fill(idx, (byte) 0);
                die(3);
            }
            names[c] = new String(idx, p + 4, nameLen);
            widths[c] = num(idx, p + 4 + nameLen);
            totals[c] = num(idx, p + 8 + nameLen);
            p += 12 + nameLen;
        }
        Arrays.fill(idx, (byte) 0);

        Loader loader = new Loader();
        int live = 0;
        List<byte[]> deferred = new ArrayList<>();
        List<String> deferredNames = new ArrayList<>();

        for (int c = 0; c < count; c++) {
            if (widths[c] <= 0 || totals[c] <= 0) {
                break;
            }

            byte[] whole = new byte[totals[c]];
            int have = 0;
            boolean broke = false;
            for (int f = 0; f < widths[c]; f++) {
                byte[] frag = take(files, salt, guard);
                if (frag == null || have + frag.length > totals[c]) {
                    broke = true;
                    break;
                }
                System.arraycopy(frag, 0, whole, have, frag.length);
                have += frag.length;
                Arrays.fill(frag, (byte) 0);
            }
            if (broke || have != totals[c]) {
                die(6);
            }
            if (loader.known(names[c])) {
                Arrays.fill(whole, (byte) 0);
                live++;
                continue;
            }
            try {
                loader.define(names[c], whole);
                live++;
            } catch (Throwable t) {
                loader.forget(names[c]);
                deferredNames.add(names[c]);
                deferred.add(whole);
            }
        }

        for (int pass = 0; pass < 3 && !deferred.isEmpty(); pass++) {
            List<byte[]> still = new ArrayList<>();
            List<String> stillNames = new ArrayList<>();
            for (int i = 0; i < deferred.size(); i++) {
                byte[] body = deferred.get(i);
                try {
                    loader.define(deferredNames.get(i), body);
                    live++;
                } catch (Throwable t) {
                    still.add(body);
                    stillNames.add(deferredNames.get(i));
                }
            }
            deferred = still;
            deferredNames = stillNames;
        }
        for (byte[] body : deferred) {
            Arrays.fill(body, (byte) 0);
        }

        if (live == 0) {
            die(4);
        }

        Method m;
        try {
            Class<?> c = loader.load(entryClass);
            m = null;
            for (Method x : c.getDeclaredMethods()) {
                Class<?>[] pt = x.getParameterTypes();
                if (x.getName().equals(entryMethod) && pt.length == 1 && pt[0] == String[].class) {
                    m = x;
                    break;
                }
            }
        } catch (Throwable t) {
            m = null;
        }
        if (m == null) {
            die(5);
        }
        SELF = self;
        startWatch();
        m.setAccessible(true);
        m.invoke(null, (Object) args);
    }

    private static byte[] take(List<byte[]> files, byte[] salt, int guard) {
        int i = at++;
        byte[] blob = i < files.size() ? files.get(i) : null;
        if (blob == null) {
            return null;
        }
        byte[] fk = null;
        byte[] body = null;
        byte[] sealed = null;
        try {
            fk = Cx.slot(INPUTS, salt, guard, i, CHAIN);
            sealed = dearmor(blob);
            body = sealed == null ? null : unwrap(sealed, fk);
        } catch (Exception e) {
            body = null;
        }
        if (body != null) {
            try {
                CHAIN = Cx.advance(CHAIN, body);
            } catch (Exception e) {
                body = null;
            }
        }
        Arrays.fill(blob, (byte) 0);
        Arrays.fill(fk, (byte) 0);
        if (sealed != null) {
            Arrays.fill(sealed, (byte) 0);
        }
        files.set(i, null);
        return body;
    }

    private static Object[] INPUTS;
    private static byte[] ROOT;
    private static byte[] CHAIN;
    private static Map<String, byte[]> BOOTREF;
    private static File SELF;
    private static int at;

    private static void startWatch() {
        Thread w = new Thread(() -> {
            long d = 1500L + (ROOT[0] & 0xff) * 4L;
            while (true) {
                try {
                    Thread.sleep(d);
                    Map<String, byte[]> fresh = bootBytes(SELF);
                    if (fresh.size() != BOOTREF.size()) {
                        System.exit(0);
                    }
                    for (Map.Entry<String, byte[]> e : BOOTREF.entrySet()) {
                        byte[] g = fresh.get(e.getKey());
                        if (g == null || !java.util.Arrays.equals(g, e.getValue())) {
                            System.exit(0);
                        }
                    }
                } catch (Throwable t) {
                    System.exit(0);
                }
            }
        });
        w.setDaemon(true);
        w.start();
    }

    private static Map<String, byte[]> bootBytes(File self) throws Exception {
        Map<String, byte[]> out = new HashMap<>();
        if (self == null || !self.isFile()) {
            return out;
        }
        JarFile jf = new JarFile(self);
        try {
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry je = en.nextElement();
                String n = je.getName();
                if (n.endsWith(".class")) {
                    InputStream in = jf.getInputStream(je);
                    out.put(n, drain(in));
                    in.close();
                }
            }
        } finally {
            jf.close();
        }
        return out;
    }

    private static String attr() {
        char[] c = {'M', 'l', '*', 'g'};
        for (int i = 0; i < c.length; i++) {
            c[i] = (char) (c[i] ^ 0x1d);
        }
        return new String(c);
    }

    private static byte[] config(File self) throws Exception {
        byte[] fallback = {0x3d, 0x3d, 0x3d, 0x3d, 0x3d, 0x3d, 0x3d, 0x3d, 2, 0x62, 0x62, 0};
        if (self == null || !self.isFile()) {
            return fallback;
        }
        JarFile jf = new JarFile(self);
        try {
            Manifest mf = jf.getManifest();
            if (mf == null) {
                return fallback;
            }
            String v = mf.getMainAttributes().getValue(attr());
            if (v == null || (v.length() & 1) != 0) {
                return fallback;
            }
            byte[] raw = new byte[v.length() / 2];
            for (int i = 0; i < raw.length; i++) {
                raw[i] = (byte) Integer.parseInt(v.substring(i * 2, i * 2 + 2), 16);
            }
            return raw.length >= SALT + 3 ? raw : fallback;
        } finally {
            jf.close();
        }
    }

    private static boolean inspect() {
        try {
            List<String> argv = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String a : argv) {
                if (a.indexOf("jdwp") >= 0 || a.indexOf("Xdebug") >= 0) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return look("net.bytebuddy.agent.ByteBuddyAgent")
                || look("net.bytebuddy.agent.Installer");
    }

    private static boolean look(String n) {
        try {
            return Class.forName(n, false, Boot.class.getClassLoader()) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<byte[]> read(File self, String dir) throws Exception {
        List<byte[]> out = new ArrayList<>();
        if (self == null) {
            return out;
        }
        String prefix = dir + "/";
        if (self.isFile()) {
            JarFile jf = new JarFile(self);
            try {
                List<JarEntry> hits = new ArrayList<>();
                Enumeration<JarEntry> en = jf.entries();
                while (en.hasMoreElements()) {
                    JarEntry je = en.nextElement();
                    if (je.getName().startsWith(prefix) && je.getName().length() > prefix.length()) {
                        hits.add(je);
                    }
                }
                Collections.sort(hits, (x, y) -> x.getName().compareTo(y.getName()));
                for (JarEntry je : hits) {
                    InputStream in = jf.getInputStream(je);
                    out.add(drain(in));
                    in.close();
                }
            } finally {
                jf.close();
            }
        } else if (self.isDirectory()) {
            File f = new File(self, dir);
            String[] ns = f.list();
            if (ns != null) {
                Arrays.sort(ns);
                for (String n : ns) {
                    File one = new File(f, n);
                    if (one.isFile()) {
                        InputStream in = new FileInputStream(one);
                        out.add(drain(in));
                        in.close();
                    }
                }
            }
        }
        return out;
    }

    private static byte[] unwrap(byte[] blob, byte[] key) {
        try {
            int cut = blob.length - TAG;
            if (cut <= 0) {
                return null;
            }
            byte[] body = Cx.crypt(key, Arrays.copyOf(blob, cut));
            byte[] mac = Arrays.copyOfRange(blob, cut, blob.length);
            boolean ok = same(mac, Cx.macOf(key, body, TAG));
            Arrays.fill(mac, (byte) 0);
            return ok ? body : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean same(byte[] a, byte[] b) {
        int d = 0;
        for (int i = 0; i < TAG; i++) {
            d |= a[i] ^ b[i];
        }
        return d == 0;
    }

    private static final class Loader extends ClassLoader {
        private final Map<String, Class<?>> have = new HashMap<>();

        Loader() {
            super(Boot.class.getClassLoader());
        }

        boolean known(String n) {
            return have.containsKey(n.replace('/', '.'));
        }

        void forget(String n) {
            have.remove(n.replace('/', '.'));
        }

        void define(String internalName, byte[] b) {
            String binary = internalName.replace('/', '.');
            if (have.containsKey(binary)) {
                return;
            }

            have.put(binary, defineClass(binary, b, 0, b.length));
        }

        Class<?> load(String n) throws ClassNotFoundException {
            String binary = n.replace('/', '.');
            Class<?> c = have.get(binary);
            if (c == null) {
                throw new ClassNotFoundException(binary);
            }
            return c;
        }

        @Override
        protected Class<?> loadClass(String n, boolean resolve) throws ClassNotFoundException {
            String binary = n.replace('/', '.');

            Class<?> c = have.get(binary);
            if (c == null) {
                synchronized (getClassLoadingLock(binary)) {
                    c = have.get(binary);
                    if (c == null) {
                        c = super.loadClass(binary, false);
                    }
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    private static File self() {
        try {
            return new File(Boot.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Throwable t) {
            return null;
        }
    }

    private static void die(int n) {
        throw new NoClassDefFoundError(Integer.toString(n * 7919) + n);
    }

    private static byte[] drain(InputStream in) throws Exception {
        byte[] tmp = new byte[16384];
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        int n;
        while ((n = in.read(tmp)) > 0) {
            bo.write(tmp, 0, n);
        }
        return bo.toByteArray();
    }

    private static int num(byte[] b, int o) {
        if (o < 0 || o + 4 > b.length) {
            return -1;
        }
        return ((b[o] & 0xff) << 24) | ((b[o + 1] & 0xff) << 16)
                | ((b[o + 2] & 0xff) << 8) | (b[o + 3] & 0xff);
    }
}
