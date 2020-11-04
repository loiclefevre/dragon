package com.oracle.dragon.util.exception;

public class ConfigurationMissingKeyFileException extends DSException {
    public ConfigurationMissingKeyFileException(String configKeyFileParameterName, String keyFilename) {
        super(ErrorCode.ConfigurationMissingKeyFile, String.format("The parameter \"%s\" references a key file \"%s\" that doesn't exist!", configKeyFileParameterName, keyFilename));
    }
}
