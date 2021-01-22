package dsu.pasta.javaparser.factory.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedWildcard;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.typesystem.NullType;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import dsu.pasta.config.ProjectConfig;
import dsu.pasta.config.VersionConfig;
import dsu.pasta.dpg.ExtractProjectUpdatedInfoProcessor;
import dsu.pasta.javaparser.factory.stmt.ZApiConstructor;
import dsu.pasta.javaparser.factory.stmt.ZApiField;
import dsu.pasta.javaparser.factory.stmt.ZApiMethod;
import dsu.pasta.javaparser.factory.stmt.ZCastStmt;
import dsu.pasta.javaparser.gadget.collect.GadgetsCollections;
import dsu.pasta.javassist.BuildCallGraphFromCode;
import dsu.pasta.javassist.JavassistSolver;
import dsu.pasta.utils.ZFileUtils;
import dsu.pasta.utils.ZPrint;
import javassist.CtClass;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import static dsu.pasta.utils.ZPrint.print;

public class JavaparserSolver {
    public static final String gadgetFileExtension = ".gadgets";
    public static JavaParser parser;
    /**
     * Javaparser visitor adapter.
     */
    public static ZGenericListVisitorAdapter visitor;
    /**
     * Javaparser combined type solver that can solve source factory and necessary byte factory.
     */
    public static CombinedTypeSolver combinedTypeSolver;

    /**
     * This is a source type solver. Used to determine whether a type is from source file or byte code.
     */
    private static JavaParserTypeSolver javaParserTypeSolver;

    /**
     * All resolved type visited by this type solver, for better time efficiency.
     */
    private static HashMap<String, ResolvedType> visitedTypeByMe = new HashMap<String, ResolvedType>();

    /**
     * If <tt>target</tt> is <tt>java.lang.Object</tt>, any <tt>source</tt> type can be assigned to target directly.
     * Therefore, if <tt>target</tt> is <tt>java.lang.Object</tt>, we return false.
     *
     * @param target
     * @param source
     * @return
     */
    public static boolean isAssignableBy(ResolvedType target, ResolvedType source) {
        try {
            //fixme
            if (JavaparserSolver.myDescribe(target).contains("org.apache.tomcat.util.net.AbstractEndpoint")
                    && JavaparserSolver.myDescribe(source).contains("org.apache.tomcat.util.net.Nio2Endpoint"))
                return true;

            if (target != null && source != null && myDescribe(target).equals(myDescribe(source)))
                return true;
            if (myDescribe(target).equals("java.lang.Object"))
                return false;
            return target != null && source != null && target.isAssignableBy(source);
        } catch (Exception e) {
//			e.printStackTrace();
            return false;
        }
    }

    public static void BuildSolver(VersionConfig config) {
        parser = new JavaParser();
        visitedTypeByMe = new HashMap<String, ResolvedType>();

        visitor = new ZGenericListVisitorAdapter();
        combinedTypeSolver = new CombinedTypeSolver();
        // add source roots
        javaParserTypeSolver = new JavaParserTypeSolver(config.projectSourceDir);
        combinedTypeSolver.add(javaParserTypeSolver);
        // add reflection type solver
        combinedTypeSolver.add(new ZReflectionTypeSolver(true));
        // add Jars
        if (config.jars != null) {
            for (File jar : config.jars) {
                try {
                    combinedTypeSolver.add(new ZJarTypeSolver(jar));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //ignoring all exceptions while solving gadgets
        combinedTypeSolver.setExceptionHandler(CombinedTypeSolver.ExceptionHandlers.IGNORE_ALL);

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);

        parser.getParserConfiguration()
                .setAttributeComments(false)
                .setDoNotAssignCommentsPrecedingEmptyLines(false)
                .setLexicalPreservationEnabled(false)
                .setIgnoreAnnotationsWhenAttributingComments(true)
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8)
                .setSymbolResolver(symbolSolver);
        //while setStoreTokens is false, many statements will be skipped
        //				.setStoreTokens(false)
    }

    public static void SolveAllSourceByteTypes(VersionConfig config) {
        String allTypes = String.join(File.separator, ProjectConfig.one().logRoot, config.versionString + BuildCallGraphFromCode.AllTypeName);
        List<String> types = ZFileUtils.readFileToList(allTypes);
        types.addAll(ProjectConfig.one().apiTypes);

        for (String t : ProjectConfig.apiTypes) {
            BuildCallGraphFromCode.typeDistance.put(t, 0);
        }

        //changed class should be extracted on both old and new version
        ProgressBarBuilder pbb = new ProgressBarBuilder()
                .setUnit(" files", 1)
                .setTaskName("Distilling");
        int count = 0;
        for (String type : ProgressBar.wrap(types, pbb)) {
            Integer distance = BuildCallGraphFromCode.typeDistance.get(type);
            if (distance == null)
                continue;
            if (distance != null && Math.abs(distance) >= ProjectConfig.readGadgetRange)
                continue;
            count++;
            String gadgetsFile = getGadgetsFile(type, config.versionString);
            if (!new File(gadgetsFile).exists()) {
                CtClass target = JavassistSolver.getCtClass(type);
                String file = getPath(target, config.projectSourceDir);

                boolean write = false;
                GadgetsCollections.clearTemp();
                if (file == null) {
                    //solve byte factory api
                    try {
                        SolveByteAPIs(type);
                        write = true;
                    } catch (Exception e) {
                    }
                } else {
                    //solve source factory gadgets
                    TypeDeclaration dec = getAccurateDeclaration(target, config.projectSourceDir);
                    if (dec == null) {
                    } else {
                        SolveSourceClass(dec);
                        write = true;
                    }
                }
                ZCastStmt.visitSuperToSubCast(type);
                if (write) {
                    GadgetsCollections.writeTempSketchesContextsTo(gadgetsFile);
                }
            }
        }
        ZPrint.verbose("Distilling gadgets from " + count + " files within range");
    }

    /**
     * The original describe method of Javaparser can't handle all ResolvedType.
     * We use this method as a wrapper to handle exceptions.
     *
     * @param t
     * @return
     */
    public static String myDescribe(ResolvedType t) {
        if (t == null)
            return null;
        String desc = null;
        try {
            desc = t.describe();
        } catch (Exception e) {
            desc = t.toString();
        }
        return desc;
    }

    public static boolean descEqual(ResolvedType ori, ResolvedType tar) {
        if (ori == null || tar == null)
            return false;
        String oriDesc = myDescribe(ori);
        String tarDesc = myDescribe(tar);
        if (oriDesc != null)
            return oriDesc.equals(tarDesc);
        else if (tarDesc == null)
            return true;
        else
            return false;
    }

    public static String myDescribeSimple(ResolvedType t) {
        String desc = myDescribe(t);

        return desc.replaceAll("([a-zA-Z$]*\\.)+|\\s+", "");
    }

    /**
     * Is this type in a source file?
     *
     * @param type
     * @return <tt>true</tt> if this type can be obtained from source file;
     * <tt>false</tt> if this type can only be obtained from byte code
     */
    public static boolean isSourceType(String type) {
        ResolvedType rt = getType(type);
        if (rt == null) {
            //TODO, for some types, rt is null. But this is rare. They usually are user defined types. We can read them.
            return true;
        }
        String desc = JavaparserSolver.myDescribe(rt);

        boolean has = false;
        try {
            has = javaParserTypeSolver.hasType(desc);
        } catch (Exception e) {
//			e.printStackTrace();
        }
        return has;
    }

    /**
     * Get resolved type for type name.
     * All <tt>$</tt> inside <tt>type</tt> are automatically replaced with <tt>.</tt>
     *
     * @param type
     * @return
     */
    public static ResolvedType getType(String type) {
        if (type == null || type.length() == 0)
            return null;
        if (type.contains("$"))
            type = type.replaceAll("\\$", "\\.");
        type = type.trim().replaceAll(" +", " ");
        if (visitedTypeByMe.containsKey(type))
            return visitedTypeByMe.get(type);
        ResolvedType result = null;

        //is null type
        if (type.equals("null"))
            return NullType.INSTANCE;
        // is primitive?
        try {
            result = ResolvedPrimitiveType.byName(type);
        } catch (Exception e) {
        }
        if (result != null) { // is primitive type, return;{
            visitedTypeByMe.put(type, result);
            return result;
        }
        // array type
        if (type.endsWith("[]")) {
            String base = type.substring(0, type.indexOf("["));
            ResolvedType baseType = getType(base);
            result = new ResolvedArrayType(baseType);
            visitedTypeByMe.put(type, result);
            return result;
        }
        // reference type.
        //java.lang.ref.Reference<? extends java.lang.Class<? extends java.lang.Object>>
        if (type.contains("<") && type.contains(">")) {
            //? is inside <>: a.b.c<? extends d.e.f>
            //or a.b.c<d.e.f>, no ?
            if (type.contains("?") && type.indexOf("?") > type.indexOf("<")
                    || !type.contains("?")) {
                String container = type.substring(0, type.indexOf("<"));
                String elements = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
                ResolvedReferenceTypeDeclaration contDec = null;
                try {
                    contDec = JavaparserSolver.combinedTypeSolver.solveType(container);
                } catch (Exception e) {
                }
                if (contDec == null)
                    return null;
                List<Integer> commaIndexs = new ArrayList<>();
                int l = 0, r = 0;
                for (int i = 0; i < elements.length(); i++) {
                    if (elements.charAt(i) == ',' && l == r) {
                        commaIndexs.add(i);
                    }
                    if (elements.charAt(i) == '<')
                        l++;
                    if (elements.charAt(i) == '>')
                        r++;
                }
                List<ResolvedType> eleList = new ArrayList<>();
                if (commaIndexs.size() > 0) {
                    int pre = 0;
                    for (int i : commaIndexs) {
                        String element = elements.substring(pre, i);
                        eleList.add(getType(element));
                        pre = i + 1;
                    }
                    //last one
                    String element = elements.substring(pre);
                    eleList.add(getType(element));
                } else {
                    eleList.add(getType(elements));
                }
                result = new ReferenceTypeImpl(contDec, eleList, JavaparserSolver.combinedTypeSolver);
                visitedTypeByMe.put(type, result);
                return result;
            } else if (type.contains("?") && type.indexOf("?") < type.indexOf("<")) {
                //? is ahead of <: ? extends a.b.c<d.e.f>
                String[] sp = type.split("\\s+");
                assert (sp.length >= 3);
                assert (sp[0].equals("?"));
                assert (sp[1].equals("extends") || sp[1].equals("super"));
                type = type.replaceFirst("\\?", "");
                if (sp[1].equals("extends")) {
                    type = type.replaceFirst("extends", "");
                    type = type.trim();
                    result = ResolvedWildcard.extendsBound(getType(type));
                } else if (sp[1].equals("super")) {
                    type = type.replaceFirst("super", "");
                    type = type.trim();
                    result = ResolvedWildcard.superBound(getType(type));
                }
                if (result != null)
                    visitedTypeByMe.put(type, result);
                return result;
            }
            //should not reach here
            return result;
        } else if (type.contains("?") && !type.equals("?")) {
            String[] sp = type.split("\\s+");
            assert (sp[0].equals("?"));
            assert (sp.length == 3);
            ResolvedReferenceTypeDeclaration base = null;
            try {
                base = JavaparserSolver.combinedTypeSolver.solveType(sp[2]);
            } catch (Exception e) {
                return null;
            }
            // super
            if (sp[1].equals("super")) {
                result = ResolvedWildcard.superBound(new ReferenceTypeImpl(base, JavaparserSolver.combinedTypeSolver));
            }
            // extends
            if (sp[1].equals("extends")) {
                result = ResolvedWildcard.extendsBound(new ReferenceTypeImpl(base, JavaparserSolver.combinedTypeSolver));
            }
            visitedTypeByMe.put(type, result);
            return result;
        } else {
            try {
                ResolvedReferenceTypeDeclaration dec = JavaparserSolver.combinedTypeSolver.solveType(type);
                result = new ReferenceTypeImpl(dec, JavaparserSolver.combinedTypeSolver);
                visitedTypeByMe.put(type, result);
                return result;
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Extract constructor, method apis and fields.
     *
     * @param type
     */
    public static void SolveByteAPIs(String type) {
        // solve api methods in special types
        ResolvedType apiType = getType(type);
        if (apiType == null)
            return;
        if (apiType.isPrimitive()) {
            String wrapper = GetWrapper(type);
            apiType = getType(wrapper);
        }
        ResolvedReferenceTypeDeclaration rtd = null;
        try {
            rtd = apiType.asReferenceType().getTypeDeclaration().get();
        } catch (java.lang.UnsupportedOperationException e) {
            return;
        }
        // api types must be reference type
        // get constructor
        for (ResolvedConstructorDeclaration c : rtd.getConstructors()) {
            try {
                ZApiConstructor.visit(c, apiType);
            } catch (Exception e) {
                continue;
            }
        }
        // get api methods
        //the get declared methods results is not sorted and not in any perticular order.
        for (ResolvedMethodDeclaration md : rtd.getDeclaredMethods()) {
            try {
                MethodUsage m = new MethodUsage(md);
                ZApiMethod.visit(m, apiType);
            } catch (Exception e) {
                continue;
            }
        }
        for (ResolvedFieldDeclaration fd : rtd.getDeclaredFields()) {
            try {
                ZApiField.visit(fd, apiType);
            } catch (Exception e) {
                //TODO here
            }
        }
    }

    /**
     * Get source file path if this class's source factory is available.
     *
     * @param cc
     * @param root
     * @return, null if there is no source file for this type; path if there is one source file for this type
     */
    private static String getPath(CtClass cc, String root) {
        if (cc == null)
            return null;
        if (cc.isPrimitive())
            return null;
        String file = "";
        String name = cc.getSimpleName();
        if (name.contains("$")) {
            name = name.substring(0, name.indexOf("$"));
        }
        if (!cc.getName().contains(".")) {
            //no dot inside fully qualified name. it doesn't have package name.
            file = name + ".java";
        } else {
            String packageName = cc.getPackageName();
            if (packageName == null)
                return null;
            if (File.separator.equals("/"))
                file = packageName.replaceAll("\\.", File.separator) + File.separator + name + ".java";
            else
                file = packageName.replaceAll("\\.", "\\\\") + "\\" + name + ".java";
        }

        String result = String.join(File.separator, root, file);
        if (!new File(result).isFile()) {
            return null;
        }
        return result;
    }

    /**
     * Some class name may be outer class or inner class.
     * Get the accurate declaration of target class.
     *
     * @param cc,   the javassist class of target name.
     * @param root, source root.
     * @return
     */
    private static TypeDeclaration getAccurateDeclaration(CtClass cc, String root) {
        if (cc == null)
            return null;
        String path = getPath(cc, root);
        // not a source file
        if (path == null) {
            return null;
        }
        List<TypeDeclaration> targetClasses = new ArrayList<>();
        try {
            ParseResult<CompilationUnit> result = parser.parse(new File(path));
            if (!result.isSuccessful())
                return null;
            CompilationUnit cu = result.getResult().get();

            targetClasses = cu.findAll(TypeDeclaration.class);
        } catch (Exception e) {
            return null;
        }
        if (targetClasses.size() == 0) {
//            ZPrint.verbose("No class declarations in this file " + path);
            return null;
        }
        // get class simple name, if there is "$", get the real name of inner class
        String name = cc.getSimpleName();
        if (name.contains("$")) {
            // inner name, accurate name
            String innerName = name.substring(name.lastIndexOf("$") + 1);
            for (TypeDeclaration cl : targetClasses) {
                // visit inner class
                if (cl.getNameAsString().equals(innerName)) {
                    return cl;
                }
            }
        } else {
            for (TypeDeclaration cl : targetClasses) {
                // visit outer class
                if (cl.getNameAsString().equals(name)) {
                    return cl;
                }
            }
        }
        return null;
    }

    /**
     * Extract gadgets from all classes declared in the file.
     *
     * @param path
     */
    public static int SolveSourceFileCompletely(String path, String version) {
        List<ClassOrInterfaceDeclaration> targetClasses = new ArrayList<>();
        try {
            ParseResult<CompilationUnit> result = parser.parse(new File(path));
            if (!result.isSuccessful())
                return 0;
            CompilationUnit cu = result.getResult().get();

            targetClasses = cu.findAll(ClassOrInterfaceDeclaration.class);
        } catch (Exception e) {
            return 0;
        }
        if (targetClasses.size() == 0) {
//			ZPrint.verbose("No class declarations in this file " + path);
            return 0;
        }
        int resolved = 0;
        GadgetsCollections.allParsedNode.clear();
        for (ClassOrInterfaceDeclaration cl : targetClasses) {
            GadgetsCollections.clearTemp();

            Optional<String> name = cl.getFullyQualifiedName();
            if (!name.isPresent())
                continue;
            String fullyName = name.get();
            String gadgetsFile = getGadgetsFile(fullyName, version);
            if (ZFileUtils.fileExistNotEmpty(gadgetsFile)) {
                continue;
            }
            print("Extract gadgets from " + fullyName);
            SolveSourceClass(cl);
            resolved++;

            GadgetsCollections.writeTempSketchesContextsTo(gadgetsFile);
        }
        return resolved;
    }

    public static String getGadgetsFile(String fullyName, String version) {
        if (ExtractProjectUpdatedInfoProcessor.isChangedClass(fullyName))
            return String.join(File.separator, ProjectConfig.one().gadgetRoot, fullyName + "_" + version + gadgetFileExtension);
        else
            return String.join(File.separator, ProjectConfig.one().gadgetRoot, fullyName + gadgetFileExtension);
    }

    /**
     * Visit fields, methods, constructors in the class declaration.
     *
     * @param type
     */
    public static void SolveSourceClass(TypeDeclaration type) {
        if (type instanceof EnumDeclaration) {
            EnumDeclaration ed = (EnumDeclaration) type;
            for (EnumConstantDeclaration ecd : ed.getEntries()) {
                try {
                    visitor.visit(ecd, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (type instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration cl = (ClassOrInterfaceDeclaration) type;
            for (BodyDeclaration b : cl.getMembers()) {
                if (b.isFieldDeclaration()) {
                    // visit field
                    FieldDeclaration fd = b.asFieldDeclaration();
                    try {
                        visitor.visit(fd, null);
                    } catch (Exception e) {
//						e.printStackTrace();
                    }
                } else if (b.isInitializerDeclaration()) {
                    // visit static initializer
                    // b.isStatic()
                    InitializerDeclaration id = b.asInitializerDeclaration();
                    if (id.isStatic()) {
                        try {
                            visitor.visit(id, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (b.isConstructorDeclaration()) {
                    ConstructorDeclaration cd = (ConstructorDeclaration) b;
                    try {
                        visitor.visit(cd, null);
                    } catch (Exception e) {
//						e.printStackTrace();
                    }
                } else if (b.isMethodDeclaration()) {
                    MethodDeclaration md = (MethodDeclaration) b;
                    try {
                        visitor.visit(md, null);
                    } catch (Exception e) {
//						e.printStackTrace();
                    }
                }
            }
        }
        // solve byte factory api
        //TODO do we still need byte api here?
//		SolveByteAPIs(cl.resolve().getQualifiedName());
    }

    private static String GetWrapper(String type) {
        if (type.equals(byte.class.getName()))
            return Byte.class.getName();
        if (type.equals(short.class.getName()))
            return Short.class.getName();
        if (type.equals(int.class.getName()))
            return Integer.class.getName();
        if (type.equals(long.class.getName()))
            return Long.class.getName();
        if (type.equals(float.class.getName()))
            return Float.class.getName();
        if (type.equals(double.class.getName()))
            return Double.class.getName();
        if (type.equals(boolean.class.getName()))
            return Boolean.class.getName();
        if (type.equals(char.class.getName()))
            return Character.class.getName();
        return null;
    }

    private static class ExtractedFormatter extends Formatter {
        private static final String format = "%s%n";

        @Override
        public synchronized String format(LogRecord record) {
            return String.format(format, record.getMessage());
        }
    }

}
