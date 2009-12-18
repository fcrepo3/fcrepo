/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that an object existed when it wasn't expected to have existed.
 * 
 * @author Chris Wilper
 */
public class ObjectExistsException
        extends StorageException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an ObjectExistsException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public ObjectExistsException(String message) {
        super(message);
    }

}
