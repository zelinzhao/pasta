package org.javelus.impl;

import org.javelus.DSUMethod;
import org.javelus.MethodUpdateType;

public class DSUMethodImpl implements DSUMethod {

    private MethodUpdateType updateType;

    private String name;
    private String signature;
    private boolean isStatic;
    public DSUMethodImpl(MethodUpdateType updateType, String name, String signature, boolean isStatic) {
        this.updateType = updateType;
        this.name = name;
        this.signature = signature;
        this.isStatic = isStatic;
    }

    @Override
    public MethodUpdateType getUpdateType() {
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
