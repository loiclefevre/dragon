package com.oracle.dragon.util.exception;

public class ObjectStorageConfigurationFailedException extends DSException {
    public ObjectStorageConfigurationFailedException() {
        super(ErrorCode.ObjectStorageConfigurationFailed, "The object storage credential for the database couldn't be configured.");
    }
}
