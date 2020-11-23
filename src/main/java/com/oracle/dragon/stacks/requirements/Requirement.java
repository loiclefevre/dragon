package com.oracle.dragon.stacks.requirements;

import com.oracle.dragon.util.DSSession;

public interface Requirement {
    boolean isPresent(DSSession.Platform platform);

    String[] getCommands(DSSession.Platform platform, boolean ociCloudShell);

    String getDescription();
}
