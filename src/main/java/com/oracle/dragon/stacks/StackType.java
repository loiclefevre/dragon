package com.oracle.dragon.stacks;

public enum StackType {
    REACT("React", "create-react-app");

    public final String resourceDir;
    public final String humanName;

    StackType(String humanName, String resourceDir) {
        this.humanName = humanName;
        this.resourceDir = resourceDir;
    }
}
