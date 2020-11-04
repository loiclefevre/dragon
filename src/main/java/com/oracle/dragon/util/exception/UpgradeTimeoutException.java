package com.oracle.dragon.util.exception;

public class UpgradeTimeoutException extends DSException {
    public UpgradeTimeoutException(int timeout) {
        super(ErrorCode.UpgradeTimeout,String.format("Unable to get upgrade metadata in the given %d seconds timeout!", timeout));
    }
}
