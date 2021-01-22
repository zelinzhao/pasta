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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author tiger
 */
public final class DSUBootstrapClassStore extends DSUClassStore {

	private static DSUBootstrapClassStore instance = new DSUBootstrapClassStore();

	public static DSUClass java_lang_Object_class = instance.getOrCreate("java/lang/Object");

	private String javaVersion = System.getProperty("java.version");
	/**
	 * This rtPath meant to be the path of rt.jar file, which is only available in
	 * JRE of jdk1.8 or lower. On jdk9 and above, the JRE is removed and the rt.jar
	 * is splited into submodules, so that the rtPath is not valid anymore. We here
	 * use javaVersion to determine whether use rtPath or not.
	 */
	private String rtPath;

	static DSUBootstrapClassStore getInstance() {
		return instance;
	}

	private boolean isJava8() {
		return this.javaVersion != null && this.javaVersion.startsWith("1.8");
	}

	private boolean isHigherThanJava8() {
		// TODO: here, the java may be lower than 8
		return !isJava8();
	}

	private DSUBootstrapClassStore() {
		super(null, null);

		if (javaVersion == null) {
			System.out.print("Can not determine java.version. Assume it is java 8");
		}
		if (isJava8()) {
			String fileName = String.class.getResource("String.class").toExternalForm();
			Matcher matcher = Pattern.compile("jar:(file:/.*)!.*").matcher(fileName);
			if (matcher.matches()) {
				String path = matcher.group(1);
				// add the root of jar to the path list.
				URI uri;
				try {
					uri = new URI(path);
					uri.toURL();
					rtPath = uri.getPath();
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				rtPath = null;
				System.err.println("Runtime Path Error!");
			}
		}

	}

	/**
	 * try for jdk 11
	 */
	@Override
	public DSUClass getOrCreate(String className) {
		DSUClass clazz = null;
		clazz = classes.get(className);

		if (clazz != null) {
			return clazz;
		}

		String fileName = className.replace('.', '/') + ".class";
		if (isJava8()) {
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(rtPath);
				JarEntry entry = jarFile.getJarEntry(fileName);
				if (entry != null) {
					URL url = new URL(String.format("jar:file:/%s!/%s", rtPath, entry.getName()));
					ClassNode cn = new ClassNode();
					ClassReader reader = new ClassReader(jarFile.getInputStream(entry));
					reader.accept(cn, 0);
					clazz = addClass(url, cn);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (isHigherThanJava8()) {
			try {
				URL test = new URL(String.format("jrt:/java.base/%s", fileName));
				ClassNode tcn = new ClassNode();
				ClassReader tcr = new ClassReader(test.openStream());
				tcr.accept(tcn, 0);
				clazz = addClass(test, tcn);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return clazz;
	}
//    
//    /**
//     * Create only when the class exists
//     */
//    @Override
//    public DSUClass getOrCreate(String className) {
//        DSUClass clazz = null;
//        clazz = classes.get(className);
//
//        if (clazz != null) {
//            return clazz;
//        }
//
//        String fileName = className.replace('.', '/') + ".class";
//        JarFile jarFile = null;
//        try {
//            jarFile = new JarFile(rtPath);
//            JarEntry entry = jarFile.getJarEntry(fileName);
//            if (entry != null) {
//                URL url = new URL(
//                        String.format("jar:file:/%s!/%s", rtPath,
//                        entry.getName()));
//                ClassNode cn = new ClassNode();
//                ClassReader reader = new ClassReader(
//                        jarFile.getInputStream(entry));
//                reader.accept(cn, 0);
//                clazz = addClass(url, cn);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return clazz;
//    }

	public String getPathString() {
		return rtPath;
	}

	public boolean isBootstrapStore() {
		return true;
	}
}
