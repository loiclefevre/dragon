package com.oracle.dragon.stacks.requirements;

import com.google.common.base.Strings;
import com.oracle.dragon.model.Version;
import com.oracle.dragon.util.DSSession;

import java.io.IOException;

public class MavenRequirement extends AbstractRequirement {
    private final String version;

    public MavenRequirement(String version) {
        this.version = version;
    }

    @Override
    public boolean isPresent(DSSession.Platform platform) {
        // Check M2_HOME/release file
        final String m2Home = System.getenv("M2_HOME");

        if (!Strings.isNullOrEmpty(m2Home)) {
            final Version currentVersion = new Version(m2Home.substring(m2Home.lastIndexOf('-')+1));
            final Version expectedVersion = new Version(version);

            return currentVersion.compareTo(expectedVersion) >= 0;
        }

        final ProcessBuilder pb = new ProcessBuilder("mvn", "--version")
                .redirectErrorStream(true);

        try {
            final Process p = pb.start();
            final StringBuilder sb = getProcessOutput(p.getInputStream());
            int a = p.waitFor();
            if (a != 0) {
                throw new RuntimeException(pb + " (" + p.exitValue() + "):\n" + getProcessOutput(p.getInputStream()).toString());
            }

            final String header = sb.toString().split("\n")[0];
            final Version currentVersion = new Version(header.split(" ")[2]);

            final Version expectedVersion = new Version(version);

            return currentVersion.compareTo(expectedVersion) >= 0;
        } catch (IOException | InterruptedException ignored) {
        }

        return false;
    }

    @Override
    public String[] getCommands(DSSession.Platform platform, boolean ociCloudShell) {
        switch (platform) {
            case Linux:
            case LinuxARM:
                return new String[]{
                        "wget https://mirrors.ircam.fr/pub/apache/maven/maven-3/"+version+"/binaries/apache-maven-"+version+"-bin.tar.gz",
                        "tar -xvf apache-maven-"+version+"-bin.tar.gz",
                        "export M2_HOME=\"`pwd`/apache-maven-"+version+"\"",
                        "export PATH=${M2_HOME}/bin:$PATH"
                };

            case Windows:
                return new String[]{
                        "powershell wget https://mirrors.ircam.fr/pub/apache/maven/maven-3/"+version+"/binaries/apache-maven-"+version+"-bin.zip -OutFile apache-maven-"+version+"-bin.zip",
                        "powershell Expand-Archive -Path apache-maven-"+version+"-bin.zip -DestinationPath .\\",
                        "set M2_HOME=%CD%\\apache-maven-"+version,
                        "set PATH=%M2_HOME%\\bin:%PATH%"
                };

            case MacOS:
                return new String[]{
                        "curl -L -O https://mirrors.ircam.fr/pub/apache/maven/maven-3/"+version+"/binaries/apache-maven-"+version+"-bin.tar.gz",
                        "tar -xvf apache-maven-"+version+"-bin.tar.gz",
                        "export M2_HOME=\"`pwd`/apache-maven-"+version+"\"",
                        "export PATH=${M2_HOME}/bin:$PATH"
                };
        }

        return new String[]{"No command"};
    }

    @Override
    public String getDescription() {
        return "To install Maven version " + version + ", please follow these instructions:";
    }
}
