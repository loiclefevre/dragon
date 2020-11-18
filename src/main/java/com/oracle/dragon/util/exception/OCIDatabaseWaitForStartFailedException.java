package com.oracle.dragon.util.exception;

public class OCIDatabaseWaitForStartFailedException extends DSException {
    public OCIDatabaseWaitForStartFailedException(Exception e) {
        super(ErrorCode.OCIDatabaseWaitForStartFailed,"Unable to wait for complete startup of the database!",e);
    }
}
