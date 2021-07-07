package com.oracle.dragon.stacks;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.oracle.dragon.model.LocalDragonConfiguration;
import com.oracle.dragon.model.StackMetadata;
import com.oracle.dragon.stacks.patch.POMAnalyzer;
import com.oracle.dragon.stacks.requirements.EnvironmentRequirementDeserializer;
import com.oracle.dragon.util.DSSession;
import com.oracle.dragon.util.ZipUtil;
import com.oracle.dragon.util.exception.*;
import org.stringtemplate.v4.ST;

import java.io.*;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.oracle.dragon.util.Console.Style.ANSI_BRIGHT;
import static com.oracle.dragon.util.Console.Style.ANSI_RESET;
import static com.oracle.dragon.util.DSSession.*;

/**
 * "Generates" the code for a given stack. By generates, we mean either:
 * - downloading code
 * - filtering code (e.g. configure properties using StringTemplate)
 * - installing components (e.g. dependencies such as React libraries...)
 */
public class CodeGenerator {
    private final StackType type;
    private final String name;
    private final String override;
    private final LocalDragonConfiguration localConfiguration;
    private final String profileName;
    private final String configFilename;
    private DSSession.Section section;
    private StackMetadata stackMetadata;

    public CodeGenerator(StackType type, String name, String override, LocalDragonConfiguration localConfiguration, String profileName, String configFilename) {
        this.type = type;
        this.name = name;
        this.override = override;
        this.localConfiguration = localConfiguration;
        this.profileName = profileName;
        this.configFilename = configFilename;
    }

    public void work() throws DSException {
        section = DSSession.Section.CreateStack;
        section.print("generating");

        //try {
        final File dest = new File(name);

        if (!dest.exists()) {
            dest.mkdirs();
        }

        String rootResourcesDir = "stacks/";
        final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final SimpleModule indexKeyModule = new SimpleModule();
        indexKeyModule.addDeserializer(EnvironmentRequirement.class, new EnvironmentRequirementDeserializer());
        mapper.registerModule(indexKeyModule);

        // Overrides relies upon existing base stacks (embedded with DRAGON).
        // Overrides will download the additional resources from GitHub.
        if (override != null) {
            String envRequirement;
            ST st;

            switch (type) {

                case REACT:
                    section.print("overriding");
                    try {
                        rootResourcesDir = "https://raw.githubusercontent.com/loiclefevre/dragon/master/stacks/create-react-app/";
                        stackMetadata = mapper.readValue(downloadFile(rootResourcesDir + override + "/metadata.json"), StackMetadata.class);

                        if (stackMetadata.hasURL()) {
                            downloadFile(stackMetadata.getUrl(), dest, stackMetadata.getSkipDirectoryLevel());
                        }

                        for (String fileName : stackMetadata.getFiles()) {
                            if (fileName.endsWith("/")) {
                                final File dir = new File(dest, fileName);
                                if (!dir.exists()) dir.mkdirs();
                            } else {
                                final String path = fileName.substring(fileName.lastIndexOf('/') + 1);
                                final String subDir = fileName.substring(0, fileName.length() - path.length());
                                final InputStream inputStream = downloadFile(rootResourcesDir + override + "/data/" + fileName);
                                if (inputStream == null) {
                                    throw new StackFileNotFoundException(type.humanName, fileName, rootResourcesDir + type.resourceDir + "/data/" + fileName);
                                }
                                extractFileContent(path, new File(dest, subDir), inputStream);
                            }
                        }
                    } catch( com.fasterxml.jackson.databind.JsonMappingException me ){
                        section.printlnKO();
                        throw new UnknownEnvironmentRequirementForStackException(me.getMessage());
                    } catch (IOException e) {
                        section.printlnKO();
                        throw new LoadStackMetadataException(type.humanName, e);
                    }

                    section.printlnOK(type.humanName + ": " + name+"#"+override);

                    // environment requirements checking...
                    envRequirement = processEnvironmentRequirements(platform,OCICloudShell);

                    st = new ST(
                            new BufferedReader(
                                    new InputStreamReader(downloadFile(rootResourcesDir + override + "/message.st"), StandardCharsets.UTF_8))
                                    .lines()
                                    .collect(Collectors.joining("\n")), '<', '>');
                    st.add("name", name);
                    st.add("path", dest.getAbsolutePath());
                    st.add("override", override);
                    st.add("executable", EXECUTABLE_NAME);
                    st.add("envRequirement", envRequirement);
                    st.add("profile",profileName);
                    st.add("dragonConfigFilename",configFilename);

                    System.out.println(st.render());

                    generateStackLocalConfigFile(dest);

                    break;


                case MICRO_SERVICE:
                    section.print("overriding");
                    try {
                        rootResourcesDir = "https://raw.githubusercontent.com/loiclefevre/dragon/master/stacks/create-micro-service/";
                        stackMetadata = mapper.readValue(downloadFile(rootResourcesDir + override + "/metadata.json"), StackMetadata.class);

                        if (stackMetadata.hasURL()) {
                            downloadFile(stackMetadata.getUrl(), dest, stackMetadata.getSkipDirectoryLevel());
                        }

                        for (String fileName : stackMetadata.getFiles()) {
                            if (fileName.endsWith("/")) {
                                final File dir = new File(dest, fileName);
                                if (!dir.exists()) dir.mkdirs();
                            } else {
                                final String path = fileName.substring(fileName.lastIndexOf('/') + 1);
                                final String subDir = fileName.substring(0, fileName.length() - path.length());
                                final InputStream inputStream = downloadFile(rootResourcesDir + override + "/data/" + fileName);
                                if (inputStream == null) {
                                    throw new StackFileNotFoundException(type.humanName, fileName, rootResourcesDir + type.resourceDir + "/data/" + fileName);
                                }
                                extractFileContent(path, new File(dest, subDir), inputStream);
                            }
                        }
                    } catch( com.fasterxml.jackson.databind.JsonMappingException me ){
                        section.printlnKO();
                        throw new UnknownEnvironmentRequirementForStackException(me.getMessage());
                    } catch (IOException e) {
                        section.printlnKO();
                        throw new LoadStackMetadataException(type.humanName, e);
                    }

                    section.printlnOK(type.humanName + ": " + name+"#"+override);

                    final Map<String, String> patchParameters = new HashMap<>();

                    if (stackMetadata.getCodePatchers() != null && stackMetadata.getCodePatchers().length > 0) {
                        section = DSSession.Section.PostProcessingStack;
                        section.print("patching");

                        for(String codePatcherName:stackMetadata.getCodePatchers()) {
                            switch(codePatcherName) {
                                case "POMAnalyzer":
                                    patchParameters.putAll(new POMAnalyzer().patch(dest, localConfiguration));
                                    break;
                            }
                        }

                        section.printlnOK();
                    }


                    // environment requirements checking...
                    envRequirement = processEnvironmentRequirements(platform,OCICloudShell);

                    st = new ST(
                            new BufferedReader(
                                    new InputStreamReader(downloadFile(rootResourcesDir + override + "/message.st"), StandardCharsets.UTF_8))
                                    .lines()
                                    .collect(Collectors.joining("\n")), '<', '>');
                    st.add("name", name);
                    st.add("path", dest.getAbsolutePath());
                    st.add("override", override);
                    st.add("executable", EXECUTABLE_NAME);
                    st.add("envRequirement", envRequirement);

                    st.add("stackName", name);
                    st.add("config", localConfiguration);
                    st.add("dbNameLower", localConfiguration.getDbName().toLowerCase());
                    st.add("profile",profileName);
                    st.add("dragonConfigFilename",configFilename);

                    for (String key : patchParameters.keySet()) {
                        st.add(key, patchParameters.get(key));
                    }

                    System.out.println(st.render());

                    generateStackLocalConfigFile(dest);

                    break;
            }
        } else {

            try {
                stackMetadata = mapper.readValue(Thread.currentThread().getContextClassLoader().getResourceAsStream(rootResourcesDir + type.resourceDir + "/metadata.json"), StackMetadata.class);

                if (stackMetadata.hasURL()) {
                    downloadFile(stackMetadata.getUrl(), dest, stackMetadata.getSkipDirectoryLevel());
                }

                //System.out.println(stackMetadata.getFiles());

                for (String fileName : stackMetadata.getFiles()) {
                    if (fileName.endsWith("/")) {
                        final File dir = new File(dest, fileName);
                        if (!dir.exists()) dir.mkdirs();
                    } else {
                        final String path = fileName.substring(fileName.lastIndexOf('/') + 1);
                        final String subDir = fileName.substring(0, fileName.length() - path.length());
                        final InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(rootResourcesDir + type.resourceDir + "/data/" + fileName);
                        if (inputStream == null) {
                            throw new StackFileNotFoundException(type.humanName, fileName, rootResourcesDir + type.resourceDir + "/data/" + fileName);
                        }
                        extractFileContent(path, new File(dest, subDir), inputStream);
                    }
                }

            } catch( com.fasterxml.jackson.databind.JsonMappingException me ){
                section.printlnKO();
                throw new UnknownEnvironmentRequirementForStackException(me.getMessage());
            } catch (IOException e) {
                section.printlnKO();
                throw new LoadStackMetadataException(type.humanName, e);
            }


            section.printlnOK(type.humanName + ": " + name);

            final Map<String, String> patchParameters = new HashMap<>();

            if (type.codePatcher != null) {
                section = DSSession.Section.PostProcessingStack;
                section.print("patching");
                patchParameters.putAll(type.codePatcher.patch(dest, localConfiguration));
                section.printlnOK();
            }

            // environment requirements checking...
            String envRequirement = processEnvironmentRequirements(platform,OCICloudShell);

            final ST st = new ST(
                    new BufferedReader(
                            new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(rootResourcesDir + type.resourceDir + "/message.st"), StandardCharsets.UTF_8))
                            .lines()
                            .collect(Collectors.joining("\n")), '<', '>');
            st.add("name", name);
            st.add("path", dest.getAbsolutePath());
            st.add("executable", EXECUTABLE_NAME);
            st.add("envRequirement", envRequirement);
            st.add("profile",profileName);
            st.add("dragonConfigFilename",configFilename);

            for (String key : patchParameters.keySet()) {
                st.add(key, patchParameters.get(key));
            }

            System.out.println(st.render());

            generateStackLocalConfigFile(dest);
        }
    }

    private void generateStackLocalConfigFile(File dest) {
        try (PrintWriter out = new PrintWriter(new File(dest,DSSession.LOCAL_CONFIGURATION_FILENAME))) {
            out.println("{\"redirect\": \"..\"}");
        } catch (FileNotFoundException ignored) {
        }
    }

    private String processEnvironmentRequirements(DSSession.Platform platform, boolean OCICloudShell) {
        section = DSSession.Section.StackEnvironmentValidation;

        final StringBuilder help = new StringBuilder("\n");

        for(EnvironmentRequirement er:stackMetadata.getRequires()) {
            section.print("requires "+er.name()+" for "+platform.name());
            if(er.isPresent(platform)) {
                section.printlnOK(er.name());
            } else {

                help.append(er.getDescription());
                help.append('\n');

                for(String c:er.getCommands(platform,OCICloudShell)) {
                    help.append("  ").append(ANSI_BRIGHT).append(c).append(ANSI_RESET).append('\n');
                }
            }
        }

        if(help.length() > 1) {
            help.append("\nAnd then...\n");
        }

        section.printlnOK();

        return help.toString();
    }

    private InputStream downloadFile(String url) throws DSException {
        try {
            final HttpRequest requestDownload = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .setHeader("Pragma", "no-cache")
                    .setHeader("Cache-Control", "no-store")
                    .GET()
                    .build();

            final HttpResponse<InputStream> responseDownload = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .proxy(ProxySelector.getDefault())
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build()
                    .send(requestDownload, HttpResponse.BodyHandlers.ofInputStream());

            if (responseDownload.statusCode() != 200) {
                section.printlnKO();
                throw new StackFileDownloadException(url, responseDownload.statusCode());
            }

            return new BufferedInputStream(responseDownload.body(),1024*128);
        } catch (Exception e) {
            throw new StackFileDownloadException(url, e);
        }
    }

    private void downloadFile(String url, File dest, int skipDirLevel) throws DSException {
        try {
            final HttpRequest requestDownload = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .setHeader("Pragma", "no-cache")
                    .setHeader("Cache-Control", "no-store")
                    .GET()
                    .build();

            final HttpResponse<InputStream> responseDownload = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .proxy(ProxySelector.getDefault())
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build()
                    .send(requestDownload, HttpResponse.BodyHandlers.ofInputStream());

            if (responseDownload.statusCode() != 200) {
                section.printlnKO();
                throw new StackFileDownloadException(url, responseDownload.statusCode());
            }

            if (url.toLowerCase().endsWith(".zip")) {
                ZipUtil.unzipInputStream(responseDownload.body(), dest, skipDirLevel);
            }

        } catch (Exception e) {
            section.printlnKO();
            throw new StackFileDownloadException(url, e);
        }
    }

    private void extractFileContent(String path, File parent, InputStream inputStream) throws IOException {
        if (path.endsWith(".st")) {
            final String realFilename = path.substring(0, path.length() - 3);
            section.print(realFilename);
            // replace tokens!
            final boolean taggedFile = realFilename.endsWith(".js") || realFilename.endsWith(".html") || realFilename.endsWith(".htm") || realFilename.endsWith(".xml");
            final char startDelim = taggedFile ? '%' : '<';
            final char stopDelim = taggedFile ? '%' : '>';

            try (final InputStream content = inputStream) {
                final ST st = new ST(
                        new BufferedReader(
                                new InputStreamReader(content, StandardCharsets.UTF_8))
                                .lines()
                                .collect(Collectors.joining("\n")), startDelim, stopDelim);
                // stackName
                st.add("stackName", name);
                st.add("config", localConfiguration);
                st.add("dbNameLower", localConfiguration.getDbName().toLowerCase());
                st.add("profile",profileName);
                st.add("dragonConfigFilename",configFilename);

                try (PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(parent, realFilename))))) {
                    out.print(st.render());
                }
            }
        } else {
            section.print(path);
            try (final InputStream content = inputStream) {
                Files.copy(content, new File(parent, path).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
