package com.oracle.dragon.util.exception;

public class ConfigurationOCIDNotStartingByExpectedPatternException extends DSException {
    public ConfigurationOCIDNotStartingByExpectedPatternException(String parameter, String expectedStartingPattern) {
        super(ErrorCode.ConfigurationOCIDNotStartingByExpectedPattern, String.format("The configuration parameter \"%s\" doesn't start by the expected pattern \"%s\"", parameter, expectedStartingPattern));
    }
}
