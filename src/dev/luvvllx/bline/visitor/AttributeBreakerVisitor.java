package dev.luvvllx.bline.visitor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import dev.luvvllx.bline.utils.Utils;

import java.util.Collections;
import java.util.List;

public class AttributeBreakerVisitor extends AbstractVisitor {

    public AttributeBreakerVisitor(byte[] bytes, String[] args) {
        super(bytes, args);
    }

    @Override
    public byte[] transfer(byte[] bytes) {

        int start = findClassAttributesCountOffset(bytes);

        bytes[start] = (byte) 0;
        bytes[start + 1] = (byte) 1;

        return bytes;
    }

    @Override
    public List<String> getVisitorTags() {
        return Collections.emptyList();
    }

    public static int findClassAttributesCountOffset(byte[] classBytes) {
        int offset = 8;

        int constantPoolCount = readUnsignedShort(classBytes, offset);
        offset += 2;
        for (int i = 1; i < constantPoolCount; i++) {
            int tag = classBytes[offset] & 0xFF;
            switch (tag) {
                case 1:
                    int len = readUnsignedShort(classBytes, offset + 1);
                    offset += 3 + len;
                    break;
                case 3: case 4:
                    offset += 5;
                    break;
                case 5: case 6:
                    offset += 9;
                    i++;
                    break;
                case 7: case 8: case 16: case 19: case 20:
                    offset += 3;
                    break;
                case 9: case 10: case 11: case 12: case 17: case 18:
                    offset += 5;
                    break;
                case 15:
                    offset += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown constant pool tag: " + tag);
            }
        }

        offset += 6;

        int interfacesCount = readUnsignedShort(classBytes, offset);
        offset += 2 + interfacesCount * 2;

        int fieldsCount = readUnsignedShort(classBytes, offset);
        offset += 2;
        for (int i = 0; i < fieldsCount; i++) {
            offset += 6;
            int attributesCount = readUnsignedShort(classBytes, offset);
            offset += 2;
            for (int j = 0; j < attributesCount; j++) {
                offset += 2;
                int attributeLength = readInt(classBytes, offset);
                offset += 4 + attributeLength;
            }
        }

        int methodsCount = readUnsignedShort(classBytes, offset);
        offset += 2;
        for (int i = 0; i < methodsCount; i++) {
            offset += 6;
            int attributesCount = readUnsignedShort(classBytes, offset);
            offset += 2;
            for (int j = 0; j < attributesCount; j++) {
                offset += 2;
                int attributeLength = readInt(classBytes, offset);
                offset += 4 + attributeLength;
            }
        }

        return offset;
    }

    private static int readUnsignedShort(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

}
