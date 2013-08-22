/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * A utility class for common string operations.
 *
 * @author Ross Wayland
 */
public class StringUtility {

    public StringUtility() {
    }

    /**
     * Method that attempts to break a string up into lines no longer than the
     * specified line length.
     *
     * <p>The string is assumed to consist of tokens separated by a delimeter.
     * The default delimiter is a space. If the last token to be added to a
     * line exceeds the specified line length, it is written on the next line
     * so actual line length is approximate given the specified line length
     * and the length of tokens in the string.
     *
     * @param in
     *        The input string to be split into lines.
     * @param lineLength
     *        The maximum length of each line.
     * @param delim
     *        The character delimiter separating each token in the input string;
     *        if null, defaults to the space character.
     * @return A string split into multiple lines whose lenght is less than the
     *         specified length. Actual length is approximate depending on line
     *         length, token size, and how many complete tokens will fit into
     *         the specified line length.
     */
    public static String prettyPrint(String in, int lineLength, String delim) {
        // make a guess about resulting length to minimize copying
        StringBuilder sb = new StringBuilder(in.length() + in.length()/lineLength);
        if (delim == null) {
            delim = " ";
        }
        StringTokenizer st = new StringTokenizer(in, delim);
        int charCount = 0;
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            charCount = charCount + s.length();
            if (charCount < lineLength) {
                sb.append(s);
                sb.append(' ');
                charCount++;
            } else {
                sb.append('\n');
                sb.append(s);
                sb.append(' ');
                charCount = s.length() + 1;
            }
        }
        return sb.toString();
    }

    /**
     * Method that attempts to break a string up into lines no longer than the
     * specified line length.
     *
     * <p>The string is assumed to a large chunk of undifferentiated text such
     * as base 64 encoded binary data.
     *
     * @param str
     *        The input string to be split into lines.
     * @param indent
     *        The number of spaces to insert at the start of each line.
     * @param numChars
     *        The maximum length of each line (not counting the indent spaces).
     * @return A string split into multiple indented lines whose length is less
     *         than the specified length + indent amount.
     */
    public static String splitAndIndent(String str, int indent, int numChars) {
        final int inputLength = str.length();
        // to prevent resizing, we can predict the size of the indented string
        // the formatting addition is the indent spaces plus a newline
        // this length is added once for each line
        boolean perfectFit = (inputLength % numChars == 0);
        int fullLines = (inputLength / numChars);
        int formatLength = perfectFit ?
                (indent + 1) * fullLines :
                (indent + 1) * (fullLines + 1);
        int outputLength = inputLength + formatLength;
        
        StringBuilder sb = new StringBuilder(outputLength);
        char[] ib = new char[indent];
        Arrays.fill(ib, ' ');

        for (int offset = 0; offset < inputLength; offset += numChars) {
            sb.append(ib);
            sb.append(str, offset, offset + numChars);
            sb.append('\n');
        }
        if (!perfectFit) {
            sb.append(ib);
            sb.append(str, fullLines * numChars, inputLength);
            sb.append('\n');
        }

        return sb.toString();

    }
    
    public static void splitAndIndent(String str, int indent,
            int numChars, PrintWriter writer) {
        final int inputLength = str.length();
        boolean perfectFit = (inputLength % numChars == 0);
        int fullLines = (inputLength / numChars);
        
        char[] ib = new char[indent];
        Arrays.fill(ib, ' ');

        for (int offset = 0; offset < inputLength; offset += numChars) {
            writer.print(ib);
            writer.append(str, offset, offset + numChars);
            writer.print('\n');
        }
        if (!perfectFit) {
            writer.print(ib);
            writer.append(str, fullLines * numChars, inputLength);
            writer.print('\n');
        }

    }
    
    private static final char[] HEX_CHARS = new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f'
    };

    public static String byteArraytoHexString(byte[] array) {
        StringBuffer buf = new StringBuffer(array.length * 2);
        for (byte val: array) {
            int v1 = val >>> 4 & 0x0f;
            int v2 = val & 0x0f;
            buf.append(HEX_CHARS[v1]).append(HEX_CHARS[v2]);
        }
        return buf.toString();
    }

    public static byte[] hexStringtoByteArray(String str) {
        if ((str.length() & 0x01) != 0) {
            throw new NumberFormatException();
        }
        byte ret[] = new byte[str.length() / 2];
        for (int i = 0; i < str.length() / 2; i++) {
            ret[i] =
                    (byte) Integer
                            .parseInt(str.substring(i * 2, i * 2 + 2), 16);
        }
        return ret;
    }

    public static void main(String[] args) {
        new StringUtility();
        String pid = "demo:1";
        String in =
                "org.apache.cxf.binding.soap.SoapFault: The digital object \""
                        + pid
                        + "\" is used by one or more other objects "
                        + "in the repository. All related objects must be removed "
                        + "before this object may be deleted. Use the search "
                        + "interface with the query \"bDef~" + pid
                        + "\" to obtain a list of dependent objects.";
        System.out
                .println("123456789+123456789+123456789+123456789+123456789+123456789+123456789+123456789+");
        System.out.println(StringUtility.prettyPrint(in, 70, null));
        byte test[] =
                {0x04, 0x5a, -0x69/* 0x97 */, -0x44 /* 0xbc */,
                        -0x10/* 0xf0 */, -0x7e/* 0x82 */, -0x12/* 0xee */,
                        -0x2f/* 0xd1 */, 0x63};
        String testStr = byteArraytoHexString(test);
        System.out.println(testStr);
        byte result[] = hexStringtoByteArray(testStr);
        for (byte element : result) {
            System.out.println(element);
        }
    }
}
