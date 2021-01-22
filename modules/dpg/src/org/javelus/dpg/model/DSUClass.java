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

import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.javelus.ClassUpdateType;
import org.javelus.dpg.comparator.DSUComparator;
import org.javelus.dpg.io.Utils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 
 * @author tiger
 * 
 */
public class DSUClass extends DSUVersionedElement<DSUClass> {

    class MethodIterator implements Iterator<DSUMethod> {

        int curIndex = -1;

        boolean isStatic;

        MethodIterator(boolean isStatic) {
            this.isStatic = isStatic;
            moveNext();
        }

        private void moveNext() {
            for (curIndex = curIndex + 1; curIndex < declaredMethods.length; curIndex++) {
                if (declaredMethods[curIndex].isStatic() == isStatic) {
                    break;
                }
            }
        }

        @Override
        public boolean hasNext() {
            return curIndex < declaredMethods.length;
        }

        @Override
        public DSUMethod next() {
            DSUMethod next = declaredMethods[curIndex];
            moveNext();
            return next;
        }

        @Override
        public void remove() {
        }

    }

    class FieldIterator implements Iterator<DSUField> {

        int curIndex = -1;

        boolean isStatic;

        FieldIterator(boolean isStatic) {
            this.isStatic = isStatic;
            moveNext();
        }

        private void moveNext() {
            for (curIndex = curIndex + 1; curIndex < declaredFields.length; curIndex++) {
                if (declaredFields[curIndex].isStatic() == isStatic) {
                    break;
                }
            }
        }

        @Override
        public boolean hasNext() {
            return curIndex < declaredFields.length;
        }

        @Override
        public DSUField next() {
            DSUField next = declaredFields[curIndex];
            moveNext();
            return next;
        }

        @Override
        public void remove() {
        }

    }

    private URL classFile;

    private ClassUpdateType updateType = ClassUpdateType.NONE;

    public static DSUField[] EMPTY_FIELD_ARRAY = new DSUField[0];

    private String name;

    private DSUClass superClass;

    private HashSet<DSUClass> subClasses;

    private DSUClass[] declaredInterfaces;

    private DSUClass[] declaredClasses;

    private DSUMethod[] declaredMethods;

    private DSUField[] declaredFields;

    private DSUClass declaringClass;

    private final DSUClassStore classStore;
    /**
     * @DILEPIS 类所在的包
     */
    private DSUPackage pkg;
    /**
     * @DILEPIS 该类相关的类节点，参见ASM包
     */
    private ClassNode classNode;

    public DSUClass(DSUClassStore classStore, String className) {
        this(classStore, className, null, null);
    }

    public DSUClass(DSUClassStore classStore, String className,
            URL classFile,
            ClassNode classNode) {
        this.classStore = classStore;
        this.classFile = classFile;
        this.name = className;
        this.subClasses = new HashSet<DSUClass>();
        setClassNode(classNode);
    }

    public DSUPackage getPackage() {
        return pkg;
    }

    @SuppressWarnings("rawtypes")
    private void init() {

        try {
            if (name != null && !name.equals(classNode.name)) {
                System.err.println("ClassNode Error: name " + name
                        + " node name " + classNode.name);
            }
            // this.name = classNode.name;

            int end = name.replace("/", ".").lastIndexOf("."); 

            pkg = DSUPackage.createPackage(end == -1 ? null 
                    : name.substring(0, end), classStore);
            pkg.addRVMClass(this);
            // Super Class
            String superName = classNode.superName;
            if (superName == null) {

            } else {
                DSUClass superClass = classStore.getOrCreate(superName);
                if (superClass == null) {
                    System.err.println("SuperClassNull");
                }
                this.superClass = superClass;
                if (superClass != DSUBootstrapClassStore.java_lang_Object_class) {
                    this.superClass.addSubClass(this);
                }

            }

            // interfaces
            List itfcs = classNode.interfaces;
            declaredInterfaces = new DSUClass[itfcs.size()];
            for (int i = 0; i < itfcs.size(); i++) {
                String ifceName = (String) itfcs.get(i);
                DSUClass iface = classStore.getOrCreate(ifceName);
                if (iface == null) {
                    System.err.println("Class " + name
                            + "'s interface is null, interface name is "
                            + ifceName);
                }
                declaredInterfaces[i] = iface;
                if (isInterface()) {
                    iface.addSubClass(this);
                }
            }

            if (isInterface()) {
                // XXX we append interface to its parent interface

            }

            // There are five kinds of classes (or interfaces):
            // a) Top level classes
            // b) Nested classes (static member classes)
            // c) Inner classes (non-static member classes)
            // d) Local classes (named classes declared within a method)
            // e) Anonymous classes
            // ClassNode: innerClasses outerClass outerMethod outerMethodDesc
            if (classNode.outerClass != null) {
                // a local class
                declaringClass = classStore.getOrCreate(classNode.outerClass);
                DSUClass[] cls = declaringClass.declaredClasses;
                if (declaringClass.classNode != null && cls != null) {
                    for (int i = 0; i < cls.length; i++) {
                        if (cls[i] == null) {
                            cls[i] = this;
                            break;
                        }
                    }
                }
            }
            //
            // // if(classNode.innerClasses.size() > 0){
            List innerClasses = classNode.innerClasses;
            declaredClasses = new DSUClass[innerClasses.size()];
            for (int i = 0; i < innerClasses.size(); i++) {
                InnerClassNode innerNode = (InnerClassNode) innerClasses.get(i);

                if (innerNode.outerName == null) {
                    // may be a local class or anonymous class
                    DSUClass localClass = classStore.getOrCreate(innerNode.name);
                    if (localClass.classNode != null
                            && localClass.classNode.outerClass != null
                            && localClass.classNode.outerClass
                            .equals(this.name)) {
                        declaredClasses[i] = localClass;
                        localClass.declaringClass = this;
                    }
                } else if (innerNode.outerName.equals(this.name)
                        && innerNode.name != null) {
                    // must be a declared inner class
                    DSUClass innerClass = classStore.getOrCreate(innerNode.name);
                    if (innerClass == null) {
                        System.err.println("Class " + name
                                + "'s innerClass is null, inner class name is "
                                + innerNode);
                    }
                    declaredClasses[i] = innerClass;
                    innerClass.declaringClass = this;
                }
            }

            // Methods

            List methodNodes = classNode.methods;
            declaredMethods = new DSUMethod[methodNodes.size()];
            for (int i = 0; i < methodNodes.size(); i++) {
                declaredMethods[i] = new DSUMethod(this,
                        (MethodNode) methodNodes.get(i));
            }

            // Fields
            List fieldNodes = classNode.fields;
            declaredFields = new DSUField[fieldNodes.size()];
            for (int i = 0; i < fieldNodes.size(); i++) {
                declaredFields[i] = new DSUField(this,
                        (FieldNode) fieldNodes.get(i));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public DSUClassStore getClassStore() {
        return classStore;
    }

    public ClassNode getClassNode() {
        return classNode;
    }

    public void setClassNode(ClassNode classNode) {
        if(isLibraryClass()){
            return;
        }
        if(classNode == null){
            return;
        }
        if (this.classNode == null) {
            this.classNode = classNode;
            init();
        }
    }

    public DSUMethod[] getDeclaredMethods() {
        return declaredMethods;
    }

    public void setDeclaredMethods(DSUMethod[] declaredMethods) {
        this.declaredMethods = declaredMethods;
    }

    public DSUField[] getDeclaredFields() {
        return declaredFields;
    }

    public void setDeclaredFields(DSUField[] declaredFields) {
        this.declaredFields = declaredFields;
    }

    public DSUClass[] getDeclaredInterfaces() {
        return declaredInterfaces;
    }

    public DSUClass[] getDeclaredClasses() {
        int count = 0;
        for (int i = 0; i < declaredClasses.length; i++) {
            if (declaredClasses[i] != null) {
                count++;
            }
        }

        DSUClass[] cls = new DSUClass[count];
        count = 0;
        for (int i = 0; i < declaredClasses.length; i++) {
            if (declaredClasses[i] != null) {
                cls[count++] = declaredClasses[i];
            }
        }
        return cls;
    }

    public DSUClass getDeclaringClass() {
        return declaringClass;
    }

    public DSUMethod getEnclosingMethod() {
        if (declaringClass != null) {
            return declaringClass.getMethod(classNode.outerMethod,
                    classNode.outerMethodDesc);
        }
        return null;
    }

    public void setDeclaredInterfaces(DSUClass[] declaredInterface) {
        this.declaredInterfaces = declaredInterface;
    }

    public Iterator<DSUField> getStaticFields() {
        return new FieldIterator(true);
    }

    public Iterator<DSUField> getInstanceFields() {
        return new FieldIterator(false);
    }

    public Iterator<DSUMethod> getStaticMethods() {
        return new MethodIterator(true);
    }

    public Iterator<DSUMethod> getInstanceMethods() {
        return new MethodIterator(false);
    }

    public DSUClass getSuperClass() {
        return superClass;
    }

    public void setSuperClass(DSUClass superClass) {
        this.superClass = superClass;
    }

    public HashSet<DSUClass> getSubClasses() {
        return subClasses;
    }

    public void addSubClass(DSUClass sub) {
        if (!this.subClasses.contains(sub)) {
            this.subClasses.add(sub);
        }
    }

    public boolean isInterface() {
        return (classNode.access & ACC_INTERFACE) != 0;
    }

    public boolean isAnnotation() {
        return (classNode.access & ACC_ANNOTATION) != 0;
    }

    public boolean isEnum() {
        return (classNode.access & ACC_ENUM) != 0;
    }

    public boolean isAbstract() {
        return (classNode.access & ACC_ABSTRACT) != 0;
    }

    public boolean isJavaLangObject() {
        return superClass == null;
    }

    /**
     * is a static inner class
     * 
     * @return
     */
    public boolean isStatic() {
        return (classNode.access & ACC_STATIC) != 0;
    }

    public boolean isSynthetic() {
        return (classNode.access & ACC_SYNTHETIC) != 0;
    }

    /**
     * @DILEPIS 判断该类是否在分析时被加载
     * @return true if this class has been loaded
     */
    public boolean isLoaded() {
        return classNode != null;
    }

    public boolean isFullLoaded() {
        if (classNode == null) {
            return false;
        }

        if (superClass != null) {
            return superClass.isLoaded();
        }

        return true;
    }

    /**
     * @DILEPIS 设置该类的新版本，同时为该类的所有方法和域设置新版本。
     */
    @Override
    public void setNewVersion(DSUClass newVersion) {
        super.setNewVersion(newVersion);
        DSUClass clazz = (DSUClass) newVersion;

        if (clazz.getClassNode() == null) {
            return;
        }

        DSUField[] newFields = clazz.getDeclaredFields();
        for (DSUField f1 : declaredFields) {
            for (DSUField f2 : newFields) {
                if (DSUComparator.compareFieldNode(f1.getFieldNode(), f2.getFieldNode())) {
                    f1.setNewVersion(f2);
                    f2.setOldVersion(f1);
                }
            }
        }

        DSUMethod[] newMethods = clazz.getDeclaredMethods();
        for (DSUMethod m1 : declaredMethods) {
            for (DSUMethod m2 : newMethods) {
                if (DSUComparator.compareMethodNode(m1.getMethodNode(), m2.getMethodNode())) {
                    m1.setNewVersion(m2);
                    m2.setOldVersion(m1);
                }
            }
        }
    }

    public DSUMethod getDeclaredMethod(String name, String descriptor) {
        for (DSUMethod m : declaredMethods) {
            if (m.getName().equals(name)
                    && m.getDescriptor().equals(descriptor)) {
                return m;
            }
        }
        return null;
    }

    public DSUField getDeclaredField(String name, String descriptor) {
        for (DSUField f : declaredFields) {
            if (f.getName().equals(name)
                    && f.getDescriptor().equals(descriptor)) {
                return f;
            }
        }
        return null;
    }

    public DSUField getField(String name, String descriptor) {
        DSUField field = getDeclaredField(name, descriptor);
        if (field != null) {
            return field;
        }
        for (DSUClass i : declaredInterfaces) {
            field = i.getDeclaredField(name, descriptor);
            if (field != null) {
                return field;
            }
        }
        if (getSuperClass() != null) {
            return getSuperClass().getDeclaredField(name, descriptor);
        }
        return null;
    }

    public String getAccessFlagsVerbose() {
        return Utils.accessToString(classNode.access, Utils.CLASS);
    }
    
    /**
     * used only for calcIndirect to decide offset
     * 
     * @param name
     * @param descriptor
     * @return getMethod by name and descriptor
     */
    public DSUMethod getMethod(String name, String descriptor) {
        DSUMethod method = getDeclaredMethod(name, descriptor);
        if (method != null) {
            return method;
        }

        if (getSuperClass() != null && getSuperClass().isLoaded()) {
            return getSuperClass().getDeclaredMethod(name, descriptor);
        }
        return null;
    }

    /**
     * 
     * @return the simple name of this class
     */
    public String getSimpleName() {
        String simpleName = "";
        if (declaringClass != null) {
            simpleName = name.substring(declaringClass.name.length());
            int length = simpleName.length();
            int index = 1;
            while (index < length && isAsciiDigit(simpleName.charAt(index)))
                index++;
            simpleName.substring(index);
            return simpleName.substring(index);
        }
        return name.substring(name.replace("/", ".").lastIndexOf(".") + 1);
    }

    private static boolean isAsciiDigit(char c) {
        return '0' <= c && c <= '9';
    }

    public int getAccess() {
        return classNode.access;
    }

    public void updateChangedType(ClassUpdateType type) {
        updateType = updateType.join(type);
    }

    public ClassUpdateType getChangeType() {
        return updateType;
    }

    public boolean needRedefineClass() {
        if (!isLoaded()) {
            return false;
        }
        if (isLibraryClass()) {
            return false;
        }

        DSUClass superClass = getSuperClass();
        if (superClass.isLoaded()) {
            if (superClass.isDeleted()) {
                return true;
            } else if (superClass.needRedefineClass()) {
                return true;
            }
        }

        for (DSUClass intf : getDeclaredInterfaces()) {
            if (intf.isLoaded()) {
                if (intf.isDeleted()) {
                    return true;
                } else if (intf.needRedefineClass()) {
                    return true;
                }
            }
        }
        
        return updateType == ClassUpdateType.CC;
    }

    public boolean needReloadClass() {
        return needRedefineClass() || updateType == ClassUpdateType.CC || updateType == ClassUpdateType.BC;
    }

    /**
     * class that not a top level class
     * 
     * @return true if it is a inner class
     */
    public boolean isInnerClass() {
        return declaringClass != null;
    }

    public boolean isLocalClass() {
        return classNode.outerClass != null;
    }

    public boolean isAnonymousClass() {
        return "".equals(getSimpleName());
    }

    public String toString() {
        return name;
    }

    /**
     * 
     * @return true if this is a updated class
     */
    public boolean isUpdated() {
        switch (this.updateType) {
        case NONE:
            return false;
        case DEL:
        case MC:
        case BC:
        case CC: 
        case ADD:
            return true;
        }
        throw new RuntimeException("sanity check failed");
    }
    
    public boolean isChanged() {
        return updateType.isChanged();
    }

    public String getClassFileName() {
        int index = name.lastIndexOf(".");
        return name.substring(index + 1);
    }

    /**
     * 
     * @return the resource that used to create this RVMClass
     * @throws Exception 
     */
    public URL getClassFile() {
        return classFile;
    }

    public boolean isLibraryClass() {
        return this.classStore.isBootstrapStore();
    }

    public void setClassFile(URL classFile) {
        this.classFile = classFile;
    }

    public boolean isDeleted() {
        return this.updateType.isDeleted();
    }

}
