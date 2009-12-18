/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that an object (serialized or deserialized) is inappropriately
 * formed in the context that it is being examined.
 * 
 * @author Chris Wilper
 */
public class ObjectIntegrityException
        extends StorageException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an ObjectIntegrityException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public ObjectIntegrityException(String message) {
        super(null, message, null, null, null);
    }

    public ObjectIntegrityException(String message, Throwable th) {
        super(null, message, null, null, th);
    }

    public ObjectIntegrityException(String a,
                                    String message,
                                    String[] b,
                                    String[] c,
                                    Throwable th) {
        super(a, message, b, c, th);
    }

}
