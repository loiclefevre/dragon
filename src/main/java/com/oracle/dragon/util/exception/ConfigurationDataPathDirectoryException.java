package com.oracle.dragon.util.exception;

import com.oracle.dragon.util.DSSession;

public class ConfigurationDataPathDirectoryException extends DSException {
    public ConfigurationDataPathDirectoryException(String path) {
        super(ErrorCode.ConfigurationDataPathDirectory, String.format("The data path \"%s\" specified in the \"%s\" file must be a directory.", path, DSSession.CONFIGURATION_FILENAME));
    }
}
