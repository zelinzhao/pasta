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
package org.javelus.dpg.comparator;

import org.javelus.ClassUpdateType;
import org.javelus.MethodUpdateType;
import org.javelus.dpg.io.Utils;
import org.javelus.dpg.model.DSUClass;
import org.javelus.dpg.model.DSUMethod;
import org.javelus.dpg.model.DSU;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * @author tiger
 * 
 */
public class IndirectUpdate implements Opcodes {

    /**
     * @param klass
     * @param reloadedClasses
     * @return true if mc changed
     */
    public static boolean detectMCChanged(DSUClass klass, DSU update) {
        boolean mcChanged = false;
        DSUMethod[] methods = klass.getDeclaredMethods();
        for (DSUMethod m : methods) {
            if (m.bcChanged()) {
                continue;
            }
            if (doesMethodRefto(m, update)) {
                m.updateMethodUpdateType(MethodUpdateType.MC);
                mcChanged = true;
            }
        }
        if (mcChanged) {

            klass.updateChangedType(ClassUpdateType.MC);
        }

        return mcChanged;
    }

    /**
     * @param m
     * @param reloadedClass
     * @return true if method's MC changed
     */
    static boolean doesMethodRefto(DSUMethod m, DSU update) {
        InsnList insnNodes = m.getMethodNode().instructions;
        boolean isRefto = false;
        AbstractInsnNode insnNode = insnNodes.getFirst();
        while (insnNode != null) {
            int type = insnNode.getType();
            switch (type) {
            case AbstractInsnNode.FIELD_INSN:
                if (doesFieldInsnNodeRefto((FieldInsnNode) insnNode, m, update)) {
                    isRefto = true;
                }
                break;
            case AbstractInsnNode.METHOD_INSN:
                if (doesMethodInsnNodeRefto((MethodInsnNode) insnNode, m,
                        update)) {
                    isRefto = true;
                }
                break;
            case AbstractInsnNode.MULTIANEWARRAY_INSN:
                if (doesMultiANewArrayInsnNodeRefto(
                        (MultiANewArrayInsnNode) insnNode, m, update)) {
                    isRefto = true;
                }
                break;
            case AbstractInsnNode.TYPE_INSN:
                if (doesTypeInsnNodeRefto((TypeInsnNode) insnNode, m, update)) {
                    isRefto = true;
                }
                break;
            default:
            }
            insnNode = insnNode.getNext();
        }

        return isRefto;
    }

    /**
     * @param insn
     * @param method
     * @param classes
     *            , Redefined classes
     * @return true if field instruction refer to a offset changed field
     */
    static boolean doesFieldInsnNodeRefto(FieldInsnNode insn, DSUMethod method,
    		DSU update) {
        return update.constantPoolChanged(method.getDeclaringClass(),
                insn.owner, insn.name, insn.desc);
    }

    /**
     * @param insn
     * @param method
     * @param classes
     * @return true if method instruction refer to a offset changed method
     */
    static boolean doesMethodInsnNodeRefto(MethodInsnNode insn,
            DSUMethod method, DSU update) {
        return update.constantPoolChanged(method.getDeclaringClass(),
                insn.owner, insn.name, insn.desc);
    }

    /**
     * @param insn
     * @param classes
     * @return true if MultiANewArray instruction refer to a offset changed type
     */
    static boolean doesMultiANewArrayInsnNodeRefto(MultiANewArrayInsnNode insn,
            DSUMethod method, DSU update) {
        return false;
    }

    /**
     * FIXME! Array is special!
     * 
     * @param insn
     * @param classes
     * @return true if MultiANewArray instruction refer to changed type
     */
    static boolean doesTypeInsnNodeRefto(TypeInsnNode insn, DSUMethod method,
    		DSU update) {
        // getInnerMostInternalName here, for Dimension has been checked
        String internalName = Utils.getInnerMostInternalName(insn.desc);
        return update.constantPoolChanged(method.getDeclaringClass(),
                internalName);
    }

}
