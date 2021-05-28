package com.oracle.dragon.stacks.requirements;

import com.oracle.dragon.model.Version;
import com.oracle.dragon.util.DSSession;

import java.io.IOException;

public class SQLCLRequirement extends AbstractRequirement {
    private final int majorVersion;

    public SQLCLRequirement(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    @Override
    public boolean isPresent(DSSession.Platform platform) {
        final ProcessBuilder pb = new ProcessBuilder("sql", "-v").redirectErrorStream(true);

        try {
            final Process p = pb.start();
            final StringBuilder sb = getProcessOutput(p.getInputStream());
            int a = p.waitFor();
            if (a != 0) {
                throw new RuntimeException(pb + " (" + p.exitValue() + "):\n" + getProcessOutput(p.getInputStream()).toString());
            }

            final String[] temp = sb.substring(15).trim().split("\\.");

            return new Version(temp[0]+"."+temp[1]+"."+temp[2]).getMajor() >= majorVersion;
        } catch (IOException | InterruptedException ignored) {
        }

        return false;
    }

    @Override
    public String[] getCommands(DSSession.Platform platform, boolean ociCloudShell) {
        switch (platform) {
            case Linux:
                return new String[]{
                        "wget https://download.oracle.com/otn_software/java/sqldeveloper/sqlcl-latest.zip",
                        "unzip sqlcl-latest.zip",
                        "export SQLCL_HOME=\"`pwd`/sqlcl\"",
                        "export PATH=${SQLCL_HOME}/bin:$PATH"
                };

            case Windows:
                return new String[]{
                        "powershell wget https://download.oracle.com/otn_software/java/sqldeveloper/sqlcl-latest.zip -OutFile sqlcl-latest.zip",
                        "powershell Expand-Archive -Path sqlcl-latest.zip -DestinationPath .\\",
                        "set PATH=%CD%\\sqlcl\\bin;%PATH%"
                };

            case MacOS:
                return new String[]{
                        "curl -L -O https://download.oracle.com/otn_software/java/sqldeveloper/sqlcl-latest.zip",
                        "unzip sqlcl-latest.zip",
                        "export SQLCL_HOME=\"`pwd`/sqlcl\"",
                        "export PATH=${SQLCL_HOME}/bin:$PATH"
                };
        }

        return new String[]{"No command"};
    }

    @Override
    public String getDescription() {
        return "To install latest SQLcl, please follow these instructions:";
    }
}
