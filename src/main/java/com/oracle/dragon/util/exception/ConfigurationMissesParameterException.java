package com.oracle.dragon.util.exception;

public class ConfigurationMissesParameterException extends DSException {
    public ConfigurationMissesParameterException(final String parameterName) {
        super(ErrorCode.ConfigurationMissesParameter, "The parameter \""+parameterName+"\" is missing in configuration file \"config.txt\"!");
    }
}
