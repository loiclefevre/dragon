package com.oracle.dragon.util.exception;

import com.oracle.dragon.util.DSSession;

import java.io.IOException;

public class ConfigurationLoadException extends DSException {
    public ConfigurationLoadException(IOException ioe) {
        super(ErrorCode.ConfigurationFileLoadProblem, String.format("Error encountered while loading configuration file \"%s\"!", DSSession.CONFIGURATION_FILENAME), ioe);
    }
}
