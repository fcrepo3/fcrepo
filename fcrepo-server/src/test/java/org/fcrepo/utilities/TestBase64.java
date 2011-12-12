/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import org.fcrepo.utilities.Base64;

/**
 * Unit tests for Base64 utility class.
 */
public class TestBase64 extends junit.framework.TestCase {

    public static final byte[] FOO_BYTES = new byte[] { 0x66, 0x6f, 0x6f };
    public static final byte[] FOO_BYTES_ENCODED = new byte[] { 0x5a, 0x6d, 0x39, 0x76 };
    public static final String FOO_STRING = "foo";
    public static final String FOO_STRING_ENCODED = "Zm9v";

    @Test
    public void testEncodeByteArray() {
        assertTrue(sameBytes(FOO_BYTES_ENCODED, Base64.encode(FOO_BYTES)));
    }

    @Test
    public void testEncodeInputStream() {
        assertTrue(sameBytes(FOO_BYTES_ENCODED,
                             Base64.encode(new ByteArrayInputStream(FOO_BYTES))));
    }

    @Test
    public void testEncodeToStringByteArray() {
        assertEquals(FOO_STRING_ENCODED,
                     Base64.encodeToString(FOO_BYTES));
    }
    
    @Test
    public void testEncodeToStreamInputStream() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayInputStream bis = new ByteArrayInputStream(FOO_BYTES);
        InputStream encoded = Base64.encodeToStream(bis);
        int read = -1;
        while ((read = encoded.read()) > -1){
            bos.write(read);
        }
        byte [] actual = bos.toByteArray();
        assertSameBytes(FOO_BYTES_ENCODED, actual);
    }

    @Test
    public void testEncodeToStringInputStream() {
        assertEquals(FOO_STRING_ENCODED,
                     Base64.encodeToString(new ByteArrayInputStream(FOO_BYTES)));
    }

    @Test
    public void testDecodeByteArray() {
        assertTrue(sameBytes(FOO_BYTES, Base64.decode(FOO_BYTES_ENCODED)));
    }

    @Test
    public void testDecodeString() {
        assertTrue(sameBytes(FOO_BYTES, Base64.decode(FOO_STRING_ENCODED)));
    }

    @Test
    public void testDecodeToStringByteArray() {
        assertEquals(FOO_STRING,
                     Base64.decodeToString(FOO_BYTES_ENCODED));
    }

    private static final boolean sameBytes(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
    
    private static final void assertSameBytes(byte [] a, byte [] b) {
        if (a.length != b.length) fail("Bytes not equal\nE: " + inspectBytes(a) + "\nA: " + inspectBytes(b));
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                fail("Bytes not equal at position " + Integer.toString(i) + "\nE: " + inspectBytes(a) + "\nA: " + inspectBytes(b));
            }
        }
    }
    
    private static String inspectBytes(byte [] a) {
        StringBuilder builder = new StringBuilder(a.length * 3);
        for (byte b: a){
            builder.append(Integer.toHexString(b));
            builder.append(' ');
        }
        return builder.toString();
    }

}
