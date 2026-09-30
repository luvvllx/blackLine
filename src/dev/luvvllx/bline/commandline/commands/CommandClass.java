package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.interfaces.ICommandExecutor;

public abstract class CommandClass implements ICommandExecutor {
    @Override
    public String getCmdArgs() {
        return "<class1> [class2] [class3] ...";
    }

}
