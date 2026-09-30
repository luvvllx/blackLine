package dev.luvvllx.bline.utils.customclassloader;

import dev.luvvllx.bline.Main;

import java.net.URLClassLoader;

public class DefiningClassLoader extends URLClassLoader {
    public DefiningClassLoader() {
        super(Main.loader.getURLs(), null);
    }

    public Class<?> define(byte[] b) {
        return super.defineClass(b, 0, b.length);
    }
}
