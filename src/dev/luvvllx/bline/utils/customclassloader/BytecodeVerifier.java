package dev.luvvllx.bline.utils.customclassloader;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.*;
import dev.luvvllx.runtime.Ex0;
import dev.luvvllx.bline.config.Config;
import dev.luvvllx.bline.visitor.AbstractVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

public class BytecodeVerifier {

    public final static LineURLClassLoader LegacyLoader;
    private static Method legacyVerify;

    static {
        try {
            File tempfile = File.createTempFile("asm503legacy", ".jar");
            tempfile.deleteOnExit();

            try (InputStream is = Class.forName("dev.luvvllx.runtime.B1").getResourceAsStream("/asm503legacy.jar");
                 FileOutputStream fos = new FileOutputStream(tempfile, false)) {

                if (is == null) {
                    throw new RuntimeException();
                }

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }

                fos.flush();
            }

            if (tempfile.length() == 0) {
                throw new RuntimeException();
            }

            LegacyLoader = new LineURLClassLoader(new URL[] {tempfile.toURI().toURL()}, null);

        } catch (Throwable e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException();
        }
    }

    public static void verify(byte[] bytes) {
        verify(bytes, false);
    }

    public static void verify(byte[] bytes, boolean useOldASM) {

        if(legacyVerify == null) {
            try {
                legacyVerify = Class.forName("Verifyer", false, LegacyLoader).getMethod("verify", byte[].class);
            } catch (ClassNotFoundException | NoSuchMethodException e) {

            }
        }
        if(Config.dontverify) {
            return;
        }

        try {
            if(useOldASM && Config.verifyWith508ASM) legacyVerify.invoke(null, (Object) bytes);
        } catch (IllegalAccessException | InvocationTargetException e) {
            a:{
                Throwable t = e;
                while ((t = t.getCause()) != null) {
                    if(t.toString().contains("java.lang.ClassNotFoundException")) break a;
                }

                throw new RuntimeException();
            }
        }
        try(DefiningClassLoader classLoader = new DefiningClassLoader()) {
            ClassNode classNode = AbstractVisitor.byteToClassNode(bytes);

            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicVerifier());
            for (MethodNode method : classNode.methods) {
                analyzer.analyze(classNode.name, method);
            }

            Class<?> cla$$ = classLoader.define(bytes);
            cla$$.getDeclaredMethods();

        } catch(AnalyzerException e) {
            throw new RuntimeException();
        } catch(Throwable e) {
            if(Config.dontverify) {
                return;
            }
            if(e instanceof ClassFormatError || e instanceof VerifyError) {
                if(e.getLocalizedMessage().startsWith("Class file version does not support constant tag")) throw new Error("try -fixVersion to work around this", e);
                throw (Error)e;
            }
            if(e instanceof Ex0 || e instanceof IndexOutOfBoundsException) {
                throw new RuntimeException();
            }
        }
    }

}
