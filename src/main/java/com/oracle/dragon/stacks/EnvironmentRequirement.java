package com.oracle.dragon.stacks;

import com.oracle.dragon.stacks.requirements.JDKRequirement;
import com.oracle.dragon.stacks.requirements.NodeRequirement;
import com.oracle.dragon.stacks.requirements.Requirement;
import com.oracle.dragon.util.DSSession;

public enum EnvironmentRequirement {
    jdk11(new JDKRequirement(11)),
    //jdk8(new JDKRequirement(8)),
    node14(new NodeRequirement(14));

    private final Requirement req;

    EnvironmentRequirement(Requirement req) {
        this.req = req;
    }

    public boolean isPresent(DSSession.Platform platform) {
        return req.isPresent(platform);
    }

    public String[] getCommands(DSSession.Platform platform, boolean OCICloudShell) {
        return req.getCommands(platform,OCICloudShell);
    }

    public String getDescription() {
        return req.getDescription();
    }
}
