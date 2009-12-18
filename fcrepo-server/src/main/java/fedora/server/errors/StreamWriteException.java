/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals a low-level error while writing to a stream.
 * 
 * @author Chris Wilper
 */
public class StreamWriteException
        extends StreamIOException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an instance.
     * 
     * @param message
     *        an informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public StreamWriteException(String message) {
        super(message);
    }

    /**
     * Creates an instance with a cause.
     * 
     * @param message
     *        an informative message explaining what happened and (possibly) how
     *        to fix it.
     * @param cause
     *        the cause.
     */
    public StreamWriteException(String message, Throwable cause) {
        super(message, cause);
    }

}