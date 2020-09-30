package com.oracle.dragon.util.exception;

public class AlwaysFreeDatabaseLimitReachedException extends DSException {
    public AlwaysFreeDatabaseLimitReachedException(int databaseNumberLimit) {
        super(ErrorCode.OCIAlwaysFreeDatabaseLimitReached, String.format("You've reached the maximum limit of %d databases for OCI Free Tier Autonomous Database.",databaseNumberLimit));
    }
}
