package com.oracle.dragon.util.exception;

public class UnsupportedPlatformException extends DSException {
    public UnsupportedPlatformException(final String osName) {
        super(ErrorCode.UnsupportedPlatform, "Platform "+osName+" not yet supported!");
    }
}
