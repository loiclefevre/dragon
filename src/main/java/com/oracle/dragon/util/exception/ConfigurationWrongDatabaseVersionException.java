package com.oracle.dragon.util.exception;

public class ConfigurationWrongDatabaseVersionException extends DSException {
	public ConfigurationWrongDatabaseVersionException(String wantedVersion) {
		super(ErrorCode.ConfigurationWrongDatabaseVersion,String.format("The database version \"%s\" is not available, please choose either \"19c\" or \"21c\".", wantedVersion));
	}
}
