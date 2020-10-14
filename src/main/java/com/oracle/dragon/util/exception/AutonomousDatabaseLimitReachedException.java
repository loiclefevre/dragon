package com.oracle.dragon.util.exception;

public class AutonomousDatabaseLimitReachedException extends DSException {
    public AutonomousDatabaseLimitReachedException(String message) {
        super(ErrorCode.AutonomousDatabaseLimitReached,message);
    }
}
