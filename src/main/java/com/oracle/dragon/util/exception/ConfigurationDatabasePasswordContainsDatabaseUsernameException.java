package com.oracle.dragon.util.exception;

public class ConfigurationDatabasePasswordContainsDatabaseUsernameException extends DSException {
    public ConfigurationDatabasePasswordContainsDatabaseUsernameException(String databaseUsername) {
        super(ErrorCode.ConfigurationDatabasePasswordContainsDatabaseUsername,
                String.format("The database password must not contain the database username \"%s\" or \"admin\".", databaseUsername));
    }
}
