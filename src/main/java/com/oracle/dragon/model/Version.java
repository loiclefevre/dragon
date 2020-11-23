package com.oracle.dragon.model;

public class Version implements Comparable<Version> {
    private int major;
    private int middle;
    private int minor;

    public Version(String version) {
        int dot = version.indexOf('.');
        major = Integer.parseInt(version.substring(0, dot));
        int firstDot = dot;
        dot = version.indexOf('.', dot + 1);
        middle = Integer.parseInt(version.substring(firstDot + 1, dot));
        minor = Integer.parseInt(version.substring(version.lastIndexOf('.') + 1, version.length()));
    }

    public Version(int major, int middle, int minor) {
        this.major = major;
        this.middle = middle;
        this.minor = minor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version) o;

        if (major != version.major) return false;
        if (middle != version.middle) return false;
        return minor == version.minor;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + middle;
        result = 31 * result + minor;
        return result;
    }

    private int getLongVersion() {
        return 1000000 * major + 1000 * middle + minor;
    }

    @Override
    public int compareTo(Version o) {
        if (getLongVersion() < o.getLongVersion()) return -1;
        else if (getLongVersion() > o.getLongVersion()) return 1;
        else return 0;
    }

    public int getMajor() {
        return major;
    }

    public int getMiddle() {
        return middle;
    }

    public int getMinor() {
        return minor;
    }

    public static boolean isAboveVersion(String latestVersion, String currentVersion) {
        Version current = new Version(currentVersion);
        Version maybeNewVersion = new Version(latestVersion);

        return current.compareTo(maybeNewVersion) < 0;
    }
}
