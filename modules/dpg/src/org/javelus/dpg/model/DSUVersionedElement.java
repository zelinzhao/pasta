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

import org.objectweb.asm.Opcodes;

/**
 * @author tiger
 * 
 */
public abstract class DSUVersionedElement<E extends DSUVersionedElement<E>>
        implements
        /* AnnotatedMember , */Opcodes {

    protected E oldVersion;

    protected E newVersion;

    public E getOldVersion() {
        return oldVersion;
    }

    /**
     * @param oldVersion
     */
    public void setOldVersion(E oldVersion) {
        this.oldVersion = oldVersion;
    }

    /**
     * @return the new version of this element
     */
    public E getNewVersion() {
        return newVersion;
    }

    /**
     * @param newVersion
     */
    public void setNewVersion(E newVersion) {
        this.newVersion = newVersion;
    }

    /**
     * @return true if it has
     */
    public boolean hasOldVersion() {
        return oldVersion != null;
    }

    /**
     * @return true if it has
     */
    public boolean hasNewVersion() {
        return newVersion != null;
    }
}
