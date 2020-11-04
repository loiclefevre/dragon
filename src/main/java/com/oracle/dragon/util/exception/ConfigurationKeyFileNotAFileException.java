package com.oracle.dragon.util.exception;

public class ConfigurationKeyFileNotAFileException extends DSException {
    public ConfigurationKeyFileNotAFileException(String configKeyFileParameterName, String keyFilename) {
        super(ErrorCode.ConfigurationKeyFileNotAFile, String.format("The parameter \"%s\" references a key file \"%s\" that is not a file!", configKeyFileParameterName, keyFilename));
    }
}
