/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.fcrepo.common.FaultException;


/**
 * Base-64 utility methods.
 */
public abstract class Base64 {

    private static Charset UTF8 = Charset.forName("UTF-8");
    /**
     * Encodes bytes to base 64, returning bytes.
     *
     * @param in bytes to encode
     * @return encoded bytes
     */
    public static byte[] encode(byte[] in) {
        return org.apache.commons.codec.binary.Base64.encodeBase64(in);
    }

    /**
     * Encodes an input stream to base64, returning bytes.
     * <p>
     * The stream is guaranteed to be closed when this method returns, whether
     * successful or not.
     *
     * @param in stream to encode
     * @param encoded bytes, or null if there's an error reading the stream
     */
    public static byte[] encode(InputStream in) {
        Base64OutputStream out = null;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            out = new Base64OutputStream(bytes);
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            try {
                in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                throw new FaultException(e);
            }
        }
    }
    
    public static InputStream encodeToStream(InputStream in) {
        return new org.apache.commons.codec.binary.Base64InputStream(in, true, -1, null);
    }

    /**
     * Encodes bytes to base 64, returning a string.
     *
     * @param in bytes to encode
     * @return encoded string
     */
    public static String encodeToString(byte[] in) {
        return getString(encode(in));
    }

    /**
     * Encodes an input stream to base64, returning a string.
     * <p>
     * The stream is guaranteed to be closed when this method returns, whether
     * successful or not.
     *
     * @param in stream to encode
     * @param encoded string, or null if there's an error reading the stream
     */
    public static String encodeToString(InputStream in) {
        return getString(encode(in));
    }


    /**
     * Decodes bytes from base 64, returning bytes.
     *
     * @param in bytes to decode
     * @return decoded bytes
     */
    public static byte[] decode(byte[] in) {
        return org.apache.commons.codec.binary.Base64.decodeBase64(in);
    }

    /**
     * Decodes a string from base 64, returning bytes.
     *
     * @param in string to decode
     * @return decoded bytes
     */
    public static byte[] decode(String in) {
        return decode(getBytes(in));
    }
    
    /**
     * Decode an input stream of b64-encoded data to an array of bytes. 
     * @param in
     * @return the decoded bytes, or null if there was an error reading the bytes
     */
    public static byte[] decode(InputStream in) {
        try {
            return IOUtils.toByteArray(decodeToStream(in));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static InputStream decodeToStream(InputStream in) {
        return new org.apache.commons.codec.binary.Base64InputStream(in, false);
    }

    /**
     * Decodes bytes from base 64, returning a string.
     *
     * @param in bytes to decode
     * @return decoded string
     */
    public static String decodeToString(byte[] in) {
        return getString(decode(in));
    }

    private static String getString(byte[] bytes) {
        if (bytes == null) return null;
        return new String(bytes, UTF8);
    }

    private static byte[] getBytes(String string) {
        if (string == null) return null;
        return string.getBytes(UTF8);
    }

}
