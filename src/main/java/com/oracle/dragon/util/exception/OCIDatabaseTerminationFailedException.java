package com.oracle.dragon.util.exception;

public class OCIDatabaseTerminationFailedException extends DSException {
    public OCIDatabaseTerminationFailedException(String dbName, String opcRequestId) {
        super(ErrorCode.OCIDatabaseTerminationFailed,String.format("The termination of your %s database failed, see the request Id %s",dbName,opcRequestId));
    }
}
