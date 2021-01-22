package org.javelus.impl;

import java.util.List;

import org.javelus.ClassUpdateType;
import org.javelus.DSUClass;
import org.javelus.DSUField;
import org.javelus.DSUMethod;

public class DSUClassImpl implements DSUClass {

	private ClassUpdateType updateType;
	private String name;
	private List<DSUMethod> methods;
	private List<DSUField> fields;
	private byte[] classBytes;
	
	
	public DSUClassImpl(ClassUpdateType updateType, String name,
			List<DSUMethod> methods, List<DSUField> fields, byte[] classBytes) {
		this.updateType = updateType;
		this.name = name;
		this.methods = methods;
		this.fields = fields;
		this.classBytes = classBytes;
	}

	public DSUClassImpl(String name, byte[] classBytes) {
		this.name = name;
		this.classBytes = classBytes;
	}
	
	@Override
	public ClassUpdateType getUpdateType() {
		return updateType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public byte[] getClassBytes() {
		return classBytes;
	}

	@Override
	public List<DSUMethod> getDSUMethods() {
		return methods;
	}

	@Override
	public List<DSUField> getDSUFields() {
		return fields;
	}

}
