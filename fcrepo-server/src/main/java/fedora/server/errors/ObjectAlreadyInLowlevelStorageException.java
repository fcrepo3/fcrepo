/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * @author Bill Niebel
 */
public class ObjectAlreadyInLowlevelStorageException
        extends LowlevelStorageException {

    private static final long serialVersionUID = 1L;

    public ObjectAlreadyInLowlevelStorageException(String message,
                                                   Throwable cause) {
        super(false, message, cause);
    }

    public ObjectAlreadyInLowlevelStorageException(String message) {
        this(message, null);
    }
}
