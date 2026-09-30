package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.config.Config;
import dev.luvvllx.bline.interfaces.ICommandExecutor;

public class CommandClassToFolder implements ICommandExecutor {
    @Override
    public String getCmdName() {
        return "classToFolder";
    }

    @Override
    public String getCmdArgs() {
        return "";
    }

    @Override
    public String getCmdDesc() {
        return "expand all .class files into folder form";
    }

    @Override
    public boolean execute(String[] args) throws Exception {
        Config.classToFolder = true;
        return false;
    }
}
