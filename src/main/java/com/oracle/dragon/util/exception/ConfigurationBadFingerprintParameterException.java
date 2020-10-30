package com.oracle.dragon.util.exception;

public class ConfigurationBadFingerprintParameterException extends DSException {
    public ConfigurationBadFingerprintParameterException(String parameterName, String configurationFilename, String fingerprintValue) {
        super(ErrorCode.ConfigurationBadFingerprintParameter,String.format("The value \"%s\" of parameter %s you specified in the configuration file \"%s\" must have 47 chars.", fingerprintValue,parameterName,configurationFilename));
    }
}
