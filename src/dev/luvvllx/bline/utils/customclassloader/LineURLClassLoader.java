package dev.luvvllx.bline.utils.customclassloader;

import dev.luvvllx.bline.Main;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Objects;

public class LineURLClassLoader extends URLClassLoader {

    public static final File libFolder = new File(Main.directionary, "lib");

    public static final URL[] libURLs;

    static {
        try {
            if (!libFolder.exists()) {
                libFolder.mkdirs();
            }
            libURLs = Arrays.stream(Objects.requireNonNull(libFolder.listFiles())).filter(f -> f.getAbsolutePath().endsWith(".jar")).map(File::toURI).map(i -> {
                try {
                    return i.toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException();
                }
            }).toArray(URL[]::new);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
    public LineURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);

        for (URL libURL : libURLs) {
            addURL(libURL);
        }
    }

    @Override
    public void addURL(URL url) {

        super.addURL(url);
    }

    public Class<?> defineClass(byte[] b) {
        return super.defineClass(b, 0, b.length);
    }

}
