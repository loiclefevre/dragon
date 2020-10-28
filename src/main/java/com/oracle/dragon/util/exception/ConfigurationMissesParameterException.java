package com.oracle.dragon.util.exception;

import com.oracle.dragon.util.DSSession;

public class ConfigurationMissesParameterException extends DSException {
    public ConfigurationMissesParameterException(final String parameterName) {
        super(ErrorCode.ConfigurationMissesParameter, String.format("The parameter \"%s\" is missing in configuration file \"%s\"!", parameterName, DSSession.CONFIGURATION_FILENAME));
    }
}
