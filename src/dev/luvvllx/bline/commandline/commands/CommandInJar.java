package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.Main;
import dev.luvvllx.bline.config.Config;

import java.io.File;
import java.io.FileNotFoundException;

public class CommandInJar extends CommandFile {
    @Override
    public String getCmdName() {
        return "inJar";
    }

    @Override
    public String getCmdDesc() {
        return "set input jar";
    }

    @Override
    public boolean execute(String[] args) throws Exception {
        File temp = Config.inputFile = new File(args[0]);
        if (!temp.exists()) {
            throw new FileNotFoundException("file does not exist");
        }
        Main.loader.addURL(temp.toURI().toURL());
        return true;
    }
}
