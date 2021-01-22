package org.javelus.impl;

import org.javelus.DSUField;
import org.javelus.FieldUpdateType;

public class DSUFieldImpl implements DSUField {

    private FieldUpdateType updateType;
    private String name;
    private String signature;
    private boolean isStatic;
    public DSUFieldImpl(FieldUpdateType updateType, String name, String signature, boolean isStatic) {
        this.updateType = updateType;
        this.name = name;
        this.signature = signature;
        this.isStatic = isStatic;
    }

    @Override
    public FieldUpdateType getUpdateType() {
        return updateType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

}
