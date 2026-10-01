package dev.luvvllx.runtime;

import java.security.MessageDigest;

public final class Forge {

    private static final int HALT = 0, NOP = 1, CONST = 2, MOV = 3, XOR = 4, XORI = 5,
            ADD = 6, ADDI = 7, SUB = 8, MUL = 9, MULI = 10, AND = 11, ANDI = 12,
            OR = 13, NOT = 14, SHL = 15, SHLI = 16, SHR = 17, SHRI = 18,
            LCG = 19, SHR9 = 20, TRUNC = 21, SEED = 22, SALTV = 23, GUARD = 24,
            MEMR = 25, MEMW = 26, MEMX = 27, JMP = 28, JZ = 29, JNZ = 30;

    private static final int WIDTH = 3;

    private Forge() {
    }

    public static byte[] derive(int[] prog, int[] rmap, int[] seeds, byte[] salt,
                                int guard, int slot, byte[] chain) {
        int mask = seeds[0] ^ seeds[7] ^ seeds[13] ^ seeds[31] ^ 0x5bf03635;

        int[] r = new int[16];
        int[] m = new int[32];
        int pc = 0;

        while (pc >= 0 && pc + 2 < prog.length) {
            int op = prog[pc] ^ mask;
            int a = prog[pc + 1] ^ mask;
            int b = prog[pc + 2] ^ mask;
            prog[pc] = op ^ mask;
            prog[pc + 1] = a ^ mask;
            prog[pc + 2] = b ^ mask;
            pc += WIDTH;

            switch (op & 31) {
                case HALT:
                    pc = -1;
                    break;
                case NOP:
                    break;
                case CONST:
                    r[rmap[a & 15]] = b;
                    break;
                case MOV:
                    r[rmap[a & 15]] = r[rmap[b & 15]];
                    break;
                case XOR:
                    r[rmap[a & 15]] ^= r[rmap[b & 15]];
                    break;
                case XORI:
                    r[rmap[a & 15]] ^= b;
                    break;
                case ADD:
                    r[rmap[a & 15]] += r[rmap[b & 15]];
                    break;
                case ADDI:
                    r[rmap[a & 15]] += b;
                    break;
                case SUB:
                    r[rmap[a & 15]] -= r[rmap[b & 15]];
                    break;
                case MUL:
                    r[rmap[a & 15]] = r[rmap[a & 15]] * r[rmap[b & 15]];
                    break;
                case MULI:
                    r[rmap[a & 15]] *= b;
                    break;
                case AND:
                    r[rmap[a & 15]] &= r[rmap[b & 15]];
                    break;
                case ANDI:
                    r[rmap[a & 15]] &= b;
                    break;
                case OR:
                    r[rmap[a & 15]] |= r[rmap[b & 15]];
                    break;
                case NOT:
                    r[rmap[a & 15]] = ~r[rmap[a & 15]];
                    break;
                case SHL:
                    r[rmap[a & 15]] <<= (r[rmap[b & 15]] & 31);
                    break;
                case SHLI:
                    r[rmap[a & 15]] <<= (b & 31);
                    break;
                case SHR:
                    r[rmap[a & 15]] >>>= (r[rmap[b & 15]] & 31);
                    break;
                case SHRI:
                    r[rmap[a & 15]] >>>= (b & 31);
                    break;
                case LCG:
                    r[rmap[a & 15]] = r[rmap[a & 15]] * 1664525 + 1013904223;
                    break;
                case SHR9:
                    r[rmap[a & 15]] >>>= 9;
                    break;
                case TRUNC:
                    r[rmap[a & 15]] = (byte) r[rmap[a & 15]];
                    break;
                case SEED:
                    r[rmap[a & 15]] = seeds[r[rmap[b & 15]] & 255];
                    break;
                case SALTV:
                    r[rmap[a & 15]] = salt[r[rmap[b & 15]] & 7];
                    break;
                case GUARD:
                    r[rmap[a & 15]] = guard;
                    break;
                case MEMR:
                    r[rmap[a & 15]] = m[r[rmap[b & 15]] & 31];
                    break;
                case MEMW:
                    m[r[rmap[a & 15]] & 31] = (byte) r[rmap[b & 15]];
                    break;
                case MEMX:
                    m[r[rmap[a & 15]] & 31] =
                            (byte) (m[r[rmap[a & 15]] & 31] ^ r[rmap[b & 15]]);
                    break;
                case JMP:
                    pc = b;
                    break;
                case JZ:
                    if (r[rmap[a & 15]] == 0) {
                        pc = b;
                    }
                    break;
                case JNZ:
                    if (r[rmap[a & 15]] != 0) {
                        pc = b;
                    }
                    break;
                default:
                    pc = -1;
            }
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < 32; i++) {
                md.update((byte) m[i]);
            }
            md.update(salt);
            md.update((byte) 0x9e);
            md.update(chain);
            md.update((byte) (slot >>> 24));
            md.update((byte) (slot >>> 16));
            md.update((byte) (slot >>> 8));
            md.update((byte) slot);
            return md.digest();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
