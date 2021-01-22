package org.javelus;

public interface UpdateType {
    boolean isDeleted();

    boolean isAdded();

    boolean isChanged();

    boolean isUnchanged();
}
