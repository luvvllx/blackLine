package dev.luvvllx.runtime;

public final class Vm {

    private Vm() {
    }

    public static final int WIDTH = 3;

    public static final int HALT = 0, NOP = 1, CONST = 2, MOV = 3, XOR = 4, XORI = 5,
            ADD = 6, ADDI = 7, SUB = 8, MUL = 9, MULI = 10, AND = 11, ANDI = 12,
            OR = 13, NOT = 14, SHL = 15, SHLI = 16, SHR = 17, SHRI = 18,
            LCG = 19, SHR9 = 20, TRUNC = 21, SEED = 22, SALTV = 23, GUARD = 24,
            MEMR = 25, MEMW = 26, MEMX = 27, JMP = 28, JZ = 29, JNZ = 30,
            OPS = 31;
}
