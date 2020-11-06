package com.oracle.dragon.stacks.patch;

import com.oracle.dragon.model.LocalDragonConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpringBootPetclinicCodePatcher implements CodePatcher {

    public SpringBootPetclinicCodePatcher() {
    }

    @Override
    public Map<String, String> patch(File destinationDirectory, LocalDragonConfiguration localConfiguration) {
        final Map<String, String> result = new HashMap<>();

        final File pomFile = new File(destinationDirectory, "pom.xml");
        if (pomFile.exists() && pomFile.isFile()) {
            try {
                String content = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
                //System.out.println("POM: "+content);

                // -1- get full final jar path whatever the POM project version
                final Pattern versionPattern = Pattern.compile("<version>(.*?)</version>");
                final Matcher matcher = versionPattern.matcher(content.substring(0, content.indexOf("</version>") + "</version>".length()));
                String applicationVersion = matcher.find() ? matcher.group(1) : "*.BUILD-SNAPSHOT"; // fallback
                String applicationName = "spring-petclinic-" + applicationVersion + ".jar";

                result.put("applicationPath", "target" + File.separator + applicationName);

                // TODO? https://www.baeldung.com/spring-boot-starter-parent

                // -2- modify the pom.xml file:
                // REMOVE:
                // <!-- Spring Boot Actuator displays build-related information if a git.properties
                //        file is present at the classpath -->
                //      <plugin>
                //        <groupId>pl.project13.maven</groupId>
                //        <artifactId>git-commit-id-plugin</artifactId>
                //        <executions>
                //          <execution>
                //            <goals>
                //              <goal>revision</goal>
                //            </goals>
                //          </execution>
                //        </executions>
                //        <configuration>
                //          <verbose>true</verbose>
                //          <dateFormat>yyyy-MM-dd'T'HH:mm:ssZ</dateFormat>
                //          <generateGitPropertiesFile>true</generateGitPropertiesFile>
                //          <generateGitPropertiesFilename>${project.build.outputDirectory}/git.properties
                //          </generateGitPropertiesFilename>
                //          <!-- <failOnUnableToExtractRepoInfo>false</failOnUnableToExtractRepoInfo> -->
                //          <failOnNoGitDirectory>false</failOnNoGitDirectory>
                //        </configuration>
                //      </plugin>
                String pattern = "<!-- Spring Boot Actuator displays build-related information if a git.properties";
                int start = content.indexOf(pattern);
                if (start != -1) {
                    pattern = "</plugin>";
                    final int end = content.indexOf(pattern, start + pattern.length());
                    if (end != -1) {
                        content = content.substring(0, start) + content.substring(end + pattern.length());
                    }
                }

                // -3- add all Oracle Autonomous JDBC dependencies...
                pattern = "<dependencies>";
                start = content.indexOf(pattern);
                if (start != -1) {
                    final String ORACLE_JDBC_VERSION = "19.8.0.0";

                    content = content.substring(0, start + pattern.length()) +
                            String.format("\n" +
                            "    <!-- Oracle JDBC dependencies -->\n" +
                            "       <dependency>\n" +
                            "            <groupId>com.oracle.database.security</groupId>\n" +
                            "            <artifactId>oraclepki</artifactId>\n" +
                            "            <version>%1$s</version>\n" +
                            "        </dependency>\n" +
                            "        <dependency>\n" +
                            "            <groupId>com.oracle.database.security</groupId>\n" +
                            "            <artifactId>osdt_core</artifactId>\n" +
                            "            <version>%1$s</version>\n" +
                            "        </dependency>\n" +
                            "        <dependency>\n" +
                            "            <groupId>com.oracle.database.security</groupId>\n" +
                            "            <artifactId>osdt_cert</artifactId>\n" +
                            "            <version>%1$s</version>\n" +
                            "        </dependency>\n" +
                            "        <dependency>\n" +
                            "            <groupId>com.oracle.database.jdbc</groupId>\n" +
                            "            <artifactId>ojdbc8</artifactId>\n" +
                            "            <version>%1$s</version>\n" +
                            "        </dependency>", ORACLE_JDBC_VERSION)
                            + content.substring(start + pattern.length());
                }

                Files.writeString(pomFile.toPath(), content, StandardCharsets.UTF_8, new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE});

                // -4- The wallet from localConfiguration.walletFile is configured later in the CodeGenerator process, extracting and overriding application.properties

            } catch (IOException e) {
            }
        }


        return result;
    }
}
