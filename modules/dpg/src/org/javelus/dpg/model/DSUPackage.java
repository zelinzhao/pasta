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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DSUPackage implements Comparable<DSUPackage> {

    // String simpleName;
    String fullName;
    List<DSUClass> clazzes = new ArrayList<DSUClass>();

    static Map<DSUClassStore, Map<String, DSUPackage>> allPackages = new HashMap<DSUClassStore, Map<String, DSUPackage>>();

    public static String DEFAULT_PACKAGE = "<default>";

    public File getPackageFile(File root) {
        if (isDefaultPackage()) {
            return root;
        }

        return new File(root, this.fullName.replace('.', File.separatorChar));
    }

    private DSUPackage(String fullName) {
        this.fullName = fullName;
    }

    public boolean isDefaultPackage() {
        return this.fullName == DEFAULT_PACKAGE;
    }

    public void addRVMClass(DSUClass clazz) {
        clazzes.add(clazz);
    }

    public List<DSUClass> getRVMClasses() {
        return clazzes;
    }

    public static DSUPackage createPackage(String name, DSUClassStore cs) {
        if (name == null) {
            name = DEFAULT_PACKAGE;
        }
        Map<String, DSUPackage> pkgs = allPackages.get(cs);
        if (pkgs == null) {
            pkgs = new HashMap<String, DSUPackage>();
            allPackages.put(cs, pkgs);
        }
        DSUPackage p = pkgs.get(name);
        if (p == null) {
            p = new DSUPackage(name);
            pkgs.put(name, p);
        }
        return p;
    }

    public static DSUPackage getPackage(String name, DSUClassStore cs) {
        return allPackages.get(cs).get(name);
    }

    public static List<DSUPackage> getAllPackages(DSUClassStore cs) {
        List<DSUPackage> packs = new ArrayList<DSUPackage>();
        packs.addAll(allPackages.get(cs).values());
        return packs;
    }

    /**
     * separate with '/'
     * 
     * @return
     */
    public String getFullName() {
        return fullName;
    }

    @Override
    public int compareTo(DSUPackage o) {
        // TODO Auto-generated method stub
        return fullName.compareTo(o.getFullName());
    }

    public boolean equals(Object o) {
        if (o instanceof DSUPackage) {
            return fullName.equals(((DSUPackage) o).getFullName());
        }
        return false;
    }

    public static void main(String[] args) {
        String a = "a";
        String ab = "Ab";
        String b = "b";

        System.out.println(a.compareTo(b));
        System.out.println(ab.compareTo(b));
        System.out.println(a.compareTo(ab));

        String name = "org.jikesrvm.VM";
        System.out.println(name.substring(name.lastIndexOf(".")));
        List<DSUClass> cls = new ArrayList<DSUClass>();
        Iterator<DSUClass> i = cls.iterator();
        DSUClass c = i.next();
        System.out.println(c);
    }

}
