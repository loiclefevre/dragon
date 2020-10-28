package com.oracle.dragon.util.exception;

import com.oracle.dragon.util.DSSession;

public class ConfigurationProfileNotFoundException extends DSException {
    public ConfigurationProfileNotFoundException(String profileName) {
        super(ErrorCode.ConfigurationProfileNotFound, String.format("The profile %s doesn't exist in the configuration file \"%s\"",profileName, DSSession.CONFIGURATION_FILENAME));
    }
}
