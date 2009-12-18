/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * @author Bill Niebel
 */
public class LowlevelStorageInconsistencyException
        extends LowlevelStorageException {

    private static final long serialVersionUID = 1L;

    public LowlevelStorageInconsistencyException(String message, Throwable cause) {
        super(true, message, cause);
    }

    public LowlevelStorageInconsistencyException(String message) {
        this(message, null);
    }
}
