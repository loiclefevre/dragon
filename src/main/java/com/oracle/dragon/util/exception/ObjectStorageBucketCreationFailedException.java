package com.oracle.dragon.util.exception;

public class ObjectStorageBucketCreationFailedException extends DSException {
    public ObjectStorageBucketCreationFailedException(String bucketName) {
        super(ErrorCode.ObjectStorageBucketCreationFailed,String.format("Error creating object storage bucket %", bucketName));
    }
}
