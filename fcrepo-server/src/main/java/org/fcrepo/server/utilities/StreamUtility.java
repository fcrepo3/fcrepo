/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.CharBuffer;

import org.fcrepo.common.FaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utility methods for working with character-based or raw sequences of data.
 *
 * @author Chris Wilper
 */
public abstract class StreamUtility {

    private static final Logger logger =
            LoggerFactory.getLogger(StreamUtility.class);

    /**
     * Returns an XML-appropriate encoding of the given String.
     *
     * @param in
     *        The String to encode.
     * @return A new, encoded String.
     */
    public static String enc(String in) {
        if (in == null || in.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        enc(in, out);
        return out.toString();
    }

    /**
     * Appends an XML-appropriate encoding of the given String to the given
     * Appendable.
     *
     * @param in
     *        The String to encode.
     * @param out
     *        The Appendable to write to.
     */
    public static void enc(String in, Appendable out) {
        if (in == null) return;
        int startAt = 0;
        try {
            int inLen = in.length();
            for (int i = 0; i < inLen; i++) {
                char c = in.charAt(i);
                if (c == '&' || c == '<' || c == '>' || c == '"' || c == '\'') {
                    if (i != startAt) out.append(in, startAt, i);
                    enc(in.charAt(i), out);
                    startAt = i + 1;
                }
            }
            if (startAt == 0) {
                // we never encountered a character to escape for xml
                out.append(in);
            } else if (startAt < inLen){
                // append the remaining safe characters
                out.append(in, startAt, inLen);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Prints an XML-appropriate encoding of the given range of characters to
     * the given Writer.
     *
     * @param in
     *        The char buffer to read from.
     * @param start
     *        The starting index.
     * @param length
     *        The number of characters in the range.
     * @param out
     *        The Appendable to write to.
     */
    public static void enc(char[] in, int start, int length, Appendable out) {
        for (int i = start; i < length + start; i++) {
            enc(in[i], out);
        }
    }

    /**
     * Appends an XML-appropriate encoding of the given character to the given
     * Appendable.
     *
     * @param in
     *        The character.
     * @param out
     *        The Appendable to write to. Since we expect only PrintStream,
     *        PrintWriter, and the String-building classes, we wrap
     *        the IOException in a RuntimeException
     */
    public static void enc(char in, Appendable out) {
        try {
            if (in == '&') {
                out.append("&amp;");
            } else if (in == '<') {
                out.append("&lt;");
            } else if (in == '>') {
                out.append("&gt;");
            } else if (in == '"') {
                out.append("&quot;");
            } else if (in == '\'') {
                out.append("&apos;");
            } else {
                out.append(in);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    /**
     * Copies the contents of an InputStream to an OutputStream, then closes
     * both.
     *
     * @param in
     *        The source stream.
     * @param out
     *        The target stram.
     * @param bufSize
     *        Number of bytes to attempt to copy at a time.
     * @throws IOException
     *         If any sort of read/write error occurs on either stream.
     */
    public static void pipeStream(InputStream in, OutputStream out, int bufSize)
            throws IOException {
        try {
            byte[] buf = new byte[bufSize];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                logger.warn("Unable to close stream", e);
            }
        }
    }

    /**
     * Gets a byte array for the given input stream.
     */
    public static byte[] getBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pipeStream(in, out, 4096);
        return out.toByteArray();
    }

    /**
     * Gets a stream for the given string.
     */
    public static InputStream getStream(String string) {
        try {
            return new ByteArrayInputStream(string.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException wontHappen) {
            throw new FaultException(wontHappen);
        }
    }

}
