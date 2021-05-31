package com.oracle.dragon.stacks.requirements;

import com.oracle.dragon.model.Version;
import com.oracle.dragon.util.DSSession;

import java.io.*;

public class NodeRequirement extends AbstractRequirement {
	private final int majorVersion;

	public NodeRequirement(int majorVersion) {
		this.majorVersion = majorVersion;
	}

	@Override
	public boolean isPresent(DSSession.Platform platform) {
		final ProcessBuilder pb = new ProcessBuilder("node", "-v").redirectErrorStream(true);

		try {
			final Process p = pb.start();
			final StringBuilder sb = getProcessOutput(p.getInputStream());
			int a = p.waitFor();
			if (a != 0) {
				throw new RuntimeException(pb + " (" + p.exitValue() + "):\n" + getProcessOutput(p.getInputStream()).toString());
			}

			return new Version(sb.substring(1).trim()).getMajor() >= majorVersion;
		} catch (IOException | InterruptedException ignored) {
		}

		return false;
	}

	@Override
	public String[] getCommands(DSSession.Platform platform, boolean ociCloudShell) {
		final String NODEJS_VERSION = "14.17.0";

		switch (platform) {
			case Linux:
				return ociCloudShell ?
						new String[]{
								"nvm install "+NODEJS_VERSION+" --latest-npm"
						}
						:
						new String[]{
								"wget https://nodejs.org/dist/v"+NODEJS_VERSION+"/node-v"+NODEJS_VERSION+"-linux-x64.tar.xz",
								"tar -xvf node-v"+NODEJS_VERSION+"-linux-x64.tar.xz",
								"export PATH=\"`pwd`\"/node-v"+NODEJS_VERSION+"-linux-x64/bin:$PATH"
						};

			case LinuxARM:
				return new String[]{
						"wget https://nodejs.org/dist/v"+NODEJS_VERSION+"/node-v"+NODEJS_VERSION+"-linux-arm64.tar.xz",
						"tar -xvf node-v"+NODEJS_VERSION+"-linux-arm64.tar.xz",
						"export PATH=\"`pwd`\"/node-v"+NODEJS_VERSION+"-linux-arm64/bin:$PATH"
				};


			case Windows:
				return new String[]{
						"powershell wget https://nodejs.org/dist/v"+NODEJS_VERSION+"/node-v"+NODEJS_VERSION+"-win-x64.zip -OutFile node-v"+NODEJS_VERSION+"-win-x64.zip",
						"powershell Expand-Archive -Path node-v"+NODEJS_VERSION+"-win-x64.zip -DestinationPath .\\",
						"set PATH=%CD%\\node-v"+NODEJS_VERSION+"-win-x64;%PATH%"
				};

			case MacOS:
				return new String[]{
						"curl -L -O https://nodejs.org/dist/v"+NODEJS_VERSION+"/node-v"+NODEJS_VERSION+"-darwin-x64.tar.gz",
						"tar -xvf node-v"+NODEJS_VERSION+"-darwin-x64.tar.xz",
						"export PATH=\"`pwd`\"/node-v"+NODEJS_VERSION+"-darwin-x64/bin:$PATH"
				};
		}

		return new String[]{"No command"};
	}

	@Override
	public String getDescription() {
		return "To install Node version " + majorVersion + ", please follow these instructions:";
	}
}
