package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.interfaces.ICommandExecutor;

public abstract class CommandFile implements ICommandExecutor {
    @Override
    public String getCmdArgs() {
        return "<file>";
    }
}
