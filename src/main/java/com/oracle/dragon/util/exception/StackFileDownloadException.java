package com.oracle.dragon.util.exception;

public class StackFileDownloadException extends DSException {
    public StackFileDownloadException(String url, int errorCode) {
        super(ErrorCode.StackFileDownload,String.format("Unable to download stack file from %s (HTTP error code: %d)", url, errorCode));
    }

    public StackFileDownloadException(String url, Exception e) {
        super(ErrorCode.StackFileDownload,String.format("Unable to download stack file from %s", url), e);
    }
}
