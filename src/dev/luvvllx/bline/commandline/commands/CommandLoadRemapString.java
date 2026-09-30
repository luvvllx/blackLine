package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.runtime.Ex0;
import dev.luvvllx.bline.Main;
import dev.luvvllx.bline.config.Config;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class CommandLoadRemapString extends CommandFile {
    @Override
    public String getCmdName() {
        return "applymap";
    }

    @Override
    public String getCmdDesc() {
        return "load custom member name map file";
    }

    @Override
    public boolean execute(String[] args) throws Exception {

        try {

            File mapFolder = new File(Main.directionary, "maps");
            if(!mapFolder.exists()) mapFolder.mkdirs();

            File target = new File(args[0]);
            if(!target.exists()) target = new File(mapFolder, args[0]);
            if(!target.exists()) {
                File f = new File(mapFolder, "maps");
                f.createNewFile();
                throw new RuntimeException();
            }
            Config.remapStrings = readAndParseToMap(String.join("\n", Files.readAllLines(target.toPath()).toArray(new String[0])));
        } catch (UnsupportedEncodingException e) {

            throw new AssertionError(e);
        }

        return true;
    }

    public static String[] readAndParseToMap(String s) {
        char[] chars = s.toCharArray();

        Set<String> names = new HashSet<>();
        StringBuilder buffer = new StringBuilder();

        for(char c : chars) {
            boolean flag = buffer.length() == 0 ? Character.isJavaIdentifierStart(c) : Character.isJavaIdentifierPart(c);
            if(flag && c != '\n' && c != '\r') {
                buffer.append(c);
            } else if(buffer.length() > 0) {
                names.add(buffer.toString());
                buffer.setLength(0);
            }
        }
        return names.toArray(new String[0]);
    }
}
