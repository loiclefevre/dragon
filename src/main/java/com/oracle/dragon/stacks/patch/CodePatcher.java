package com.oracle.dragon.stacks.patch;

import com.oracle.dragon.model.LocalDragonConfiguration;

import java.io.File;
import java.util.Map;

public interface CodePatcher {
    Map<String,String> patch(File destinationDirectory, LocalDragonConfiguration localConfiguration);
}
