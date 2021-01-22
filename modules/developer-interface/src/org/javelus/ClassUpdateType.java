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
package org.javelus;

public enum ClassUpdateType implements UpdateType {
    /* MATCH */
    NONE,
    /* Only Machine Code */
    MC,
    /* Only Bytecode */
    BC,
    /* Class signature changed */
    CC,

    /* Only OLD */
    DEL,
    /**
     * Only NEW
     */
    ADD;

    /**
     * this join larger
     * 
     * @param larger
     * @return
     */
    public ClassUpdateType join(ClassUpdateType larger) {
        if (larger == this) {
            return this;
        }

        if (compareTo(larger) > 0) {
            return larger.join(this);
        }

        switch (larger) {
        case NONE:
        case MC:
        case BC:
        case CC:
            return larger;
        case DEL:
        case ADD:
            if (!isUnchanged()) {
                throw new RuntimeException("sanity check failed! this is " + this + " that is " + larger);
            }
            return larger;
        default:
        }
        throw new RuntimeException("Should not reach here");
    }

    /**
     * XXX
     * 
     * @return
     */
    public int intValue() {
        return this.ordinal();
    }

    public boolean isDeleted() {
        return this == DEL;
    }

    public boolean isAdded() {
        return this == ADD;
    }

    /**
     * Now, all machine code changed are collected at runtime during a walk of SystemDictionary.
     */
    public boolean isChanged() {
        return this == BC || this == CC;
    }

    @Override
    public boolean isUnchanged() {
        return this == NONE || this == MC;
    }

}