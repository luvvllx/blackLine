package dev.luvvllx.bline.commandline.commands;

import org.objectweb.asm.Opcodes;
import dev.luvvllx.bline.config.Config;
import dev.luvvllx.bline.interfaces.ICommandExecutor;

public class CommandUseASM implements ICommandExecutor {
    @Override
    public String getCmdName() {
        return "asmVer";
    }

    @Override
    public String getCmdArgs() {
        return "<version>";
    }

    @Override
    public String getCmdDesc() {
        return "ASM API version to use, 4-10, default 5";
    }

    @Override
    public boolean execute(String[] args) throws Exception {
        int temp = Integer.parseInt(args[0]);
        int targetASM;
        switch(temp) {
            case 4:
                targetASM = Opcodes.ASM4;
                break;
            case 5:
                targetASM = Opcodes.ASM5;
                break;
            case 6:
                targetASM = Opcodes.ASM6;
                break;
            case 7:
                targetASM = Opcodes.ASM7;
                break;
            case 8:
                targetASM = Opcodes.ASM8;
                break;
            case 9:
                targetASM = Opcodes.ASM9;
                break;
            case 10:
                targetASM = Opcodes.ASM10_EXPERIMENTAL;
                break;
            default:
                throw new IllegalArgumentException("-asmVer wants an integer in 4..10, got: " + args[0]);
        }
        Config.AsmApi = targetASM;
        return true;
    }
}
