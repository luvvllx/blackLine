package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.config.Config;
import dev.luvvllx.bline.interfaces.ICommandExecutor;

public class CommandNoVerify implements ICommandExecutor {
    @Override
    public String getCmdName() {
        return "noVerify";
    }

    @Override
    public String getCmdArgs() {
        return "";
    }

    @Override
    public String getCmdDesc() {
        return "skip bytecode verification after obfuscation (broken code still throws VerifyError without -noverify)";
    }

    @Override
    public boolean execute(String[] args) throws Exception {
        Config.dontverify = true;
        return true;
    }
}
