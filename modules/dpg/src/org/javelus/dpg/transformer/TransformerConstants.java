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

import org.objectweb.asm.Opcodes;

public interface TransformerConstants extends Opcodes {

    public static final int TRAN_VERSION = V1_6;
    public static final String TRAN_NAME = "JavelusTransformers";

    /**
     * used to find method in single class
     */
    public static final String CLASS_METHOD_NAME = "updateClass";
    public static final String CLASS_METHOD_DESC = "()V";

    public static final String OBJECT_METHOD_NAME = "updateObject";
    public static final String OBJECT_METHOD_DESC = "()V";

    public static final String ANONYMOUS_OBJECT_METHOD_NAME = "$Anonymous";

    /**
     * used to merge class method into Transformer
     */
    public static final String CLASS_CONVERT_NAME = "updateClass";
    public static final String OBJECT_CONVERT_NAME = "updateObject";

    String KEEPOFFSET_ANN = "Lorg/jikesrvm/dsu/annotation/KeepOffset;";

    String DSUFIELD_ANN = "Lorg/jikesrvm/dsu/annotation/DsuMember;";
    String DSUFIELD_ANN_CHANGEDTYPE = "value";

    String REMAP_ANN = "Lorg/jikesrvm/dsu/annotation/Remap;";
    String REMAP_CLASS = "className";
    String REMAP_NAME = "name";
    String REMAP_TYPE = "type";

    String CHANGEDTYPE_ENUM = "Lorg/jikesrvm/dsu/annotation/MemberChangedType;";
    String CHANGEDTYPE_DEL = "DELETE";
    String CHANGEDTYPE_MATCH = "MATCH";
    String CHANGEDTYPE_ADD = "ADD";

    String CONFLICTFIELDS_ANN = "Lorg.jikesrvm.dsu.annotation.ConflictFields;";

    String OUTER_THIS = "this\\$[0-9]+[$]*";
    String VAL_LOCAL = "val\\$.*";

    String ANONYMOUS_CONSTRUCTOR_NAME = "$Anonymous";

    String DEFAULT_RENAME_PREFIX = "_old_";
}
