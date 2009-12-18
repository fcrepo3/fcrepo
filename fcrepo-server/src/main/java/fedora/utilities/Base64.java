/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import fedora.common.FaultException;

/**
 * Base-64 utility methods.
 */
public abstract class Base64 {

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
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return encode(out.toByteArray());
        } catch (IOException e) {
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                throw new FaultException(e);
            }
        }
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
     * Decodes bytes from base 64, returning a string.
     *
     * @param in bytes to decode
     * @return decoded string
     */
    public static String decodeToString(byte[] in) {
        return getString(decode(in));
    }

    private static String getString(byte[] bytes) {
        try {
            if (bytes == null) return null;
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException wontHappen) {
            throw new FaultException(wontHappen);
        }
    }

    private static byte[] getBytes(String string) {
        try {
            if (string == null) return null;
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException wontHappen) {
            throw new FaultException(wontHappen);
        }
    }

}
