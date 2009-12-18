/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that an object was locked.
 * 
 * @author Chris Wilper
 */
public class ObjectLockedException
        extends StorageException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an ObjectLockedException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public ObjectLockedException(String message) {
        super(message);
    }

}
