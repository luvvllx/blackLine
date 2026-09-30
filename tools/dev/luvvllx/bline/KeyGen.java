package dev.luvvllx.bline;

import dev.luvvllx.runtime.Vm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class KeyGen {

    static byte[] reference(int[] seeds, byte[] salt, int guard) throws Exception {
        int n = seeds.length;
        int step = salt[3] & 0x7f;
        if (step % n == 0) {
            step++;
        }
        int off = salt[4] & 0xff;
        byte[] m = new byte[32];
        for (int i = 0; i < n; i++) {
            int idx = (i * step + off) & (n - 1);
            int x = seeds[idx];
            int pos = (i * 7) & 31;
            for (int j = 0; j < 4; j++) {
                x = x * 1664525 + 1013904223;
                int at = (pos + j * 5) & 31;
                int k = (salt[j & 7] + i * 13 + guard) & 0xff;
                m[at] ^= (byte) ((x >>> 9) ^ k);
            }
        }
        for (int i = 0; i < 32; i++) {
            m[i] ^= (byte) (m[(i + 11) & 31] + i);
        }
        return dev.luvvllx.runtime.Cx.digest(m, salt);
    }

    private static final int I = 0, STEP = 1, OFF = 2, X = 3, POS = 4, J = 5,
            AT = 6, T = 7, ZERO = 8, AVAL = 9, T2 = 10, G = 11, T3 = 12;

    private final List<int[]> ops = new ArrayList<>();
    private final Map<String, Integer> here = new LinkedHashMap<>();
    private final List<Object[]> pending = new ArrayList<>();
    private final Random rnd;
    private int[] rmap = new int[16];
    private int seedMask;

    KeyGen(Random rnd) {
        this.rnd = rnd;
    }

    private void e(int op, int a, int b) {
        ops.add(new int[] {op, a, b});
    }

    private void mark(String n) {
        here.put(n, ops.size());
    }

    private void jmp(int op, int a, String label) {
        ops.add(new int[] {op, a, 0});
        pending.add(new Object[] {ops.size() - 1, label});
    }

    private int[] permute() {
        int[] p = new int[16];
        for (int i = 0; i < 16; i++) {
            p[i] = i;
        }
        for (int i = 15; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        rmap = p;
        return p;
    }

    int[] build(int[] seeds) {
        seedMask = seedMask(seeds);
        permute();

        e(Vm.CONST, ZERO, 0);
        e(Vm.GUARD, G, 0);

        e(Vm.CONST, I, 33);
        mark("clr");
        e(Vm.ADDI, I, -1);
        jmp(Vm.JZ, I, "clrEnd");
        e(Vm.ADDI, I, -1);
        e(Vm.MEMW, I, ZERO);
        jmp(Vm.JMP, 0, "clr");
        mark("clrEnd");

        e(Vm.SALTV, STEP, 3);
        e(Vm.CONST, T, 0x7f);
        e(Vm.AND, STEP, T);
        e(Vm.SALTV, OFF, 4);
        e(Vm.CONST, T, 0xff);
        e(Vm.AND, OFF, T);

        e(Vm.CONST, I, 0);
        mark("main");
        e(Vm.MOV, T, I);
        e(Vm.CONST, T2, 256);
        e(Vm.SUB, T, T2);
        jmp(Vm.JZ, T, "aval");

        e(Vm.MOV, T, I);
        e(Vm.MUL, T, STEP);
        e(Vm.ADD, T, OFF);
        e(Vm.CONST, T2, 255);
        e(Vm.AND, T, T2);
        e(Vm.SEED, X, T);

        e(Vm.MOV, POS, I);
        e(Vm.CONST, T, 7);
        e(Vm.MUL, POS, T);
        e(Vm.CONST, T2, 31);
        e(Vm.AND, POS, T2);

        e(Vm.CONST, J, 0);
        mark("inner");
        e(Vm.MOV, T, J);
        e(Vm.CONST, T2, 4);
        e(Vm.SUB, T, T2);
        jmp(Vm.JZ, T, "nextI");

        e(Vm.LCG, X, 0);

        e(Vm.MOV, AT, J);
        e(Vm.CONST, T, 5);
        e(Vm.MUL, AT, T);
        e(Vm.ADD, AT, POS);
        e(Vm.CONST, T2, 31);
        e(Vm.AND, AT, T2);

        e(Vm.SALTV, T, 0);
        e(Vm.MOV, T2, I);
        e(Vm.CONST, T3, 13);
        e(Vm.MUL, T2, T3);
        e(Vm.ADD, T, T2);
        e(Vm.ADD, T, G);
        e(Vm.CONST, T2, 255);
        e(Vm.AND, T, T2);

        e(Vm.SHR9, X, 0);
        e(Vm.XOR, X, T);
        e(Vm.TRUNC, X, 0);
        e(Vm.MEMX, AT, X);

        e(Vm.ADDI, J, 1);
        jmp(Vm.JMP, 0, "inner");
        mark("nextI");

        e(Vm.ADDI, I, 1);
        jmp(Vm.JMP, 0, "main");

        mark("aval");
        e(Vm.CONST, I, 0);
        mark("aloop");
        e(Vm.MOV, T, I);
        e(Vm.CONST, T2, 32);
        e(Vm.SUB, T, T2);
        jmp(Vm.JZ, T, "done");

        e(Vm.MOV, AVAL, I);
        e(Vm.CONST, T, 11);
        e(Vm.ADD, AVAL, T);
        e(Vm.CONST, T2, 31);
        e(Vm.AND, AVAL, T2);

        e(Vm.MEMR, T, AVAL);
        e(Vm.ADD, T, I);
        e(Vm.TRUNC, T, 0);
        e(Vm.MEMX, I, T);

        e(Vm.ADDI, I, 1);
        jmp(Vm.JMP, 0, "aloop");
        mark("done");
        e(Vm.HALT, 0, 0);

        inject();

        int[] flat = new int[ops.size() * Vm.WIDTH];
        for (int i = 0; i < ops.size(); i++) {
            flat[i * Vm.WIDTH] = ops.get(i)[0];
            flat[i * Vm.WIDTH + 1] = ops.get(i)[1];
            flat[i * Vm.WIDTH + 2] = ops.get(i)[2];
        }
        for (Object[] p : pending) {
            int pc = (Integer) p[0];
            Integer target = here.get((String) p[1]);
            if (target == null) {
                throw new IllegalStateException("no label " + p[1]);
            }
            flat[pc * Vm.WIDTH + 2] = target * Vm.WIDTH;
        }

        int mask = seedMask;
        for (int i = 0; i < flat.length; i++) {
            flat[i] ^= mask;
        }
        return flat;
    }

    static int seedMask(int[] seeds) {
        return seeds[0] ^ seeds[7] ^ seeds[13] ^ seeds[31] ^ 0x5bf03635;
    }

    private void inject() {
        int blocks = 4 + ops.size() / 40;
        for (int i = 0; i < blocks; i++) {
            int n = 3 + rnd.nextInt(6);
            for (int k = 0; k < n; k++) {
                e(rnd.nextInt(Vm.OPS - 1) + 1, rnd.nextInt(16), rnd.nextInt(64));
            }
        }
    }

    int[] regmap() {
        return rmap;
    }
}
