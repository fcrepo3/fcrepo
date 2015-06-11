/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.common.http;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.fcrepo.utilities.io.NullInputStream;

/**
 * An InputStream from an HttpMethod. When this InputStream is close()d, the
 * underlying http connection is automatically released.
 */
public class HttpInputStream
        extends InputStream {

    private final HttpUriRequest m_method;
    
    private final HttpResponse m_response;

    private int m_code;

    private InputStream m_in;

    public HttpInputStream(final HttpClient client, final HttpUriRequest method)
            throws IOException {
        m_method = method;
        try {
            m_response = client.execute(m_method);
            m_code = m_response.getStatusLine().getStatusCode();
            if (m_response.getEntity() == null) {
                m_in = NullInputStream.NULL_STREAM;
            } else {
                m_in = m_response.getEntity().getContent();
            }
        } catch (IOException e) {
            if (m_in != null) {
                try {
                    m_in.close();
                } catch (IOException ioe) {}
            }
            throw e;
        }
    }

    /**
     * Get the http method name (GET or POST).
     */
    public String getMethodName() {
        return m_method.getRequestLine().getMethod();
    }

    /**
     * Get the original URL of the http request this InputStream is based on.
     */
    public String getURL() {
        return m_method.getRequestLine().getUri();
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
     * Get the response headers
     */
    public Header[] getResponseHeaders() {
        return m_response.getAllHeaders();
    }

    /**
     * Get a header value.
     */
    public Header getResponseHeader(String name) {
        return m_response.getFirstHeader(name);
    }
    
    /**
     * Return the first value of a header, or the default
     * if the fighter is not present
     * @param name the header name
     * @param defaultVal the default value
     * @return String
     */
    public String getResponseHeaderValue(String name, String defaultVal) {
        if (m_response.containsHeader(name)) {
            return m_response.getFirstHeader(name).getValue();
        } else {
            return defaultVal;
        }
    }

    /**
     * Get CONTENT-TYPE
     */
    public String getContentType() {
        return getResponseHeader("Content-Type").getValue();
    }

    /**
     * Get CONTENT-LENGTH in bytes.
     */
    public long getContentLength() {
        if (m_response.containsHeader("Content-Length")) {
            return Long.parseLong(m_response.getFirstHeader("Content-Length").getValue());
        }
        return -1;
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