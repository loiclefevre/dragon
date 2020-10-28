package com.oracle.dragon.util.exception;

import com.oracle.dragon.util.DSSession;

public class ConfigurationDataPathNotFoundException extends DSException {
    public ConfigurationDataPathNotFoundException(String path) {
        super(ErrorCode.ConfigurationDataPathNotFound,String.format( "The data path \"%s\" specified in the \"%s\" file doesn't exist.",path, DSSession.CONFIGURATION_FILENAME));
    }
}
