/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

/**
 * Data structure for holding a MIME-typed stream.
 *
 * @author Ross Wayland
 * @version $Id$
 */
public class MIMETypedStream {

    private static final Logger LOG = Logger.getLogger(MIMETypedStream.class);

    public String MIMEType;

    private InputStream stream;

    public Property[] header;

    private boolean gotStream = false;

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
        this.MIMEType = MIMEType;
        this.header = header;
        setStream(stream);
    }

    /**
     * Retrieves the underlying stream.
     * Caller is responsible to close the stream,
     * either by calling MIMETypedStream.close()
     * or by calling close() on the stream.
     *
     * @return The byte stream
     */
    public InputStream getStream() {
        return stream;
    }

    public void setStream(InputStream stream) {
        this.stream = stream;
    }

    /**
     * Closes the underlying stream if it's not already closed.
     *
     * In the event of an error, a warning will be logged.
     */
    public void close() {
        if (this.stream != null) {
            try {
                this.stream.close();
                this.stream = null;
            } catch (IOException e) {
                LOG.warn("Error closing stream", e);
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
        if(!gotStream) {
            close();
        }
    }

}
