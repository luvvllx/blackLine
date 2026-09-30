package dev.luvvllx.bline.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import dev.luvvllx.bline.Main;
import dev.luvvllx.bline.utils.customclassloader.LineURLClassLoader;

public class ClassWriter1 extends ClassWriter {

    public ClassWriter1(int flags) {
        super(flags);
    }

    public ClassWriter1(ClassReader var1, int var2) {
        super(var1, var2);

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected String getCommonSuperClass(String var1, String var2) {

        Class var4;
        Class var5;
        try {

            var4 = Main.loader.loadClass(var1.replace('/', '.'));
        } catch (ClassNotFoundException var7) {

            if(var7 instanceof ClassNotFoundException) {

                throw new RuntimeException();
            }

            throw null;
        }
        try {

            var5 = Main.loader.loadClass(var2.replace('/', '.'));
        } catch (ClassNotFoundException var7) {

            if(var7 instanceof ClassNotFoundException) {

                throw new RuntimeException();
            }

            throw null;
        }

        if (var4.isAssignableFrom(var5)) {
            return var1;
        } else if (var5.isAssignableFrom(var4)) {
            return var2;
        } else if (!var4.isInterface() && !var5.isInterface()) {
            do {
                var4 = var4.getSuperclass();
            } while(!var4.isAssignableFrom(var5));

            return var4.getName().replace('.', '/');
        } else {
            return "java/lang/Object";
        }
    }

    protected ClassLoader getClassLoader() {
        return Main.loader;
    }

    private byte[] getClass(String name) {

        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, name, null, "java/lang/Object", null);

        return cw.toByteArray();

    }

}
