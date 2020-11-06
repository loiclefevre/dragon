package com.oracle.dragon.model;

public class StackMetadata {
    private String url;
    private String[] files;
    private int skipDirectoryLevel;

    public StackMetadata() {
    }

    public String[] getFiles() {
        return files;
    }

    public void setFiles(String[] files) {
        this.files = files;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean hasURL() {
        return url != null && url.trim().length() > 0;
    }

    public int getSkipDirectoryLevel() {
        return skipDirectoryLevel;
    }

    public void setSkipDirectoryLevel(int skipDirectoryLevel) {
        this.skipDirectoryLevel = skipDirectoryLevel;
    }
}
