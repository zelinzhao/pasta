package dsu.pasta.javassist;

import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMember;
import javassist.CtMethod;

public class Call {
    ///////////////////////
    public static final String FIELD = "FIELD";
    public static final String METHOD = "METHOD";
    public static final String CONSTRUCTOR = "CONSTRUCTOR";
    public static final String SEP = "@";
    public static final String CALL = "->";

    ///////////////////////
    protected String callStr;
    protected String callerStr;
    protected String calleeStr;
    protected CtMember callee;
    private CtMember caller;

    public Call(CtMember caller, CtMember callee) {
        this.callerStr = getId(caller);
        this.calleeStr = getId(callee);
        this.caller = caller;
        this.callee = callee;
        this.callStr = this.callerStr + CALL + this.calleeStr;
    }

    /**
     * @param cm
     * @return @KIND@DECLARING TYPE@NAME@SIGNATURE@
     */
    public static String getId(CtMember cm) {
        String result = SEP;
        if (cm instanceof CtField) {
            CtField cf = (CtField) cm;
            result += FIELD + SEP;
        } else if (cm instanceof CtMethod) {
            result += METHOD + SEP;
        } else if (cm instanceof CtConstructor) {
            result += CONSTRUCTOR + SEP;
        }
        result += cm.getDeclaringClass().getName() + SEP;
        result += cm.getName() + SEP;
        result += cm.getSignature() + SEP;
        return result;
    }

    @Override
    public int hashCode() {
        return callStr.hashCode();
    }

    @Override
    public boolean equals(Object c) {
        if (c == null)
            return false;
        Call t = (Call) c;
        return this.callStr.equals(t.callStr);
    }

    @Override
    public String toString() {
        return callStr;
    }
}
