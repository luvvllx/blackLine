package dev.luvvllx.bline.visitor;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import dev.luvvllx.bline.utils.Utils;

import java.util.Collections;
import java.util.List;

public class AntiDebuggerObfVisitor extends AbstractVisitor {

    public AntiDebuggerObfVisitor(byte[] bytes, String[] args) {
        super(bytes, args);
    }

    public byte[] transfer(byte[] bytes) {
        ClassNode cn = byteToClassNode(bytes);
        for (MethodNode method : cn.methods) {
            InsnList insnList = new InsnList();
            for (AbstractInsnNode insnNode : method.instructions) {
                if(insnNode instanceof LdcInsnNode) {
                    LdcInsnNode ldcInsnNode = (LdcInsnNode) insnNode;
                    if(ldcInsnNode.cst instanceof Integer) {
                        visitInteger((Integer) ldcInsnNode.cst, insnList);
                        continue;
                    }
                } else if(insnNode instanceof IntInsnNode) {
                    IntInsnNode intInsnNode = (IntInsnNode) insnNode;
                    if((intInsnNode.getOpcode() == Opcodes.BIPUSH || intInsnNode.getOpcode() == Opcodes.SIPUSH)) {
                        visitInteger(intInsnNode.operand, insnList);
                        continue;
                    }
                } else if(insnNode instanceof InsnNode) {
                    Integer in = null;

                    int opcode = insnNode.getOpcode();
                    if(opcode >= 2 && opcode <= 8) {
                        in = opcode - 3;
                    }
                    if(in != null) {
                        visitInteger(in, insnList);
                        continue;
                    }
                }
                insnList.add(insnNode);
            }
            method.instructions = insnList;
        }
        return classNodeToBytes(cn);
    }

    @Override
    public List<String> getVisitorTags() {
        return Collections.emptyList();
    }

    public static void visitInteger(int value, InsnList insnList) {
        BlobSerializableVisitor data;
        while((data = BlobSerializableVisitor.INSTANCE) == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        };

        String field = Utils.r.nextBoolean() ? Utils.getRandomMember(data.blobFieldNames) : data.totalHashedSerializedFieldName;

        int hash = data.blobHashIndex.get(field);

        insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, "dev/luvvllx/runtime/B1", "i", "Ldev/luvvllx/runtime/B1;"));
        BlobSerializableVisitor.FieldType fieldType = data.blobFieldTypes.get(field);
        insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "dev/luvvllx/runtime/B1", field, fieldType.desc));

        switch (fieldType) {
            case STRING:
                insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false));
                break;
            case INT:
            default:
                break;
        }

        int hash2 = hash ^ value;

        insnList.add(new LdcInsnNode(hash2));
        insnList.add(new InsnNode(Opcodes.IXOR));
    }

}
