package com.oracle.dragon.util.exception;

import com.oracle.dragon.util.DSSession;

public class ConfigurationFileNotFoundException extends DSException {
    public ConfigurationFileNotFoundException() {
        super(ErrorCode.ConfigurationFileNotFound, String.format("The expected configuration file named \"%s\" is not present in this directory.", DSSession.CONFIGURATION_FILENAME));
    }
}
