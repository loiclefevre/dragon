package com.oracle.dragon.util.exception;

public class DataFileNotLoadedException extends DSException {
    public DataFileNotLoadedException(String filePath) {
        super(ErrorCode.DataFileNotLoaded,String.format("The data file %s can't be loaded.", filePath));
    }
}
