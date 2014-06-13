/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.types;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.fcrepo.server.errors.RangeNotSatisfiableException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.utilities.io.ByteRangeInputStream;
import org.fcrepo.utilities.io.NullInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data structure for holding a MIME-typed stream.
 *
 * @author Ross Wayland
 * @version $Id$
 */
public class MIMETypedStream {

    private static final Logger logger =
            LoggerFactory.getLogger(MIMETypedStream.class);

    public static final String MIME_INTERNAL_REDIRECT = "application/fedora-redirect";

    public static final String MIME_INTERNAL_NOT_MODIFIED = "application/fedora-unmodified";
    
    public static final long NO_CONTENT_LENGTH = -1L;

    private String MIMEType;

    private InputStream m_stream;

    public Property[] header;

    private long m_size = -1;

    private boolean m_gotStream = false;

    private int m_httpStatus = HttpStatus.SC_OK;
    /**
     * Constructs a MIMETypedStream.
     *
     * @param MIMEType
     *        The MIME type of the byte stream.
     * @param stream
     *        The byte stream.
     */
    public MIMETypedStream(String MIMEType,
                           InputStream stream,
                           Property[] header) {
        this(MIMEType, stream, header, -1L);
    }

    /**
     * Constructs a MIMETypedStream.
     *
     * @param MIMEType
     *        The MIME type of the byte stream.
     * @param stream
     *        The byte stream.
     */
    public MIMETypedStream(String MIMEType,
                           InputStream stream,
                           Property[] header,
                           long size) {
        this(MIMEType, stream, header, size, 200);
    }
    
    private MIMETypedStream(String MIMEType,
                           InputStream stream,
                           Property[] header,
                           long size,
                           int status) {
        this.MIMEType = MIMEType;
        this.header = header;
        this.m_size = size;
        this.m_httpStatus = status;        
        setStream(stream);
    }

    public String getMIMEType() {
        return this.MIMEType;
    }

    /**
     * Retrieves the underlying stream.
     * Caller is responsible to close the stream,
     * either by calling MIMETypedStream.close()
     * or by calling close() on the stream.
     *
     * @return The byte stream
     */
    public synchronized InputStream getStream() {
        m_gotStream = true;
        return m_stream;
    }

    public synchronized void setStream(InputStream stream) {
        m_gotStream = false;
        this.m_stream = stream;
    }

    /**
     * Closes the underlying stream if it's not already closed.
     *
     * In the event of an error, a warning will be logged.
     */
    public void close() {
        if (this.m_stream != null) {
            try {
                this.m_stream.close();
                this.m_stream = null;
            } catch (IOException e) {
                logger.warn("Error closing stream", e);
            }
        }
    }

    /**
     * Ensures the underlying stream is closed at garbage-collection time
     * if the stream has not been retrieved. If getStream() has been called
     * the caller is responsible to close the stream.
     *
     * {@inheritDoc}
     */
    @Override
    public void finalize() {
        if(!m_gotStream) {
            close();
        }
    }

    public long getSize() {
        return m_size;
    }

    public void setRange(String rangeRequest) throws ServerException {
        if(rangeRequest != null && !rangeRequest.isEmpty() && !m_gotStream){
            try{
                ByteRangeInputStream range =
                        new ByteRangeInputStream(m_stream, m_size, rangeRequest);
                setStream(range);
                m_size = range.length;
                m_httpStatus = HttpStatus.SC_PARTIAL_CONTENT;
                setContentRange(range.contentRange);
            } catch (IOException e) {
                throw new StreamIOException(e.getMessage(),e);
            } catch (IndexOutOfBoundsException e) {
                throw new RangeNotSatisfiableException(e.getMessage());
            }
        }
    }
    /**
     * Typically 200, but control group R datastream content responses will use
     * 302, and conditional GET of datastream contents may return a 304
     * @return
     */
    public int getStatusCode() {
        return m_httpStatus;
    }

    public void setStatusCode(int status) {
        m_httpStatus = status;
    }
    private void setContentRange(String val) {
        boolean found = false;
        for(Property prop: this.header){
            if (prop.name.equalsIgnoreCase(HttpHeaders.CONTENT_RANGE)) {
                prop.value = val;
                found = true;
            }
        }
        if (!found){
            Property[] newHeader = new Property[this.header.length + 1];
            System.arraycopy(this.header, 0, newHeader,0,this.header.length);
            newHeader[newHeader.length - 1] = new Property(HttpHeaders.CONTENT_RANGE, val);
            this.header = newHeader;
        }
    }

    public static MIMETypedStream getRedirect(Property[] header) {
        return new MIMETypedStream(
                MIME_INTERNAL_REDIRECT, NullInputStream.NULL_STREAM, header,
                NO_CONTENT_LENGTH, HttpStatus.SC_MOVED_TEMPORARILY);
    }

    public static MIMETypedStream getRedirect(String location) {
        MIMETypedStream result = getRedirect(new Property[]{new Property(HttpHeaders.LOCATION, location)});
        result.setStream(new ByteArrayInputStream(location.getBytes(Charset.forName("UTF-8"))));
        return result;
    }

    public static MIMETypedStream getNotModified(Property[] header) {
        return new MIMETypedStream(
                MIME_INTERNAL_NOT_MODIFIED, NullInputStream.NULL_STREAM, header,
                NO_CONTENT_LENGTH, HttpStatus.SC_NOT_MODIFIED);
    }
    
}
