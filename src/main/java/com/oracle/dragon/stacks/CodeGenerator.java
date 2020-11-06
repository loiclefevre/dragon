package com.oracle.dragon.stacks;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.dragon.model.LocalDragonConfiguration;
import com.oracle.dragon.model.StackMetadata;
import com.oracle.dragon.util.DSSession;
import com.oracle.dragon.util.ZipUtil;
import com.oracle.dragon.util.exception.DSException;
import com.oracle.dragon.util.exception.LoadStackMetadataException;
import com.oracle.dragon.util.exception.StackFileDownloadException;
import com.oracle.dragon.util.exception.StackFileNotFoundException;
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

public class CodeGenerator {
    private final StackType type;
    private final String name;
    private final LocalDragonConfiguration localConfiguration;
    private DSSession.Section section;
    private StackMetadata stackMetadata;

    public CodeGenerator(StackType type, String name, LocalDragonConfiguration localConfiguration) {
        this.type = type;
        this.name = name;
        this.localConfiguration = localConfiguration;
    }

    public void work() throws DSException {
        section = DSSession.Section.CreateStack;
        section.print("generating");

        //try {
        final File dest = new File(name);

        if (!dest.exists()) {
            dest.mkdirs();
        }

        final String rootResourcesDir = "stacks/";

        final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

        } catch (IOException e) {
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

        final ST st = new ST(
                new BufferedReader(
                        new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(rootResourcesDir + type.resourceDir + "/message.st"), StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n")), '<', '>');
        st.add("name", name);
        st.add("path", dest.getAbsolutePath());

        for (String key : patchParameters.keySet()) {
            st.add(key, patchParameters.get(key));
        }

        System.out.println(st.render());
    }

    private void downloadFile(String url, File dest, int skipDirLevel) throws DSException {
        try {
            final HttpRequest requestDownload = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .setHeader("Pragma", "no-cache")
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
            throw new StackFileDownloadException(url, e);
        }
    }

    private void extractFileContent(String path, File parent, InputStream inputStream) throws IOException {
        if (path.endsWith(".st")) {
            final String realFilename = path.substring(0, path.length() - 3);
            section.print(realFilename);
            // replace tokens!
            final char startDelim = realFilename.endsWith(".js") ? '%' : '<';
            final char stopDelim = realFilename.endsWith(".js") ? '%' : '>';

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
