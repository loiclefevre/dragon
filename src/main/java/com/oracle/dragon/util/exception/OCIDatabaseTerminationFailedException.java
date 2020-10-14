package com.oracle.dragon.util.exception;

public class OCIDatabaseTerminationFailedException extends DSException {
    public OCIDatabaseTerminationFailedException(String dbName, String error) {
        super(ErrorCode.OCIDatabaseTerminationFailed, String.format("The termination of your %s database failed:\n%s", dbName, error));
    }
}
