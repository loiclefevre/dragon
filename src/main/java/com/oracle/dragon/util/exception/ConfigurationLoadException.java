package com.oracle.dragon.util.exception;

import java.io.IOException;

public class ConfigurationLoadException extends DSException {
    public ConfigurationLoadException(IOException ioe) {
        super(ErrorCode.ConfigurationFileLoadProblem, "Error encountered while loading configuration file \"config.txt\"!", ioe);
    }
}
