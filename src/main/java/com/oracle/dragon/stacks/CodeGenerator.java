package com.oracle.dragon.stacks;

import com.oracle.dragon.model.LocalDragonConfiguration;
import com.oracle.dragon.util.DSSession;
import org.stringtemplate.v4.ST;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.stream.Collectors;

public class CodeGenerator {
    private final StackType type;
    private final String name;
    private final DSSession.Platform platform;
    private final LocalDragonConfiguration localConfiguration;
    private DSSession.Section section;

    public CodeGenerator(StackType type, String name, DSSession.Platform platform, LocalDragonConfiguration localConfiguration) {
        this.type = type;
        this.name = name;
        this.platform = platform;
        this.localConfiguration = localConfiguration;
    }

    public void work() {
        section = DSSession.Section.CreateStack;
        section.print("generating");

        try {
            final File dest = new File(name);

            if (!dest.exists()) {
                dest.mkdirs();
            }

            final Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("stacks/" + type.resourceDir);
            if (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                final Scanner root = new Scanner((InputStream) url.getContent()).useDelimiter("\\n");
                // list root content of resourceDir
                while (root.hasNext()) {
                    final String path = root.next();
                    if (path.equals("message.st")) {
                        continue;
                    } else if (path.indexOf('.') >= 0) {
                        // file
                        extractFileContent("stacks/" + type.resourceDir + "/" + path, dest.getParentFile(), path);
                    } else if ("data".equals(path)) {
                        // directory
                        final Scanner dataDir = new Scanner(Thread.currentThread().getContextClassLoader().getResourceAsStream("stacks/" + type.resourceDir + "/data")).useDelimiter("\\n");
                        extractDirectoryContent(dataDir, "stacks/" + type.resourceDir + "/data", dest);
                    }
                }
            }

            section.printlnOK(type.humanName+": "+name);

            final ST st = new ST(
                    new BufferedReader(
                            new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("stacks/" + type.resourceDir + "/message.st"), StandardCharsets.UTF_8))
                            .lines()
                            .collect(Collectors.joining("\n")), '<', '>');
            st.add("name", name);
            st.add("path", dest.getAbsolutePath());
            System.out.println(st.render());



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractDirectoryContent(Scanner files, String rootURL, File rootDest) throws IOException {
        if (!rootDest.exists()) rootDest.mkdirs();
        while (files.hasNext()) {
            final String path = files.next();
            if (path.indexOf('.') >= 0) {
                // file
                extractFileContent(rootURL + "/" + path, rootDest, path);
            } else {
                extractDirectoryContent(new Scanner(Thread.currentThread().getContextClassLoader().getResourceAsStream(rootURL + "/" + path)).useDelimiter("\\n"), rootURL + "/" + path, new File(rootDest, path));
            }
        }
    }

    private void extractFileContent(String contentPath, File parent, String path) throws IOException {
        if (contentPath.endsWith(".st")) {
            final String realFilename = path.substring(0, path.length() - 3);
            section.print(realFilename);
            // replace tokens!
            final char startDelim = realFilename.endsWith(".js") ? '%' : '<';
            final char stopDelim = realFilename.endsWith(".js") ? '%' : '>';

            final ST st = new ST(
                    new BufferedReader(
                            new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(contentPath), StandardCharsets.UTF_8))
                            .lines()
                            .collect(Collectors.joining("\n")), startDelim, stopDelim);
            // stackName
            st.add("stackName", name);
            st.add("config", localConfiguration);

            try (PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(parent, realFilename))))) {
                out.print(st.render());
            }
        } else {
            section.print(path);
            try (final InputStream content = Thread.currentThread().getContextClassLoader().getResourceAsStream(contentPath)) {
                Files.copy(content, new File(parent, path).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
