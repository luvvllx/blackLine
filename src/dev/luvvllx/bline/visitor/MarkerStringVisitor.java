package dev.luvvllx.bline.visitor;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import dev.luvvllx.bline.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MarkerStringVisitor extends AbstractVisitor {

    public static final String[] markerStrings;

    static {
        List<String> sl = new ArrayList<>();
        String[] sl2 = new String[] {"\uEF94", "\uF229", "\uF770", "ᨬ", "\uFD4B"};
        for (int i = 0; i < 3; i++) {
            StringBuilder ss = new StringBuilder("B1");
            for(int j = 0; j < 150; j++) {
                ss.append(Utils.getRandomMember(sl2));
            }
            sl.add(ss.toString());
        }
        markerStrings = sl.toArray(new String[0]);
    }

    public MarkerStringVisitor(byte[] bytes, String[] args) {
        super(bytes, args);
    }

    @Override
    public byte[] transfer(byte[] bytes) {
        ClassNode cn = byteToClassNode(bytes);
        if(cn.name.equals("dev/luvvllx/runtime/B1")) {
            for (MethodNode method : cn.methods) {
                if(method.name.equals("decKey") && method.desc.equals("(Ljava/lang/Object;)Ljava/lang/String;")) {
                    findAndReplaceString(method, new String[][] { {"dynamicStringKey", ThrowableStringObfVisitor.dynamicStringKey} });
                }
                if(isClinitNode(method)) {
                    findAndReplaceString(method, new String[][] {
                            {"jullySlot0", markerStrings[0]},
                            {"jullySlot1", markerStrings[1]},
                            {"jullySlot2", markerStrings[2]},
                    });
                }
            }
        }
        return classNodeToBytes(cn);
    }

    public static void findAndReplaceString(MethodNode method, String[][] strs) {
        for (AbstractInsnNode insnNode : method.instructions) {
            if(insnNode instanceof LdcInsnNode) {
                LdcInsnNode ldcInsnNode = (LdcInsnNode) insnNode;
                if (ldcInsnNode.cst instanceof String) {
                    for (String[] str : strs) {
                        if(str[0].equals(ldcInsnNode.cst)) {
                            ldcInsnNode.cst = str[1];
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<String> getVisitorTags() {
        return Collections.emptyList();
    }
}
