package com.oracle.dragon.util.exception;

public class ConfigurationDataPathNotFoundException extends DSException {
    public ConfigurationDataPathNotFoundException(String path) {
        super(ErrorCode.ConfigurationDataPathNotFound,String.format( "The data path \"%s\" specified in the \"config.txt\" file doesn't exist.",path));
    }
}
