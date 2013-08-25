package org.fcrepo.server.utilities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class StreamUtilityTest {

    @Test
    public void testEncoding() {
        // test encoding the first character
        String in = "\"foo";
        String expected = "&quot;foo";
        String actual = StreamUtility.enc(in);
        assertEquals(expected, actual);
        // test encoding the last character
        in = "foo\"";
        expected = "foo&quot;";
        actual = StreamUtility.enc(in);
        assertEquals(expected, actual);
        // test encoding all the characters
        in = "&<foo>'\"";
        expected = "&amp;&lt;foo&gt;&apos;&quot;";
        actual = StreamUtility.enc(in);
        assertEquals(expected, actual);
        // test encoding no characters
        in = "foo bar";
        expected = in;
        actual = StreamUtility.enc(in);
        assertEquals(expected, actual);
    }
}
