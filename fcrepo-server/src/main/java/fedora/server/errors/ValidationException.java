/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals an error while validating.
 * 
 * @author Sandy Payette
 */
public class ValidationException
        extends ObjectIntegrityException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String bundleName,
                               String code,
                               String[] values,
                               String[] details,
                               Throwable cause) {
        super(bundleName, code, values, details, cause);
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
