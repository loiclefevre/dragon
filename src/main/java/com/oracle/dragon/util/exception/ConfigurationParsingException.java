package com.oracle.dragon.util.exception;

import com.oracle.dragon.util.DSSession;

public class ConfigurationParsingException extends DSException {
    public ConfigurationParsingException(Throwable throwable) {
        super(ErrorCode.ConfigurationParsing, String.format("Unable to parse the configuration file \"%s\" correctly!", DSSession.CONFIGURATION_FILENAME), throwable);
    }
}
