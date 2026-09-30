package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.config.Config;
import dev.luvvllx.bline.interfaces.ICommandExecutor;

public class CommandSecurityManagerAntiDebugger implements ICommandExecutor {
    @Override
    public String getCmdName() {
        return "useSecurityManagerAntiDebugger";
    }

    @Override
    public String getCmdArgs() {
        return "";
    }

    @Override
    public String getCmdDesc() {
        return "insert SecurityManager based anti-debug";
    }

    @Override
    public boolean execute(String[] args) throws Exception {
        Config.useSecurityManagerAntiDebugger = true;
        return true;
    }
}
