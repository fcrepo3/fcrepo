/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signifies that an error occurred during the server's shutdown.
 * 
 * @author Chris Wilper
 */
public class ServerShutdownException
        extends ShutdownException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a ServerShutdownException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public ServerShutdownException(String message) {
        super(message);
    }

}
