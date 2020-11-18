package com.oracle.dragon.util.exception;

public class OCIDatabaseWaitForShutdownFailedException extends DSException {
    public OCIDatabaseWaitForShutdownFailedException(Exception e) {
        super(ErrorCode.OCIDatabaseWaitForShutdownFailed,"Unable to wait for complete shutdown of the database!",e);
    }
}
