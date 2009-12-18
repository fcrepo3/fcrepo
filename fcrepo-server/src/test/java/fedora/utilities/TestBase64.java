/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.io.ByteArrayInputStream;

import org.junit.Test;

/**
 * Unit tests for Base64 utility class.
 */
public class TestBase64 extends junit.framework.TestCase {

    private static final byte[] FOO_BYTES = new byte[] { 0x66, 0x6f, 0x6f };
    private static final byte[] FOO_BYTES_ENCODED = new byte[] { 0x5a, 0x6d, 0x39, 0x76 };
    private static final String FOO_STRING = "foo";
    private static final String FOO_STRING_ENCODED = "Zm9v";

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

}
