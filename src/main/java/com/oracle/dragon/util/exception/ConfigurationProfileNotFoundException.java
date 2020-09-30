package com.oracle.dragon.util.exception;

public class ConfigurationProfileNotFoundException extends DSException {
    public ConfigurationProfileNotFoundException(String profileName) {
        super(ErrorCode.ConfigurationProfileNotFound, String.format("The profile %s doesn't exist in the configuration file \"config.txt\"",profileName));
    }
}
