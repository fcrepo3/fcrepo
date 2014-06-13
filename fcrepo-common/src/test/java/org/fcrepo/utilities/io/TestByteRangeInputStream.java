/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities.io;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;

import junit.framework.JUnit4TestAdapter;

import org.junit.Test;
import org.apache.commons.io.IOUtils;
import org.fcrepo.utilities.io.NullInputStream;

/**
 * @author armintor@gmail.com
 */
public class TestByteRangeInputStream {


	@Test
	public void testGoodRangeHeaders() throws IOException {
		// these are semantically equivalent for a 10 byte stream
		String [] inputs = new String[]{"bytes=0-9","bytes=0","bytes=-10", "bytes=-12"};
		for (String input: inputs) {
		    ByteRangeInputStream test = new ByteRangeInputStream(NullInputStream.NULL_STREAM, 10, input);
		    try {
		        assertEquals("bad offset of " + test.offset + " for " + input, 0, test.offset);
		        assertEquals("bad length of " + test.length + " for " + input, 10, test.length);
                assertEquals("bytes 0-9/10", test.contentRange);
		    } finally {
		        test.close();
		    }
		}
		inputs = new String[]{"bytes=1-9","bytes=1","bytes=-9","bytes=1-12"};
        for (String input: inputs) {
            ByteRangeInputStream test = new ByteRangeInputStream(NullInputStream.NULL_STREAM, 10, input);
            try {
                assertEquals("bad offset of " + test.offset + " for " + input, 1, test.offset);
                assertEquals("bad length of " + test.length + " for " + input, 9, test.length);
                assertEquals("bytes 1-9/10", test.contentRange);
            } finally {
                test.close();
            }
        }
        inputs = new String[]{"bytes= 1-9 ","bytes= 1","bytes= - 9","bytes = 1-12"};
        for (String input: inputs) {
            ByteRangeInputStream test = new ByteRangeInputStream(NullInputStream.NULL_STREAM, 10, input);
            try {
                assertEquals("bad offset of " + test.offset + " for " + input, 1, test.offset);
                assertEquals("bad length of " + test.length + " for " + input, 9, test.length);
                assertEquals("bytes 1-9/10", test.contentRange);
            } finally {
                test.close();
            }
        }
        String input = "bytes=2-6";
        ByteRangeInputStream test = new ByteRangeInputStream(NullInputStream.NULL_STREAM, 10, input);
        try {
            assertEquals("bad offset of " + test.offset + " for " + input, 2, test.offset);
            assertEquals("bad length of " + test.length + " for " + input, 5, test.length);
            assertEquals("bytes 2-6/10", test.contentRange);
        } finally {
            test.close();
        }
        input = "bytes=2-2";
        test = new ByteRangeInputStream(NullInputStream.NULL_STREAM, 10, input);
        try {
            assertEquals("bad offset of " + test.offset + " for " + input, 2, test.offset);
            assertEquals("bad length of " + test.length + " for " + input, 1, test.length);
            assertEquals("bytes 2-2/10", test.contentRange);
        } finally {
            test.close();
        }
        
	}
	
	@Test(expected=IndexOutOfBoundsException.class)
	public void testBadRangeHeaderOffset() throws IOException {
        @SuppressWarnings({"unused", "resource"})
        ByteRangeInputStream test = new ByteRangeInputStream(NullInputStream.NULL_STREAM, 10, "bytes=10");
	}
	
	@SuppressWarnings("resource")
    @Test
	public void testSkippedBytes() throws IndexOutOfBoundsException, IOException {
	    String data = "1234567890";
	    String input = "bytes=3-12";
	    InputStream bytes = new ByteArrayInputStream(data.getBytes(Charset.forName("UTF-8")));
        ByteRangeInputStream test = new ByteRangeInputStream(bytes, 10, input);
        assertEquals("bad offset of " + test.offset + " for " + input, 3, test.offset);
        assertEquals("bad length of " + test.length + " for " + input, 7, test.length);
        assertEquals("bytes 3-9/10", test.contentRange);
        bytes.reset();
        InputStream bytes2 = new ByteArrayInputStream(data.getBytes(Charset.forName("UTF-8")));
        test = new ByteRangeInputStream(bytes, 10, "bytes=0-2");
        ByteRangeInputStream test2 = new ByteRangeInputStream(bytes2, 10, "bytes=-7");
        SequenceInputStream test3 = new SequenceInputStream(test, test2);
        String actual = IOUtils.toString(test3);
        assertEquals(data, actual);
	}

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(TestByteRangeInputStream.class);
	}

}
