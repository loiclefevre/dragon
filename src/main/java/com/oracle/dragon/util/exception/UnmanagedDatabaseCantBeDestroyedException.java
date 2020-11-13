package com.oracle.dragon.util.exception;

public class UnmanagedDatabaseCantBeDestroyedException extends DSException {
    public UnmanagedDatabaseCantBeDestroyedException() {
        super(ErrorCode.UnmanagedDatabaseCantBeDestroyed, "There is no managed database to destroy for this project/folder.");
    }

    public UnmanagedDatabaseCantBeDestroyedException(String dbName) {
        super(ErrorCode.UnmanagedDatabaseCantBeDestroyed, String.format("The database \"%s\" can't be destroyed because it is not related to this project/folder.",dbName));
    }
}
