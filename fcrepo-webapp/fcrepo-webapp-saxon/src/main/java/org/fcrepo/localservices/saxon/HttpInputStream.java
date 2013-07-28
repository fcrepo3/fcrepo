/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.localservices.saxon;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

/**
 * An InputStream from an HttpMethod. When this InputStream is close()d, the
 * underlying http connection is automatically released.
 */
public class HttpInputStream
        extends InputStream {

    private final HttpClient m_client;

    private final HttpUriRequest m_method;
    
    private final HttpResponse m_response;

    private final String m_url;

    private int m_code;

    private InputStream m_in;

    public HttpInputStream(HttpClient client, HttpUriRequest method)
            throws IOException {
        m_client = client;
        m_method = method;
        m_url = method.getURI().toString();

        m_response = m_client.execute(m_method);
        m_code = m_response.getStatusLine().getStatusCode();
        if (m_response.getEntity() != null) {
            m_in = m_response.getEntity().getContent();
        } else {
            new ByteArrayInputStream(new byte[0]);
        }
    }

    /**
     * Get the http method name (GET or POST).
     */
    public String getMethodName() {
        return m_method.getMethod();
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
        return m_response.getStatusLine().getReasonPhrase();
    }

    /**
     * Get a header value.
     */
    public Header getResponseHeader(String name) {
        return m_response.getFirstHeader(name);
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
        m_in.close();
    }
}