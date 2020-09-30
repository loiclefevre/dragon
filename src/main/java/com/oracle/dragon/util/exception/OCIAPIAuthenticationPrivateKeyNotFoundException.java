package com.oracle.dragon.util.exception;

public class OCIAPIAuthenticationPrivateKeyNotFoundException extends DSException {
    public OCIAPIAuthenticationPrivateKeyNotFoundException(String privateKeyFilePath) {
        super(ErrorCode.OCIAPIAuthenticationPrivateKeyNotFound, String.format("The path for your private key (%s) specified by the property \"key_file\" in file \"config.txt\" leads to no file!",privateKeyFilePath));
    }
}
