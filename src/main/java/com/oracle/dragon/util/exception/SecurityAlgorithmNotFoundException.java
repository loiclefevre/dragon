package com.oracle.dragon.util.exception;

public class SecurityAlgorithmNotFoundException extends DSException {
    public SecurityAlgorithmNotFoundException(String algorithm) {
        super(ErrorCode.SecurityAlgorithmNotFound,String.format("%s algorithm not found!", algorithm));
    }
}
