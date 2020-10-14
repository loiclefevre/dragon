package com.oracle.dragon.util.exception;

public class OCIDatabaseCreationFaileDException extends DSException {
    public OCIDatabaseCreationFaileDException(String dbName, String error) {
        super(ErrorCode.OCIDatabaseCreationFailed, String.format("The creation of your %s database failed:\n%s", dbName, error));
    }
}
