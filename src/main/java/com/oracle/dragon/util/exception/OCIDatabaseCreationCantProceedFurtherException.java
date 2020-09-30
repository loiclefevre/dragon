package com.oracle.dragon.util.exception;

public class OCIDatabaseCreationCantProceedFurtherException extends DSException {
    public OCIDatabaseCreationCantProceedFurtherException() {
        super(ErrorCode.OCIDatabaseCreationCantProceedFurther, "The OCI API response provided doesn't allow to proceed further.");
    }
}
