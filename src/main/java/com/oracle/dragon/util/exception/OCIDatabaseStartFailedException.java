package com.oracle.dragon.util.exception;

public class OCIDatabaseStartFailedException extends DSException {
    public OCIDatabaseStartFailedException(String dbName, String error) {
        super(ErrorCode.OCIDatabaseStartFailed, String.format("The startup of your %s database failed:\n%s", dbName, error));
    }
}
