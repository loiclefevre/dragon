package com.oracle.dragon.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

public class JSONCollectionFilenameFilter implements FilenameFilter {
    private final Pattern pattern;

    public JSONCollectionFilenameFilter(String collectionName) {
        pattern = Pattern.compile(collectionName + "(_\\d+)?\\.json", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean accept(File dir, String name) {
        return pattern.matcher(name).matches();
    }
}
