package com.oracle.dragon.util.exception;

public class MissingDatabaseNameParameterException extends DSException {
    public MissingDatabaseNameParameterException() {
        super(ErrorCode.MissingDatabaseNameParameter, "Please provide a valid name for your DRAGON database: -db <database name>");
    }
}
