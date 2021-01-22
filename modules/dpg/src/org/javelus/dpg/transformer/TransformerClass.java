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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author tiger
 * 
 */
public class TransformerClass extends ClassVisitor implements Opcodes,
        TransformerConstants {

    final TransformerGenerator generator;

    private String className;
    private String superName;
    private TransformerClass superTransformer;

    private MethodNode objectUpdater;

    private int copyCount = 0;

    /**
     * 
     * @param generator
     */
    public TransformerClass(TransformerGenerator generator) {
        super(Opcodes.ASM5);
        this.generator = generator;
    }

    public TransformerClass(TransformerGenerator generator, String className) {
        super(Opcodes.ASM5);
        this.generator = generator;
        this.className = className;
    }

    int allocateLocalOffset() {
        return copyCount++;
    }

    public TransformerClass getSuperTransformer() {
        return superTransformer;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSuperName() {
        return superName;
    }

    public void setSuperName(String superName) {
        this.superName = superName;
    }

    /**
     * @author tiger
     * 
     */

    /**
     * 
     * @see org.objectweb.asm.ClassVisitor#visit(int, int, java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String[])
     */
    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        this.className = name;
        this.superName = superName;

        // superClass
        superTransformer = generator.mergeClass(superName);
        // outer class
        // if( name.indexOf('$') !=0){
        //
        // }
    }

    @Override
    public void visitSource(String source, String debug) {
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return null;
    }

    @Override
    public void visitAttribute(Attribute attr) {
    }

    @Override
    public void visitInnerClass(String name, String outerName,
            String innerName, int access) {
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc,
            String signature, Object value) {
        return null;
    }

    /**
     * 
     * @param className
     *            e.g. java.lang.Object
     * @return (Ljava/lang/Object;)V
     */
    public static String getTransformerDesc(String className) {
        return "(L" + className.replace('.', '/') + ";)V";
    }

    /**
     * visit method and transform it
     * 
     * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String[])
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        if (name.equals(TransformerGenerator.CLASS_METHOD_NAME)
        /* && desc.equals(TransformerGenerator.CLASS_METHOD_DESC) */) {
            MethodNode mn = new MethodNode(ACC_PUBLIC + ACC_STATIC
                    + ACC_SYNTHETIC, TransformerGenerator.CLASS_CONVERT_NAME,
                    desc, signature, exceptions);
            generator.addMethodNode(mn);
            return mn;
        } else if (name.equals(TransformerGenerator.OBJECT_METHOD_NAME)
        /* && desc.equals(TransformerGenerator.OBJECT_METHOD_DESC) */) {
            MethodNode mn = new MethodNode(
                    ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC,
                    TransformerGenerator.OBJECT_CONVERT_NAME,
                    desc.replace("(", "(L" + className.replace(".", "/") + ";"),
                    signature, exceptions);
            generator.addMethodNode(mn);
            // this.objectUpdater = mv;
            return mn;
        }
        return null;
    }

    @Override
    public void visitEnd() {
        // add this method to
        // if (objectUpdater != null) {
        // generator.addMethodNode(objectUpdater);
        // }

    }

    public MethodNode getObjectUpdater() {
        return objectUpdater;
    }

    public int getCopyCount() {
        // TODO Auto-generated method stub
        return copyCount;
    }

    public int getSuperCopyCount() {
        if (superTransformer != null) {
            return superTransformer.getCopyCount();
        }
        return 0;
    }

}
