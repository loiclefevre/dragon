package com.oracle.dragon.util.exception;

public class DatabaseWalletSavingException extends DSException {
    public DatabaseWalletSavingException(String absolutePath) {
        super(ErrorCode.DatabaseWalletSaving, String.format("Unable to save wallet file to %s",absolutePath) );
    }
}
