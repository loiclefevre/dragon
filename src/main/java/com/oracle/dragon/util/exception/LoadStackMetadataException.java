package com.oracle.dragon.util.exception;

import java.io.IOException;

public class LoadStackMetadataException extends DSException {
    public LoadStackMetadataException(String stackName, Throwable t) {
        super(ErrorCode.LoadStackMetadata,String.format("Unable to load metadata for stack %s!", stackName), t);
    }
}
