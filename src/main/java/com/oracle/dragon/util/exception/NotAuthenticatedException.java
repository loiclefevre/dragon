package com.oracle.dragon.util.exception;

import com.oracle.dragon.util.DSSession;

public class NotAuthenticatedException extends DSException {
	public NotAuthenticatedException(String profileName) {
		super(ErrorCode.NotAuthenticated, String.format("The authentication process did not complete!\nCheck the following parameters provided in the \"%s\" file%s:\n" +
						"- %s\n" +
						"- %s\n" +
						"- %s\n" +
						"- %s\n" +
						"- %s\n" +
						"- %s\n" +
						"- %s\n" +
						"- %s (object storage access)"
				, DSSession.CONFIGURATION_FILENAME, profileName == null ? "" : " for profile "+profileName,
				DSSession.CONFIG_USER, DSSession.CONFIG_KEY_FILE, DSSession.CONFIG_PASS_PHRASE, DSSession.CONFIG_FINGERPRINT,
				DSSession.CONFIG_REGION, DSSession.CONFIG_TENANCY_ID, DSSession.CONFIG_COMPARTMENT_ID, DSSession.CONFIG_AUTH_TOKEN));
	}
}
