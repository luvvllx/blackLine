package dev.luvvllx.bline.interfaces;

public interface ICommandExecutor {

    public String getCmdName();
    public String getCmdArgs();
    public String getCmdDesc();
    public boolean execute(String[] args) throws Exception;

}
