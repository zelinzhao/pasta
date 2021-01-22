package org.javelus.impl;

import java.util.List;

import org.javelus.DSUClass;
import org.javelus.DSUClassLoader;

public class DSUClassLoaderImpl implements DSUClassLoader {

	private String id;
	private String loaderId;
	private DSUClass transformer;
	private List<DSUClass> classes;
	
	
	
	public DSUClassLoaderImpl(String id, String loaderId, DSUClass transformer, List<DSUClass> classes) {
		this.id = id;
		this.loaderId = loaderId;
		this.transformer = transformer;
		this.classes = classes;
	}

	@Override
	public DSUClass getTransformer() {
		return transformer;
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public String getLoaderClassLoaderID() {
		return loaderId;
	}

	@Override
	public List<DSUClass> getDSUClasses() {
		return classes;
	}

}
