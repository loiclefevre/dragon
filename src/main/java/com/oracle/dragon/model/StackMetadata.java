package com.oracle.dragon.model;

import com.oracle.dragon.stacks.EnvironmentRequirement;

public class StackMetadata {
    private String url;
    private String[] files;
    private int skipDirectoryLevel;
    private String[] codePatchers;
    private EnvironmentRequirement[] requires;

    public StackMetadata() {
    }

    public String[] getFiles() {
        return files;
    }

    public void setFiles(String[] files) {
        this.files = files;
    }

    public String[] getCodePatchers() {
        return codePatchers;
    }

    public void setCodePatchers(String[] codePatchers) {
        this.codePatchers = codePatchers;
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

    public EnvironmentRequirement[] getRequires() {
        return requires;
    }

    public void setRequires(EnvironmentRequirement[] requires) {
        this.requires = requires;
    }
}
