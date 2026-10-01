package dev.luvvllx.bline.visitor;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import dev.luvvllx.bline.config.Config;
import dev.luvvllx.bline.utils.Utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.LongStream;

public class NumberObfVisitor extends AbstractVisitor {

    private final String seedStr1;
    private final String seedStr2;
    private final int seedInt;
    private final long randomSeed;
    private ClassNode cn;

    private String[] fieldNames;
    private int[] keyPool;
    private int currentSlot;

    public NumberObfVisitor(byte[] bytes, String[] args) {
        super(bytes, args);
        this.randomSeed = (long) (seedStr1 = Utils.spawnRandomChar(10, true)).hashCode() * (seedStr2 = Utils.spawnRandomChar(10, true)).hashCode() * (seedInt = Utils.getRandomSafeLineNumber());
    }

    @Override
    public byte[] transfer(byte[] bytes) {
        ClassNode cn = this.cn = byteToClassNode(bytes);

        List<MethodNode> keyed = new java.util.ArrayList<>();
        for(MethodNode method : cn.methods) {
            if(!isClinitNode(method)) keyed.add(method);
        }

        fieldNames = buildFieldNames(cn, keyed.size());
        keyPool = new int[fieldNames.length];
        Random poolRandom = new Random(randomSeed);
        for(int i = 0;i < keyPool.length;i++) keyPool[i] = poolRandom.nextInt();

        if(fieldNames.length > 0) {
            MethodNode clinitNode = getOrCreateClinitNode(cn);
            InsnList insnList = new InsnList();

            insnList.add(new TypeInsnNode(Opcodes.NEW, "java/util/Random"));
            insnList.add(new InsnNode(Opcodes.DUP));

            insnList.add(new LdcInsnNode(seedStr1));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I"));
            insnList.add(new InsnNode(Opcodes.I2L));

            LabelNode labelNode = new LabelNode();
            insnList.add(labelNode);
            insnList.add(new LineNumberNode(seedInt, labelNode));

            insnList.add(new TypeInsnNode(Opcodes.NEW, "java/lang/Throwable"));
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(new LdcInsnNode(seedStr2));
            insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Throwable", "<init>", "(Ljava/lang/String;)V"));
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "getMessage", "()Ljava/lang/String;"));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I"));
            insnList.add(new InsnNode(Opcodes.I2L));
            insnList.add(new InsnNode(Opcodes.DUP2_X1));
            insnList.add(new InsnNode(Opcodes.POP2));

            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "getStackTrace", "()[Ljava/lang/StackTraceElement;"));
            insnList.add(new InsnNode(Opcodes.ICONST_0));
            insnList.add(new InsnNode(Opcodes.AALOAD));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StackTraceElement", "getLineNumber", "()I"));
            insnList.add(new InsnNode(Opcodes.I2L));

            insnList.add(new InsnNode(Opcodes.LMUL));
            insnList.add(new InsnNode(Opcodes.LMUL));

            insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Random", "<init>", "(J)V"));

            for(int i = 0;i < fieldNames.length;i++) {
                insnList.add(new InsnNode(Opcodes.DUP));
                insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Random", "nextInt", "()I"));
                insnList.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, fieldNames[i], "I"));
                cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, fieldNames[i], "I", null, null));
            }
            insnList.add(new InsnNode(Opcodes.POP));
            insnList.add(clinitNode.instructions);

            clinitNode.instructions = insnList;
        }

        int slot = 0;
        for (MethodNode method : cn.methods) {
            if(isClinitNode(method)) currentSlot = 0;
            else currentSlot = slot++;
            InsnList insnList = new InsnList();
            for (AbstractInsnNode insnNode : method.instructions) {
                if(insnNode instanceof LdcInsnNode) {
                    LdcInsnNode ldcInsnNode = (LdcInsnNode) insnNode;
                    if(ldcInsnNode.cst instanceof Integer) {
                        visitInteger(this, (Integer) ldcInsnNode.cst, insnList, isClinitNode(method));
                        continue;
                    } else if(ldcInsnNode.cst instanceof Long) {
                        visitLong((Long) ldcInsnNode.cst, insnList);
                        continue;
                    }
                } else if(insnNode instanceof IntInsnNode) {
                    IntInsnNode intInsnNode = (IntInsnNode) insnNode;
                    if((intInsnNode.getOpcode() == Opcodes.BIPUSH || intInsnNode.getOpcode() == Opcodes.SIPUSH)) {
                        visitInteger(this, intInsnNode.operand, insnList, isClinitNode(method));
                        continue;
                    }
                } else if(insnNode instanceof InsnNode) {
                    Integer in = null;
                    Long l = null;

                    int opcode = insnNode.getOpcode();
                    if(opcode >= 2 && opcode <= 8) {
                        in = opcode - 3;
                    } else if(opcode >= 9 && opcode <= 10) {
                        l = (long) (opcode - 9);
                    }
                    if(in != null) {
                        visitInteger(this, in, insnList, isClinitNode(method));
                        continue;
                    } else if(l != null) {
                        visitLong(l, insnList);
                        continue;
                    }
                }
                insnList.add(insnNode);
            }
            method.instructions = insnList;
        }
        return classNodeToBytes(cn);
    }

    public static void visitInteger(NumberObfVisitor instance, int value, InsnList insnList, boolean isClinitMethod) {
        if(Config.useLCMPNumber && Utils.r.nextBoolean()) {
            int number1 = Utils.r.nextInt(3) - 1;
            LongStream randLongs = LongStream.generate(Utils.r::nextLong).limit(2);
            boolean flag = Utils.r.nextBoolean();
            int opcode = flag ? Opcodes.IADD : Opcodes.ISUB;
            int number2 = flag ? value - number1 : value + number1;

            insnList.add(new LdcInsnNode(number2));
            if(number1 == -1) {
                randLongs.boxed().sorted(Comparator.naturalOrder()).forEach(l -> visitLong(l, insnList));
            } else if(number1 == 1) {
                randLongs.boxed().sorted(Comparator.reverseOrder()).forEach(l -> visitLong(l, insnList));
            } else if(number1 == 0) {
                long l = randLongs.findFirst().getAsLong();
                visitLong(l, insnList);
                visitLong(l, insnList);
            } else throw new AssertionError();
            insnList.add(new InsnNode(Opcodes.LCMP));
            insnList.add(new InsnNode(opcode));
            return;
        }

        int flag1 = Utils.r.nextInt(2);
        int number1 = Utils.r.nextInt();
        int number2 = flag1 == 0 ? number1 + value : value - number1;

        int[] nums = {number2, number1};
        for (int num : nums) {
            boolean flag2 = Utils.r.nextBoolean();
            int[] pairs = flag2 ? andNums(num) : orNums(num);
            if(instance == null || isClinitMethod) {
                insnList.add(new LdcInsnNode(pairs[0]));
                insnList.add(new LdcInsnNode(pairs[1]));
            } else {
                int slot = instance.currentSlot % instance.keyPool.length;
                int key = instance.keyPool[slot];
                String field = instance.fieldNames[slot];
                int salt = Utils.r.nextInt();
                if(Utils.r.nextBoolean()) {
                    insnList.add(new LdcInsnNode(pairs[0]));
                    insnList.add(new LdcInsnNode(pairs[1] ^ key ^ salt));
                    insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, instance.cn.name, field, "I"));
                    insnList.add(new InsnNode(Opcodes.IXOR));
                    insnList.add(new LdcInsnNode(salt));
                    insnList.add(new InsnNode(Opcodes.IXOR));
                } else {
                    insnList.add(new LdcInsnNode(pairs[0] ^ key ^ salt));
                    insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, instance.cn.name, field, "I"));
                    insnList.add(new InsnNode(Opcodes.IXOR));
                    insnList.add(new LdcInsnNode(salt));
                    insnList.add(new InsnNode(Opcodes.IXOR));
                    insnList.add(new LdcInsnNode(pairs[1]));
                }
            }
            insnList.add(flag2 ? new InsnNode(Opcodes.IAND) : new InsnNode(Opcodes.IOR));
        }

        if (flag1 == 0) {

            insnList.add(new InsnNode(Opcodes.ISUB));
        } else {

            insnList.add(new InsnNode(Opcodes.IADD));
        }
    }

    public static void visitLong(long value, InsnList insnList) {
        int flag1 = Utils.r.nextInt(2);
        long number1 = Utils.r.nextLong();
        long number2 = flag1 == 0 ? number1 + value : value - number1;

        long[] nums = {number2, number1};
        for (long num : nums) {
            boolean flag2 = Utils.r.nextBoolean();
            long[] pairs = flag2 ? andNumsLong(num) : orNumsLong(num);
            insnList.add(new LdcInsnNode(pairs[0]));
            insnList.add(new LdcInsnNode(pairs[1]));
            insnList.add(flag2 ? new InsnNode(Opcodes.LAND) : new InsnNode(Opcodes.LOR));
        }

        if (flag1 == 0) {

            insnList.add(new InsnNode(Opcodes.LSUB));
        } else {

            insnList.add(new InsnNode(Opcodes.LADD));
        }
    }

    @Override
    public List<String> getVisitorTags() {
        return Collections.emptyList();
    }

    public static int[] andNums(int x) {
        Random random = Utils.r;

        int a = x;
        int b = x;

        boolean flag = random.nextBoolean();
        int extraMask = random.nextInt() & ~x;

        int aExtra = extraMask & random.nextInt();
        int bExtra = extraMask & ~aExtra;

        a |= aExtra;
        b |= bExtra;

        if((a & b) != x) return andNums(x);
        return new int[] {a, b};
    }

    public static int[] orNums(int x) {
        if (x == 0) {
            return new int[]{0, 0};
        }

        int a = 0;
        int b = 0;

        for (int i = 0; i < 32; i++) {
            int mask = 1 << i;

            if ((x & mask) != 0) {

                int choice = Utils.r.nextInt(3);
                switch (choice) {
                    case 0: a |= mask; break;
                    case 1: b |= mask; break;
                    case 2:
                        a |= mask;
                        b |= mask;
                        break;
                }
            }

        }

        if((a | b) != x) return orNums(x);
        return new int[] {a, b};
    }

    public static long[] andNumsLong(long x) {
        Random random = Utils.r;

        long a = x;
        long b = x;

        long extraMask = random.nextLong() & ~x;

        long aExtra = extraMask & random.nextLong();
        long bExtra = extraMask & ~aExtra;

        a |= aExtra;
        b |= bExtra;

        if((a & b) != x) return andNumsLong(x);
        return new long[] {a, b};
    }

    public static long[] orNumsLong(long x) {
        if (x == 0) {
            return new long[]{0, 0};
        }

        long a = 0;
        long b = 0;

        for (int i = 0; i < 64; i++) {
            long mask = 1L << i;

            if ((x & mask) != 0) {

                int choice = Utils.r.nextInt(3);
                switch (choice) {
                    case 0: a |= mask; break;
                    case 1: b |= mask; break;
                    case 2:
                        a |= mask;
                        b |= mask;
                        break;
                }
            }

        }

        if((a | b) != x) return orNumsLong(x);
        return new long[] {a, b};
    }

    private String[] buildFieldNames(ClassNode cn, int count) {
        String[] names = new String[count];
        for(int i = 0;i < count;i++) {
            String name = null;
            int tries = 0;
            do {
                tries++;
                name = Utils.getRandomNameFromMap();
            } while(tries < 100 && exists(cn, names, i, name));
            if(name == null || exists(cn, names, i, name)) {
                name = "_" + Utils.getRandomNameFromMap() + "_" + i;
            }
            names[i] = name;
        }
        return names;
    }

    private static boolean exists(ClassNode cn, String[] names, int len, String name) {
        if(name == null) return true;
        for(int j = 0;j < len;j++) {
            if(name.equals(names[j])) return true;
        }
        return cn.fields.stream().anyMatch(f -> f.name.equals(name));
    }
}
