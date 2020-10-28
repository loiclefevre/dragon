package com.oracle.dragon.util.exception;

import java.io.IOException;

public class LoadLocalConfigurationException extends DSException {
    public LoadLocalConfigurationException(String localConfigurationFilename, Throwable t) {
        super(ErrorCode.LoadLocalConfiguration,String.format("Can't load local configuration file \"%s\"!", localConfigurationFilename),t);
    }
}
