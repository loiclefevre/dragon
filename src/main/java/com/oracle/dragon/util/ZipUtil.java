package com.oracle.dragon.util;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipUtil {
    public static void unzipFile(File file, File destinationDirectory) {
        // create output directory if it doesn't exist
        if (!destinationDirectory.exists()) destinationDirectory.mkdirs();
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(file), 64 * 1024))) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(destinationDirectory, fileName);
                //create directories for sub directories in zip
                boolean dirCreated = new File(newFile.getParent()).mkdirs();
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                //close this ZipEntry
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();

        } catch (IOException e) {
            throw new RuntimeException("Unable to unzip file " + file.getAbsolutePath(), e);
        }
    }

    public static boolean isValid(File file) {
        try (ZipFile zipfile = new ZipFile(file);
             ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry ze = zis.getNextEntry();
            if (ze == null) {
                return false;
            }
            while (ze != null) {
                // if it throws an exception fetching any of the following then we know the file is corrupted.
                zipfile.getInputStream(ze);
                ze.getCrc();
                ze.getCompressedSize();
                ze.getName();
                ze = zis.getNextEntry();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
