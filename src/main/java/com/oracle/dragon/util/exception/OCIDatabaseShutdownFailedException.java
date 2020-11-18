package com.oracle.dragon.util.exception;

public class OCIDatabaseShutdownFailedException extends DSException {
    public OCIDatabaseShutdownFailedException(String dbName, String error) {
        super(ErrorCode.OCIDatabaseShutdownFailed, String.format("The shutdown of your %s database failed:\n%s", dbName, error));
    }
}
