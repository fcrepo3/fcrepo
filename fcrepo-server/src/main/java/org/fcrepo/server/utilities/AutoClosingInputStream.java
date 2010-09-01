/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities;

import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream that closes and releases the underlying stream once the last byte
 * has been read.
 *
 * Similar to org.apache.commons.io.input.AutoCloseInputStream, except:
 * - read methods return -1 if stream has been auto-closed (rather than throwing IOException)
 *   (see https://issues.apache.org/jira/browse/HTTPCLIENT-910)
 * - read methods throw IOException if stream was closed by calling close(); (consistent with FileInputStream)
 *
 * Note: Does not support mark (nor does FileInputStream)
 *
 * See https://jira.duraspace.org/browse/FCREPO-775
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class AutoClosingInputStream
        extends InputStream {


    private InputStream m_in;
    private boolean explicitlyClosed = false;

    @SuppressWarnings("unused")
    private AutoClosingInputStream() { }

    public AutoClosingInputStream(InputStream in) {
        m_in = in;
    }

    @Override
    public int read() throws IOException {
        if (m_in == null) {
            // as per FileInputStream
            if (explicitlyClosed)
                throw new IOException("Attempting to read from closed stream");
            return -1;
        } else {
            int c = m_in.read();
            if (c == -1) {
                doClose();
            }
            return c;
        }
    }

    @Override
    public int available() throws IOException {
        if (m_in == null) {
            // see InputStream.available() and FileInputStream.available()
            throw new IOException("Stream has been closed");
        } else {
            return m_in.available();
        }
    }


    private void doClose() throws IOException {
        if (m_in != null) {
            m_in.close();
            m_in = null;
        }
    }


    @Override
    public void close() throws IOException {
        doClose();
        explicitlyClosed = true;
    }

    @Override
    public void mark(int readlimit) {
        // mark is not supported, noop
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (m_in == null) {
            // as per InputStream
            if (b == null)
                throw new NullPointerException();
            // as per FileInputStream
            if (explicitlyClosed)
                throw new IOException("Attempting to read from closed stream");
            return -1;
        } else {
            int c = m_in.read(b);
            if (c == -1) {
                doClose();
            }
            return c;
        }
    }
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (m_in == null) {
            // as per InputStream
            if (b == null)
                throw new NullPointerException();
            // as per InputStream
            if (off < 0 || len < 0 || len > (b.length - off))
                throw new IndexOutOfBoundsException();
            // as per FileInputStream
            if (explicitlyClosed)
                throw new IOException("Attempting to read from closed stream");
            return -1;
        } else {
            int c = m_in.read(b, off, len);
            if (c == -1) {
                doClose();
            }
            return c;
        }
    }
    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset is not supported");
    }

    @Override
    public long skip(long n) throws IOException {
        if (m_in == null) {
            if (n < 0)
                // as per FileInputStream
                throw new IOException();
            return 0;
        } else {
            // don't close -
            // FileInputStream.skip():
            // cannot determine if we have reached EOF from return value
            // (see javadocs for FileInputStream; when testing skip() returned
            // the number of bytes requested to skip even if this was beyond EOF)
            return m_in.skip(n);
        }
    }

    // for testing
    protected boolean closed() {
        return (m_in == null);
    }
}
