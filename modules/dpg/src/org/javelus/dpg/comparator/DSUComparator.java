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

import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;

import org.javelus.ClassUpdateType;
import org.javelus.FieldUpdateType;
import org.javelus.MethodUpdateType;
import org.javelus.dpg.DynamicPatchGenerator;
import org.javelus.dpg.model.DSUClass;
import org.javelus.dpg.model.DSUField;
import org.javelus.dpg.model.DSUMethod;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author tiger
 * 
 */
public class DSUComparator {


    /**
     * @param klass
     * @return true if this class should be loaded during DSU
     */
    public static boolean checkRedefineClass(DSUClass klass) {
        if (!klass.isLoaded()) {
            return false;
        }
        if (klass.isLibraryClass()) {
            return false;
        }

        DSUClass superClass = klass.getSuperClass();
        if (superClass.isLoaded()) {
            if (superClass.isDeleted()) {
                klass.updateChangedType(ClassUpdateType.CC);
            } else if (superClass.needRedefineClass()) {
                klass.updateChangedType(ClassUpdateType.CC);
            }
        }

        for (DSUClass intf : klass.getDeclaredInterfaces()) {
            if (intf.isLoaded()) {
                if (intf.isDeleted()) {
                    klass.updateChangedType(ClassUpdateType.CC);
                } else if (intf.needRedefineClass()) {
                    klass.updateChangedType(ClassUpdateType.CC);
                }
            }
        }

        return klass.needRedefineClass();
    }

    /**
     * @param oldClass
     * @param newClass
     */
    public static void compareClassStructure(DSUClass oldClass, DSUClass newClass) {
        ClassNode oldNode = oldClass.getClassNode();
        ClassNode newNode = newClass.getClassNode();

        compareFields(oldClass, newClass);
        compareMethods(oldClass, newClass);

        if (!oldNode.superName.equals(newNode.superName)) {
            oldClass.updateChangedType(ClassUpdateType.CC);
        }
        
        List<String> oldInterfaces = oldNode.interfaces;
        List<String> newInterfaces = newNode.interfaces;
        if (oldInterfaces.size() != newInterfaces.size()) {
            oldClass.updateChangedType(ClassUpdateType.CC);
        } else {
            for (String old:oldInterfaces) {
                if (!newInterfaces.contains(old)) {
                    oldClass.updateChangedType(ClassUpdateType.CC);
                }
            }
        }

        if (!compareAccess(oldNode.access, newNode.access)) {
            oldClass.updateChangedType(ClassUpdateType.CC);
            System.err.format("ClassNodeComparator: access flags changed of %s, old access is %s, new access is %s\n",
                    oldNode.name,
                    oldClass.getAccessFlagsVerbose(), 
                    newClass.getAccessFlagsVerbose());
        } 

    }


    public static int compareMethods(DSUMethod o1, DSUMethod o2) {
        int ans = o1.getName().compareTo(o2.getName());
        if (ans == 0) {
            return o1.getDescriptor().compareTo(o2.getDescriptor());
        }
        return ans;
    }

    public static void compareFields(DSUClass oldClass, DSUClass newClass) {
        for (DSUField field:oldClass.getDeclaredFields()) {
            if (!field.hasNewVersion()) {
                oldClass.updateChangedType(ClassUpdateType.CC);
                field.setFieldUpdateType(FieldUpdateType.DEL);
            } else if (!compareAccess(field.getAccess(), field.getNewVersion().getAccess())) {
                oldClass.updateChangedType(ClassUpdateType.CC);
            }
        }
        for (DSUField field:newClass.getDeclaredFields()) {
            if (!field.hasOldVersion()) {
                oldClass.updateChangedType(ClassUpdateType.CC);
                field.setFieldUpdateType(FieldUpdateType.ADD);
            }
        }
    }
    
    static boolean compareAccess(int a1, int a2) {
        if (DynamicPatchGenerator.outputXML) {
            return (Modifier.isStatic(a1) && Modifier.isStatic(a2))
                    || (!Modifier.isStatic(a1) && !Modifier.isStatic(a2));
        }
        return a1 == a2;
    }


    public static void compareMethods(DSUClass oldClass, DSUClass newClass) {
        for (DSUMethod method:oldClass.getDeclaredMethods()) {
            if (!method.hasNewVersion()) {
                oldClass.updateChangedType(ClassUpdateType.CC);
                method.updateMethodUpdateType(MethodUpdateType.DEL);
            } else if (!compareAccess(method.getAccess(), method.getNewVersion().getAccess())) {
                oldClass.updateChangedType(ClassUpdateType.CC);
            } else if (!compareMethodBody(method, method.getNewVersion())) {
                method.updateMethodUpdateType(MethodUpdateType.BC);
                method.getNewVersion().updateMethodUpdateType(MethodUpdateType.BC);
                oldClass.updateChangedType(ClassUpdateType.BC);
            }
        }
        for (DSUMethod method:newClass.getDeclaredMethods()) {
            if (!method.hasOldVersion()) {
                oldClass.updateChangedType(ClassUpdateType.CC);
                method.updateMethodUpdateType(MethodUpdateType.ADD);
            }
        }
    }
    
    public static boolean compareFieldNode(FieldNode f1, FieldNode f2) {
        return f1.name.equals(f2.name) && f1.desc.equals(f2.desc)
                && compareAccess(f1.access, f2.access);
    }

    public static boolean compareMethodNode(MethodNode m1, MethodNode m2) {
        return m1.name.equals(m2.name) && m1.desc.equals(m2.desc)
                && compareAccess(m1.access, m2.access);
    }
    
    /**
     * 
     * @param oldMethod
     * @param newMethod
     * @return true if they are the same
     */
    private static boolean compareMethodBody(DSUMethod oldMethod, DSUMethod newMethod) {
        if (oldMethod.hasCode()) {
            if (newMethod.hasCode()) {
                if (!areMethodCodeTheSame(
                        oldMethod.getMethodNode(),
                        newMethod.getMethodNode())) {
                    return false;
                }
            } else if (newMethod.isAnnotationMethod()) {
                throw new RuntimeException("Not implemented yet");
            } else {
                // old has code but new doesn't
                return false;
            }
        } else if (oldMethod.isAnnotationMethod()) {
            // no comparison of annotation methods
        } else {
            // a interface method
            if (newMethod.hasCode()) {
                return false;
            } else if (newMethod.isAnnotationMethod()) {
                throw new RuntimeException("Not implemented yet");
            } else {
                // both have no code
            }
        }
        
        return true;
    }

    /**
     * @param oldMethod
     * @param newMethod
     * @return true if both method's code are same
     */
    public static boolean areMethodCodeTheSame(MethodNode oldMethod,
            MethodNode newMethod) {
        InsnList oldInsnList = oldMethod.instructions;
        InsnList newInsnList = newMethod.instructions;

        int oldSize = oldInsnList.size();
        int newSize = newInsnList.size();

        if (oldSize != newSize) {
            return false;
        }

        Iterator<AbstractInsnNode> it1 = oldInsnList.iterator();
        Iterator<AbstractInsnNode> it2 = newInsnList.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            AbstractInsnNode node1 = it1.next();
            AbstractInsnNode node2 = it2.next();
            if (!InsnNodeComparator.compareAbstractInsnNode(node1, node2)) {
                return false;
            }
        }
        return true;

    }

    /**
     * @param oldMethod
     * @param newMethod
     * @return true if both annotation methods are same
     */
    public static boolean areAnnotationMethodTheSame(MethodNode oldMethod,
            MethodNode newMethod) {
        Object oldAnnotationDefault = oldMethod.annotationDefault;
        Object newAnnotationDefault = oldMethod.annotationDefault;

        // Annotation Method
        if (oldAnnotationDefault == null) {
            if (newAnnotationDefault == null) {
                return true;
            } else {
                return false;
            }
        } else {
            if (newAnnotationDefault == null) {
                return false;
            } else {
                return compareAnnotationDefault(oldAnnotationDefault,
                        newAnnotationDefault);
            }
        }
    }

    /**
     * @param ann1
     * @param ann2
     * @return true if both annotation methods annotationdefault are same
     */
    public static boolean compareAnnotationDefault(Object ann1, Object ann2) {
        Class<?> cls1 = ann1.getClass();
        Class<?> cls2 = ann2.getClass();
        if (cls1 != cls2) {
            return false;
        }
        if (ann1 instanceof List && ann2 instanceof List) {
            return compareAnnotationDefault((List<?>)ann1, (List<?>)ann2);
        } else if (ann1 instanceof AnnotationNode
                && ann2 instanceof AnnotationNode) {
            AnnotationNode aNode1 = (AnnotationNode) ann1;
            AnnotationNode aNode2 = (AnnotationNode) ann1;
            if (!aNode1.desc.equals(aNode2.desc)) {
                return false;
            }
            // compare List
            // FIXME to be decide
            return compareAnnotationDefault(aNode1.values, aNode2.values);
        } else {
            //
            return ann1.equals(ann2);
        }
    }

    static boolean compareAnnotationDefault(List<?> values1, List<?> values2) {
        int size = values1.size();
        if (size != values2.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            Object ann1 = values1.get(i);
            Object ann2 = values2.get(i);
            if (!ann1.equals(ann2)) {
                return false;
            }
        }
        return true;
    }
}
