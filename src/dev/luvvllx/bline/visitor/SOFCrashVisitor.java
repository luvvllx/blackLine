package dev.luvvllx.bline.visitor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

import java.lang.annotation.Annotation;
import java.util.*;

public class SOFCrashVisitor extends AbstractVisitor {

    public static final Map<String, Integer> hashmap = new HashMap<>();

    public static final String author = "𝒋𝒖𝒍𝒍𝒚𝑪𝒓𝒚𝒑𝒕";

    public SOFCrashVisitor(byte[] bytes, String[] args) {
        super(bytes, args);
    }

    public byte[] transfer(byte[] classData) {
        ClassNode classNode = byteToClassNode(classData);

        if((classNode.access & (Opcodes.ACC_ENUM | Opcodes.ACC_INTERFACE)) != 0) return classData;

        Class<?> annType;
        try {
            annType = Class.forName("dev.luvvllx.runtime.Ann");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }
        AnnotationNode annNode = new AnnotationNode(Type.getDescriptor(annType));

        AnnotationNode curNode = annNode;

        annNode.values = new ArrayList<>();

        Random r = new Random();
        int k = r.nextInt();
        int result = 0;
        r = new Random(k);

        for (int i = 0; i < 1700; i++) {
            if(i == 0) {
                curNode.visit("\uD835\uDC8B\uD835\uDC96\uD835\uDC8D\uD835\uDC8D\uD835\uDC9A\uD835\uDC6A\uD835\uDC93\uD835\uDC9A\uD835\uDC91\uD835\uDC95", k);
                result ^= k;
            } else {
                int value = r.nextInt();
                curNode.visit("\uD835\uDC8B\uD835\uDC96\uD835\uDC8D\uD835\uDC8D\uD835\uDC9A\uD835\uDC6A\uD835\uDC93\uD835\uDC9A\uD835\uDC91\uD835\uDC95", value);
                result ^= value;
            }
            curNode.visit("C", curNode = new AnnotationNode(Type.getDescriptor(annType)));
        }

        if (classNode.visibleAnnotations == null) {
            classNode.visibleAnnotations = new ArrayList<>();
        }
        classNode.visibleAnnotations.add(annNode);

        hashmap.put(classNode.name, result);

        return classNodeToBytes(classNode);
    }

    @Override
    public List<String> getVisitorTags() {
        return Collections.emptyList();
    }

}
