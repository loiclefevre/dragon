package com.oracle.dragon.util.exception;

public class StackFileNotFoundException extends DSException {
    public StackFileNotFoundException(String stackName, String fileName, String resourcePath) {
        super(ErrorCode.StackFileNotFound,String.format("Resource %s of stack %s not found. Internal resource path: %s", fileName, stackName,resourcePath));
    }
}
