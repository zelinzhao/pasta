package dsu.pasta.javassist;

import dsu.pasta.config.ProjectConfig;
import dsu.pasta.config.UpdateConfig;
import dsu.pasta.config.VersionConfig;
import dsu.pasta.dpg.ExtractProjectUpdatedInfoProcessor;
import dsu.pasta.javaparser.gadget.collect.GadgetUtils;
import dsu.pasta.utils.ZFileUtils;
import javassist.*;
import javassist.expr.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;
import java.util.logging.Handler;

/**
 * The core of our call graph is the target class and the target new field's type.
 *
 * <p>Distance 0 means target class, changed fields' types:
 * If changed field's type is primitive or string (too common), we don't set their distance to 0.</p>
 * <p>Distance of the super/subclass of target class and changed fields' type is 1</p>
 * <p>Distance positive number, other types that call me</p>
 * <p>Distance negative number, other types that are called by me</p>
 */
public class BuildCallGraphFromCode extends JavassistSolver {
    public static final String FullCallGraphName = "FullCallGraph.log";
    public static final String TargetCallGraphName = "TargetCallGraph.log";
    public static final String TypeDistanceName = "TypeDistance.log";
    public static final String AllTypeName = "AllTypes.log";
    //should build distance for all types
    public static LinkedHashMap<String, Integer> typeDistance = new LinkedHashMap<>();
    /**
     * Node that is accessed or accesses target type.
     */
    protected static HashSet<Call> toAndFromTargetCGSet;
    private static Logger fullCGLogger = null;
    private static Logger targetCGLogger = null;
    private static Logger distanceLogger = null;
    private static Logger allTypeLogger = null;
    private static HashMap<String, HashSet<String>> fromToTypes = new HashMap<>();
    private static HashSet<String> allTypes = new HashSet<>();
    /**
     * Including target class and son class;
     */
    private static HashMap<String, CtClass> targetClassmap;
    private static HashSet<Call> fullCGSet;
    private static CtBehavior caller = null;

    private static boolean onlyUseTargetCG = false;
    private static ExprEditor readMethodCall = new ExprEditor() {
        @Override
        public void edit(MethodCall mc) {
            CtMethod method = null;
            try {
                method = mc.getMethod();
                Call call = new Call(caller, method);

                if (isTarget(caller) || isTarget(method)) {
                    toAndFromTargetCGSet.add(call);
                    if (onlyUseTargetCG)
                        return;
                    targetCGLogger.info(call.callStr);
                }
                if (onlyUseTargetCG)
                    return;


                fullCGSet.add(call);
                fullCGLogger.info(call.callStr);

                calculateTypeDistance(caller.getDeclaringClass().getName(), method.getDeclaringClass().getName());
                calculateTypeDistance(caller.getDeclaringClass().getName(), method.getReturnType().getName());

                allTypes.add(caller.getDeclaringClass().getName());
                allTypes.add(method.getDeclaringClass().getName());
                allTypes.add(method.getReturnType().getName());


            } catch (NotFoundException e) {
            }
        }
    };
    private static ExprEditor readConstructorCall = new ExprEditor() {
        // Constructor call such as this() and super() within a constructor body.
        @Override
        public void edit(ConstructorCall cc) {
            CtConstructor constructor = null;
            try {
                constructor = cc.getConstructor();
                Call call = new Call(caller, constructor);
                if (isTarget(caller) || isTarget(constructor)) {
                    toAndFromTargetCGSet.add(call);
                    if (onlyUseTargetCG)
                        return;
                    targetCGLogger.info(call.callStr);
                }
                if (onlyUseTargetCG)
                    return;

                fullCGSet.add(call);
                fullCGLogger.info(call.callStr);

                calculateTypeDistance(caller.getDeclaringClass().getName(), constructor.getDeclaringClass().getName());
                allTypes.add(caller.getDeclaringClass().getName());
                allTypes.add(constructor.getDeclaringClass().getName());

            } catch (NotFoundException e) {
            }
        }
    };
    private static ExprEditor readNewExprCall = new ExprEditor() {
        @Override
        public void edit(NewExpr ne) {
            CtConstructor constructor = null;
            try {
                constructor = ne.getConstructor();
                Call call = new Call(caller, constructor);
                if (isTarget(caller) || isTarget(constructor)) {
                    toAndFromTargetCGSet.add(call);
                    if (onlyUseTargetCG)
                        return;
                    targetCGLogger.info(call.callStr);
                }
                if (onlyUseTargetCG)
                    return;

                fullCGSet.add(call);
                fullCGLogger.info(call.callStr);

                calculateTypeDistance(caller.getDeclaringClass().getName(), constructor.getDeclaringClass().getName());
                allTypes.add(caller.getDeclaringClass().getName());
                allTypes.add(constructor.getDeclaringClass().getName());

            } catch (NotFoundException e) {
            }
        }
    };
    private static ExprEditor readFieldAccess = new ExprEditor() {
        @Override
        public void edit(FieldAccess fa) {
            CtField field = null;
            try {
                field = fa.getField();
                Call call = new Call(caller, field);
                if (isTarget(caller) || isTarget(field)) {
                    toAndFromTargetCGSet.add(call);
                    if (onlyUseTargetCG)
                        return;
                    targetCGLogger.info(call.callStr);
                }
                if (onlyUseTargetCG)
                    return;

                fullCGSet.add(call);
                fullCGLogger.info(call.callStr);

                calculateTypeDistance(caller.getDeclaringClass().getName(), field.getDeclaringClass().getName());
                calculateTypeDistance(caller.getDeclaringClass().getName(), field.getType().getName());
                allTypes.add(caller.getDeclaringClass().getName());
                allTypes.add(field.getDeclaringClass().getName());
                allTypes.add(field.getType().getName());

            } catch (NotFoundException e) {
            }
        }
    };

    private static Logger createLog(String fileName) {
        Logger tempLog = Logger.getLogger(BuildCallGraphFromCode.class.getName() + fileName);
        tempLog.setUseParentHandlers(false);
        tempLog.setLevel(Level.INFO);
        try {
            FlushFilehandler fileHandler = new FlushFilehandler(
                    String.join(File.separator, ProjectConfig.one().logRoot, fileName));
            fileHandler.setLevel(Level.INFO);
            fileHandler.setFormatter(new CallGraphFormatter());
            tempLog.addHandler(fileHandler);
        } catch (IOException e) {
        }
        return tempLog;
    }

    private static void initLog(String version) {
        fullCGLogger = createLog(version + FullCallGraphName);
        targetCGLogger = createLog(version + TargetCallGraphName);
        distanceLogger = createLog(version + TypeDistanceName);
        allTypeLogger = createLog(version + AllTypeName);
    }

    private static void closeLog(Logger log) {
        for (Handler h : log.getHandlers()) {
            h.flush();
            h.close();
        }
    }

    private static void closeLogs() {
        closeLog(fullCGLogger);
        closeLog(targetCGLogger);
        closeLog(distanceLogger);
        closeLog(allTypeLogger);
    }

    /**
     * Call graph, including test nodes.
     *
     * @param config
     */
    public static void BuildCallGraph(VersionConfig config) {
        String allTypeFilePath = String.join(File.separator, ProjectConfig.one().logRoot, config.versionString + AllTypeName);
        if (ZFileUtils.fileExistNotEmpty(allTypeFilePath)) {
            typeDistance = new LinkedHashMap<>();
            typeDistance.putAll(GadgetUtils.readDistancesOfType());
            return;
        }
        onlyUseTargetCG = false;

        initLog(config.versionString);
        allTypes = new HashSet<>();
        targetClassmap = new HashMap<>();
        fullCGSet = new HashSet<>();
        toAndFromTargetCGSet = new HashSet<>();
        typeDistance = new LinkedHashMap<>();
        fromToTypes = new HashMap<>();
        allTypes = new HashSet<>();

        BuildCallGraphForInstanceTypeAndFieldType(config);
        resolveTypeDistanceFromCG();

        for (String type : typeDistance.keySet()) {
            distanceLogger.info(type + "," + typeDistance.get(type));
        }
        for (String type : allTypes)
            allTypeLogger.info(type);

        closeLogs();
    }

    protected static boolean isTarget(CtMember cm) {
        CtClass cc = null;
        if (cm instanceof CtField)
            try {
                cc = ((CtField) cm).getType();
            } catch (NotFoundException e) {
            }
        else
            cc = cm.getDeclaringClass();
        if (cc == null)
            return false;
        return targetClassmap.containsKey(cc.getName());
    }

    private static HashSet<CtClass> getSuperSubTypes(VersionConfig config, String type) {
        HashSet<CtClass> result = new HashSet<>();
        result.addAll(JavassistSolver.getSubClassesOf(config, type).values());
        result.addAll(JavassistSolver.getSuperClassesof(config, type).values());
        return result;
    }

    private static void addSuperSubTypesDistance(VersionConfig config, String type, int distance) {
        for (CtClass cc : getSuperSubTypes(config, type)) {
            putTypeDistance(cc.getName(), distance);
        }
    }

    private static void BuildCallGraphForInstanceTypeAndFieldType(VersionConfig config) {
        LinkedHashSet<CtClass> readFirst = new LinkedHashSet<>();

        CtClass targetClass = getCtClass(UpdateConfig.one().targetClass);
        HashMap<String, CtClass> subOfTarget = JavassistSolver.getSubClassesOf(config, targetClass.getName());
        //the core of our call graph
        targetClassmap.put(targetClass.getName(), targetClass);
        targetClassmap.putAll(subOfTarget);
        readFirst.add(targetClass);
        readFirst.addAll(getSuperSubTypes(config, targetClass.getName()));
        putTypeDistance(targetClass.getName(), 0);
        addSuperSubTypesDistance(config, targetClass.getName(), 1);

        CtClass newFieldClass = getCtClass(UpdateConfig.one().getNewFieldTypeRealString());
        if (newFieldClass != null) {
            HashMap<String, CtClass> subOfNewField = JavassistSolver.getSubClassesOf(config, newFieldClass.getName());
            if (!newFieldClass.isPrimitive() && !newFieldClass.getName().equals(String.class.getName())) {
                targetClassmap.put(newFieldClass.getName(), newFieldClass);
                targetClassmap.putAll(subOfNewField);
                //put distance of different types;
                putTypeDistance(newFieldClass.getName(), 0);
                addSuperSubTypesDistance(config, newFieldClass.getName(), 1);
            }
            readFirst.add(newFieldClass);
            readFirst.addAll(getSuperSubTypes(config, newFieldClass.getName()));
        }
        for (String type : ExtractProjectUpdatedInfoProcessor.getChangedFieldTypesOfChangedClass(targetClass.getName())) {
            if (type != null) {
                CtClass fieldClass = getCtClass(type);
                if (fieldClass != null) {
                    readFirst.add(fieldClass);
                    readFirst.addAll(getSuperSubTypes(config, type));
                    if (!fieldClass.isPrimitive() && !fieldClass.getName().equals(String.class.getName())) {
                        putTypeDistance(type, 0);
                        addSuperSubTypesDistance(config, type, 1);
                    }
                }
            }
        }

        //build call graph
        for (CtClass cc : readFirst)
            readCtClass(cc);
        for (CtClass cc : getAllClass(config)) {
            if (readFirst.contains(cc))
                continue;
            readCtClass(cc);
        }
    }

    private static void readCtClass(CtClass cc) {
        allTypes.add(cc.getName());
        CtConstructor con = cc.getClassInitializer();
        if (con != null)
            readCtConstructor(con);

        CtConstructor[] cons = cc.getConstructors();
        for (CtConstructor c : cons)
            readCtConstructor(c);

        CtMethod[] mets = cc.getDeclaredMethods();
        for (CtMethod m : mets)
            readCtMethod(m);

        CtField[] fields = cc.getDeclaredFields();
        for (CtField f : fields)
            readCtField(f);
    }

    private static void putTypeDistance(String type, int distance) {
        int newDistance = distance;
        if (typeDistance.containsKey(type)) {
            int nowDistance = typeDistance.get(type);
            newDistance = Math.abs(nowDistance) < Math.abs(distance) ? nowDistance : distance;
        }
        typeDistance.put(type, newDistance);
    }

    /**
     * The distance between target and new field
     * from-->to
     *
     * @param from, type
     * @param to,   type
     */
    private static boolean calculateTypeDistance(String from, String to) {
        boolean solved = false;
        if (typeDistance.containsKey(from)) {
            int fromDistance = typeDistance.get(from);
            if (fromDistance <= 0) {
                putTypeDistance(to, fromDistance - 1);
                solved = true;
            }
        }
        if (typeDistance.containsKey(to)) {
            int toDistance = typeDistance.get(to);
            if (toDistance >= 0) {
                putTypeDistance(from, toDistance + 1);
                solved = true;
            }
        }
        if (!solved) {
            HashSet<String> tos = fromToTypes.get(from);
            if (tos == null)
                tos = new HashSet<>();
            tos.add(to);
            fromToTypes.put(from, tos);
        }
        return solved;
    }

    public static void resolveTypeDistanceFromCG() {
        while (true) {
            boolean end = true;
            Iterator ite = fromToTypes.entrySet().iterator();
            while (ite.hasNext()) {
                Map.Entry<String, HashSet<String>> entry = (Map.Entry<String, HashSet<String>>) ite.next();
                String from = entry.getKey();
                HashSet<String> tos = entry.getValue();

                Iterator toite = tos.iterator();
                while (toite.hasNext()) {
                    String to = (String) toite.next();
                    if (calculateTypeDistance(from, to)) {
                        toite.remove();
                        end = false;
                    }
                }
                if (fromToTypes.get(from).isEmpty()) {
                    ite.remove();
                    end = false;
                }
            }
            if (end)
                break;
        }

    }

    private static void readCtField(CtField cf) {
        try {
            if (onlyUseTargetCG)
                return;
            CtClass fieldType = cf.getType();
            CtClass declareType = cf.getDeclaringClass();
            calculateTypeDistance(declareType.getName(), fieldType.getName());

            allTypes.add(fieldType.getName());
            allTypes.add(declareType.getName());
        } catch (NotFoundException e) {
//            e.printStackTrace();
        }
    }

    private static void readCtConstructor(CtConstructor cc) {
        if (Modifier.isAbstract(cc.getModifiers()) || Modifier.isNative(cc.getModifiers()))
            return;
        try {
            caller = cc;
            cc.instrument(readMethodCall);
            cc.instrument(readConstructorCall);
            cc.instrument(readNewExprCall);
            cc.instrument(readFieldAccess);
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
    }

    private static void readCtMethod(CtMethod cm) {
        if (Modifier.isAbstract(cm.getModifiers()) || Modifier.isNative(cm.getModifiers()))
            return;
        try {
            caller = cm;
            cm.instrument(readMethodCall);
            cm.instrument(readConstructorCall);
            cm.instrument(readNewExprCall);
            cm.instrument(readFieldAccess);
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
    }

    private static class CallGraphFormatter extends Formatter {
        private static final String format = "%s%n";

        @Override
        public synchronized String format(LogRecord record) {
            return String.format(format, record.getMessage());
        }
    }

    private static class FlushFilehandler extends FileHandler {
        public FlushFilehandler(String pattern) throws IOException, SecurityException {
            super(pattern);
        }

        @Override
        public void publish(LogRecord record) {
            super.publish(record);
            this.flush();
        }
    }
}