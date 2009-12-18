/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that a method parameter associated with a Service Deployment 
 * could not be found.
 * 
 * @author Ross Wayland
 */
public class MethodParmNotFoundException
        extends StorageException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a MethodParmNotFoundException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public MethodParmNotFoundException(String message) {
        super(message);
    }
}
