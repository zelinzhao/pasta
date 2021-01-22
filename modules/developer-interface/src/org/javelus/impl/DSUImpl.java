package org.javelus.impl;

import java.util.Collections;
import java.util.List;

import org.javelus.DSU;
import org.javelus.DSUClassLoader;

public class DSUImpl implements DSU {

	List<DSUClassLoader> classloaders;
	
	public DSUImpl() {
		classloaders = Collections.emptyList();
	}
	
	public DSUImpl(List<DSUClassLoader> classloaders) {
		this.classloaders = classloaders;
	}
	
	@Override
	public List<DSUClassLoader> getClassLoaders() {
		return classloaders;
	}
	
}
