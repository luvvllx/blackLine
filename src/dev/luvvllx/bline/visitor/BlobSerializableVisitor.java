package dev.luvvllx.bline.visitor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import dev.luvvllx.bline.Main;
import dev.luvvllx.bline.config.Config;
import dev.luvvllx.bline.utils.ClassWriter1;
import dev.luvvllx.bline.utils.Utils;
import dev.luvvllx.bline.utils.customclassloader.LineURLClassLoader;
import dev.luvvllx.bline.visitor.visitorfactory.VisitorFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;

public class BlobSerializableVisitor extends AbstractVisitor {

    public volatile static BlobSerializableVisitor INSTANCE;

    public String totalHashedSerializedFieldName;
    public final Map<String, FieldType> blobFieldTypes = new HashMap<>();
    public final Map<String, Integer> blobHashIndex = new HashMap<>();
    public int totalSerializedHash;
    public byte[] xsdSerializedClassBytes;
    public String[] blobFieldNames;

    public BlobSerializableVisitor(byte[] bytes, String[] args) {
        super(bytes, args);
    }

    @Override
    public byte[] transfer(byte[] classData) {

        ClassNode classNode = byteToClassNode(classData);

        if(!classNode.name.contains("dev/luvvllx/runtime/B1")) return classData;

        INSTANCE = this;

        if(!Config.useAntiDebugger && !VisitorManager.isVisitorEnabled(VisitorFactory.AntiDebugger)) {
            classNode.methods.removeIf(method -> method.name.equals("init") || method.name.equals("init2") || method.name.contains("lambda$init"));
            return classNodeToBytes(classNode);
        }

        if(!Config.useSecurityManagerAntiDebugger) classNode.methods.removeIf(method -> method.name.equals("init2"));

        for (int i = 0; i < 100; i++) {
            FieldType type = FieldType.values()[Utils.r.nextInt(99) % FieldType.values().length];

            String fieldName;
            switch (type) {
                case STRING:
                    classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, fieldName = Utils.spawnRandomChar(10, false) + "b9f", "Ljava/lang/String;", null, null));
                    break;
                case INT:
                    classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, fieldName = Utils.spawnRandomChar(10, false) + "b9f", "I", null, null));
                    break;
                default:
                    throw new RuntimeException();
            }

            blobFieldTypes.put(fieldName, type);
        }

        totalHashedSerializedFieldName = Utils.spawnRandomChar(10, false) + "b9f";

        classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_TRANSIENT, totalHashedSerializedFieldName, "I", null, null));

        a:for (MethodNode method : classNode.methods) {
            if("init".equals(method.name)) {
                for (AbstractInsnNode insn : method.instructions) {
                    if(insn instanceof LdcInsnNode) {
                        LdcInsnNode ldc = (LdcInsnNode) insn;
                        if("unusedObject".equals(ldc.cst)) {
                            ldc.cst = totalHashedSerializedFieldName;
                            break a;
                        }
                    }
                }
            }
        }

        byte[] bytes = classNodeToBytes(classNode);

        try(LineURLClassLoader loader = new LineURLClassLoader(new URL[0], BlobSerializableVisitor.class.getClassLoader())) {
            Class<?> xsdClass = loader.defineClass(bytes);
            Object xsd = xsdClass.newInstance();

            Field[] fields = xsd.getClass().getDeclaredFields();

            for (Field field : fields) {
                if(!field.getName().endsWith("b9f")) continue;
                if(field.getType() == int.class) field.set(xsd, Utils.r.nextInt());
                if(field.getType() == String.class) field.set(xsd, Utils.spawnRandomChar(10, false));
            }

            int hash = 0;

            int i = 0;
            for (Field field : fields) {
                if(!totalHashedSerializedFieldName.equals(field.getName()) && field.getName().endsWith("b9f")) {
                    int hash1 = field.get(xsd).hashCode();
                    hash ^= hash1;
                    i++;
                    blobHashIndex.put(field.getName(), hash1);
                }
            }

            totalSerializedHash = hash;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(xsd);
            byte[] bytes1 = baos.toByteArray();
            for (int j = 0; j < bytes1.length; j++) {
                bytes1[j] ^= (byte)(new Random(j).nextInt(256));
            }
            xsdSerializedClassBytes = bytes1;
            Main.writeToOutput(new ZipEntry("b.blob"), bytes1);

        } catch (IOException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException();
        }

        blobFieldTypes.put(totalHashedSerializedFieldName, FieldType.INT);
        blobHashIndex.put(totalHashedSerializedFieldName, totalSerializedHash);

        blobFieldNames = blobHashIndex.keySet().toArray(new String[0]);

        return bytes;
    }

    @Override
    public List<String> getVisitorTags() {
        return Collections.emptyList();
    }

    public enum FieldType {
        STRING("Ljava/lang/String;"),
        INT("I");

        public final String desc;

        FieldType(String desc) {
            this.desc = desc;
        }
    }

}
