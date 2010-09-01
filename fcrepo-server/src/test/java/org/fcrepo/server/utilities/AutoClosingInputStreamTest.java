/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test AutoClosingInputStream
 *
 * Note: tests are run on AutoClosingInputStream and also on
 * FileInputStream
 *
 * This is to test the tests; ie ensure that AutoClosingInputStream exhibits
 * the same behaviour as FileInputStream as a sanity check
 * (and will guard against any future change in these two streams)
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class AutoClosingInputStreamTest {

    private static int SIZE = 1000;

    @Test
    public void testFileInputStream() throws Exception {

        // supply a factory that generates a FileInputStream from a temp file
        StreamFactory f =
            new StreamFactory() {
            private InputStream m_is;
            private File f;
            @Override
            public InputStream getStream() throws IOException {
                f = File.createTempFile("testautoclose", null);
                FileOutputStream fos = new FileOutputStream(f);
                byte[] b = new byte[SIZE];
                fos.write(b);
                fos.close();
                m_is = new FileInputStream(f);
                return m_is;
            }
            @Override
            public void cleanUp() throws IOException {
                m_is.close();
                if (!f.delete())
                    fail("Failed to delete temp file");
            }
        };

        allTests(SIZE, f, false);

    }


    @Test
    public void testAutoClosingInputStreamFile() throws Exception {

        // supply a factory that returns an AutoClosingInputStream, wrapping a FileInputStream
        StreamFactory f =
            new StreamFactory() {
            private InputStream m_is;
            private InputStream m_inner;
            private File f;
            @Override
            public InputStream getStream() throws IOException {
                f = File.createTempFile("testautoclose", null);
                FileOutputStream fos = new FileOutputStream(f);
                byte[] b = new byte[SIZE];
                fos.write(b);
                fos.close();
                m_inner = new FileInputStream(f);
                m_is = new AutoClosingInputStream(m_inner);
                return m_is;
            }
            @Override
            public void cleanUp() throws IOException {
                m_inner.close();
                m_is.close();
                if (!f.delete())
                    fail("Failed to delete temp file");
            }
        };

        allTests(SIZE, f, true);

    }


    private void allTests(int length, StreamFactory f, boolean testClosed) throws Exception {
        InputStream in;
        byte[] b;

        // do each test, with a fresh stream each time

        in = f.getStream();
        testRead(in, testClosed);
        f.cleanUp();

        // read into buffer smaller than stream
        in = f.getStream();
        b = new byte[SIZE - 1];
        testReadBytes(in, b, testClosed);
        f.cleanUp();

        // read into buffer larger than stream
        in = f.getStream();
        b = new byte[SIZE +1];
        testReadBytes(in, b, testClosed);
        f.cleanUp();

        // read, buffer smaller than stream
        in = f.getStream();
        b = new byte[SIZE - 1];
        testReadByteRange(in, b, 0, SIZE - 1, testClosed);
        f.cleanUp();

        // read, buffer larger than stream
        in = f.getStream();
        b = new byte[SIZE + 1];
        testReadByteRange(in, b, 0, SIZE + 1, testClosed);
        f.cleanUp();

        in = f.getStream();
        testCloseRead(in, testClosed);
        f.cleanUp();

        in = f.getStream();
        testExceptions(in);
        f.cleanUp();

        // test mark is not supported (consistency with FileInputStream)
        in = f.getStream();
        assertFalse("FileInputStream markSupported", in.markSupported());
        f.cleanUp();

    }


    private void testExceptions(InputStream in) throws Exception {
        boolean caught = false;
        try {
            in.read(new byte[10], -1, 10);
        } catch (IndexOutOfBoundsException e) {
            caught = true;
        }
        assertTrue("IndexOutOfBounds for negative offset", caught);

        caught = false;
        try {
            in.read(new byte[10], 0, -1);
        } catch (IndexOutOfBoundsException e) {
            caught = true;
        }
        assertTrue("IndexOutOfBounds for negative len", caught);

        caught = false;
        try {
            in.read(new byte[10], 0, 100);
        } catch (IndexOutOfBoundsException e) {
            caught = true;
        }
        assertTrue("IndexOutOfBounds for len greater than buffer - offset", caught);


        caught = false;
        try {
            in.read(new byte[10], 5, 6);
        } catch (IndexOutOfBoundsException e) {
            caught = true;
        }
        assertTrue("IndexOutOfBounds for len greater than buffer - offset", caught);


        caught = false;
        try {
            in.read(null, 0, 100);
        } catch (NullPointerException e) {
            caught = true;
        }
        assertTrue("NullPointerException null byte array", caught);


        caught = false;
        try {
            in.read(null);
        } catch (NullPointerException e) {
            caught = true;
        }
        assertTrue("NullPointerException null byte array", caught);


        caught = false;
        try {
            in.skip(-1);
        } catch (IOException e) {
            caught = true;
        }
        assertTrue("IOException negative skip", caught);




    }



    // read() past end returns -1
    private void testRead(InputStream in, boolean testClosed) throws Exception{

        if (testClosed)
            assertEquals("Stream closed()", false, ((AutoClosingInputStream)in).closed());

        // check we can read a byte
        assertTrue("read single byte",in.read() != -1);

        // read until we reach the end
        while (in.read() != -1) {
        }

        if (testClosed)
            assertEquals("Stream closed()", true, ((AutoClosingInputStream)in).closed());


        // reading again should return -1
        assertEquals("read() past end of stream", -1, in.read());


    }

    // read(byte[] b) past end returns -1
    private void testReadBytes(InputStream in, byte[] b, boolean testClosed) throws Exception{

        if (testClosed)
            assertEquals("Stream closed()", false, ((AutoClosingInputStream)in).closed());

        // read until we reach the end
        boolean didRead = false;
        while (in.read(b) != -1) {
            didRead = true;
        }

        // check bytes were read
        assertTrue("bytes read", didRead);

        if (testClosed)
            assertEquals("Stream closed()", true, ((AutoClosingInputStream)in).closed());

        // reading again should return -1
        assertEquals("read(byte[]) past end of stream did not return -1", -1, in.read(b));

}

    // read(byte[] b, int off, int len) past end returns -1
    private void testReadByteRange(InputStream in, byte[] b, int off, int len, boolean testClosed) throws Exception{

        if (testClosed)
            assertEquals("Stream closed()", false, ((AutoClosingInputStream)in).closed());

        // read until we reach the end
        boolean didRead = false;
        while (in.read(b, off, len) != -1) {
            didRead = true;
        }
        // check bytes were read
        assertTrue("bytes read", didRead);

        if (testClosed)
            assertEquals("Stream closed()", true, ((AutoClosingInputStream)in).closed());

        // reading again should return -1
        assertEquals("read(byte[]) past end of stream did not return -1", -1, in.read(b, off, len));

    }


    // close then read methods throw exception
    protected void testCloseRead(InputStream in, boolean testClosed) throws Exception {
        if (testClosed)
            assertEquals("Stream closed()", false, ((AutoClosingInputStream)in).closed());

        in.close();

        if (testClosed)
            assertEquals("Stream closed()", true, ((AutoClosingInputStream)in).closed());

        boolean caught = false;
        try {
            in.read();
        } catch (IOException e) {
            caught = true;
        }
        assertTrue("read() after close throws exception", caught);

        caught = false;
        try {
            in.read(new byte[1]);
        } catch (IOException e) {
            caught = true;
        }
        assertTrue("read(byte[]) after close throws exception", caught);

        caught = false;
        try {
            in.read(new byte[1],0,1);
        } catch (IOException e) {
            caught = true;
        }
        assertTrue("read(byte[], int, int) after close throws exception", caught);

        // test repeated close - should not throw exception
        in.close();

        if (testClosed)
            assertEquals("Stream closed()", true, ((AutoClosingInputStream)in).closed());
    }


    abstract class StreamFactory {
        abstract InputStream getStream() throws Exception;
        abstract void cleanUp() throws Exception;
        // just in case...
        @Override
        protected void finalize() throws Throwable {
            try {
                cleanUp();
            } finally {
                super.finalize();
            }
        }
    }


}
