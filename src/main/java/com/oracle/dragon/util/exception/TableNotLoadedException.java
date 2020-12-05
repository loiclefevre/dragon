package com.oracle.dragon.util.exception;

public class TableNotLoadedException extends DSException {
    public TableNotLoadedException(String tableName, Throwable t) {
        super(ErrorCode.TableNotLoaded,String.format("Table %s files could not be loaded!", tableName),t);
    }
}
