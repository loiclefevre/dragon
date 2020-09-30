package com.oracle.dragon.util.exception;

public class OCIAPIDatabaseException extends DSException {
    public OCIAPIDatabaseException(Throwable throwable) {
        super(ErrorCode.OCIAPIDatabase, "Unable to connect to Oracle Cloud Infrastructure Database API.", throwable);
    }
}
