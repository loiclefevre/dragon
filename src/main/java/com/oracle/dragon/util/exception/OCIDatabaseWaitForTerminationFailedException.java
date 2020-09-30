package com.oracle.dragon.util.exception;

public class OCIDatabaseWaitForTerminationFailedException extends DSException {
    public OCIDatabaseWaitForTerminationFailedException(Throwable throwable) {
        super(ErrorCode.OCIDatabaseWaitForTerminationFailed, "Unable to wait for complete termination of the database!", throwable);
    }
}
