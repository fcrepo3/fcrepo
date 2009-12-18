/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that an object is not valid.
 * 
 * @author Sandy Payette
 */
public class ObjectValidityException
        extends ServerException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an ObjectValidityException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public ObjectValidityException(String message) {
        super(null, message, null, null, null);
    }

    public ObjectValidityException(String message, Throwable cause) {
        super(null, message, null, null, cause);
    }

    public ObjectValidityException(String a,
                                   String message,
                                   String[] b,
                                   String[] c,
                                   Throwable th) {
        super(a, message, b, c, th);
    }

}
