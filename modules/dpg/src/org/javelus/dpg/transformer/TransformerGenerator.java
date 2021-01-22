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

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.javelus.dpg.DynamicPatchGenerator;
import org.javelus.dpg.model.DSUClass;
import org.javelus.dpg.model.DSUClassStore;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author tiger
 * 
 */
public class TransformerGenerator implements TransformerConstants {

    /**
     */
    private ClassNode transformers;

    private String transformerName;

    private final Map<String, ClassNode> classes;

    /**
     * if a class has been visited, put its transformer into visited;
     */
    private Map<String, TransformerClass> visited;

    public TransformerGenerator(Map<String, ClassNode> classes, String name) {
        this.transformerName = name;
        this.classes = classes;
        this.visited = new HashMap<String, TransformerClass>(classes.size());

    }

    public TransformerGenerator(Map<String, ClassNode> classes) {
        this.transformerName = TRAN_NAME;
        this.classes = classes;
        this.visited = new HashMap<String, TransformerClass>(classes.size());
    }

    /**
     */
    void generateTransformers() {

        if (transformers != null) {
            return;
        }

        // String simpleName =
        // transformerName.substring(transformerName.lastIndexOf("/") + 1);

        transformers = new ClassNode();
        transformers.version = TRAN_VERSION;
        transformers.access = ACC_ABSTRACT + ACC_PUBLIC;
        transformers.name = transformerName;
        transformers.superName = "java/lang/Object";
        // transformers.sourceFile = simpleName + ".java";

        for (ClassNode cn : classes.values()) {
            mergeClass(cn);
        }
    }

    boolean hasVisited(String className) {
        return this.visited.keySet().contains(className);
    }

    TransformerClass getTransformerClass(String className) {
        return this.visited.get(className);
    }

    ClassNode getClassNode(String className) {
        return this.classes.get(className);
    }

    public TransformerClass mergeClass(String name) {
        ClassNode cn = getClassNode(name);
        if (cn == null) {
            return null;
        }
        return mergeClass(cn);
    }

    private TransformerClass mergeClass(ClassNode cn) {
        TransformerClass transformerClass = getTransformerClass(cn.name);
        if (transformerClass != null) {
            return transformerClass;
        }
        return visitClassNode(cn);
    }

    TransformerClass visitClassNode(ClassNode cn) {
        TransformerClass transformerClass = new TransformerClass(this);
        cn.accept(transformerClass);
        visited.put(cn.name, transformerClass);
        return transformerClass;
    }

    void addMethodNode(MethodNode mn) {
        transformers.methods.add(mn);
    }

    public String getTransformerName() {
        return this.transformerName;
    }

    public void accept(final ClassVisitor cv) {
        if (this.transformers == null) {
            generateTransformers();
        }
        this.transformers.accept(cv);
    }

    /**
     * @DILEPIS 输出合并的状态迁移器
     * @param parent
     * @throws Exception
     */
    public void write(File parent) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES
                + ClassWriter.COMPUTE_MAXS);
        accept(cw);

        File folders = new File(parent, transformerName.substring(0,
                transformerName.lastIndexOf("/") + 1));
        folders.mkdirs();

        FileOutputStream fos = new FileOutputStream(new File(parent,
                transformerName + ".class"));
        fos.write(cw.toByteArray());
        fos.close();
    }

    public static void main(String[] args) throws Exception {

        String ROOT;
        if (args.length == 1)
            ROOT = args[0];
        else
            ROOT = "/home/tiger/tmp/java/jdus/03-simple-class-adding-integer-field/transformers/";

        DSUClassStore classPath = DynamicPatchGenerator.createClassStore(ROOT);

        Map<String, ClassNode> classes = new HashMap<String, ClassNode>();

        Iterator<DSUClass> it = classPath.getClassIterator();
        while (it.hasNext()) {
            DSUClass cls = it.next();
            if (cls.isLoaded()) {
                classes.put(cls.getClassNode().name, cls.getClassNode());
            }
        }

        TransformerGenerator generator = new TransformerGenerator(classes,
                "JDUSTransformers");
        generator.write(new File(ROOT));

    }

}
