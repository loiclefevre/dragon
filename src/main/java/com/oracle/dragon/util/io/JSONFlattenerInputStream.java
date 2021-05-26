package com.oracle.dragon.util.io;

import java.io.*;
import java.util.Arrays;

public class JSONFlattenerInputStream extends FilterInputStream {
    /**
     * Creates a <code>FilterInputStream</code>
     * by assigning the  argument <code>in</code>
     * to the field <code>this.in</code> so as
     * to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     */
    public JSONFlattenerInputStream(InputStream in) {
        super(in);
    }

    private long count;
    private int level;
    private boolean string;
    private int strippedBytes;

    public long getCount() {
        return count;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = super.read(b, off, len);
        if(result == -1) { //return -1;
            if(strippedBytes >= len) {
                strippedBytes -= len;
                Arrays.fill(b,off,off+len-1,(byte)'\n');
                return len;
            } else {
                if(strippedBytes == 0) return -1;
                Arrays.fill(b,off,off+strippedBytes,(byte)'\n');
                result = strippedBytes;
                strippedBytes = 0;
                return result;
            }
        }

        final int length = result;


        final byte[] t = new byte[length*2]; // worst case!

        int lastAntiSlash = -1;
        for(int i = off, j = 0; i < length; i++, j++) {
            switch(b[i]) {
                case ' ':
                    if(string) {
                        t[j] = b[i];
                    } else {
                        result--;
                        j--;
                        strippedBytes++;
                    }
                    break;

                case '\r':
                    result--;
                    j--;
                    strippedBytes++;
                    break;

                case '\n':
                    if(string) {
                        t[j] = '\\';
                        t[++j] = 'n';
                        result++;
                        strippedBytes--;
                    } else if( level == 0){
                        t[j] = b[i];
                    } else {
                        result--;
                        j--;
                        strippedBytes++;
                    }
                    break;

                case '\\':
                    lastAntiSlash = i;
                    t[j] = b[i];
                    break;

                case '"':
                    if(string && lastAntiSlash >= 0 && lastAntiSlash == i - 1) {
                        t[j] = b[i];
                    } else {
                        string = !string;
                        t[j] = b[i];
                    }
                    break;

                case '{':
                    t[j] = b[i];
                    if(string) continue;
                    level++;
                    break;

                case '}':
                    t[j] = b[i];
                    if(string) continue;
                    level--;
                    if(level == 0) {
                        count++;
                    }
                    break;

                default:
                    t[j] = b[i];
                    break;
            }
        }

        System.arraycopy(t,0,b,off, result);

        return result;
    }

    public static void main(String[] args) throws Throwable {
        JSONFlattenerInputStream j;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(j=
                new JSONFlattenerInputStream(new FileInputStream(new File("test.json")))))) {

            String line;
            PrintWriter printWriter = new PrintWriter(new File("out.json"));
            while ((line = in.readLine()) != null) {
                printWriter.print(line+"\n");
            }

            printWriter.flush();
            printWriter.close();

            System.out.println(j.getCount());
        }


    }
}
