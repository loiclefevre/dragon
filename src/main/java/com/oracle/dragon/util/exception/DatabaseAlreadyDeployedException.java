package com.oracle.dragon.util.exception;

public class DatabaseAlreadyDeployedException extends DSException {
    public DatabaseAlreadyDeployedException(String dbName) {
        super(ErrorCode.DatabaseAlreadyDeployed,String.format("The database %s has already been deployed.", dbName));
    }
}
