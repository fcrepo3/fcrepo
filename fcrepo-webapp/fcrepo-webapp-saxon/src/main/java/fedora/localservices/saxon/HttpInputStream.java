/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.localservices.saxon;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;

/**
 * An InputStream from an HttpMethod. When this InputStream is close()d, the
 * underlying http connection is automatically released.
 */
public class HttpInputStream
        extends InputStream {

    private final HttpClient m_client;

    private final HttpMethod m_method;

    private final String m_url;

    private int m_code;

    private InputStream m_in;

    public HttpInputStream(HttpClient client, HttpMethod method, String url)
            throws IOException {
        m_client = client;
        m_method = method;
        m_url = url;
        try {
            m_code = m_client.executeMethod(m_method);
            m_in = m_method.getResponseBodyAsStream();
            if (m_in == null) {
                new ByteArrayInputStream(new byte[0]);
            }
        } catch (IOException e) {
            m_method.releaseConnection();
            throw e;
        }
    }

    /**
     * Get the http method name (GET or POST).
     */
    public String getMethodName() {
        return m_method.getName();
    }

    /**
     * Get the original URL of the http request this InputStream is based on.
     */
    public String getURL() {
        return m_url;
    }

    /**
     * Get the http status code.
     */
    public int getStatusCode() {
        return m_code;
    }

    /**
     * Get the "reason phrase" associated with the status code.
     */
    public String getStatusText() {
        return m_method.getStatusLine().getReasonPhrase();
    }

    /**
     * Get a header value.
     */
    public Header getResponseHeader(String name) {
        return m_method.getResponseHeader(name);
    }

    /**
     * Automatically close on garbage collection.
     */
    @Override
    public void finalize() {
        try {
            close();
        } catch (Exception e) {
        }
    }

    //////////////////////////////////////////////////////////////////////////
    /////////////////// Methods from java.io.InputStream /////////////////////
    //////////////////////////////////////////////////////////////////////////

    @Override
    public int read() throws IOException {
        return m_in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return m_in.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return m_in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return m_in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return m_in.available();
    }

    @Override
    public void mark(int readlimit) {
        m_in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        m_in.reset();
    }

    @Override
    public boolean markSupported() {
        return m_in.markSupported();
    }

    /**
     * Release the underlying http connection and close the InputStream.
     */
    @Override
    public void close() throws IOException {
        m_method.releaseConnection();
        m_in.close();
    }
}