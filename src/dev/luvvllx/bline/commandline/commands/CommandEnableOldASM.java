package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.config.Config;
import dev.luvvllx.bline.interfaces.ICommandExecutor;

public class CommandEnableOldASM implements ICommandExecutor {
    @Override
    public String getCmdName() {
        return "verifyWith508ASM";
    }

    @Override
    public String getCmdArgs() {
        return "";
    }

    @Override
    public String getCmdDesc() {
        return "verify bytecode with ASM 5.0.8, for Minecraft Forge 1.8.9 mods";
    }

    @Override
    public boolean execute(String[] args) throws Exception {
        Config.verifyWith508ASM = true;
        return true;
    }
}
