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

import org.objectweb.asm.tree.FieldNode;
import org.javelus.FieldUpdateType;
import org.javelus.dpg.io.Utils;

/**
 * @author tiger
 * 
 */
public class DSUField extends DSUMember<DSUField> {

    private FieldNode fieldNode;
    private FieldUpdateType updateType = FieldUpdateType.NONE;
    /**
     * ClassTypeCode 'L' ArrayTypeCode '[' VoidTypeCode 'V' BooleanTypeCode 'Z'
     * ByteTypeCode 'B' ShortTypeCode 'S' IntTypeCode 'I' LongTypeCode 'J'
     * FloatTypeCode 'F' DoubleTypeCode 'D' CharTypeCode 'C'
     * 
     * @param declaredClass
     * @param member
     */
    public DSUField(DSUClass declaredClass, FieldNode field) {
        super(declaredClass);
        this.fieldNode = field;
    }

    public boolean isInstanceField() {
        return (fieldNode.access & ACC_STATIC) == 0;
    }

    public boolean isStatic() {
        return (fieldNode.access & ACC_STATIC) != 0;
    }

    public boolean isFinal() {
        return (fieldNode.access & ACC_FINAL) != 0;
    }

    public boolean isSynthetic() {
        return (fieldNode.access & ACC_SYNTHETIC) != 0;
    }

    @Override
    public String getAccessFlagsVerbose() {
        return Utils.accessToString(fieldNode.access, Utils.FIELD);
    }

    @Override
    public String getDescriptor() {
        return fieldNode.desc;
    }

    @Override
    public String getName() {
        return fieldNode.name;
    }

    public FieldNode getFieldNode() {
        return fieldNode;
    }

    public int getAccess() {
        return fieldNode.access;
    }
    
    public FieldUpdateType getUpdateType() {
        return this.updateType;
    }
    
    public void setFieldUpdateType(FieldUpdateType type) {
        this.updateType = type;
    }

}
