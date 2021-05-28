package com.oracle.dragon.stacks.requirements;

import java.io.*;

public abstract class AbstractRequirement implements Requirement {
    public StringBuilder getProcessOutput(final InputStream in) throws IOException {
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
