package com.oracle.dragon.util.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

public class CSVTableFilenameFilter implements FilenameFilter {
    private final Pattern pattern;

    public CSVTableFilenameFilter(String collectionName) {
        pattern = Pattern.compile(collectionName + "(_\\d+)?\\.csv", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean accept(File dir, String name) {
        return pattern.matcher(name).matches();
    }
}
