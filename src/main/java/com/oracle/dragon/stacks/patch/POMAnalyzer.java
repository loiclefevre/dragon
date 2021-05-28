package com.oracle.dragon.stacks.patch;

import com.oracle.dragon.model.LocalDragonConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class POMAnalyzer implements CodePatcher {

    public POMAnalyzer() {
    }

    @Override
    public Map<String, String> patch(File destinationDirectory, LocalDragonConfiguration localConfiguration) {
        final Map<String, String> result = new HashMap<>();

        final File pomFile = new File(destinationDirectory, "pom.xml");
        if (pomFile.exists() && pomFile.isFile()) {
            try {
                String content = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
                //System.out.println("POM: " + content);

                // -1- get full final jar path whatever the POM project version
                final Pattern versionPattern = Pattern.compile("<version>(.*?)</version>");
                final Matcher versionMatcher = versionPattern.matcher(content.substring(0, content.indexOf("</version>") + "</version>".length()));
                String applicationVersion = versionMatcher.find() ? versionMatcher.group(1) : "*.BUILD-SNAPSHOT"; // fallback

                final Pattern artifactPattern = Pattern.compile("<artifactId>(.*?)</artifactId>");
                final Matcher artifactMatcher = artifactPattern.matcher(content.substring(0, content.indexOf("</artifactId>") + "</artifactId>".length()));
                String applicationName = artifactMatcher.find() ? artifactMatcher.group(1) : ""; // fallback

                final Pattern packagingPattern = Pattern.compile("<packaging>(.*?)</packaging>");
                final Matcher packagingMatcher = packagingPattern.matcher(content.substring(0, content.indexOf("</packaging>") + "</packaging>".length()));
                String packaging = packagingMatcher.find() ? packagingMatcher.group(1) : "jar"; // fallback

                final boolean jarWithDependencies = content.indexOf("<descriptorRef>jar-with-dependencies</descriptorRef>") > 0 && content.indexOf("<mainClass>") > 0 && content.indexOf("</mainClass>") > 0 && content.indexOf("<artifactId>maven-assembly-plugin</artifactId>") > 0;

                final String fqApplicationName = applicationName + "-" + applicationVersion + (jarWithDependencies ? "-jar-with-dependencies" : "") + "." + packaging;

                result.put("applicationPath", "target" + File.separator + fqApplicationName);
            } catch (IOException ignored) {
            }
        }

        return result;
    }
}
