package com.oracle.dragon.stacks.requirements;

import com.google.common.base.Strings;
import com.oracle.dragon.model.Version;
import com.oracle.dragon.util.DSSession;

import java.io.*;
import java.util.Properties;

public class JDKRequirement implements Requirement {

    private final int majorVersion;

    public JDKRequirement(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    @Override
    public boolean isPresent(DSSession.Platform platform) {
        // Check JAVA_HOME/release file
        File javaHome = new File(System.getenv("JAVA_HOME"));

        if (javaHome.exists() && javaHome.isDirectory()) {
            try {
                Properties properties = new Properties();
                properties.load(new FileInputStream(new File(javaHome, "release")));

                String javaVersion = (String) properties.get("JAVA_VERSION");
                if (!Strings.isNullOrEmpty(javaVersion)) {
                    javaVersion = javaVersion.replaceAll("\"", "");
                }

                Version version = new Version(javaVersion);

                return version.getMajor() >= majorVersion;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            final ProcessBuilder pb = new ProcessBuilder("java", "-version")
                    .redirectErrorStream(true);

            try {
                final Process p = pb.start();
                final StringBuilder sb = getProcessOutput(p.getInputStream());
                int a = p.waitFor();
                if (a != 0) {
                    throw new RuntimeException(pb.toString() + " (" + p.exitValue() + "):\n" + getProcessOutput(p.getInputStream()).toString());
                }

                return new Version(sb.toString().split("\n")[0].split(" ")[2].replaceAll("\"", "")).getMajor() >= majorVersion;
            } catch (IOException | InterruptedException ioe) {
                ioe.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public String[] getCommands(DSSession.Platform platform, boolean ociCloudShell) {
        switch (platform) {
            case Linux:
                // TODO: check for EE version on ociCloudShell
                return majorVersion >= 11 ? new String[]{
                        "wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.3.0/graalvm-ce-java11-linux-amd64-20.3.0.tar.gz",
                        "tax -xvf graalvm-ce-java11-linux-amd64-20.3.0.tar.gz",
                        "export JAVA_HOME=\"`pwd`/graalvm-ee-java11-20.3.0\"",
                        "export PATH=${JAVA_HOME}/bin:$PATH"
                        }
                        :
                        new String[]{
                                "wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.3.0/graalvm-ce-java8-linux-amd64-20.3.0.tar.gz",
                                "tax -xvf graalvm-ce-java8-linux-amd64-20.3.0.tar.gz",
                                "export JAVA_HOME=\"`pwd`/graalvm-ee-java8-20.3.0\"",
                                "export PATH=${JAVA_HOME}/bin:$PATH"
                        };

            case Windows:
                return majorVersion >= 11 ? new String[]{
                        "powershell wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.3.0/graalvm-ce-java11-windows-amd64-20.3.0.zip -OutFile graalvm-ce-java11-windows-amd64-20.3.0.zip",
                        "powershell Expand-Archive -Path graalvm-ce-java11-windows-amd64-20.3.0.zip -DestinationPath .\\",
                        "set JAVA_HOME=%CD%\\graalvm-ce-java11-20.3.0",
                        "set PATH=%JAVA_HOME%\\bin;%PATH%"
                        }
                        :
                        new String[]{
                                "powershell wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.3.0/graalvm-ce-java8-windows-amd64-20.3.0.zip -OutFile graalvm-ce-java8-windows-amd64-20.3.0.zip",
                                "powershell Expand-Archive -Path graalvm-ce-java8-windows-amd64-20.3.0.zip -DestinationPath .\\",
                                "set JAVA_HOME=%CD%\\graalvm-ce-java8-20.3.0",
                                "set PATH=%JAVA_HOME%\\bin;%PATH%"
                        };

            case MacOS:
                return majorVersion >= 11 ? new String[]{
                        "curl -L -O https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.3.0/graalvm-ce-java11-darwin-amd64-20.3.0.tar.gz",
                        "tax -xvf graalvm-ce-java11-darwin-amd64-20.3.0.tar.gz",
                        "export JAVA_HOME=\"`pwd`/graalvm-ee-java11-20.3.0\"",
                        "export PATH=${JAVA_HOME}/bin:$PATH"
                }
                        :
                        new String[]{
                                "curl -L -O https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.3.0/graalvm-ce-java8-darwin-amd64-20.3.0.tar.gz",
                                "tax -xvf graalvm-ce-java8-darwin-amd64-20.3.0.tar.gz",
                                "export JAVA_HOME=\"`pwd`/graalvm-ee-java8-20.3.0\"",
                                "export PATH=${JAVA_HOME}/bin:$PATH"
                        };
        }

        return new String[] {"No command"};
    }

    @Override
    public String getDescription() {
        return "To install Java Development Kit version "+majorVersion+", please follow these instructions:";
    }

    public static StringBuilder getProcessOutput(final InputStream in) throws IOException {
        final Reader r = new BufferedReader(new InputStreamReader(in));
        final StringBuilder sb = new StringBuilder();
        char[] chars = new char[4 * 1024];
        int len;
        while ((len = r.read(chars)) >= 0) {
            sb.append(chars, 0, len);
        }
        return sb;
    }
}
