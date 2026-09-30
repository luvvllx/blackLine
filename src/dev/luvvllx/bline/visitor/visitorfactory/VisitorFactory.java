package dev.luvvllx.bline.visitor.visitorfactory;

import dev.luvvllx.bline.config.Config;
import dev.luvvllx.bline.interfaces.ICommandExecutor;
import dev.luvvllx.bline.interfaces.IVisitor;
import dev.luvvllx.bline.interfaces.IVisitorFactory;
import dev.luvvllx.bline.visitor.*;

public enum VisitorFactory implements IVisitorFactory, ICommandExecutor {

    LocalVar("localVarObf", "(false/true)", "obfuscate local variables; false drops them, true renames via -applymap, default false", LocalVarObfVisitor::new, LocalVarObfVisitor.class),
    ObjSubstCmd("objSubst", "", "rebase classes extending java.lang.Object onto a decoy supertype", ObjSubstVisitor::new, ObjSubstVisitor.class),
    LabelResorter("labelResorter", "", "shuffle code blocks; unstable, may cause VerifyError", LabelResorterObfVisitor::new, LabelResorterObfVisitor.class),
    FieldMethodResorter("fieldMethodResorter", "", "shuffle field and method order", FieldMethodResorterObfVisitor::new, FieldMethodResorterObfVisitor.class),
    SwitchCaseObfVisitor("junkSwitchCaseObf", "", "inject junk switch/case blocks", SwitchCaseObfVisitor::new, SwitchCaseObfVisitor.class),
    JunkTryCatchObf("junkTryCatchObf", "", "inject junk try/catch blocks", JunkTryCatchBlockObfVisitor::new, JunkTryCatchBlockObfVisitor.class),
    AntiDebugger("useAntiDebugger", "", "insert anti-debug checks", (bytes, args) -> { Config.useAntiDebugger = true; return new AntiDebuggerObfVisitor(bytes, args); }, AntiDebuggerObfVisitor.class),
    StringObf("stringObf", "(false/true)", "obfuscate strings; false = simple, true = throwable-backed decode, default false", (bytes, args) -> args.length > 0 && Boolean.parseBoolean(args[0]) ? new ThrowableStringObfVisitor(bytes, args) : new SimpleStringObfVisitor(bytes, args), SimpleStringObfVisitor.class),
    SimpleJunkLabelObf("junkThrowObf", "", "inject junk throw dead code", SimpleJunkLabelObfVisitor::new, SimpleJunkLabelObfVisitor.class),
    NumberObf("numberObf", "", "obfuscate numbers with heavy arithmetic", NumberObfVisitor::new, NumberObfVisitor.class),
    InvokeDynamicObf("invokeDynamicObf", "(false/true)", "route method calls through invokedynamic; false = simple, true = throwable-backed, default false", (bytes, args) -> args.length > 0 && Boolean.parseBoolean(args[0]) ? new ThrowableInvokeDynamicObfVisitor(bytes, args) : new InvokeDynamicObfVisitor(bytes, args), InvokeDynamicObfVisitor.class),
    SOFCrashVisitor("SOFCrasher", "", "try to blow up decompilers with a SOF bomb", SOFCrashVisitor::new, SOFCrashVisitor.class),
    MethodThrowableSignRemoverObfVisitor("removeMethodThrows", "", "strip throws clauses from method signatures", MethodThrowableSignRemoverObfVisitor::new, MethodThrowableSignRemoverObfVisitor.class),
    SyntheticFlag("syntheticFlag", "(false/true)", "mark members Synthetic/Bridge so some decompilers hide them", SyntheticBridgeApplyerObfVisitor::new, SyntheticBridgeApplyerObfVisitor.class),
    AttributeBreaker("attributeBreaker", "", "break assorted class attributes; may need -dontVerify", AttributeBreakerVisitor::new, AttributeBreakerVisitor.class),
    ;

    public interface Creator<T extends IVisitor> {
        public T createVisitor(byte[] bytes, String[] args);
    }

    private final Creator<? extends IVisitor> creator;
    private final String cmdName;
    private final String cmdArgs;
    private String[] cmdArgsArray = new String[0];
    private final String cmdDesc;
    private final Class<? extends IVisitor> visitorType;

    <T extends IVisitor> VisitorFactory(String cmdName, String cmdArgs, String cmdDesc, Creator<T> creator, Class<T> visitorType) {
        this.creator = creator;
        this.cmdName = cmdName;
        this.cmdArgs = cmdArgs;
        this.cmdDesc = cmdDesc;
        this.visitorType = visitorType;
    }

    @Override
    public IVisitor getVisitor(byte[] bytes) {
        return creator.createVisitor(bytes, cmdArgsArray);
    }

    @Override
    public byte[] transfer(byte[] bytes) {
        return getVisitor(bytes).transfer();
    }

    @Override
    public Class<? extends IVisitor> getVisitorType() {
        return visitorType;
    }

    @Override
    public int getOrder() {
        return ordinal();
    }

    @Override
    public String getCmdName() {
        return cmdName;
    }

    @Override
    public String getCmdArgs() {
        return cmdArgs;
    }

    @Override
    public String getCmdDesc() {
        return cmdDesc;
    }

    @Override
    public boolean execute(String[] args) {
        cmdArgsArray = args;
        VisitorManager.enableVisitor(this);
        return true;
    }

}
