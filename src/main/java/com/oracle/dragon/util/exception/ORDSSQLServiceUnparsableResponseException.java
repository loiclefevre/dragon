package com.oracle.dragon.util.exception;

public class ORDSSQLServiceUnparsableResponseException extends Exception {
    public ORDSSQLServiceUnparsableResponseException(String responseAsText, Throwable t) {
        super("Unparsable ORDS SQL Service response: "+responseAsText, t);
    }
}
