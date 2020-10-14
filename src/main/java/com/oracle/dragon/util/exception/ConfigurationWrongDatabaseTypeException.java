package com.oracle.dragon.util.exception;

public class ConfigurationWrongDatabaseTypeException extends DSException {
    public ConfigurationWrongDatabaseTypeException(String value) {
        super(ErrorCode.ConfigurationWrongDatabaseType, String.format("The database type specified %s is wrong, either let the parameter empty (Always Free Database) or set it to ajd, atp or adw (respectively for Autonomous JSON Database, Autonomous Transaction Processing, and Autonomous Data Warehouse).", value));
    }
}
