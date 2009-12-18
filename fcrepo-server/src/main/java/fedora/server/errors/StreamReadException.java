/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that a low-level error occurred reading from a stream.
 * 
 * @author Chris Wilper
 */
public class StreamReadException
        extends StreamIOException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a StreamReadException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public StreamReadException(String message) {
        super(message);
    }

}
