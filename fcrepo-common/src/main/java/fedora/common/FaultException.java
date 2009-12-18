/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common;

/**
 * An unchecked exception that signals an unrecoverable error.
 * <p>
 * This type of exception is usually not caught, except at the fault barrier of
 * the application.
 * </p>
 * 
 * @see <a
 *      href="http://dev2dev.bea.com/pub/a/2006/11/effective-exceptions.html">
 *      Effective Java Exceptions</a>
 * @author Chris Wilper
 */
public class FaultException
        extends RuntimeException {

    /** Version of this class. */
    private static final long serialVersionUID = 0L;

    /**
     * Creates an instance with a detail message.
     * 
     * @param message
     *        the detail message.
     */
    public FaultException(String message) {
        super(message);
    }

    /**
     * Creates an instance with no detail message and cause.
     * 
     * @param cause
     *        the cause.
     */
    public FaultException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance with a detail message and cause.
     * 
     * @param message
     *        the detail message.
     * @param cause
     *        the cause.
     */
    public FaultException(String message, Throwable cause) {
        super(message, cause);
    }

}
