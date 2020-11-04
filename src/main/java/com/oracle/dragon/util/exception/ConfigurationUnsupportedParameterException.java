package com.oracle.dragon.util.exception;

public class ConfigurationUnsupportedParameterException extends DSException {
    public ConfigurationUnsupportedParameterException(final String parameterName, final String profileName) {
        super(ErrorCode.ConfigurationUnsupportedParameter,String.format("The parameter \"%s\" found in profile \"%s\" is not supported!",parameterName, profileName));
    }
}
