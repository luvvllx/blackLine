package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.config.Config;
import dev.luvvllx.bline.interfaces.ICommandExecutor;

public class CommandUseLCMPNumber implements ICommandExecutor {
    @Override
    public String getCmdName() {
        return "useLCMPNumber";
    }

    @Override
    public String getCmdArgs() {
        return "";
    }

    @Override
    public String getCmdDesc() {
        return "use LCMP for some number comparisons";
    }

    @Override
    public boolean execute(String[] args) throws Exception {
        Config.useLCMPNumber = true;
        return true;
    }
}
