package dev.luvvllx.bline;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.*;

final class Shred {

    private final String owner;
    private final String poolName;
    private final int mask;
    private final String[] encoded;
    private final int[] keys;
    private final String rt;
    private final Map<String, String> reverse;
    private final ClassLoader probe;
    private final Map<String, Boolean> access = new HashMap<>();
    private final Map<Object, Integer> slotOf = new HashMap<>();
    private final Random rnd;

    Shred(String owner, String poolName, int mask, String[] encoded, int[] keys, String rt,
          Map<String, String> reverse, ClassLoader probe, Map<String, Integer> slots) {
        this.owner = owner;
        this.poolName = poolName;
        this.mask = mask;
        this.encoded = encoded;
        this.keys = keys;
        this.rt = rt;
        this.reverse = reverse;
        this.probe = probe;
        this.slotOf.putAll(slots);
        this.rnd = new Random();
    }

    void run(ClassNode cn) {
        for (MethodNode mn : cn.methods) {
            if ((mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                continue;
            }
            strip(mn);
            literals(mn);
            guard(mn);
            calls(mn);
        }
        for (MethodNode mn : cn.methods) {
            if ((mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                continue;
            }
            if ((mn.access & Opcodes.ACC_STATIC) == 0) {

                continue;
            }
            if (mn.name.equals("<init>") || mn.name.equals("<clinit>")) {
                continue;
            }
            MethodNode flat = flatten(mn);
            if (flat != null) {
                int at = cn.methods.indexOf(mn);
                cn.methods.set(at, flat);
            }
        }
        poolField(cn);
    }

    private void literals(MethodNode mn) {
        if (poolName == null) {
            return;
        }
        for (AbstractInsnNode in = mn.instructions.getFirst(); in != null; ) {
            AbstractInsnNode next = in.getNext();
            if (in instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) in;
                Integer at = slotOf.get(ldc.cst);
                if (at != null) {
                    InsnList rep = new InsnList();
                    rep.add(new FieldInsnNode(Opcodes.GETSTATIC, owner, poolName, "[Ljava/lang/String;"));
                    BlackLine.push(rep, at ^ mask);
                    BlackLine.push(rep, mask);
                    rep.add(new MethodInsnNode(Opcodes.INVOKESTATIC, rt, "x",
                            "([Ljava/lang/String;II)Ljava/lang/String;", false));
                    mn.instructions.insertBefore(ldc, rep);
                    mn.instructions.remove(ldc);
                }
            }
            in = next;
        }
    }

    private static void strip(MethodNode mn) {
        mn.localVariables.clear();
        mn.visibleLocalVariableAnnotations = null;
        mn.invisibleLocalVariableAnnotations = null;
        if (mn.parameters != null) {
            mn.parameters.clear();
        }
    }

    private void poolField(ClassNode cn) {
        if (poolName == null || encoded.length == 0) {
            return;
        }
        cn.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, poolName,
                "[Ljava/lang/String;", null, null));

        InsnList init = new InsnList();
        BlackLine.push(init, encoded.length);
        init.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
        for (int i = 0; i < encoded.length; i++) {
            init.add(new InsnNode(Opcodes.DUP));
            BlackLine.push(init, i);
            init.add(new LdcInsnNode(encoded[i]));
            BlackLine.push(init, keys[i]);
            init.add(new MethodInsnNode(Opcodes.INVOKESTATIC, rt, "a",
                    "(Ljava/lang/String;I)Ljava/lang/String;", false));
            init.add(new InsnNode(Opcodes.AASTORE));
        }
        init.add(new FieldInsnNode(Opcodes.PUTSTATIC, owner, poolName, "[Ljava/lang/String;"));

        MethodNode clinit = null;
        for (MethodNode m : cn.methods) {
            if (m.name.equals("<clinit>")) {
                clinit = m;
            }
        }
        if (clinit == null) {
            clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions = init;
            clinit.maxStack = 4;
            clinit.maxLocals = 0;
            cn.methods.add(clinit);
        } else {
            clinit.instructions.insert(init);
            clinit.tryCatchBlocks.clear();
        }
    }

    private void calls(MethodNode mn) {
        for (AbstractInsnNode in = mn.instructions.getFirst(); in != null; ) {
            AbstractInsnNode next = in.getNext();
            if (in instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) in;
                InvokeDynamicInsnNode indy = indy(mi);
                if (indy != null) {
                    mn.instructions.set(in, indy);
                    Type back = widenBack(mi);
                    if (back != null) {
                        mn.instructions.insert(indy, new TypeInsnNode(Opcodes.CHECKCAST, back.getInternalName()));
                    }
                }
            }
            in = next;
        }
    }

    private InvokeDynamicInsnNode indy(MethodInsnNode mi) {
        int op = mi.getOpcode();
        if (op != Opcodes.INVOKESTATIC && op != Opcodes.INVOKEVIRTUAL && op != Opcodes.INVOKEINTERFACE) {
            return null;
        }
        if (mi.owner.equals(rt) || mi.owner.equals("java/lang/Math") && mi.name.equals("abs")) {
            return null;
        }
        if (mi.name.equals("<clinit>") || mi.name.startsWith("lambda$")) {
            return null;
        }
        if (!visible(mi.owner, mi.name, mi.desc, op == Opcodes.INVOKESTATIC)) {
            return null;
        }

        int ko = rnd.nextInt();
        int kn = rnd.nextInt();
        int kd = rnd.nextInt();
        InvokeDynamicInsnNode out = new InvokeDynamicInsnNode(
                BlackLine.name(), site(mi.desc, op),
                new Handle(Opcodes.H_INVOKESTATIC, rt, "b",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                                + "Ljava/lang/invoke/MethodType;Ljava/lang/String;I"
                                + "Ljava/lang/String;ILjava/lang/String;II)"
                                + "Ljava/lang/invoke/CallSite;", false),
                new Object[]{
                        enc(mi.owner, ko), ko,
                        enc(mi.name, kn), kn,
                        enc(mi.desc, kd), kd,
                        op == Opcodes.INVOKESTATIC ? 0 : 1
                });
        return out;
    }

    private static String site(String desc, int op) {
        int cut = desc.indexOf(')');
        String params = desc.substring(1, cut);
        String ret = desc.substring(cut + 1);
        if (op != Opcodes.INVOKESTATIC) {
            params = "Ljava/lang/Object;" + params;
        }
        if (ret.startsWith("L") || ret.startsWith("[")) {
            ret = "Ljava/lang/Object;";
        }
        return "(" + params + ")" + ret;
    }

    private static Type widenBack(MethodInsnNode mi) {
        int cut = mi.desc.indexOf(')');
        String ret = mi.desc.substring(cut + 1);
        return ret.startsWith("L") || ret.startsWith("[") ? Type.getType(ret) : null;
    }

    private String enc(String plain, int key) {
        return dev.luvvllx.runtime.Cx.encode(plain, key);
    }

    private boolean visible(String owner, String name, String desc, boolean isStatic) {
        String key = owner + '.' + name + desc;
        Boolean cached = access.get(key);
        if (cached != null) {
            return cached;
        }
        boolean ok = false;
        try {
            String orig = reverse.getOrDefault(owner, owner);
            Deque<Class<?>> queue = new ArrayDeque<>();
            Set<Class<?>> seen = new HashSet<>();
            queue.add(Class.forName(orig.replace('/', '.'), false, probe));
            while (!queue.isEmpty() && !ok) {
                Class<?> c = queue.poll();
                if (c == null || !seen.add(c)) {
                    continue;
                }
                for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                    if (!m.getName().equals(name) || !signature(m).equals(desc)) {
                        continue;
                    }
                    int mod = m.getModifiers();
                    ok = Modifier.isPublic(mod) && Modifier.isStatic(mod) == isStatic;
                    break;
                }
                if (c.getSuperclass() != null) {
                    queue.add(c.getSuperclass());
                }
                queue.addAll(Arrays.asList(c.getInterfaces()));
            }
        } catch (Throwable ignored) {
        }
        access.put(key, ok);
        return ok;
    }

    private static String signature(java.lang.reflect.Method m) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> p : m.getParameterTypes()) {
            sb.append(Type.getDescriptor(p));
        }
        return sb.append(')').append(Type.getDescriptor(m.getReturnType())).toString();
    }

    private void guard(MethodNode mn) {
        if (mn.instructions.size() < 4 || rnd.nextInt(2) != 0) {
            return;
        }
        long[] p = inverse();
        InsnList dead = new InsnList();
        dead.add(new LdcInsnNode(p[0]));
        dead.add(new LdcInsnNode(p[1]));
        dead.add(new InsnNode(Opcodes.LMUL));
        dead.add(new LdcInsnNode(p[2]));
        dead.add(new InsnNode(Opcodes.LREM));
        dead.add(new InsnNode(Opcodes.L2I));
        dead.add(new InsnNode(Opcodes.ICONST_1));
        LabelNode skip = new LabelNode();
        dead.add(new JumpInsnNode(Opcodes.IF_ICMPNE, skip));
        for (int i = 0; i < 4; i++) {
            dead.add(new IntInsnNode(Opcodes.BIPUSH, 3 + rnd.nextInt(90)));
            dead.add(new IntInsnNode(Opcodes.BIPUSH, 3 + rnd.nextInt(90)));
            dead.add(new InsnNode(Opcodes.IMUL));
            dead.add(new InsnNode(Opcodes.POP));
        }
        dead.add(new InsnNode(Opcodes.NOP));
        dead.add(skip);
        mn.instructions.insert(dead);
    }

    private static long[] inverse() {
        while (true) {
            long p = prime();
            long q = prime();
            if (p == q) {
                continue;
            }
            long m = p * q;
            long x = 2 + (long) (rndStatic.nextDouble() * (m - 4));
            BigInteger bx = BigInteger.valueOf(x);
            BigInteger bm = BigInteger.valueOf(m);
            if (!bx.gcd(bm).equals(BigInteger.ONE)) {
                continue;
            }
            long a = bx.modInverse(bm).longValueExact();
            if (a > 1 && a < m - 1) {
                return new long[]{x, a, m};
            }
        }
    }

    private static final Random rndStatic = new Random();

    private static long prime() {
        while (true) {
            long v = 3000 + (long) (rndStatic.nextDouble() * 27000);
            if (BigInteger.valueOf(v).isProbablePrime(24)) {
                return v;
            }
        }
    }

    private MethodNode flatten(MethodNode src) {
        if (src.tryCatchBlocks != null && !src.tryCatchBlocks.isEmpty()) {
            return null;
        }
        if (src.instructions.size() < 24) {
            return null;
        }
        try {
            return build(src);
        } catch (Throwable t) {
            return null;
        }
    }

    private MethodNode build(MethodNode src) {
        AbstractInsnNode[] arr = src.instructions.toArray();
        int n = arr.length;
        for (AbstractInsnNode in : arr) {
            if (in.getOpcode() == Opcodes.JSR || in.getOpcode() == Opcodes.RET) {
                return null;
            }
        }

        Map<LabelNode, Integer> at = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (arr[i] instanceof LabelNode) {
                at.put((LabelNode) arr[i], i);
            }
        }

        SortedSet<Integer> cuts = new TreeSet<>();
        cuts.add(0);
        cuts.add(n);
        for (int i = 0; i < n; i++) {
            AbstractInsnNode in = arr[i];
            if (in instanceof JumpInsnNode) {
                jump(at, ((JumpInsnNode) in).label, cuts);
                cuts.add(i + 1);
            } else if (in instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode s = (TableSwitchInsnNode) in;
                for (LabelNode l : s.labels) {
                    jump(at, l, cuts);
                }
                jump(at, s.dflt, cuts);
                cuts.add(i + 1);
            } else if (in instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode s = (LookupSwitchInsnNode) in;
                for (LabelNode l : s.labels) {
                    jump(at, l, cuts);
                }
                jump(at, s.dflt, cuts);
                cuts.add(i + 1);
            } else {
                int op = in.getOpcode();
                if (op >= Opcodes.IRETURN && op <= Opcodes.RETURN) {
                    cuts.add(i + 1);
                }
            }
        }

        List<Integer> pts = new ArrayList<>(cuts);
        int all = pts.size() - 1;
        if (all < 2) {
            return null;
        }

        int[] blockAt = new int[n + 1];
        for (int i = 0, b = 0; i < all; i++) {
            for (int p = pts.get(i); p < pts.get(i + 1); p++) {
                blockAt[p] = b;
            }
            b++;
        }
        blockAt[n] = all - 1;

        boolean[] live = new boolean[all];
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(0);
        live[0] = true;
        while (!queue.isEmpty()) {
            int b = queue.poll();
            int fall = b + 1 < all ? b + 1 : -1;
            int from = pts.get(b);
            int to = pts.get(b + 1);
            for (int i = to - 1; i >= from; i--) {
                AbstractInsnNode in = arr[i];
                if (in instanceof LabelNode || in instanceof LineNumberNode || in instanceof FrameNode) {
                    continue;
                }
                int op = in.getOpcode();
                if (op >= Opcodes.IRETURN && op <= Opcodes.RETURN) {
                    fall = -1;
                } else if (in instanceof JumpInsnNode) {
                    if (op == Opcodes.GOTO) {
                        fall = -1;
                    } else {
                        reach(queue, live, blockAt[at.get(((JumpInsnNode) in).label)]);
                    }
                } else if (in instanceof TableSwitchInsnNode) {
                    TableSwitchInsnNode s = (TableSwitchInsnNode) in;
                    for (LabelNode l : s.labels) {
                        reach(queue, live, blockAt[at.get(l)]);
                    }
                    reach(queue, live, blockAt[at.get(s.dflt)]);
                } else if (in instanceof LookupSwitchInsnNode) {
                    LookupSwitchInsnNode s = (LookupSwitchInsnNode) in;
                    for (LabelNode l : s.labels) {
                        reach(queue, live, blockAt[at.get(l)]);
                    }
                    reach(queue, live, blockAt[at.get(s.dflt)]);
                }
                break;
            }
            if (fall >= 0) {
                reach(queue, live, fall);
            }
        }

        int[] map = new int[all];
        int blocks = 0;
        for (int b = 0; b < all; b++) {
            map[b] = live[b] ? blocks++ : -1;
        }
        if (blocks < 2) {
            return null;
        }

        int[] state = new int[blocks];
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < blocks; i++) {
            int v;
            do {
                v = 1 + rnd.nextInt(0xFFFFF);
            } while (!used.add(v));
            state[i] = v;
        }

        LabelNode[] heads = new LabelNode[blocks];
        for (int i = 0; i < blocks; i++) {
            heads[i] = new LabelNode();
        }

        MethodNode out = new MethodNode(publicise(src.access), src.name, src.desc,
                src.signature, src.exceptions == null ? null : src.exceptions.toArray(new String[0]));
        out.tryCatchBlocks = new ArrayList<>();

        int slot = src.maxLocals + 4;
        out.maxLocals = slot + 1;
        out.maxStack = 0;

        InsnList code = out.instructions;
        LabelNode loop = new LabelNode();
        LabelNode dead = new LabelNode();

        BlackLine.push(code, state[0]);
        code.add(new VarInsnNode(Opcodes.ISTORE, slot));
        code.add(loop);
        code.add(new VarInsnNode(Opcodes.ILOAD, slot));

        LabelNode[] sortedHeads = heads.clone();
        int[] sortedStates = state.clone();
        Integer[] order = new Integer[blocks];
        for (int i = 0; i < blocks; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (x, y) -> Integer.compare(sortedStates[x], sortedStates[y]));
        List<Integer> keyList = new ArrayList<>(blocks);
        List<LabelNode> labelList = new ArrayList<>(blocks);
        for (int i = 0; i < blocks; i++) {
            keyList.add(sortedStates[order[i]]);
            labelList.add(sortedHeads[order[i]]);
        }
        code.add(new LookupSwitchInsnNode(dead, toArray(keyList), labelList.toArray(new LabelNode[0])));
        code.add(dead);
        throwOut(code);

        for (int b = 0; b < blocks; b++) {
            code.add(heads[b]);
            int srcBlock = b;
            while (map[srcBlock] != b) {
                srcBlock++;
            }
            int from = pts.get(srcBlock);
            int to = pts.get(srcBlock + 1);
            int next = b + 1 < blocks ? b + 1 : -1;

            for (int i = from; i < to; i++) {
                AbstractInsnNode in = arr[i];
                if (in instanceof LabelNode || in instanceof LineNumberNode || in instanceof FrameNode) {
                    continue;
                }
                if (in instanceof JumpInsnNode) {
                    JumpInsnNode j = (JumpInsnNode) in;
                    int target = map[blockAt[at.get(j.label)]];
                    if (j.getOpcode() == Opcodes.GOTO) {
                        emit(code, slot, state[target], loop);
                        continue;
                    }
                    LabelNode taken = new LabelNode();
                    code.add(new JumpInsnNode(j.getOpcode(), taken));
                    LabelNode plain = new LabelNode();
                    code.add(new JumpInsnNode(Opcodes.GOTO, plain));
                    code.add(taken);
                    emit(code, slot, state[target], loop);
                    code.add(plain);
                    if (next < 0) {
                        throwOut(code);
                    } else {
                        emit(code, slot, state[next], loop);
                    }
                    continue;
                }
                if (in instanceof TableSwitchInsnNode) {
                    TableSwitchInsnNode s = (TableSwitchInsnNode) in;
                    LabelNode[] ls = new LabelNode[s.labels.size()];
                    for (int k = 0; k < ls.length; k++) {
                        ls[k] = heads[map[blockAt[at.get(s.labels.get(k))]]];
                    }
                    code.add(new TableSwitchInsnNode(s.min, s.max, heads[map[blockAt[at.get(s.dflt)]]], ls));
                    if (next < 0) {
                        throwOut(code);
                    } else {
                        emit(code, slot, state[next], loop);
                    }
                    continue;
                }
                if (in instanceof LookupSwitchInsnNode) {
                    LookupSwitchInsnNode s = (LookupSwitchInsnNode) in;
                    List<LabelNode> ls = new ArrayList<>(s.labels.size());
                    for (LabelNode l : s.labels) {
                        ls.add(heads[map[blockAt[at.get(l)]]]);
                    }
                    code.add(new LookupSwitchInsnNode(heads[map[blockAt[at.get(s.dflt)]]],
                            toArray(s.keys), ls.toArray(new LabelNode[0])));
                    if (next < 0) {
                        throwOut(code);
                    } else {
                        emit(code, slot, state[next], loop);
                    }
                    continue;
                }
                code.add(clone(in));
            }
        }
        return out;
    }

    private static void reach(Deque<Integer> queue, boolean[] live, int b) {
        if (b >= 0 && !live[b]) {
            live[b] = true;
            queue.add(b);
        }
    }

    private static void jump(Map<LabelNode, Integer> at, LabelNode l, SortedSet<Integer> cuts) {
        Integer p = at.get(l);
        if (p == null) {
            throw new IllegalStateException("dangling label");
        }
        cuts.add(p);
    }

    private static void emit(InsnList code, int slot, int value, LabelNode loop) {
        BlackLine.push(code, value);
        code.add(new VarInsnNode(Opcodes.ISTORE, slot));
        code.add(new JumpInsnNode(Opcodes.GOTO, loop));
    }

    private static void throwOut(InsnList code) {
        code.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalStateException"));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/IllegalStateException",
                "<init>", "()V", false));
        code.add(new InsnNode(Opcodes.ATHROW));
    }

    private static int[] toArray(List<Integer> in) {
        int[] out = new int[in.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = in.get(i);
        }
        return out;
    }

    private static AbstractInsnNode clone(AbstractInsnNode in) {
        return in.clone(new HashMap<LabelNode, LabelNode>());
    }

    private static int publicise(int access) {
        return (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
    }
}
