package dev.luvvllx.bline.visitor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import dev.luvvllx.bline.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LabelResorterObfVisitor extends AbstractVisitor {

    public LabelResorterObfVisitor(byte[] bytes, String[] args) {
        super(bytes, args);
    }

    public byte[] transfer(byte[] classData) {
        ClassNode classNode = byteToClassNode(classData);

        if((classNode.access & (Opcodes.ACC_ENUM | Opcodes.ACC_INTERFACE)) != 0) return classData;

        for(MethodNode methodNode : classNode.methods) {
            if((methodNode.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0 && !methodNode.name.contains("init>")) transformMethod(classNode, methodNode);
        }

        return classNodeToBytes(classNode);
    }

    @Override
    public List<String> getVisitorTags() {
        return Collections.emptyList();
    }

    private void transformMethod(ClassNode classNode, MethodNode methodNode) {

        Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());
        try {
            analyzer.analyze(methodNode.name, methodNode);
        } catch (AnalyzerException e) {
            throw new RuntimeException();
        }

        InsnList insnList = new InsnList();
        LabelNode startLabel = new LabelNode();
        Package currentPackage = new Package();
        List<Package> packageList = new ArrayList<>();

        currentPackage.nodes.add(startLabel);
        int enterTryCatch = 0;

        for (AbstractInsnNode insnNode : methodNode.instructions) {
            if(insnNode instanceof InsnNode) {
                if(insnNode.getOpcode() == Opcodes.MONITORENTER) return;
            }
            if(enterTryCatch <= 0 && insnNode instanceof LabelNode) {
                currentPackage.nodes.add(new JumpInsnNode(Opcodes.GOTO, (LabelNode) insnNode));
                packageList.add(currentPackage);
                currentPackage = new Package();
                currentPackage.nodes.add(insnNode);
            } else {

                currentPackage.nodes.add(insnNode);
            }

            if(insnNode instanceof LabelNode) {
                if(methodNode.tryCatchBlocks.stream().anyMatch(tc -> tc.start == insnNode)) {
                    enterTryCatch++;
                } else if(methodNode.tryCatchBlocks.stream().anyMatch(tc -> tc.end == insnNode)) {
                    enterTryCatch--;
                }
            }
        }
        packageList.add(currentPackage);

        insnList.add(new JumpInsnNode(Opcodes.GOTO, startLabel));

        while(!packageList.isEmpty()) {
            Package p = packageList.remove(Utils.r.nextInt(packageList.size()));
            p.nodes.forEach(insnList::add);
        }
        methodNode.localVariables.clear();

        methodNode.instructions = insnList;
    }
}

class Package {
    List<AbstractInsnNode> nodes = new ArrayList<>();
}
