package dev.luvvllx.bline.visitor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import dev.luvvllx.bline.Main;
import dev.luvvllx.bline.taskmanager.Task;
import dev.luvvllx.bline.taskmanager.TaskManager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ObjSubstVisitor extends AbstractVisitor {

    static {
        TaskManager.addTask(new Task(() -> {
            Main.writeToOutput(dev.luvvllx.runtime.Obj.class);
            Main.writeToOutput(dev.luvvllx.runtime.ObjSubst.class);
        }, "ObjSubst"));
    }

    public static final NameAndDesc[] NameAndDescs = new NameAndDesc[] {
            new NameAndDesc("hashCode", "()I"),
            new NameAndDesc("<init>", "()V"),
            new NameAndDesc("equals", "(Ljava/lang/Object;)Z"),
            new NameAndDesc("toString", "()Ljava/lang/String;")
    };

    public ObjSubstVisitor(byte[] bytes, String[] args) {
        super(bytes, args);
    }

    @Override
    public byte[] transfer(byte[] bytes) {
        ClassNode cn = byteToClassNode(bytes);

        if((cn.access | Opcodes.ACC_ANNOTATION) == cn.access || (cn.access | Opcodes.ACC_INTERFACE) == cn.access || !cn.superName.equals("java/lang/Object")) return classNodeToBytes(cn);

        cn.superName = "dev/luvvllx/runtime/Obj";

        cn.methods.stream().filter(method -> method.name.equals("<init>")).forEach(method -> {
            for (AbstractInsnNode insnNode : method.instructions) {
                if (insnNode instanceof MethodInsnNode) {
                    MethodInsnNode insnNode1 = (MethodInsnNode) insnNode;

                    if(insnNode1.getOpcode() == Opcodes.INVOKESPECIAL) {
                        for (NameAndDesc nameAndDesc : NameAndDescs) {
                            if (insnNode1.name.equals(nameAndDesc.name) && insnNode1.desc.equals(nameAndDesc.desc) && insnNode1.owner.equals("java/lang/Object")) {
                                insnNode1.owner = "dev/luvvllx/runtime/Obj";
                                break;
                            }
                        }
                    }
                }
            }
        });

        return classNodeToBytes(cn);
    }

    @Override
    public List<String> getVisitorTags() {
        return Collections.emptyList();
    }

    public static class NameAndDesc {
        public String name;
        public String desc;
        public NameAndDesc(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }
    }

}
