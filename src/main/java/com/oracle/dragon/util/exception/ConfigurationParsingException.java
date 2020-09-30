package com.oracle.dragon.util.exception;

public class ConfigurationParsingException extends DSException {
    public ConfigurationParsingException(Throwable throwable) {
        super(ErrorCode.ConfigurationParsing,"Unable to parse the configuration file \"config.txt\" correctly!",throwable);
    }
}
