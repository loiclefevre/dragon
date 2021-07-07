package com.oracle.dragon.util.exception;

public class ConfigurationOCIDContainsSeveralTimesTheExpectedPatternException extends DSException {
    public ConfigurationOCIDContainsSeveralTimesTheExpectedPatternException(String parameter, String expectedStartingPattern) {
        super(ErrorCode.ConfigurationOCIDContainsSeveralTimesTheExpectedPattern,  String.format("The configuration parameter \"%s\" contains several times the expected starting pattern \"%s\"", parameter, expectedStartingPattern));
    }
}
