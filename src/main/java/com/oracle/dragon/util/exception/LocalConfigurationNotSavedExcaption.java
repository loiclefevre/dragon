package com.oracle.dragon.util.exception;

public class LocalConfigurationNotSavedExcaption extends DSException {
    public LocalConfigurationNotSavedExcaption(Throwable t) {
        super(ErrorCode.LocalConfigurationNotSaved, "Unable to save configuration file \"local_configuration.json\" locally!", t);
    }
}
