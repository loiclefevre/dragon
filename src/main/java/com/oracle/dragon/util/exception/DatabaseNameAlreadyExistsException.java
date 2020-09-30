package com.oracle.dragon.util.exception;

public class DatabaseNameAlreadyExistsException extends DSException {
    public DatabaseNameAlreadyExistsException(String dbName) {
        super(ErrorCode.OCIDatabaseNameAlreadyExists, String.format("The name of the database you are requesting (%s) already exists, please retry with another name (-db <database name>).", dbName));
    }
}
