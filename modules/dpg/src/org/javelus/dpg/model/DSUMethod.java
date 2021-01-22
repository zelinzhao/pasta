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
package org.javelus.dpg.model;

import org.javelus.MethodUpdateType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import org.javelus.dpg.io.Utils;

/**
 * @DILEPIS Java类方法对应在JikesRVM虚拟机中对应的抽象表示，部分方法逻辑仿照JikesRVM的类加载过程。
 * @author tiger
 * 
 */
public class DSUMethod extends DSUMember<DSUMethod> implements Opcodes {

    public static final String STANDARD_OBJECT_INITIALIZE = "<init>";

    public static final String STANDARD_CLASS_INITIALIZE = "<clinit>";

    private MethodNode methodNode;

    private MethodUpdateType updateType = MethodUpdateType.NONE;

    public DSUMethod(DSUClass declaredClass, MethodNode method) {
        super(declaredClass);
        this.methodNode = method;
    }

    /**
     * @return true if it is a instance method
     */
    public boolean isInTIB() {
        return (methodNode.access & ACC_STATIC) == 0;
    }

    public static boolean isObjectInitializer(String name) {
        return name.equals(STANDARD_OBJECT_INITIALIZE);
    }

    public static boolean isClassInitializer(String name) {
        return name.equals(STANDARD_CLASS_INITIALIZE);
    }

    public int getAccess() {
        return methodNode.access;
    }
    
    /**
     * maybe useless
     * 
     * @return true if it is not an instance method
     */
    public boolean isInJTOC() {
        // TODO ClassInitialize is in JTOC.
        // But during resolve time , rvm just ignores it because it can be
        // invoked only
        // once just after RVMClass have been created.
        return (methodNode.access & ACC_STATIC) != 0 || isObjectInitializer()
                || isClassInitializer();
    }

    public boolean isStatic() {
        return (methodNode.access & ACC_STATIC) != 0;
    }

    public boolean isObjectInitializer() {
        return methodNode.name.equals(STANDARD_OBJECT_INITIALIZE);
    }

    public boolean isClassInitializer() {
        return methodNode.name.equals(STANDARD_OBJECT_INITIALIZE);
    }

    public MethodNode getMethodNode() {
        return methodNode;
    }

    @Override
    public String getAccessFlagsVerbose() {
        return Utils.accessToString(methodNode.access, Utils.METHOD);
    }

    @Override
    public String getDescriptor() {
        return methodNode.desc;
    }

    @Override
    public String getName() {
        return methodNode.name;
    }

    private static int NullMethodId = 0;
    private static final Type[] NullMethodArg = new Type[0];

    /**
     * create null synthetic private method in declaredClass with unique name
     * and never be override
     * 
     * @param declaredClass
     * @return a null placeholder method
     */
    public static DSUMethod createNullMethod(DSUClass declaredClass) {
        MethodNode nullMethod = new MethodNode();
        nullMethod.access = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE;
        nullMethod.name = String.format("null$%d", NullMethodId++);
        nullMethod.desc = Type.getMethodDescriptor(Type.VOID_TYPE,
                NullMethodArg);

        nullMethod.instructions = new InsnList();
        nullMethod.instructions.add(new InsnNode(RETURN));

        nullMethod.maxStack = 0;
        nullMethod.maxLocals = 1;

        return new DSUMethod(declaredClass, nullMethod);
    }

    public void updateMethodUpdateType(MethodUpdateType type) {
        if (type.compareTo(this.updateType) > 0) {
            this.updateType = type;
        }
    }

    public MethodUpdateType getMethodUpdateType() {
        return updateType;
    }

    public boolean bcChanged() {
        return this.updateType == MethodUpdateType.BC;
    }

    public boolean isAbstract() {
        return (methodNode.access & ACC_ABSTRACT) != 0;
    }

    public boolean isNative() {
        return (methodNode.access & ACC_NATIVE) != 0;
    }

    public boolean hasCode() {
        return (methodNode.access & ACC_ABSTRACT) == 0;
    }

    public boolean isAnnotationMethod() {
        return declaredClass.isAnnotation();
    }

    public DSUMethod getNewVersion() {
        return (DSUMethod) newVersion;
    }

}
