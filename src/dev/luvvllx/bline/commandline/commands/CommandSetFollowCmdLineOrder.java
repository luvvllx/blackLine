package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.config.Config;
import dev.luvvllx.bline.interfaces.ICommandExecutor;

public class CommandSetFollowCmdLineOrder implements ICommandExecutor {
    @Override
    public String getCmdName() {
        return "visitorFollowCmdLineOrder";
    }

    @Override
    public String getCmdArgs() {
        return "";
    }

    @Override
    public String getCmdDesc() {
        return "apply transformers in command-line order (default order otherwise; may trigger VerifyError)";
    }

    @Override
    public boolean execute(String[] args) throws Exception {
        Config.visitorFollowCmdLineOrder = true;
        return true;
    }
}
