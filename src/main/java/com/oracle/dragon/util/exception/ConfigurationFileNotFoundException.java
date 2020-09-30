package com.oracle.dragon.util.exception;

public class ConfigurationFileNotFoundException extends DSException {
    public ConfigurationFileNotFoundException() {
        super(ErrorCode.ConfigurationFileNotFound, "The expected configuration file named \"config.txt\" is not present in this directory.");
    }
}
