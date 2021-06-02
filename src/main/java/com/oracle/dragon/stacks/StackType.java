package com.oracle.dragon.stacks;

import com.oracle.dragon.stacks.patch.CodePatcher;
import com.oracle.dragon.stacks.patch.SpringBootPetclinicCodePatcher;

public enum StackType {
    REACT("React", "create-react-app", null),
    JET("Oracle JavaScript Extension Toolkit", "create-jet-app", null),
    SPRINGBOOTPETCLINIC("Spring Boot","create-spring-boot-petclinic", new SpringBootPetclinicCodePatcher()),
    MICRO_SERVICE("Microservice","create-micro-service", null);

    public final String resourceDir;
    public final String humanName;
    public final CodePatcher codePatcher;

    StackType(String humanName, String resourceDir, CodePatcher codePatcher) {
        this.humanName = humanName;
        this.resourceDir = resourceDir;
        this.codePatcher = codePatcher;
    }
}
