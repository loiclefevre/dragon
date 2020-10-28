package com.oracle.dragon.util.exception;

import com.oracle.dragon.util.DSSession;

public class OCIAPIAuthenticationPrivateKeyNotFoundException extends DSException {
    public OCIAPIAuthenticationPrivateKeyNotFoundException(final String privateKeyFilePath) {
        super(ErrorCode.OCIAPIAuthenticationPrivateKeyNotFound, String.format("The path for your private key (%s) specified by the property \"%s\" in file \"%s\" leads to no file!",
                privateKeyFilePath, DSSession.CONFIG_KEY_FILE, DSSession.CONFIGURATION_FILENAME));
    }
}
