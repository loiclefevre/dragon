package com.oracle.dragon.util.exception;

import com.oracle.dragon.util.DSSession;

public class LocalConfigurationNotSavedException extends DSException {
    public LocalConfigurationNotSavedException(Throwable t) {
        super(ErrorCode.LocalConfigurationNotSaved, String.format("Unable to save configuration file \"%s\" locally!", DSSession.LOCAL_CONFIGURATION_FILENAME), t);
    }
}
