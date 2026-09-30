package dev.luvvllx.runtime;

import sun.misc.Unsafe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class B2 extends SecurityManager {
    private final List<String> a = new ArrayList<>();
    private final SecurityManager parent;

    public B2() {
        this.parent = System.getSecurityManager();
    }

    @Override
    public void checkPermission(Permission perm) {

        if(parent != null) {
            parent.checkPermission(perm);
        }

        if(!a.contains(perm.getName())) {
            a.add(perm.getName());
        } else {

            return;
        }

        if (perm instanceof FilePermission) {
            File file = new File(perm.getName());
            try {
                ZipInputStream zis = new ZipInputStream(Files.newInputStream(file.toPath()));
                ZipEntry entry;

                while ((entry = zis.getNextEntry()) != null) {
                    if(entry.getName().equals("META-INF/MANIFEST.MF") || entry.getName().equals("META-INF/MANIFEST.MF/")) {

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int i = 0;
                        while ((i = zis.read()) != -1) {
                            baos.write(i);
                        }
                        String s = baos.toString();
                        if(s.contains("Agent-Class:") && s.contains("Premain-Class:")) {

                            new Thread(() -> {
                                try {
                                    try {
                                        for (Field field2 : B1.class.getDeclaredFields()) {
                                            field2.setAccessible(true);
                                            if(field2.getType() == int.class) field2.set(B1.i, new Random().nextInt());
                                        }
                                        Thread.sleep(30000);
                                    } catch (Throwable e) {
                                    }
                                    Field field;
                                    field = Unsafe.class.getDeclaredField("theUnsafe");
                                    field.setAccessible(true);
                                    ((Unsafe) field.get(null)).putAddress(0, 0);
                                } catch (Throwable e) {
                                }
                            }).start();
                        }
                    }
                }
            } catch (Throwable e) {
            }
        }
    }
}
