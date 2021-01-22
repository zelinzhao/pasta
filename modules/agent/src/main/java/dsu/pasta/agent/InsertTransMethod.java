package dsu.pasta.agent;

import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class InsertTransMethod {
    private static final String TARGET_CLASS = "TARGET_CLASS";
    private static final String METHOD_BODY = "METHOD_BODY";

    private static final String METHOD_NAME = "DsuTransformDump";
    private static String targetClass;
    private static String methodBody;

    static {
        String target = System.getenv(TARGET_CLASS);
        String body = System.getenv(METHOD_BODY);
        try {
            if (target == null) {
                throw (new Exception("No target class for instrumentation agent."));
            }
            if (body == null) {
                throw (new Exception("No synthesis method body for instrumentation agent"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        targetClass = target;
        methodBody = body;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        ClassPool cp = ClassPool.getDefault();
        cp.importPackage("dsu.pasta.object.processor.ObjectApi");

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {

                String dotClassName = className.replaceAll("\\/", ".");
                byte[] code = null;
                if (!dotClassName.equals(targetClass))
                    return classfileBuffer;
                try {
                    CtClass cc = cp.get(dotClassName);
                    code = InsertMethod(cc);
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
                if (code == null)
                    return classfileBuffer;
                else
                    return code;
            }
        });
    }

    public static CtMethod CreateMethod(CtClass cc) {
        CtMethod m = null;
        try {
            m = CtNewMethod.make("public void " + METHOD_NAME + "(){" + methodBody + "}", cc);
        } catch (CannotCompileException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return m;
    }

    public static byte[] InsertMethod(CtClass cc) {
        if (cc.isInterface()) {
            return null;
        }
        byte[] code = null;
        CtMethod m = CreateMethod(cc);
        if (m == null) {
            System.err.print("Can't compile synthesis method. Not instrument.");
        } else {
            try {
                cc.addMethod(m);
                code = cc.toBytecode();
            } catch (CannotCompileException | IOException e) {
                e.printStackTrace();
                System.exit(-1);

            }
        }
        cc.defrost();
        return code;
    }
}
