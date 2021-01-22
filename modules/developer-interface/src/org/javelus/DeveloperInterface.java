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

public class DeveloperInterface {
    private DeveloperInterface() {
    }

    public static native void redefineSingleClass(String className,
            byte[] classFile);

    /**
     * use the HotSpot Style dynamic patch file.
     * 
     * @param dynamic_patch
     * @param sync
     */
    public static native void invokeDSU(String dynamic_patch, boolean sync);

    /**
     * 
     */
    public static native void invokeDSU();
    
    /**
     * 
     */
    public static native void invokeDSU(boolean sync);
    
    /**
     * get MixNewObject
     * 
     * @param obj
     * @return the MixNewObject if the obj is a MixOldObject
     */
    public static native Object getMixThat(Object obj);

    /**
     * copy all fields from old to new The old object and new object must have
     * the same type.
     * 
     * @param oldObj
     * @param newObj
     * @return null if failed..otherwise return the oldObject.
     */
    public static native Object replaceObject(Object oldObj, Object newObj);

    /**
     * redefine a set of classes
     * 
     * @param classNames
     *            an array of changed classes
     * @param classFiles
     *            an array of data of changed class files
     */
    public static native void redefineClasses(String[] classNames,
            byte[][] classFiles);

    public static native int currentRevisionNumber();
}
