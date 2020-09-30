package com.oracle.dragon.util.exception;

public class DatabaseWalletCorruptedException extends DSException {
    public DatabaseWalletCorruptedException(String absolutePath) {
        super(ErrorCode.DatabaseWalletCorrupted,String.format("Error downloading database wallet, the file %s is corrupted.", absolutePath));
    }
}
