package com.oracle.dragon.util.exception;

public class SearchIndexConfigurationFailedException extends DSException {
	public SearchIndexConfigurationFailedException(String parameter) {
		super(ErrorCode.SearchIndexConfigurationFailed, String.format("Failed to configure Full-Text index %s!", parameter));
	}
}
