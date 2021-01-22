package com.thoughtworks.xstream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class IgnoreTypes {
	private static HashSet<String> ignoreNames = new HashSet<String>();
	private static List<Pattern> ignorePatterns = new ArrayList<Pattern>();

	private static HashSet<String> ignoredResult = new HashSet<>();
	private static OutputStream log = new BufferedOutputStream(System.out);

	private static int maxDepth = Integer.MAX_VALUE;
	public static void addIgnoreName(String name) {
		if (name != null) {
			ignoreNames.add(name);
		}
	}

	public static void addAllIgnoreNames(Collection<String> names) {
		if (names != null) {
			ignoreNames.addAll(names);
			for(String s:names){
			}
		}
	}

	public static void addIgnorePattern(String pattern) {
		if (pattern != null) {
			Pattern p = Pattern.compile(pattern);
			ignorePatterns.add(p);
		}
	}

	public static void addAllIgnorePatterns(Collection<String> patterns) {
		if (patterns != null)
			for (String str : patterns) {
				addIgnorePattern(str);
			}
	}

	private static void write(String msg) {
		try {
			log.write(("[DSU] " + msg + "\n").getBytes());
			log.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void setMaxDepth(int max) {
		if (max != 0)
			maxDepth = max;
//		write("Max depth is " + maxDepth);
	}

	public static boolean ignore(Class cla, int depth) {
		if (depth > maxDepth)
			return true;
		String name1 = cla.getName();
		String name2 = cla.getCanonicalName();
		for (Pattern p : ignorePatterns) {
			if ((name1 != null && p.matcher(name1).matches()) || (name2 != null && p.matcher(name2).matches())) {
				if (!ignoredResult.contains(name1)) {
//					write("Ignore type " + name1);
					ignoredResult.add(name1);
				}
				return true;
			}
		}
		if (ignoreNames.contains(name1) || ignoreNames.contains(name2)) {
			if (!ignoredResult.contains(name1)) {
//				write("Ignore type " + name1);
				ignoredResult.add(name1);
			}
			return true;
		}
		return false;
	}

	public static boolean ignore(Object obj, int depth) {
		if (depth > maxDepth)
			return true;
		if (obj == null)
			return false;
		return ignore(obj.getClass(), depth);
	}
}
