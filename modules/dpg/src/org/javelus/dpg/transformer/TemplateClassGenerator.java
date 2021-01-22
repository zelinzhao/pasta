/*
 * Copyright (C) 2012  Tianxiao Gu. All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 * Please contact Institute of Computer Software, Nanjing University, 
 * 163 Xianlin Avenue, Nanjing, Jiangsu Provience, 210046, China,
 * or visit moon.nju.edu.cn if you need additional information or have any
 * questions.
 */
package org.javelus.dpg.transformer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.javelus.FieldUpdateType;
import org.javelus.dpg.io.Utils;
import org.javelus.dpg.model.DSUClass;
import org.javelus.dpg.model.DSUField;
import org.javelus.dpg.model.DSUMethod;
import org.javelus.dpg.model.DSU;

/**
 * @author tiger
 * 
 */
public class TemplateClassGenerator {

    static final String ANONYMOUS_CONSTRUCTOR_NAME = "$Anonymous";
    private static final String DEFAULT_PREFIX = "_old_";

    public static void generate(DSU update, File root)
            throws FileNotFoundException {
        List<DSUClass> groupA = update.getRedefinedClasses();
        for (DSUClass oldVersion : groupA) {
            DSUClass newVersion = oldVersion.getNewVersion();

            if (newVersion == null) {
                System.err.format(
                        "Redefined class[%s] does not have new version.\n",
                        oldVersion.getName());
            }

            if (oldVersion.isInterface() || oldVersion.isEnum()) {
                continue;
            } else if (oldVersion.isInnerClass()) {
                DSUClass outerClass = oldVersion.getDeclaringClass();
                if (outerClass.needRedefineClass()) {
                    // let outerClass generate inner class
                    continue;
                } else {
                    DSUClass newOuter = newVersion.getDeclaringClass();
                    if (newOuter == outerClass.getNewVersion()) {
                        // newOuter = outerClass.getNewVersion();
                    } else {
                        System.err.format("Outer class[%s] for a matched old inner class[%s] must have a new version.\n",
                                        outerClass.getName(),
                                        oldVersion.getName());
                        System.err.format("Outer class[%s] for a matched the new inner class[%s].\n",
                                        newOuter.getName(),
                                        newVersion.getName());
                    }
                    genStubClass(outerClass, newOuter, new FileOutputStream(
                            createStubClassFile(root, outerClass)));
                    continue;
                }
            }

            genStubClass(oldVersion, newVersion, new FileOutputStream(
                    createStubClassFile(root, oldVersion)));

        }
    }

    /**
     * @param update
     * @param rootDir
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void generate(DSU update, String rootDir)
            throws FileNotFoundException, IOException {
        if (!rootDir.endsWith("/")) {
            rootDir = rootDir + "/";
        }
        File root = new File(rootDir);

        generate(update, root);
    }

    static File createStubClassFile(File root, DSUClass c) {
        File pkgDir;
        // output class declaration to top level class
        if (c.getPackage().isDefaultPackage()) {
            pkgDir = root;
        } else {
            pkgDir = new File(root, c.getPackage().getFullName() + "/");
        }

        pkgDir.mkdirs();

        return new File(pkgDir, c.getSimpleName() + ".java");
    }

    /**
     * @param oldVersion
     *            oldVersion may be null
     * @param newVersion
     *            newVersion must not be null
     * @return a string represents the template class
     */
    public static void genStubClass(DSUClass oldVersion, DSUClass newVersion,
            OutputStream out) {
        if (newVersion.isEnum() || newVersion.isInterface()) {
            return;
        }

        PrintWriter pw = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(out)));

        genClassFileHeader(pw, newVersion);
        genClassBody(0, pw, oldVersion, newVersion);

        pw.flush();
        pw.close();
    }

    /**
     * for test
     * 
     * @param update
     * @param output
     * @throws IOException
     */
    public static void generate(DSU update, OutputStream output)
            throws IOException {
        List<DSUClass> reload = update.getRedefinedClasses();
        for (DSUClass c : reload) {
            if (c.isInterface() || c.isEnum()) {
                continue;
            }

            DSUClass newVersion = null;
            if (c.hasNewVersion()) {
                newVersion = c.getNewVersion();
            }

            if (!c.isInnerClass() && !c.isEnum()) {
                genStubClass(c, newVersion, output);
            }
        }
    }

    static void genClassFileHeader(PrintWriter pw) {

    }

    static void genClassFileHeader(PrintWriter pw, DSUClass newVersion) {
        // package declaration
        String fullName = newVersion.getName().replace('/', '.');
        int end = fullName.lastIndexOf('.');

        if (end != -1) {
            pw.format("package %s;\n", fullName.substring(0, end));
        }

        // import
        getAnnotationImport(pw);

        // comment
        pw.append("/**\n * this class was generated by UPTASM (LOUVA)\n */\n");
    }

    static void genFieldsDeclaration(int indent, PrintWriter pw,
            DSUClass oldVersion, DSUClass newVersion) {

        DSUField[] fields = newVersion.getDeclaredFields();

        for (DSUField f : fields) {
            // if (f.getDeclaringClass() != oldVersion.getNewVersion()) {
            // genRemapFieldAnnotation(indent, pw, f.getDeclaringClass()
            // .getName(), f.getName(), f.getDescriptor());
            // }
            genFieldDeclaration(indent, pw, f, false);
        }
    }

    static void genClassTransformer(int indent, PrintWriter pw,
            DSUClass oldVersion, DSUClass newVersion) {

        if (oldVersion == null || newVersion == null) {
            return;
        }

        if (!oldVersion.needRedefineClass()) {
            return;
        }

        if (newVersion.getDeclaringClass() != null) {
            if (newVersion.isStatic()) {
                return;
            }
        }

        Iterator<DSUField> it = newVersion.getStaticFields();
        if (it.hasNext()) {
            // class initialization
            // staic block
            genIndent(pw, indent);
            // pw.append("public static void jdusClass(");
            pw.format("public static void updateClass( %s to\n", newVersion
                    .getName().replace('/', '.'));

            if (oldVersion != null) {
                Iterator<DSUField> oit = oldVersion.getStaticFields();

                if (oit.hasNext()) {
                    while (oit.hasNext()) {
                        DSUField f = oit.next();
                        if (f.hasNewVersion()/* || f.isInstanceField() */) {
                            // skip matched field and instance field
                            continue;
                        } else {
                            // deleted field
                            pw.append(",");
                            genNewLine(pw);
                            genOldFieldAnnotation(indent + 1, pw, f);
                            genNewLine(pw);
                            genOldFieldLocalDefinition(indent + 1, pw, f);
                        }
                    }
                }

            }

            genIndent(pw, indent);
            pw.append("){\n");

            while (it.hasNext()) {
                DSUField f = it.next();
                if (f.hasOldVersion()) {
                    // skip matched fields
                    continue;
                } else {
                    // added field
                    genIndent(pw, indent);
                    pw.format("\t%s=%s;\n", f.getName(), Utils
                            .genDefaultValueFromDescriptor(f.getDescriptor()));
                }
            }
            genIndent(pw, indent);
            pw.append("}\n");

        }
    }

    public static String SINGLE_INDENT = "\t";

    static void genIndent(PrintWriter pw, int indent) {
        for (int i = 0; i < indent; i++) {
            pw.append(SINGLE_INDENT);
        }
    }

    static void genObjectTransformer(int indent, PrintWriter pw,
            DSUClass oldVersion, DSUClass newVersion) {
        if (oldVersion == null || newVersion == null) {
            return;
        }

        if (!oldVersion.needRedefineClass()) {
            return;
        }

        if (!oldVersion.isInterface() && !newVersion.isInterface()) {
            Iterator<DSUField> fields = oldVersion.getInstanceFields();
            if (!fields.hasNext()) {
                return;
            }

            // object initialization
            // default constructor
            // always public here?
            genIndent(pw, indent);
            pw.append("public void updateObject(");

            boolean genComma = false;
            while (fields.hasNext()) {
                DSUField f = fields.next();
                if (!f.hasNewVersion()) {
                    // an old deleted instance field
                    if (genComma) {
                        pw.append(",");
                        genNewLine(pw);
                    } else {
                        genComma = true;
                    }

                    genOldFieldAnnotation(indent + 1, pw, f);
                    genNewLine(pw);
                    genOldFieldLocalDefinition(indent + 1, pw, f);
                }
            }

            genIndent(pw, indent);
            pw.append("){\n");
            genIndent(pw, indent);
            pw.append("\t//new fields has already been initialized with default value\n");
            fields = newVersion.getInstanceFields();
            while (fields.hasNext()) {
                DSUField f = fields.next();
                if (f.isInstanceField()) {
                    if (f.hasOldVersion()) {
                        // a matched field
                    } else {
                        // a new added field
                        genIndent(pw, indent);
                        pw.format("\t//this.%s = %s;\n", f.getName(), Utils
                                .genDefaultValueFromDescriptor(f
                                        .getDescriptor()));
                    }
                }
            }
            pw.append("\n");
            genIndent(pw, indent);
            pw.append("}\n");
        }
    }

    static void genAnonymousClassHeader(int indent, PrintWriter pw,
            DSUClass newVersion) {
        String simpleName = null;

        DSUClass[] ifcs = newVersion.getDeclaredInterfaces();
        if (ifcs.length > 0) {
            simpleName = ifcs[0].getSimpleName();
        } else {
            DSUClass superClass = newVersion.getSuperClass();
            if (superClass.isAbstract()) {
                simpleName = superClass.getSimpleName();
            }
        }

        genIndent(pw, indent);
        pw.append(simpleName);
        pw.append(" anony = new ");
        pw.append(simpleName);
        pw.append("(){\n");
    }

    /**
     * 
     * @param indent
     * @param pw
     * @param newVersion
     * @param simpleName
     */
    static void genClassHeader(int indent, PrintWriter pw, DSUClass newVersion,
            DSUClass superClass, String simpleName, boolean isStatic) {
        // class declaration
        genIndent(pw, indent);
        if (isStatic) {
            pw.append("static ");
        }
        pw.append(Utils.accessToString(newVersion.getAccess(), Utils.CLASS));
        pw.append(simpleName);

        if (superClass != null && superClass.hasOldVersion()
                && superClass.getOldVersion().needRedefineClass()) {
            pw.append(" extends ");
            pw.append(newVersion.getSuperClass().getName().replace('/', '.')
                    .replace('$', '.'));
        }

        pw.append(" {\n");
    }

    static void genClassBody(int indent, PrintWriter pw, DSUClass oldVersion,
            DSUClass newVersion) {
        if (oldVersion.isEnum()) {
            return;
        }
        int innerIndent = indent + 1;
        String simpleName = newVersion.getSimpleName();

        // generate class header
        boolean isAnonymousClass = "".equals(simpleName);
        if (isAnonymousClass) {
            genAnonymousClassHeader(indent, pw, newVersion);
        } else if (newVersion.isInnerClass()) {
            genClassHeader(indent, pw, newVersion, newVersion.getSuperClass(),
                    simpleName, testStaticInnerClass(oldVersion));
        } else {
            genClassHeader(indent, pw, newVersion, newVersion.getSuperClass(),
                    simpleName, false);
        }

        // generate field
        genFieldsDeclaration(innerIndent, pw, oldVersion, newVersion);

        genClassTransformer(innerIndent, pw, oldVersion, newVersion);

        // genRemapperAnnotation(innerIndent , pw, "<init>");
        genObjectTransformer(innerIndent, pw, oldVersion, newVersion);
        // genMethodsDeclaration(innerIndent, pw, oldVersion);

        // inner class declaration
        DSUClass[] innerClasses = oldVersion.getDeclaredClasses();
        for (DSUClass c : innerClasses) {
            if (c.needRedefineClass()) {
                if (c.isLocalClass()) {
                    genLocalClass(innerIndent, pw, c, c.getNewVersion());
                } else {
                    genClassBody(innerIndent, pw, c, c.getNewVersion());
                }

            }
        }

        // end of class declaration
        genIndent(pw, indent);
        pw.append("}");
        if (isAnonymousClass) {
            pw.append(";");
        }
        pw.append("\n");

    }

    /**
     * 
     * @param oldVersion
     * @param newVersion
     * @return the greatest common super class
     */
    static DSUClass[][] getGreatestCommonSuperClass(DSUClass oldVersion,
            DSUClass newVersion) {
        DSUClass[][] threeSets = new DSUClass[3][];

        // collect all old super classes
        int oldDepth = 0;
        DSUClass oldSuper = oldVersion.getSuperClass();
        while (oldSuper != null) {
            oldDepth++;
            oldSuper = oldSuper.getSuperClass();
        }
        DSUClass[] oldSuperClasses = new DSUClass[oldDepth];
        oldSuper = oldVersion.getSuperClass();
        for (int i = oldDepth - 1; i >= 0; i--) {
            oldSuperClasses[i] = oldSuper;
            oldSuper = oldSuper.getSuperClass();
        }

        //
        if (newVersion == null) {
            return new DSUClass[][] { { oldVersion.getSuperClass() },
                    oldSuperClasses, new DSUClass[0] };
        }

        // collect or old new class
        int newDepth = 0;
        DSUClass newSuper = newVersion.getSuperClass();
        while (newSuper != null) {
            newDepth++;
            newSuper = newSuper.getSuperClass();
        }

        DSUClass[] newSuperClasses = new DSUClass[newDepth];
        newSuper = newVersion.getSuperClass();
        for (int i = newDepth - 1; i >= 0; i--) {
            newSuperClasses[i] = newSuper;
            newSuper = newSuper.getSuperClass();
        }

        // do
        int length = oldDepth < newDepth ? oldDepth : newDepth;
        int i;
        oldSuper = null;
        newSuper = null;
        for (i = length - 1; i >= 0; i--) {
            oldSuper = oldSuperClasses[i];
            newSuper = newSuperClasses[i];
            if ((oldSuper.getNewVersion() == newSuper && newSuper
                    .getOldVersion() == oldSuper) ||
            // bootstrap class
                    oldSuper == newSuper) {
                // every class has at least a common super class
                // java.lang.Object.
                threeSets[0] = new DSUClass[] { oldSuperClasses[i] };
                if (i < oldDepth - 1) {
                    threeSets[1] = new DSUClass[oldDepth - 1 - i];
                    System.arraycopy(oldSuperClasses, i + 1, threeSets[1], 0,
                            threeSets[1].length);
                } else {
                    threeSets[1] = new DSUClass[0];
                }

                if (i < newDepth - 1) {
                    threeSets[2] = new DSUClass[newDepth - 1 - i];
                    System.arraycopy(newSuperClasses, i + 1, threeSets[2], 0,
                            threeSets[2].length);
                } else {
                    threeSets[2] = new DSUClass[0];
                }

                return threeSets;
            }
        }

        return new DSUClass[][] { { null }, {}, {} };
    }

    static void genLocalClass(int indent, PrintWriter pw, DSUClass oldVersion,
            DSUClass newVersion) {
        DSUMethod enclosingMethod = oldVersion.getEnclosingMethod();
        if (enclosingMethod != null) {
            genMethodHeader(indent, pw, enclosingMethod);
        } else {
            // may be in ...
            // guess in static block or field initial statement..
            genIndent(pw, indent);
            pw.append("static{\n");
        }
        genClassBody(indent + 1, pw, oldVersion, newVersion);
        genIndent(pw, indent);
        pw.append("}\n");
    }

    static void genMethodHeader(int indent, PrintWriter pw, DSUMethod method) {
        genIndent(pw, indent);

        if (method.isObjectInitializer()) {
            pw.append(method.getDeclaringClass().getSimpleName());
            pw.append(" (");
            pw.append(Utils.genMethodParameters(method.getDescriptor()));
            pw.append(") {\n");
        } else if (method.isClassInitializer()) {
            pw.append("static {\n");
        } else {
            pw.append(Utils.genMethodHeader(method.getName(),
                    method.getDescriptor()));
            if (method.isAbstract() || method.isNative()) {
                pw.append(";\n");
            } else {
                pw.append(" {\n");
            }
        }
    }

    static void genMethodsDeclaration(int indent, PrintWriter pw,
            DSUClass oldVersion) {

    }

    static final String ANN_IMPORT = "import org.javelus.*;\n";

    static void getAnnotationImport(PrintWriter pw) {
        pw.append(ANN_IMPORT);
    }

    static String getRename(String name) {
        return DEFAULT_PREFIX + name;
    }

    static void genFieldDeclaration(int indent, PrintWriter pw, DSUField field,
            boolean reName) {

        genIndent(pw, indent);
        /* currently we omit final modifier.. */
        pw.append(field.getAccessFlagsVerbose().replace("final", ""));

        pw.append(Utils.extractTypeFromDescriptor(field.getDescriptor()));
        pw.append(" ");

        if (reName) {
            pw.append(getRename(field.getName()));
        } else {
            pw.append(field.getName());
        }

        pw.append(";\n");
    }

    static void genConflictFieldsAnnotation(int indent, PrintWriter pw,
            List<DSUField> conflictFields) {
        if (conflictFields == null) {
            return;
        }
        int size = conflictFields.size();
        if (size > 0) {
            genIndent(pw, indent);
            pw.append("@ConflictFields({\"");
            DSUField f = conflictFields.get(0);
            pw.append(f.getDeclaringClass().getName());
            pw.append(" ");
            pw.append(f.getName());
            pw.append(f.getDescriptor());
            pw.append("\"");

            for (int i = 1; i < size; i++) {
                f = conflictFields.get(i);
                pw.append(",\"");
                pw.append(f.getDeclaringClass().getName());
                pw.append(" ");
                pw.append(f.getName());
                pw.append(f.getDescriptor());
                pw.append("\"");
            }
            pw.append("})\n");
        }
    }

    static void genDsuFieldAnnotation(int indent, PrintWriter pw, FieldUpdateType type) {

    }

    static void genOldFieldAnnotation(int indent, PrintWriter pw, DSUField field) {
        genIndent(pw, indent);
        pw.format("@OldField(clazz=\"%s\", name=\"%s\",signature=\"%s\")",
                field.getDeclaringClass().getName(), field.getName(),
                field.getDescriptor());
    }

    static void genOldFieldLocalDefinition(int indent, PrintWriter pw,
            DSUField field) {
        genIndent(pw, indent);
        pw.format("%s %s",
                Utils.extractTypeFromDescriptor(field.getDescriptor()),
                field.getName());
    }

    static void genNewLine(PrintWriter pw) {
        pw.append("\n");
    }

    static void genRemapFieldAnnotation(int indent, PrintWriter pw,
            String className, String name, String type) {
        genIndent(pw, indent);
        pw.format("@Remap(className=\"%s\",name=\"%s\",type=\"%s\")\n",
                className, name, type);
    }

    static final String OUTER_THIS = "this\\$[0-9]+[$]*";
    static final String VAL_LOCAL = "val\\$.*";

    /**
     * return true means generate
     * 
     * @param field
     * @return false if we guess this is a synthetic field like a pointer to
     *         outer class
     */
    static boolean testGenerateField(DSUField field) {
        if (field.isSynthetic()) {
            return !field.getName().matches(OUTER_THIS);
        }
        return true;
    }

    /**
     * if cls is a inner class it must be a static inner class
     * 
     * @param cls
     *            cls is a inner class
     * @return true if this inner class is a static inner class
     */
    static boolean testStaticInnerClass(DSUClass cls) {
        DSUField[] fields = cls.getDeclaredFields();
        for (DSUField f : fields) {
            // have a out this
            if (f.isSynthetic() && f.getName().matches(OUTER_THIS)) {
                return false;
            }
        }
        return true;
    }

}
