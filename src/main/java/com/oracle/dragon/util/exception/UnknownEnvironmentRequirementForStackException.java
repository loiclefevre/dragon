package com.oracle.dragon.util.exception;

public class UnknownEnvironmentRequirementForStackException extends DSException {
    public UnknownEnvironmentRequirementForStackException(String requirement) {
        super(ErrorCode.UnknownEnvironmentRequirementForStack,String.format("The requirement %s is not a valid stack environment requirement!", requirement));
    }
}
