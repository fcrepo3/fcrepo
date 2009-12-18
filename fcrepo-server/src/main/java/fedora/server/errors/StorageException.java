/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Abstract superclass for storage-related exceptions.
 * 
 * @author Chris Wilper
 */
public abstract class StorageException
        extends ServerException {

    /**
     * Creates a StorageException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public StorageException(String message) {
        super(null, message, null, null, null);
    }

    public StorageException(String message, Throwable cause) {
        super(null, message, null, null, cause);
    }

    public StorageException(String bundleName,
                            String code,
                            String[] values,
                            String[] details,
                            Throwable cause) {
        super(bundleName, code, values, details, cause);
    }

}
