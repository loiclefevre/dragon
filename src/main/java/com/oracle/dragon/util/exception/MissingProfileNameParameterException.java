package com.oracle.dragon.util.exception;

public class MissingProfileNameParameterException extends DSException {
    public MissingProfileNameParameterException() {
        super(ErrorCode.MissingProfileNameParameter, "Please provide a valid profile name to use: -p <profile name>");
    }
}
