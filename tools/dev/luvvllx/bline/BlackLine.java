package dev.luvvllx.bline;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class BlackLine {

    private static final String LoaderSrc = "dev/luvvllx/runtime/Boot";
    private static final String BridgeSrc = "dev/luvvllx/runtime/Cx";
    private static final String RtSrc = "dev/luvvllx/runtime/Rt";

    private static String LOADER;
    private static String BRIDGE;
    private static String RT;
    private static String LoaderInner;

    private static final int TAG = 16;
    private static final int SALT = 8;
    private static final int STRIDE = 64;
    private static final int FILL = 4;

    // clipboard poison: BOMs, NUL runs, RLO, lone surrogate, overlong utf-8, ansi escapes
    private static final byte[] PREF = bytes(0xff, 0xfe, 0x00, 0x00, 0x00, 0x00,
            0xe2, 0x80, 0xae, 0xed, 0xa0, 0x80, 0xc0, 0x80, 0x00, 0x00,
            0x1b, 0x5b, 0x30, 0x3b, 0x33, 0x31, 0x6d, 0x07, 0x08, 0x08, 0x08,
            0xfe, 0xff, 0x00, 0x1a);
    private static final byte[] SUFF = bytes(0x00, 0x00, 0x00, 0x00,
            0xe2, 0x80, 0xab, 0xef, 0xbf, 0xbe, 0xf8, 0x3f, 0x3f,
            0x00, 0x0c, 0x00, 0x0d, 0x00, 0x0a, 0x04, 0x04, 0x04, 0x1a);
    private static final String A = "abcdefghijklmnopqrstuvwxyz";
    private static final String B = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    static final Random RND = new SecureRandom();

    private final Map<String, String> clsMap = new HashMap<>();
    private final Map<String, String> reverse = new HashMap<>();
    private final Map<String, Set<String>> frozen = new HashMap<>();
    private final Map<String, String> prefixOf = new HashMap<>();
    private final Map<String, String> memberMemo = new HashMap<>();
private final Set<String> taken = new HashSet<>();
    private final List<String> payload = new ArrayList<>();
    private final Set<String> payloadSet = new HashSet<>();
    private final Set<String> protect = new HashSet<>();
    private boolean modMode;
    private static final Map<String, byte[]> CURRENT = new HashMap<>();
    private ClassLoader probe;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: BlackLine <in.jar> <out.jar> [title] [vendor]");
            return;
        }
        String title = args.length > 2 ? args[2] : "blackLine";
        String vendor = args.length > 3 ? args[3] : "luvvllx";

        int guard = args.length > 4 ? Integer.parseInt(args[4]) : 0;
        BlackLine bl = new BlackLine();

        bl.runtimeNames();
        bl.run(args[0], args[1], title, vendor, guard);
    }

    private void run(String inJar, String outJar, String title, String vendor, int guard) throws Exception {
        Map<String, byte[]> raw = readJar(inJar);
        probe = new URLClassLoader(new URL[]{new File(inJar).toURI().toURL()},
                ClassLoader.getSystemClassLoader());

        for (String n : raw.keySet()) {
            if (n.endsWith(".class")) {
                String cn = n.substring(0, n.length() - 6);
                if (!isInfrastructure(cn)) {
                    payload.add(cn);
                }
            }
        }
if (payload.isEmpty()) {
            throw new IllegalStateException("no game classes in " + inJar);
        }
        Collections.sort(payload);
        payloadSet.addAll(payload);

        modMode = isMod(raw);
        if (modMode) {
            collectProtect(raw);
        }

        for (String cn : payload) {
            CURRENT.put(cn, raw.get(cn + ".class"));
        }
        for (String cn : payload) {
            prefixOf.put(cn, HOME);
            frozen.put(cn, frozenMembers(probe, cn));
        }
        for (String cn : payload) {
            if (protect.contains(cn)) {
                clsMap.put(cn, cn);
                reverse.put(cn, cn);
            } else {
                String v = prefixOf.get(cn) + "/" + rand(3 + RND.nextInt(3), A);
                clsMap.put(cn, v);
                reverse.put(v, cn);
            }
        }

        String entryOld = findEntry(probe);
        if (entryOld == null && !modMode) {
            throw new IllegalStateException("no main(String[]) in input");
        }

        Remapper remap = new Remapper() {
            @Override
            public String map(String internalName) {
                String m = clsMap.get(internalName);
                return m == null ? internalName : m;
            }

            @Override
            public String mapFieldName(String owner, String name, String descriptor) {
                if (!payloadSet.contains(owner) || protect.contains(owner) || frozen.get(owner).contains("F:" + name)) {
                    return name;
                }
                return memo("F", owner, name);
            }

            @Override
            public String mapMethodName(String owner, String name, String descriptor) {
                if (!payloadSet.contains(owner) || protect.contains(owner) || name.startsWith("<")
                        || frozen.get(owner).contains("M:" + name)) {
                    return name;
                }
                return memo("M", owner, name);
            }
        };

        Hierarchy hier = new Hierarchy(reverse, probe);

        String entryNew = entryOld == null ? null : clsMap.get(entryOld);
        String entryMethod = entryOld == null ? null : remap.mapMethodName(entryOld, "main", "([Ljava/lang/String;)V");

        Map<String, byte[]> out = new LinkedHashMap<>();
        for (String cn : payload) {
            byte[] body = protect.contains(cn)
                    ? packPlain(raw.get(cn + ".class"), cn, remap, hier)
                    : pack(raw.get(cn + ".class"), cn, remap, hier);
            out.put(clsMap.get(cn), body);
        }
        int decoys = 0;
        for (int i = 0; i < 4; i++) {
            String fake = newPackage() + "/" + rand(3 + RND.nextInt(3), A);
            reverse.put(fake, fake);
            out.put(fake, decoy(fake));
            decoys++;
        }

        List<String> slots = new ArrayList<>(out.keySet());
        Collections.shuffle(slots, RND);
        Map<String, byte[]> shuffled = new LinkedHashMap<>();
        for (String k : slots) {
            shuffled.put(k, out.get(k));
        }

        if (modMode) {
            packModJar(outJar, shuffled, raw, title, vendor);
            System.out.println("blackLine: mod mode, " + payload.size() + " classes ("
                    + protect.size() + " kept) + " + decoys + " decoys");
        } else {
            packJar(outJar, shuffled, entryNew, entryMethod, title, vendor, guard);
            System.out.println("blackLine: " + payload.size() + " classes sealed + " + decoys
                    + " decoys, entry " + entryNew + "#" + entryMethod);
        }
    }

    private boolean isMod(Map<String, byte[]> raw) {
        if (raw.containsKey("fabric.mod.json") || raw.containsKey("quilt.mod.json")
                || raw.containsKey("mcmod.info") || raw.containsKey("META-INF/mods.toml")
                || raw.containsKey("META-INF/neoforge.mods.toml") || raw.containsKey("architectury.common.json")) {
            return true;
        }
        for (String n : raw.keySet()) {
            if (n.endsWith(".mixins.json") || n.endsWith(".accesswidener")) {
                return true;
            }
        }
        return false;
    }

    private void collectProtect(Map<String, byte[]> raw) throws Exception {
        for (Map.Entry<String, byte[]> e : raw.entrySet()) {
            String n = e.getKey();
            if (n.endsWith(".class") || n.startsWith("META-INF/") || !isText(e.getValue())) {
                continue;
            }
            String text = new String(e.getValue(), StandardCharsets.UTF_8);
            for (String cn : payloadSet) {
                if (text.contains(cn) || text.contains(cn.replace('/', '.'))) {
                    protect.add(cn);
                }
            }
            if (n.endsWith(".json") && text.contains("\"package\"")) {
                protectMixins(text);
            }
        }
    }

    private void protectMixins(String text) {
        java.util.regex.Matcher pkg = java.util.regex.Pattern
                .compile("\"package\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
        if (!pkg.find()) {
            return;
        }
        String base = pkg.group(1).replace('.', '/');
        java.util.regex.Matcher arr = java.util.regex.Pattern
                .compile("\"(?:mixins|client|server)\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(text);
        while (arr.find()) {
            for (String raw : arr.group(1).split(",")) {
                String entry = raw.replaceAll("[\\s\"]", "");
                if (entry.isEmpty()) {
                    continue;
                }
                String internal = base + "/" + entry.replace('.', '/');
                if (payloadSet.contains(internal)) {
                    protect.add(internal);
                }
            }
        }
        java.util.regex.Matcher plug = java.util.regex.Pattern
                .compile("\"plugin\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
        if (plug.find()) {
            String internal = plug.group(1).replace('.', '/');
            if (payloadSet.contains(internal)) {
                protect.add(internal);
            }
        }
    }

    private static boolean isText(byte[] b) {
        if (b.length == 0) {
            return false;
        }
        int bad = 0;
        for (byte x : b) {
            int c = x & 0xff;
            if (c == 0 || (c < 0x09) || (c > 0x0d && c < 0x20)) {
                bad++;
            }
        }
        return bad * 20 < b.length;
    }

    private String memo(String kind, String owner, String old) {
        String k = kind + ' ' + owner + ' ' + old;
        String v = memberMemo.get(k);
        if (v == null) {
            v = uniqueName();
            memberMemo.put(k, v);
        }
        return v;
    }

    private byte[] pack(byte[] bytes, String owner, Remapper remap, Hierarchy hier) {
        ClassReader cr = new ClassReader(bytes);
        Collector collector = new Collector();
        cr.accept(collector, 0);
        Pool pool = new Pool(collector.literals);

        ClassNode tree = new ClassNode();
        cr.accept(new ClassRemapper(new Widen(tree), remap), 0);

        String poolName = pool.size == 0 ? null
                : "z" + Integer.toHexString(RND.nextInt(1 << 24));

        new Shred(remap.map(owner), poolName, pool.mask, pool.encoded, pool.key, RT, reverse, probe, pool.slot)
                .run(tree);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String a, String b) {
                return hier.common(a, b);
            }
        };
tree.accept(cw);
        return cw.toByteArray();
    }

    private byte[] packPlain(byte[] bytes, String owner, Remapper remap, Hierarchy hier) {
        ClassNode tree = new ClassNode();
        new ClassReader(bytes).accept(new ClassRemapper(new Widen(tree), remap), 0);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String a, String b) {
                return hier.common(a, b);
            }
        };
        tree.accept(cw);
        return cw.toByteArray();
    }

    private byte[] decoy(String owner) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        for (int i = 0; i < 5; i++) {
            MethodVisitor m = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    "m" + uniqueName(), "(I)J", null, null);
            m.visitCode();
            m.visitVarInsn(Opcodes.ILOAD, 0);
            m.visitInsn(Opcodes.I2L);
            m.visitLdcInsn(RND.nextLong());
            m.visitInsn(Opcodes.LXOR);
            m.visitMethodInsn(Opcodes.INVOKESTATIC, RT, "p", "()I", false);
            m.visitInsn(Opcodes.I2L);
            m.visitInsn(Opcodes.LXOR);
            m.visitInsn(Opcodes.LRETURN);
            m.visitMaxs(4, 1);
            m.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    static final class Collector extends ClassVisitor {
        final List<String> literals = new ArrayList<>();

        Collector() {
            super(Opcodes.ASM9);
        }

        @Override
        public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitLdcInsn(Object cst) {
                    if (cst instanceof String && !literals.contains(cst)) {
                        literals.add((String) cst);
                    }
                }
            };
        }
    }

    static final class Pool {
        final Map<String, Integer> slot = new HashMap<>();
        final String[] encoded;
        final int[] key;
        final int mask;
        final int size;

        Pool(List<String> literals) {
            List<String> shuffled = new ArrayList<>(literals);
            Collections.shuffle(shuffled, RND);
            this.size = shuffled.size();
            this.mask = RND.nextInt();
            this.key = new int[size];
            this.encoded = new String[size];
            for (int i = 0; i < size; i++) {
                slot.put(shuffled.get(i), i);
                key[i] = RND.nextInt();
                encoded[i] = dev.luvvllx.runtime.Cx.encode(shuffled.get(i), key[i]);
            }
        }
    }

    static final class Widen extends ClassVisitor {
        Widen(ClassVisitor cv) {
            super(Opcodes.ASM9, cv);
        }

        @Override
        public void visit(int v, int access, String name, String sig, String sup, String[] itf) {
            super.visit(v, publicise(access), name, sig, sup, itf);
        }

        @Override
        public void visitSource(String source, String debug) {
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String sig, Object val) {
            if (name.equals("serialVersionUID") || name.startsWith("this$")) {
                return super.visitField(access, name, desc, sig, val);
            }
            return super.visitField(publicise(access), name, desc, sig, val);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
            boolean entry = name.equals("main") && desc.equals("([Ljava/lang/String;)V");
            return super.visitMethod(publicise(access) | (entry ? Opcodes.ACC_STATIC : 0),
                    name, desc, sig, ex);
        }
    }

    static int publicise(int access) {
        return (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
    }
    static void push(MethodVisitor mv, int v) {
        if (v >= -1 && v <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + v);
        } else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, v);
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, v);
        } else {
            mv.visitLdcInsn(v);
        }
    }

    static void push(InsnList list, int v) {
        if (v >= -1 && v <= 5) {
            list.add(new InsnNode(Opcodes.ICONST_0 + v));
        } else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            list.add(new IntInsnNode(Opcodes.BIPUSH, v));
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            list.add(new IntInsnNode(Opcodes.SIPUSH, v));
        } else {
            list.add(new LdcInsnNode(v));
        }
    }

    static String name() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append((char) ('a' + RND.nextInt(26)));
        }
        return sb.toString();
    }

    static final class Hierarchy {
        private final Map<String, String> reverse;
        private final ClassLoader probe;

        Hierarchy(Map<String, String> reverse, ClassLoader probe) {
            this.reverse = reverse;
            this.probe = probe;
        }

        String common(String a, String b) {
            if (a.equals(b)) {
                return a;
            }
            if (a.charAt(0) == '[' || b.charAt(0) == '[') {
                return commonArray(a, b);
            }
            try {

                Class<?> ca = Class.forName(reverse.getOrDefault(a, a).replace('/', '.'), false, probe);
                Class<?> cb = Class.forName(reverse.getOrDefault(b, b).replace('/', '.'), false, probe);
                if (ca.isAssignableFrom(cb)) {
                    return a;
                }
                if (cb.isAssignableFrom(ca)) {
                    return b;
                }
                if (ca.isInterface() || cb.isInterface()) {
                    return "java/lang/Object";
                }
                Class<?> c = ca;
                do {
                    c = c.getSuperclass();
                } while (c != null && !c.isAssignableFrom(cb));
                if (c == null) {
                    return "java/lang/Object";
                }
                return forward(c.getName().replace('.', '/'));
            } catch (Throwable t) {
                return "java/lang/Object";
            }
        }

        private String forward(String original) {
            for (Map.Entry<String, String> e : reverse.entrySet()) {
                if (e.getValue().equals(original)) {
                    return e.getKey();
                }
            }
            return original;
        }

        private String commonArray(String a, String b) {
            if (a.equals(b)) {
                return a;
            }
            String ea = element(a);
            String eb = element(b);
            if (ea.equals(eb)) {
                return a;
            }

            if (isPrimitive(ea) || isPrimitive(eb)) {
                return "java/lang/Object";
            }
            if (ea.equals("java/lang/Object") || eb.equals("java/lang/Object")) {
                return "java/lang/Object";
            }
            return "[L" + common(ea, eb) + ";";
        }

        private static String element(String arrayDescriptor) {
            String t = arrayDescriptor.substring(arrayDescriptor.lastIndexOf('[') + 1);
            if (t.length() > 2 && t.charAt(0) == 'L' && t.charAt(t.length() - 1) == ';') {
                return t.substring(1, t.length() - 1);
            }
            return t;
        }

        private static boolean isPrimitive(String t) {
            if (t.isEmpty() || t.charAt(0) == '[' || t.charAt(0) == 'L') {
                return false;
            }
            return "ZCBSIFJDV".indexOf(t.charAt(0)) >= 0;
        }
    }

    private static String rand(int len, String alphabet) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(alphabet.charAt(RND.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private String uniqueName() {
        for (int i = 0; i < 200; i++) {
            String n = rand(1 + RND.nextInt(4), RND.nextInt(4) == 0 ? B : A);
            if (taken.add(n)) {
                return n;
            }
        }
        String n = "zz" + Integer.toHexString(RND.nextInt());
        taken.add(n);
        return n;
    }

    private String newPackage() {
        return rand(2 + RND.nextInt(2), A) + "/" + rand(2 + RND.nextInt(2), A);
    }

    private static Set<String> frozenMembers(ClassLoader probe, String cn) {
        Set<String> out = new HashSet<>();
        try {
            Class<?> c = Class.forName(cn, false, probe);
            Set<String> mine = new HashSet<>();
            for (Method m : c.getDeclaredMethods()) {
                mine.add(m.getName());
            }
            for (Field f : c.getDeclaredFields()) {
                mine.add(f.getName());
            }

            Set<Class<?>> seen = new HashSet<>();
            Deque<Class<?>> q = new ArrayDeque<>();
            push(q, c.getSuperclass());
            for (Class<?> i : c.getInterfaces()) {
                push(q, i);
            }
            while (!q.isEmpty()) {
                Class<?> k = q.poll();
                if (!seen.add(k)) {
                    continue;
                }
                for (Method m : k.getDeclaredMethods()) {
                    out.add("M:" + m.getName());
                }
                for (Field f : k.getDeclaredFields()) {
                    out.add("F:" + f.getName());
                }
                push(q, k.getSuperclass());
                for (Class<?> i : k.getInterfaces()) {
                    push(q, i);
                }
            }

            for (String n : selfReferenced(cn, CURRENT)) {
                if (!mine.contains(n)) {
                    out.add("M:" + n);
                    out.add("F:" + n);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static void push(Deque<Class<?>> q, Class<?> c) {
        if (c != null) {
            q.add(c);
        }
    }

    private static Set<String> selfReferenced(String cn, Map<String, byte[]> current) {
        Set<String> out = new LinkedHashSet<>();
        byte[] bytes = current.get(cn);
        if (bytes == null) {
            return out;
        }
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int op, String owner, String n, String d, boolean itf) {
                        if (cn.equals(owner)) {
                            out.add(n);
                        }
                    }

                    @Override
                    public void visitFieldInsn(int op, String owner, String n, String d) {
                        if (cn.equals(owner)) {
                            out.add(n);
                        }
                    }
                };
            }
        }, 0);
        return out;
    }

    private String findEntry(ClassLoader probe) {
        for (String cn : payload) {
            try {
                Class<?> c = Class.forName(cn, false, probe);
                c.getDeclaredMethod("main", String[].class);
                return cn;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean isInfrastructure(String cn) {
        return cn.equals("dev/luvvllx/runtime/B1") || cn.equals("dev/luvvllx/runtime/B2") || cn.equals("dev/luvvllx/runtime/Ann");
    }

    private void runtimeNames() {
        String home = newPackage();
        LOADER = home + "/" + rand(4, A);
        BRIDGE = home + "/" + rand(4, A);
        RT = home + "/" + rand(4, A);
        LoaderInner = LOADER + "$" + rand(3, A);
        HOME = home;
    }

    private static String HOME;

    private Map<String, byte[]> runtime() throws Exception {
        Map<String, String> names = new HashMap<>();
        names.put(LoaderSrc, LOADER);
        names.put(LoaderSrc + "$Loader", LoaderInner);
        names.put(BridgeSrc, BRIDGE);
        names.put(RtSrc, RT);

        Remapper rn = new Remapper() {
            @Override
            public String map(String internalName) {
                String m = names.get(internalName);
                return m == null ? internalName : m;
            }
        };

        Map<String, byte[]> out = new LinkedHashMap<>();
        for (String src : new String[]{LoaderSrc, LoaderSrc + "$Loader", BridgeSrc, RtSrc}) {
            ClassWriter cw = frames();

            ClassVisitor drop = new ClassVisitor(Opcodes.ASM9, new ClassRemapper(cw, rn)) {
                @Override
                public void visitSource(String source, String debug) {
                }
            };
            new ClassReader(resource(src + ".class")).accept(drop, 0);
            out.put(names.get(src) + ".class", cw.toByteArray());
        }

        String forge = BRIDGE.substring(0, BRIDGE.lastIndexOf('/')) + "/fg";
        out.put(forge + ".class", renameForge(resource("dev/luvvllx/runtime/Forge.class"), forge));
        return out;
    }

    private void packJar(String outJar, Map<String, byte[]> classes, String entry, String entryMethod,
                         String title, String vendor, int guard) throws Exception {
        byte[] salt = new byte[SALT];
        RND.nextBytes(salt);
        String folder = "jullyChild";

        Fragments fr = fragments(salt, guard);
        Map<String, byte[]> rt = runtime();
        for (Map.Entry<String, byte[]> e : fr.classes.entrySet()) {
            rt.putIfAbsent(e.getKey(), e.getValue());
        }

        ByteArrayOutputStream hb = new ByteArrayOutputStream();
        hb.write(numBytes(entry.length()));
        hb.write(entry.getBytes(StandardCharsets.UTF_8));
        hb.write(numBytes(entryMethod.length()));
        hb.write(entryMethod.getBytes(StandardCharsets.UTF_8));

        List<String> names = new ArrayList<>(classes.keySet());
        List<byte[]> frags = new ArrayList<>();
        ByteArrayOutputStream ib = new ByteArrayOutputStream();
        ib.write(numBytes(names.size()));
        for (String cn : names) {
            byte[] whole = classes.get(cn);
            byte[] nameBytes = cn.getBytes(StandardCharsets.UTF_8);
            int parts = 2 + RND.nextInt(Math.min(6, whole.length / 700 + 2));
            int chunk = (whole.length + parts - 1) / parts;
            ib.write(numBytes(nameBytes.length));
            ib.write(nameBytes);
            ib.write(numBytes(parts));
            ib.write(numBytes(whole.length));
            for (int off = 0; off < whole.length; off += chunk) {
                int len = Math.min(chunk, whole.length - off);
                frags.add(Arrays.copyOfRange(whole, off, off + len));
            }
        }

        List<byte[]> chainBodies = new ArrayList<>();
        chainBodies.add(hb.toByteArray());
        chainBodies.add(ib.toByteArray());
        chainBodies.addAll(frags);

        for (int i = 0; i < 6; i++) {
            byte[] junk = new byte[64 + RND.nextInt(400)];
            RND.nextBytes(junk);
            chainBodies.add(junk);
        }

        Manifest mf = new Manifest();
        Attributes at = mf.getMainAttributes();
        at.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        at.putValue("Main-Class", LOADER);
        at.putValue("Implementation-Title", title);
        at.putValue("Implementation-Vendor", vendor);
        at.putValue("Implementation-Version", "1.0.0");
        at.putValue("Built-By", vendor);

        byte[] cfg = new byte[SALT + 2 + folder.length()];
        System.arraycopy(salt, 0, cfg, 0, SALT);
        cfg[SALT] = (byte) folder.length();
        System.arraycopy(folder.getBytes(StandardCharsets.UTF_8), 0, cfg, SALT + 1, folder.length());
        cfg[cfg.length - 1] = (byte) guard;
        at.putValue(attrName(), hex(cfg));
        Arrays.fill(cfg, (byte) 0);

        Object[] inputs = new Object[]{fr.prog, fr.rmap, fr.seeds};
        byte[] root = dev.luvvllx.runtime.Cx.self(salt, guard, inputs, rt);

        Files.createDirectories(Paths.get(outJar).toAbsolutePath().getParent());
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(Paths.get(outJar)))) {
            jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            mf.write(jos);
            jos.closeEntry();
            for (Map.Entry<String, byte[]> e : rt.entrySet()) {
                put(jos, e.getKey(), e.getValue());
            }

            byte[] ch = dev.luvvllx.runtime.Cx.chain0(root);
            for (int i = 0; i < chainBodies.size(); i++) {
                byte[] key = fr.slotKey(i, ch);
                byte[] body = chainBodies.get(i);
                ch = dev.luvvllx.runtime.Cx.advance(ch, body);
                put(jos, folder + "/" + String.format("%04d.jullyCrypto", i),
                        armor(seal(key, body, TAG)));
            }
        }
        System.out.println("blackLine: wrote " + outJar + " (" + chainBodies.size()
                + " chained files, " + names.size() + " classes, "
                + (Files.size(Paths.get(outJar)) / 1024) + " KB)");
    }

    private static ClassWriter frames() {
        return new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String a, String b) {
                return "java/lang/Object";
            }
        };
    }

    private static byte[] renameForge(byte[] bytes, final String owner) {
        final String oldName = "dev/luvvllx/runtime/Forge";
        Remapper r = new Remapper() {
            @Override
            public String map(String internalName) {
                return internalName.equals(oldName) ? owner : internalName;
            }
        };
        ClassWriter cw = new ClassWriter(0);
        new ClassReader(bytes).accept(new org.objectweb.asm.commons.ClassRemapper(cw, r), 0);
        return cw.toByteArray();
    }

    private static final class Fragments {
        final Map<String, byte[]> classes;
        final int[] prog;
        final int[] rmap;
        final int[] seeds;
        final byte[] salt;
        final int guard;

        Fragments(Map<String, byte[]> classes, int[] prog, int[] rmap, int[] seeds,
                  byte[] salt, int guard) {
            this.classes = classes;
            this.prog = prog;
            this.rmap = rmap;
            this.seeds = seeds;
            this.salt = salt;
            this.guard = guard;
        }

        byte[] slotKey(int i, byte[] chain) {
            return dev.luvvllx.runtime.Forge.derive(prog.clone(), rmap.clone(), seeds, salt, guard, i, chain);
        }
    }

    private static String attrName() {
        char[] c = {'M', 'l', '*', 'g'};
        for (int i = 0; i < c.length; i++) {
            c[i] = (char) (c[i] ^ 0x1d);
        }
        return new String(c);
    }

    private Fragments fragments(byte[] salt, int guard) throws Exception {
        int n = dev.luvvllx.runtime.Cx.parts();
        String[] names = new String[n];
        int[] parts = new int[n];
        Map<String, byte[]> out = new LinkedHashMap<>();

        for (int i = 0; i < n; i++) {
            names[i] = newPackage() + "/" + rand(3 + RND.nextInt(2), A);
            parts[i] = RND.nextInt();
            out.put(names[i] + ".class", seed(names[i], parts[i]));
        }

        String table = dev.luvvllx.runtime.Cx.table(salt);
        KeyGen gen = new KeyGen(new Random(RND.nextLong()));
        int[] prog = gen.build(parts);
        int[] rmap = gen.regmap();

        out.put(table + ".class", table(table, names, prog, rmap));

        return new Fragments(out, prog, rmap, parts, salt, guard);
    }

    private byte[] seed(String owner, int value) {
        ClassWriter cw = frames();
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, owner, null, "java/lang/Object", null);
        MethodVisitor m = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "a", "()I", null, null);
        m.visitCode();
        push(m, value);
        m.visitInsn(Opcodes.IRETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private byte[] table(String owner, String[] names, int[] prog, int[] rmap) {
        ClassWriter cw = frames();
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, owner, null, "java/lang/Object", null);

        MethodVisitor m = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "a", "()[Ljava/lang/String;", null, null);
        m.visitCode();
        push(m, names.length);
        m.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
        for (int i = 0; i < names.length; i++) {
            m.visitInsn(Opcodes.DUP);
            push(m, i);
            m.visitLdcInsn(names[i]);
            m.visitInsn(Opcodes.AASTORE);
        }
        m.visitInsn(Opcodes.ARETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();

        emitInts(cw, owner, "p", prog);
        emitInts(cw, owner, "g", rmap);
        cw.visitEnd();
        return cw.toByteArray();
    }

    private void emitInts(ClassWriter cw, String owner, String name, int[] v) {
        MethodVisitor m = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name, "()[I", null, null);
        m.visitCode();
        push(m, v.length);
        m.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
        for (int i = 0; i < v.length; i++) {
            m.visitInsn(Opcodes.DUP);
            push(m, i);
            push(m, v[i]);
            m.visitInsn(Opcodes.IASTORE);
        }
        m.visitInsn(Opcodes.ARETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
    }

private void put(JarOutputStream jos, String name, byte[] body) throws IOException {
        JarEntry e = new JarEntry(name);
        e.setTime(0L);
        jos.putNextEntry(e);
        jos.write(body);
        jos.closeEntry();
    }

    private void packModJar(String outJar, Map<String, byte[]> classes, Map<String, byte[]> raw,
                            String title, String vendor) throws Exception {
        Map<String, byte[]> rt = runtime();
        Files.createDirectories(Paths.get(outJar).toAbsolutePath().getParent());
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Paths.get(outJar)))) {
            for (Map.Entry<String, byte[]> e : raw.entrySet()) {
                String n = e.getKey();
                if (n.endsWith(".class") || n.equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                    continue;
                }
                if (n.startsWith("META-INF/") && (n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA"))) {
                    continue;
                }
                putZip(zos, n, e.getValue());
            }

            byte[] mf = raw.get("META-INF/MANIFEST.MF");
            if (mf == null) {
                Manifest m = new Manifest();
                Attributes at = m.getMainAttributes();
                at.put(Attributes.Name.MANIFEST_VERSION, "1.0");
                at.putValue("Implementation-Title", title);
                at.putValue("Implementation-Vendor", vendor);
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                m.write(bo);
                mf = bo.toByteArray();
            }
            putZip(zos, "META-INF/MANIFEST.MF", mf);

            for (Map.Entry<String, byte[]> e : raw.entrySet()) {
                String n = e.getKey();
                if (!n.endsWith(".class")) {
                    continue;
                }
                String cn = n.substring(0, n.length() - 6);
                if (isInfrastructure(cn) || !payloadSet.contains(cn)) {
                    putZip(zos, n, e.getValue());
                }
            }

            byte[] cx = rt.get(BRIDGE + ".class");
            if (cx != null) {
                putZip(zos, BRIDGE + ".class", cx);
            }
            byte[] rtCls = rt.get(RT + ".class");
            if (rtCls != null) {
                putZip(zos, RT + ".class", rtCls);
            }

            for (Map.Entry<String, byte[]> e : classes.entrySet()) {
                putZip(zos, e.getKey() + ".class", e.getValue());
            }
        }
    }

    private void putZip(ZipOutputStream zos, String name, byte[] body) throws IOException {
        ZipEntry e = new ZipEntry(name);
        e.setTime(0L);
        zos.putNextEntry(e);
        zos.write(body);
        zos.closeEntry();
    }

    private static byte[] resource(String path) throws Exception {
        URL u = BlackLine.class.getClassLoader().getResource(path);
        if (u == null) {
            throw new IllegalStateException("missing compiled resource: " + path);
        }
        try (InputStream in = new FileInputStream(new File(u.toURI()))) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] t = new byte[8192];
            int n;
            while ((n = in.read(t)) > 0) {
                bo.write(t, 0, n);
            }
            return bo.toByteArray();
        }
    }

    private static byte[] armor(byte[] sealed) {
        int n=sealed.length, blocks=(n+STRIDE-1)/STRIDE;
        byte[] fill=new byte[blocks*FILL];
        RND.nextBytes(fill);
        byte[] out=new byte[PREF.length+n+blocks*FILL+SUFF.length];
        int o=0;
        System.arraycopy(PREF,0,out,o,PREF.length);
        o+=PREF.length;
        for(int done=0,fi=0;done<n;done+=STRIDE,fi+=FILL){
            int len=Math.min(STRIDE,n-done);
            System.arraycopy(sealed,done,out,o,len);
            o+=len;
            System.arraycopy(fill,fi,out,o,FILL);
            o+=FILL;
        }
        System.arraycopy(SUFF,0,out,o,SUFF.length);
        Arrays.fill(fill,(byte)0);
        return out;
    }

    static byte[] bytes(int... v) {
        byte[] b = new byte[v.length];
        for (int i = 0; i < v.length; i++) {
            b[i] = (byte) v[i];
        }
        return b;
    }

    private static byte[] seal(byte[] key, byte[] body, int tag) throws Exception {
        byte[] mac = dev.luvvllx.runtime.Cx.macOf(key, body, tag);
        byte[] crypt = dev.luvvllx.runtime.Cx.crypt(key, body);
        byte[] out = new byte[crypt.length + tag];
        System.arraycopy(crypt, 0, out, 0, crypt.length);
        System.arraycopy(mac, 0, out, crypt.length, tag);
        Arrays.fill(crypt, (byte) 0);
        return out;
    }

    private static byte[] numBytes(int v) {
        return new byte[]{(byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v};
    }

    private static void putInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >>> 24);
        b[off + 1] = (byte) (v >>> 16);
        b[off + 2] = (byte) (v >>> 8);
        b[off + 3] = (byte) v;
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(Character.forDigit((x >> 4) & 0xf, 16));
            sb.append(Character.forDigit(x & 0xf, 16));
        }
        return sb.toString();
    }

    private static Map<String, byte[]> readJar(String path) throws IOException {
        Map<String, byte[]> out = new LinkedHashMap<>();
        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(path))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] t = new byte[8192];
                int n;
                while ((n = zin.read(t)) > 0) {
                    bo.write(t, 0, n);
                }
                out.put(e.getName(), bo.toByteArray());
            }
        }
        return out;
    }
}
