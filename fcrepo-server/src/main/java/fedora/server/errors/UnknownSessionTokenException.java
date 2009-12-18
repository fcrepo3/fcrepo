/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that the requested session was not found.
 * 
 * @author Chris Wilper
 */
public class UnknownSessionTokenException
        extends ServerException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an UnknownSessionTokenException
     * 
     * @param msg
     *        Description of the exception.
     */
    public UnknownSessionTokenException(String msg) {
        super(null, msg, null, null, null);
    }
}
