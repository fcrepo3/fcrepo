/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that a storage device failed to behave as expected.
 * 
 * @author Chris Wilper
 */
public class StorageDeviceException
        extends StorageException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a StorageDeviceException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public StorageDeviceException(String message) {
        super(message);
    }

    public StorageDeviceException(String message, Throwable cause) {
        super(message, cause);
    }

}
