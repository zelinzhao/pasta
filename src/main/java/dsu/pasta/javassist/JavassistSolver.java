package dsu.pasta.javassist;

import dsu.pasta.config.ProjectConfig;
import dsu.pasta.config.UpdateConfig;
import dsu.pasta.config.VersionConfig;
import dsu.pasta.javaparser.gadget.program.Program;
import javassist.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JavassistSolver {

    protected static ClassPool javassistClassPool;
    private static HashMap<String, HashMap<String, CtClass>> alreadyFoundSubClasses = new HashMap<>();
    private static List<ClassPath> jarPaths = new ArrayList<>();
    private static CtClass targetClass = null;
    private static ClassPool tempOldPool = null;

    public static CtClass getCtClass(String name) {
        try {
            return javassistClassPool.getCtClass(name);
        } catch (NotFoundException e) {
            return null;
        }
    }

    public static void BuildSolver(VersionConfig config) {
        alreadyFoundSubClasses = new HashMap<>();

        jarPaths = new ArrayList<>();
        javassistClassPool = null;
        try {
            javassistClassPool = new ClassPool();
            javassistClassPool.doPruning = false;
            javassistClassPool.importPackage("dsu.pasta.object.processor.ObjectApi");
            javassistClassPool.appendSystemPath();
            if (config.jars == null || config.jars.length == 0) {
                jarPaths.add(javassistClassPool.insertClassPath(config.projectByteDir));
            } else {
                for (File oneJar : config.jars) {
                    jarPaths.add(javassistClassPool.insertClassPath(oneJar.getAbsolutePath()));
                }
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    public static ClassPool BuildOneSolver(VersionConfig config) {
        try {
            ClassPool pool = new ClassPool();
            pool.doPruning = false;
            //TODO we insert dsu.dsu.pasta.object.processor.ObjectApi here. May be useless
            pool.importPackage("dsu.pasta.object.processor.ObjectApi");
            pool.appendSystemPath();

            if (config.jars == null || config.jars.length == 0) {
                pool.insertClassPath(config.projectByteDir);
            } else {
                for (File oneJar : config.jars) {
                    pool.insertClassPath(oneJar.getAbsolutePath());
                }
            }
            return pool;
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param config
     * @param targetClass
     * @return, class name and ctclass pairs
     */
    public static HashMap<String, CtClass> getSubClassesOf(VersionConfig config, String targetClass) {
        if (alreadyFoundSubClasses.containsKey(targetClass))
            return alreadyFoundSubClasses.get(targetClass);

        HashMap<String, CtClass> result = new HashMap<>();
        CtClass target = getCtClass(targetClass);
        if (target == null)
            return result;
        for (CtClass cc : getAllClass(config)) {
            //we can assign instance of subclass to target class
            if (!cc.equals(target) && cc.subclassOf(target)) {
                result.put(cc.getName(), cc);
            }
        }

        alreadyFoundSubClasses.put(targetClass, result);
        return result;
    }

    public static HashMap<String, CtClass> getSuperClassesof(VersionConfig config, String targetClass) {
        HashMap<String, CtClass> result = new HashMap<>();
        CtClass target = getCtClass(targetClass);
        if (target == null)
            return result;
        try {
            CtClass temp = target.getSuperclass();
            while (temp != null && !temp.getName().equals("java.lang.Object")) {
                result.put(temp.getName(), temp);
                temp = temp.getSuperclass();
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static HashSet<CtClass> getAllClass(VersionConfig config) {
        HashSet<CtClass> all = new HashSet<>();
        for (File oneJar : config.jars) {
            JarFile jarfile = null;
            try {
                jarfile = new JarFile(oneJar);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (jarfile == null)
                continue;
            Enumeration<JarEntry> jarentries = jarfile.entries();
            while (jarentries.hasMoreElements()) {
                JarEntry entry = jarentries.nextElement();
                if (entry.isDirectory())
                    continue;
                String name = entry.getName();
                if (!name.endsWith(".class"))
                    continue;
                String classname = name.substring(0, name.indexOf('.'));
                classname = classname.replaceAll("\\/", "\\.");
                CtClass cc = null;
                try {
                    cc = javassistClassPool.getCtClass(classname);
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
                if (cc != null)
                    all.add(cc);
            }
        }
        return all;
    }

    public static String getFieldTypeFullQualified(String targetClass, String fieldName) {
        CtClass target = getCtClass(getFullQualifiedName(targetClass));
        if (target == null)
            return null;
        try {
            CtField field = target.getField(fieldName);
            return field.getType().getName();
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * @param name, a/b/c
     * @return a.b.c
     */
    public static String getFullQualifiedName(String name) {
        return name.replaceAll("\\/", "\\.");
    }

    public static boolean compaliableTrans(Program program) {
        for (String code : program.toCleanCodeNoNull()) {
            if (compaliableTrans(code))
                return true;
        }
        for (String code : program.toCleanCodeWithNull()) {
            if (compaliableTrans(code))
                return true;
        }
        return false;
    }

    public static boolean compaliableTrans(String trans) {
        if (tempOldPool == null) {
            tempOldPool = JavassistSolver.BuildOneSolver(ProjectConfig.one().oldVersion);
        }
        try {
            if (targetClass == null || !targetClass.getName().equals(UpdateConfig.one().targetClass)) {
                targetClass = tempOldPool.getCtClass(UpdateConfig.one().targetClass);
            }
            String method = Program.getCleanCode(trans);
            try {
                CtMethod cm = CtNewMethod.make("public void dsuMethod(){" + method + "}", targetClass);
            } catch (CannotCompileException e) {
//				e.printStackTrace();
                return false;
            }
        } catch (NotFoundException e) {
            return false;
        }
        return true;
    }
}
