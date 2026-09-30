package dev.luvvllx.bline.commandline.commands;

import dev.luvvllx.bline.visitor.AbstractVisitor;

import java.util.Arrays;

public class CommandExcludeClass extends CommandClass {
    @Override
    public String getCmdName() {
        return "exClass";
    }

    @Override
    public String getCmdDesc() {
        return "classes to exclude from inClass, regex matched until the next -flag";
    }

    @Override
    public boolean execute(String[] args) {
        AbstractVisitor.loadExcludeClasses(Arrays.asList(args));
        return true;
    }
}
