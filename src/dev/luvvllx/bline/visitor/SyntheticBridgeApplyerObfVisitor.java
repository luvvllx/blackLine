package dev.luvvllx.bline.visitor;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import dev.luvvllx.bline.utils.Utils;

import java.util.Collections;
import java.util.List;

public class SyntheticBridgeApplyerObfVisitor extends AbstractVisitor {

    public static final String[][] joking = new String[][] {
            new String[] {"state", "invalid state reached"},
            new String[] {"guard", "guard check failed"},
            new String[] {"integrity", "consistency lost", "ref 263087074"},
            new String[] {"trace", "unreachable path taken"}
        };

    public SyntheticBridgeApplyerObfVisitor(byte[] bytes, String[] args) {
        super(bytes, args);
    }

    @Override
    public byte[] transfer(byte[] bytes) {
        ClassNode cn = byteToClassNode(bytes);

        for (FieldNode field : cn.fields) {
            field.access |= Opcodes.ACC_SYNTHETIC;
        }
        for (MethodNode method : cn.methods) {
            method.access |= (Opcodes.ACC_ANNOTATION & cn.access) != 0 ? 0 : (Opcodes.ACC_SYNTHETIC | (method.name.contains("init>") ? 0 : Opcodes.ACC_BRIDGE));
        }

        if (getBooleanArgOrDefault(0, true)) {
            String[] temp = joking[Utils.r.nextInt(joking.length)];
            for(String temp2 : temp) {
                cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC, temp2, "Ldev/luvvllx/runtime/Ex0;", null, null));
            }
        }

        return classNodeToBytes(cn);
    }

    @Override
    public List<String> getVisitorTags() {
        return Collections.emptyList();
    }
}
