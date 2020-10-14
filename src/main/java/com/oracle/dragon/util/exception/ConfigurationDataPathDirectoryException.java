package com.oracle.dragon.util.exception;

public class ConfigurationDataPathDirectoryException extends DSException {
    public ConfigurationDataPathDirectoryException(String path) {
        super(ErrorCode.ConfigurationDataPathDirectory, String.format("The data path \"%s\" specified in the \"config.txt\" file must be a directory.", path));
    }
}
