package dev.luvvllx.bline;

import org.objectweb.asm.ClassReader;
import dev.luvvllx.runtime.Ex0;
import dev.luvvllx.bline.commandline.CommandHandler;
import dev.luvvllx.bline.config.Config;
import dev.luvvllx.bline.console.Printer;
import dev.luvvllx.bline.interfaces.ICommandExecutor;
import dev.luvvllx.bline.interfaces.IVisitor;
import dev.luvvllx.bline.interfaces.IVisitorFactory;
import dev.luvvllx.bline.taskmanager.Task;
import dev.luvvllx.bline.taskmanager.TaskManager;
import dev.luvvllx.bline.utils.Utils;
import dev.luvvllx.bline.utils.customclassloader.LineURLClassLoader;
import dev.luvvllx.bline.visitor.*;
import dev.luvvllx.bline.visitor.visitorfactory.OverrideVisitorFactory;
import dev.luvvllx.bline.visitor.visitorfactory.VisitorFactory;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Main {

    public static LineURLClassLoader loader;
    private static final Queue<Map.Entry<ZipEntry, byte[]>> writeQueue = new ConcurrentLinkedQueue<>();
    private static final List<AbstractVisitor.TransferResult> transferResults = Collections.synchronizedList(new ArrayList<>());

    public static final File directionary;

    static {
        try {
            File thisFile = new File(URLDecoder.decode(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8"));
            directionary = thisFile.getParentFile();
            loader = new LineURLClassLoader(new URL[0], Main.class.getClassLoader());
            loader.addURL(thisFile.toURI().toURL());
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public static final String version = "1.0.0"; // i need moreee updates

    public static void main(String[] args) throws Throwable {

        Printer printer = new Printer();

        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            printer.close();
            System.err.println("obfuscator failed :(");
            e.printStackTrace();
            System.exit(1);
        });

        CommandHandler.executeCommand(args);

        if(Config.inputFile == null || Config.outputFile == null) {
            System.out.println("\nno input or output jar given\n");
            printHelp();
            System.exit(0);
        }

        ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(Config.inputFile.toPath()));
        FileOutputStream fileOutputStream = new FileOutputStream(Config.outputFile);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { zipOutputStream.close(); } catch (IOException e) { e.printStackTrace(); }
            try { fileOutputStream.close(); } catch (IOException e) {  e.printStackTrace();  }
            try { zipInputStream.close(); } catch (IOException e) {  e.printStackTrace();  }
        }));

        printer.start();

        AbstractVisitor.initOrder();

        IVisitorFactory[] factories = {
                new IVisitorFactory() {
                    @Override
                    public IVisitor getVisitor(byte[] bytes) {
                        return new MarkerStringVisitor(bytes, new String[0]);
                    }

                    @Override
                    public byte[] transfer(byte[] bytes) {
                        return getVisitor(bytes).transfer();
                    }

                    @Override
                    public Class<? extends IVisitor> getVisitorType() {
                        return MarkerStringVisitor.class;
                    }

                    @Override
                    public int getOrder(){
                        return 0;
                    } 
                },
                new IVisitorFactory() {
                    @Override
                    public IVisitor getVisitor(byte[] bytes) {
                        return new BlobSerializableVisitor(bytes, new String[0]);
                    }

                    @Override
                    public byte[] transfer(byte[] bytes) {
                        return getVisitor(bytes).transfer();
                    }

                    @Override
                    public Class<? extends IVisitor> getVisitorType() {
                         return BlobSerializableVisitor.class;
                    }

                    @Override
                    public int getOrder() {return 0;
                    }
                },
                VisitorFactory.LocalVar,
                new OverrideVisitorFactory(VisitorFactory.StringObf) {
                    @Override
                    public IVisitor getVisitor(byte[] bytes) {
                        return new ThrowableStringObfVisitor(bytes, new String[] {"true"});
                    }
                },
                VisitorFactory.NumberObf,VisitorFactory.JunkTryCatchObf,VisitorFactory.SimpleJunkLabelObf, VisitorFactory.SwitchCaseObfVisitor,
                VisitorFactory.FieldMethodResorter,VisitorFactory.SyntheticFlag,

        };
        IVisitorFactory[] emptyFactory = {};

        writeToOutput(Class.forName("dev.luvvllx.runtime.B1"), factories);
        writeToOutput(Class.forName("dev.luvvllx.runtime.B2"), factories);
        writeToOutput(Class.forName("dev.luvvllx.runtime.Ann"), emptyFactory);
        writeToOutput(Ex0.class, emptyFactory);

        ZipEntry zipEntry;
        byte[] buffer = new byte[1024];

        Config.outputFile.createNewFile();

        AtomicInteger totalClass = new AtomicInteger(0);
        AtomicInteger totalObfClass = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        while((zipEntry = zipInputStream.getNextEntry()) != null) {
            int len;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while((len = zipInputStream.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            byte[] bytes = baos.toByteArray();

            ZipEntry finalZipEntry = zipEntry;
            TaskManager.addTask(new Task(() -> {
                if(finalZipEntry.getName().endsWith(".class")) {
                    totalClass.incrementAndGet();
                    AbstractVisitor.TransferResult transferResult = AbstractVisitor.transferClass(bytes);
                    transferResults.add(transferResult);
                    if(transferResult.bytes != bytes) {
                        totalObfClass.incrementAndGet();
                    }
                    writeQueue.add(new AbstractMap.SimpleEntry<>(finalZipEntry, transferResult.bytes));
                } else {
                    writeQueue.add(new AbstractMap.SimpleEntry<>(finalZipEntry, bytes));
                }
            }, zipEntry.getName()));

        }

        long lastTime = System.currentTimeMillis();
        while(true) {
            if(TaskManager.getTaskCount() > 0) {
                lastTime = System.currentTimeMillis();
            }
            Map.Entry<ZipEntry, byte[]> entry = writeQueue.poll();
            if(entry == null) {
                if(System.currentTimeMillis() - lastTime > 100) {
                    break;
                }
                continue;
            }
            ZipEntry entryKey = new ZipEntry(entry.getKey().getName() + (Config.classToFolder && entry.getKey().getName().endsWith(".class") ? "/" : ""));
            entryKey.setTime(Long.MAX_VALUE);
            System.out.println("wrote: " + entryKey.getName());

            try {
                zipOutputStream.putNextEntry(entryKey);
            } catch(java.util.zip.ZipException e) {
                if(e.getMessage().contains("duplicate entry")) continue;
                throw e;
            }
            zipOutputStream.write(entry.getValue());
            zipOutputStream.closeEntry();
        }
        for (AbstractVisitor.TransferResult transferResult : transferResults) {
            for (Map.Entry<String, Throwable> exception : Objects.requireNonNull(transferResult.exceptions)) {
                System.err.println("[!] failed in " + transferResult.className + " on " + exception.getKey() + ": error");
                exception.getValue().printStackTrace();
                exceptionCount.incrementAndGet();
            }
        }
        if(AbstractVisitor.getIncludeClasses().isEmpty()) System.out.println("no class matched; try -inClass (.*) to catch everything");
        System.out.println("read " + totalClass.get() + " classes, obfuscated " + totalObfClass.get() + " classes, caught " + exceptionCount.get() + " exceptions");

        System.out.println();
        System.out.println();

        System.exit(0);
    }

    private static ZipEntry getZipEntry(ClassReader thisReader) {
        return new ZipEntry(thisReader.getClassName() + ".class");
    }

    public static void printHelp() {
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println(".");
        System.out.println("flags:");
        System.out.println();
        int maxLength = CommandHandler.commands.stream().mapToInt(command -> command.getCmdName().length() + command.getCmdArgs().length() + 1).max().getAsInt();
        for (ICommandExecutor command : CommandHandler.commands) {
            System.out.print("  -");
            String cmdName = command.getCmdName();
            System.out.print(cmdName);
            System.out.print(" ");
            String cmdDesc = command.getCmdArgs();
            System.out.print(cmdDesc);
            for (int i = 0; i < maxLength - cmdName.length() - cmdDesc.length(); i++) {
                System.out.print(" ");
            }
            System.out.print("| ");
            System.out.println(command.getCmdDesc());
        }
        System.out.println();
    }

    private static void test() {

        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(TaskManager.addTask(new Task(() -> {
                try {
                    Thread.sleep(Utils.r.nextInt(1000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, "" + i)));
        }
        for (Task task : tasks) {
            try {
                task.await();
                System.out.println("task " + task.getTaskName() + " done");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeToOutput(Class<?> clazz) {
        writeToOutput(clazz, new IVisitorFactory[0]);
    }

    public static void writeToOutput(Class<?> clazz, IVisitorFactory... factories) {
        try {
            writeToOutput(new ClassReader(Objects.requireNonNull(clazz.getResourceAsStream(clazz.getSimpleName() + ".class"))), factories);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public static void writeToOutput(ClassReader cr) {
        writeToOutput(cr, new IVisitorFactory[0]);
    }

    public static void writeToOutput(ClassReader thisReader, IVisitorFactory... factories) {
        AbstractVisitor.TransferResult transferResult = AbstractVisitor.transferClassWithFactories(thisReader.b, factories);
        transferResults.add(transferResult);
        writeQueue.add(new AbstractMap.SimpleEntry<>(getZipEntry(thisReader), transferResult.bytes));
    }
//yes im forked this buttt, i added VM and mutch more transformations
    public static void writeToOutput(ZipEntry zipEntry, byte[] bytes) {
        writeQueue.add(new AbstractMap.SimpleEntry<>(zipEntry, bytes));
    }

}
