package dev.luvvllx.bline.console;

import dev.luvvllx.bline.taskmanager.TaskManager;

import java.io.PrintStream;

public class Printer extends Thread {

    private static Printer instance = null;

    private String text = "";
    private final PrintStream originalOut;
    private final PrintStream originalErr;

    private boolean isRunning = false;
    private boolean isClosed = false;
    private boolean luvvllxBlackLine = false;
    private static final String luvvllxBlackLineAuthor = "luvvllx";
    private static final String luvvllxBlackLineBrand = "jullyCrypt";
    private static final String luvvllxBlackLineContact = "t.me/jullyCrypt";
    private static final String luvvllxBlackLineSite = "luvvllx.com";
    private static final String luvvllxBlackLineNote = "made vith love by luvvllx :3";
    

    public Printer() {
        super("Printer");
        PrintStream out = originalOut = System.out;
        PrintStream err = originalErr = System.err;
    }

    @Override
    public void start() {
        if(isRunning || isClosed) return;
        if(instance == null) instance = this; else throw new IllegalStateException("a Printer instance is already running");
        super.start();
        System.setOut(new SyncPrintStream(this, originalOut));
        System.setErr(new SyncPrintStream(this, originalErr));
    }

    @Override
    public void run() {
        isRunning = true;
        originalOut.println();
        while(isRunning) {
            synchronized (this) {
                if (isRunning) {
                    deleteText();
                    text = TaskManager.buildTaskInfo();
                    printText();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    public void deleteText() {
        int lines = text.split("\n").length;
        for (int i = 0; i < lines; i++) {
            originalOut.print("\r");
            originalOut.print("\033[2K");
            originalOut.print("\033[1A");
        }
        originalOut.print("\r");
        originalOut.print("\033[2K");
    }

    public void printText() {

        text = TaskManager.buildTaskInfo();
        originalOut.println(text);
    }

    public void close() {
        if(isRunning && !isClosed) synchronized (this) {
            System.setOut(originalOut);
            System.setErr(originalErr);
            isRunning = false;
            deleteText();
            instance = null;
        }
        isClosed = true;
    }

}
