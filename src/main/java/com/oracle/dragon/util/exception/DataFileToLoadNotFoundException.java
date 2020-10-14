package com.oracle.dragon.util.exception;

public class DataFileToLoadNotFoundException extends DSException {
    public DataFileToLoadNotFoundException(String filePath) {
        super(ErrorCode.DataFileToLoadNotFound,String.format("The data file to load %s could not be found.", filePath));
    }
}
