package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.visitor.AbstractVisitor;

import java.util.Arrays;

public class CommandIncludeClass extends CommandClass {
    @Override
    public String getCmdName() {
        return "inClass";
    }

    @Override
    public String getCmdDesc() {
        return "classes to obfuscate, regex matched until the next -flag";
    }

    @Override
    public boolean execute(String[] args) {
        AbstractVisitor.loadIncludeClasses(Arrays.asList(args));
        return true;
    }
}
