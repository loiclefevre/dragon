package com.oracle.dragon.util.exception;

public class ConfigurationWrongDatabaseLicenseTypeException extends DSException {
    public ConfigurationWrongDatabaseLicenseTypeException(String value) {
        super(ErrorCode.ConfigurationWrongDatabaseLicenseType, String.format("The database license type specified %s is wrong, either let the parameter empty or set it to byol; denoting you want to Bring Your Own License to Oracle Cloud Infrastructure (remark: this doesn't work for Always Free databases nor for Autonomous JSON Database).", value));
    }
}
