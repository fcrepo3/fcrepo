package org.fcrepo.server.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.junit.Test;

public class StringUtilityTest {
    @SuppressWarnings("deprecation")
    @Test
    public void testSplitAndIndexToString() {
        String src = "abcdef";
        // test uneven division of characters into lines
        String expected = "  abcd\n  ef\n";
        assertEquals(expected, StringUtility.splitAndIndent(src, 2, 4));
        // test even division of characters into lines
        expected = " a\n b\n c\n d\n e\n f\n";
        assertEquals(expected, StringUtility.splitAndIndent(src, 1, 1));
        // test a single line
        expected = " " + src + "\n";
        assertEquals(expected, StringUtility.splitAndIndent(src, 1, src.length()));
    }
    @Test
    public void testSplitAndIndexToWriter() {
        String src = "abcdef";
        // test uneven division of characters into lines
        String expected = "  abcd\n  ef\n";
        StringWriter out = new StringWriter();
        StringUtility.splitAndIndent(src, 2, 4,new PrintWriter(out));
        assertEquals(expected, out.toString());
        // test even division of characters into lines
        expected = " a\n b\n c\n d\n e\n f\n";
        out = new StringWriter();
        StringUtility.splitAndIndent(src, 1, 1,new PrintWriter(out));
        assertEquals(expected, out.toString());
        // test a single line
        expected = " " + src + "\n";
        out = new StringWriter();
        StringUtility.splitAndIndent(src, 1, src.length(),new PrintWriter(out));
        assertEquals(expected, out.toString());
    }
    @Test
    public void testBytesToHexConversions() {
        byte [] bytes = new byte[]{0x42, 0x45, 0x4e, 0x20, 0x49, 0x53, 0x20, 0x54, 0x49, 0x52, 0x45, 0x44};
        String string = "42454e204953205449524544";
        assertEquals(string, StringUtility.byteArraytoHexString(bytes));
        assertTrue(Arrays.equals(bytes, StringUtility.hexStringtoByteArray(string)));
    }
    @Test
    public void testPrettyPrint() {
        String src = "The;quick;brown;fox;jumped;over;the;lazy;dog";
        String expected = "\n" + src.replace(";"," \n") + " "; // it always prints a terminal whitespace
        assertEquals(expected, StringUtility.prettyPrint(src, 2, ";"));
    }
}
