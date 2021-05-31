package com.oracle.dragon.stacks.requirements;

import com.google.common.base.Strings;
import com.oracle.dragon.model.Version;
import com.oracle.dragon.util.DSSession;

import java.io.*;
import java.util.Properties;

public class JDKRequirement extends AbstractRequirement {

	private final int majorVersion;

	public JDKRequirement(int majorVersion) {
		this.majorVersion = majorVersion;
	}

	@Override
	public boolean isPresent(DSSession.Platform platform) {
		// Check JAVA_HOME/release file
		final String javaHome = System.getenv("JAVA_HOME");

		if (!Strings.isNullOrEmpty(javaHome)) {
			File javaHomeFile = new File(javaHome);

			if (javaHomeFile.exists() && javaHomeFile.isDirectory()) {
				try {
					Properties properties = new Properties();
					properties.load(new FileInputStream(new File(javaHomeFile, "release")));

					String javaVersion = (String) properties.get("JAVA_VERSION");
					if (!Strings.isNullOrEmpty(javaVersion)) {
						javaVersion = javaVersion.replaceAll("\"", "");
					}

					Version version = new Version(javaVersion);

					return version.getMajor() >= majorVersion;
				} catch (IOException ignored) {
				}
			}
		}

		final ProcessBuilder pb = new ProcessBuilder("java", "-version")
				.redirectErrorStream(true);

		try {
			final Process p = pb.start();
			final StringBuilder sb = getProcessOutput(p.getInputStream());
			int a = p.waitFor();
			if (a != 0) {
				throw new RuntimeException(pb + " (" + p.exitValue() + "):\n" + getProcessOutput(p.getInputStream()).toString());
			}

			return new Version(sb.toString().split("\n")[0].split(" ")[2].replaceAll("\"", "")).getMajor() >= majorVersion;
		} catch (IOException | InterruptedException ignored) {
		}

		return false;
	}

	@Override
	public String[] getCommands(DSSession.Platform platform, boolean ociCloudShell) {
		final String GRAALVM_VERSION = "21.1.0";

		switch (platform) {
			case Linux:
				// TODO: check for EE version on ociCloudShell
				return majorVersion >= 11 ? new String[]{
						"wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-"+GRAALVM_VERSION+"/graalvm-ce-java11-linux-amd64-"+GRAALVM_VERSION+".tar.gz",
						"tar -xvf graalvm-ce-java11-linux-amd64-"+GRAALVM_VERSION+".tar.gz",
						"export JAVA_HOME=\"`pwd`/graalvm-ce-java11-"+GRAALVM_VERSION+"\"",
						"export PATH=${JAVA_HOME}/bin:$PATH"
				}
						:
						new String[]{
								"wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-"+GRAALVM_VERSION+"/graalvm-ce-java8-linux-amd64-"+GRAALVM_VERSION+".tar.gz",
								"tar -xvf graalvm-ce-java8-linux-amd64-"+GRAALVM_VERSION+".tar.gz",
								"export JAVA_HOME=\"`pwd`/graalvm-ce-java8-"+GRAALVM_VERSION+"\"",
								"export PATH=${JAVA_HOME}/bin:$PATH"
						};

			case LinuxARM:
				return majorVersion >= 11 ? new String[]{
						"wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-"+GRAALVM_VERSION+"/graalvm-ce-java11-linux-aarch64-"+GRAALVM_VERSION+".tar.gz",
						"tar -xvf graalvm-ce-java11-linux-aarch64-"+GRAALVM_VERSION+".tar.gz",
						"export JAVA_HOME=\"`pwd`/graalvm-ce-java11-"+GRAALVM_VERSION+"\"",
						"export PATH=${JAVA_HOME}/bin:$PATH"
				}
						:
						new String[]{
								"wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-"+GRAALVM_VERSION+"/graalvm-ce-java8-linux-aarch64-"+GRAALVM_VERSION+".tar.gz",
								"tar -xvf graalvm-ce-java8-linux-aarch64-"+GRAALVM_VERSION+".tar.gz",
								"export JAVA_HOME=\"`pwd`/graalvm-ce-java8-"+GRAALVM_VERSION+"\"",
								"export PATH=${JAVA_HOME}/bin:$PATH"
						};

			case Windows:
				return majorVersion >= 11 ? new String[]{
						"powershell wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-"+GRAALVM_VERSION+"/graalvm-ce-java11-windows-amd64-"+GRAALVM_VERSION+".zip -OutFile graalvm-ce-java11-windows-amd64-"+GRAALVM_VERSION+".zip",
						"powershell Expand-Archive -Path graalvm-ce-java11-windows-amd64-"+GRAALVM_VERSION+".zip -DestinationPath .\\",
						"set JAVA_HOME=%CD%\\graalvm-ce-java11-"+GRAALVM_VERSION,
						"set PATH=%JAVA_HOME%\\bin;%PATH%"
				}
						:
						new String[]{
								"powershell wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-"+GRAALVM_VERSION+"/graalvm-ce-java8-windows-amd64-"+GRAALVM_VERSION+".zip -OutFile graalvm-ce-java8-windows-amd64-"+GRAALVM_VERSION+".zip",
								"powershell Expand-Archive -Path graalvm-ce-java8-windows-amd64-"+GRAALVM_VERSION+".zip -DestinationPath .\\",
								"set JAVA_HOME=%CD%\\graalvm-ce-java8-"+GRAALVM_VERSION,
								"set PATH=%JAVA_HOME%\\bin;%PATH%"
						};

			case MacOS:
				return new String[]{
						"curl -L -O https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-"+GRAALVM_VERSION+"/graalvm-ce-java11-darwin-amd64-"+GRAALVM_VERSION+".tar.gz",
						"tar -xvf graalvm-ce-java11-darwin-amd64-"+GRAALVM_VERSION+".tar.gz",
						"export JAVA_HOME=\"`pwd`/graalvm-ce-java11-"+GRAALVM_VERSION+"\"",
						"export PATH=${JAVA_HOME}/bin:$PATH"
				};
		}

		return new String[]{"No command"};
	}

	@Override
	public String getDescription() {
		return "To install Java Development Kit version " + majorVersion + ", please follow these instructions:";
	}
}
