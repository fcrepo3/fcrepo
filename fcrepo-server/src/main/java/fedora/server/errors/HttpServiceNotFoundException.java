/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that a successful HTTP connection could NOT be made to the
 * designated URL.
 * 
 * @author Ross Wayland
 */
public class HttpServiceNotFoundException
        extends StorageException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a HttpServiceNotFoundException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public HttpServiceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a HttpServiceNotFoundException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     * @param cause
     *           A throwable containing the cause of the exception.
     */
    public HttpServiceNotFoundException(String message, Throwable cause) {
        super(message,cause);
    }

}
