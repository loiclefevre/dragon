package com.oracle.dragon.util.exception;

public class OCIDatabaseCreationFaileDException extends DSException {
    public OCIDatabaseCreationFaileDException(String dbName, String opcRequestId) {
        super(ErrorCode.OCIDatabaseCreationFailed,String.format("The creation of your %s database failed, see the request Id %s",dbName,opcRequestId));
    }
}
