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

import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * @author tiger
 * 
 */
public class InsnNodeComparator {

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareFieldInsnNode(FieldInsnNode insn1, FieldInsnNode insn2) {
        if (insn1.getOpcode() == insn2.getOpcode()
                && insn1.owner.equals(insn2.owner)
                && insn1.name.equals(insn2.name)
                && insn1.desc.equals(insn2.desc)) {
            return true;
        }
        return false;
    }

    /**
     * 
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareFrameNode(FrameNode insn1, FrameNode insn2) {
        if (insn1.getOpcode() != insn2.getOpcode()) {
            return false;
        }

        if (insn1.local.size() != insn2.local.size()) {
            return false;
        }
        // FIXME compare local & stack
        return true;
    }

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareIincInsnNode(IincInsnNode insn1, IincInsnNode insn2) {
        if (insn1.getOpcode() == insn2.getOpcode() && insn1.incr == insn2.incr
                && insn1.var == insn2.var) {
            return true;
        }
        return false;
    }

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareInsnNode(InsnNode insn1, InsnNode insn2) {
        if (insn1.getOpcode() == insn2.getOpcode()) {
            return true;
        }
        return false;
    }

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareIntInsnNode(IntInsnNode insn1, IntInsnNode insn2) {
        if (insn1.getOpcode() == insn2.getOpcode()
                && insn1.operand == insn2.operand) {
            return true;
        }
        return false;
    }

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareJumpInsnNode(JumpInsnNode insn1, JumpInsnNode insn2) {
        if (insn1.getOpcode() == insn2.getOpcode()) {
            return true;
        }
        return false;
    }

    /**
     * 比较标签节点
     * 
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareLabelNode(LabelNode insn1, LabelNode insn2) {
        return compareLabel(insn1.getLabel(), insn2.getLabel());
    }

    /**
     * 比较标签
     * 
     * @param label1
     * @param label2
     * @return true if the same
     */
    static boolean compareLabel(Label label1, Label label2) {
        boolean ans = true;
        try {
            ans = label1.getOffset() == label2.getOffset();
        } catch (Exception e) {
        }
        return ans;
    }

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareLdcInsnNode(LdcInsnNode insn1, LdcInsnNode insn2) {
        if (insn1.getOpcode() == insn2.getOpcode()
                && insn1.cst.equals(insn2.cst)) {
            return true;
        }
        return false;

    }

    /**
     * FIXME tbd
     * 
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareLineNumberNode(LineNumberNode insn1,
            LineNumberNode insn2) {
        return false;
    }

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    @SuppressWarnings("rawtypes")
    static boolean compareLookupSwitchInsnNode(LookupSwitchInsnNode insn1,
            LookupSwitchInsnNode insn2) {
        if (insn1.getOpcode() != insn2.getOpcode()) {
            return false;
        }
        if (!compareLabelNode(insn1.dflt, insn2.dflt)) {
            return false;
        }
        List keys1 = insn1.keys;
        List keys2 = insn2.keys;
        List labels1 = insn1.labels;
        List labels2 = insn2.labels;

        if (keys1.size() != keys2.size() || labels1.size() != labels2.size()) {
            return false;
        }

        for (int i = 0, length = keys1.size(); i < length; i++) {
            // compare Integer
            if (!keys1.get(i).equals(keys2.get(i))) {
                return false;
            }
            LabelNode label1 = (LabelNode) labels1.get(i);
            LabelNode label2 = (LabelNode) labels2.get(i);
            if (!compareLabelNode(label1, label2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareMethodInsnNode(MethodInsnNode insn1,
            MethodInsnNode insn2) {
        if (insn1.getOpcode() == insn2.getOpcode()
                && insn1.name.equals(insn2.name)
                && insn1.owner.equals(insn2.owner)
                && insn1.desc.equals(insn2.desc)) {
            return true;
        }
        return false;
    }

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareMultiANewArrayInsnNode(MultiANewArrayInsnNode insn1,
            MultiANewArrayInsnNode insn2) {
        if (insn1.desc.equals(insn2.desc) && insn1.dims == insn2.dims) {
            return true;
        }
        return false;
    }

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    @SuppressWarnings("rawtypes")
    static boolean compareTableSwitchInsnNode(TableSwitchInsnNode insn1,
            TableSwitchInsnNode insn2) {
        if (!compareLabelNode(insn1.dflt, insn2.dflt)) {
            return false;
        }
        if (insn1.min != insn2.min) {
            return false;
        }
        if (insn1.max != insn2.max) {
            return false;
        }
        List labels1 = insn1.labels;
        List labels2 = insn2.labels;
        if (labels1.size() != labels2.size()) {
            return false;
        }
        for (int i = 0, length = labels1.size(); i < length; i++) {
            if (!compareLabelNode((LabelNode) labels1.get(i),
                    (LabelNode) labels2.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareTypeInsnNode(TypeInsnNode insn1, TypeInsnNode insn2) {
        if (insn1.getOpcode() == insn2.getOpcode()
                && insn1.desc.equals(insn2.desc)) {
            return true;
        }
        return false;
    }

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    static boolean compareVarInsnNode(VarInsnNode insn1, VarInsnNode insn2) {
        if (insn1.getOpcode() == insn2.getOpcode() && insn1.var == insn2.var) {
            return true;
        }
        return false;
    }

    /**
     * @param insn1
     * @param insn2
     * @return true if the same
     */
    public static boolean compareAbstractInsnNode(AbstractInsnNode insn1,
            AbstractInsnNode insn2) {
        int type = insn1.getType();
        // int opcode = insn1.getOpcode();
        if (type != insn2.getType()// ||opcode!=insn2.getOpcode()
        ) {
            return false;
        }

        switch (type) {
        case AbstractInsnNode.FIELD_INSN:
            return compareFieldInsnNode((FieldInsnNode) insn1,
                    (FieldInsnNode) insn2);
        case AbstractInsnNode.IINC_INSN:
            return compareIincInsnNode((IincInsnNode) insn1,
                    (IincInsnNode) insn2);
        case AbstractInsnNode.INSN:
            return compareInsnNode((InsnNode) insn1, (InsnNode) insn2);
        case AbstractInsnNode.INT_INSN:
            return compareIntInsnNode((IntInsnNode) insn1, (IntInsnNode) insn2);
        case AbstractInsnNode.JUMP_INSN:
            return compareJumpInsnNode((JumpInsnNode) insn1,
                    (JumpInsnNode) insn2);
        case AbstractInsnNode.LDC_INSN:
            return compareLdcInsnNode((LdcInsnNode) insn1, (LdcInsnNode) insn2);
        case AbstractInsnNode.LOOKUPSWITCH_INSN:
            return compareLookupSwitchInsnNode((LookupSwitchInsnNode) insn1,
                    (LookupSwitchInsnNode) insn2);
        case AbstractInsnNode.METHOD_INSN:
            return compareMethodInsnNode((MethodInsnNode) insn1,
                    (MethodInsnNode) insn2);
        case AbstractInsnNode.MULTIANEWARRAY_INSN:
            return compareMultiANewArrayInsnNode(
                    (MultiANewArrayInsnNode) insn1,
                    (MultiANewArrayInsnNode) insn2);
        case AbstractInsnNode.TABLESWITCH_INSN:
            return compareTableSwitchInsnNode((TableSwitchInsnNode) insn1,
                    (TableSwitchInsnNode) insn2);
        case AbstractInsnNode.TYPE_INSN:
            return compareTypeInsnNode((TypeInsnNode) insn1,
                    (TypeInsnNode) insn2);
        case AbstractInsnNode.VAR_INSN:
            return compareVarInsnNode((VarInsnNode) insn1, (VarInsnNode) insn2);
        case AbstractInsnNode.LABEL:
            // return compareLabelNode((LabelNode)insn1,(LabelNode)insn2);
        case AbstractInsnNode.LINE:
            // return
            // compareLineNumberNode((LineNumberNode)insn1,(LineNumberNode)insn2);
        case AbstractInsnNode.FRAME:
            return true;// compareFrameNode((FrameNode)insn1, (FrameNode)
                        // insn2);
        default:
            return false;
        }

    }
}
