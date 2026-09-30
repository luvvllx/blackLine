package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.Main;
import dev.luvvllx.bline.interfaces.ICommandExecutor;

public class CommandHelp implements ICommandExecutor {
    @Override
    public String getCmdName() {
        return "?";
    }

    @Override
    public String getCmdArgs() {
        return "";
    }

    @Override
    public String getCmdDesc() {
        return "show this help";
    }

    @Override
    public boolean execute(String[] args) {
        Main.printHelp();
        return true;
    }
}
