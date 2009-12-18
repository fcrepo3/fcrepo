/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signifies that an error occurred during the server's initialization.
 * </p>
 * 
 * @author Chris Wilper
 */
public class ServerInitializationException
        extends InitializationException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a ServerInitializationException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public ServerInitializationException(String message) {
        super(message);
    }

    public ServerInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
