package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.config.Config;

import java.io.File;
import java.io.FileNotFoundException;

public class CommandOutJar extends CommandFile {
    @Override
    public String getCmdName() {
        return "outJar";
    }

    @Override
    public String getCmdDesc() {
        return "set output jar";
    }

    @Override
    public boolean execute(String[] args) throws Exception {
        Config.outputFile = new File(args[0]);
        return true;
    }
}
