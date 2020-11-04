package com.oracle.dragon.util.exception;

import com.oracle.dragon.util.DSSession;

public class DSException extends Exception {
    protected final ErrorCode errorCode;
    protected Throwable throwable;

    protected DSException(final ErrorCode errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected DSException(final ErrorCode errorCode, final String message, final Throwable throwable) {
        super(message);
        this.errorCode = errorCode;
        this.throwable = throwable;
    }

    public void displayMessageAndExit(String lastMessage, boolean displayConfigurationTemplate) {
        System.err.println(getMessage());
        if(throwable != null) {
            throwable.printStackTrace(System.err);
        }
        System.err.flush();

        if(displayConfigurationTemplate) {
            DSSession.printlnConfigurationTemplate();
        }

        System.out.println(lastMessage);
        System.exit(errorCode.internalErrorCode);
    }

    public void displayMessageAndExit(String lastMessage) {
        displayMessageAndExit(lastMessage,false);
    }
}
