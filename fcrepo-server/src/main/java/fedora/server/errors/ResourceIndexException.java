/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * @author eddie
 */
public class ResourceIndexException
        extends ServerException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a ResourceIndexException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public ResourceIndexException(String message) {
        super(null, message, null, null, null);
    }

    public ResourceIndexException(String message, Throwable cause) {
        super(null, message, null, null, cause);
    }

    /**
     * @param bundleName
     *        The bundle in which the message resides.
     * @param code
     *        The identifier for the message in the bundle, aka the key.
     * @param values
     *        Replacements for placeholders in the message, where placeholders
     *        are of the form {num} where num starts at 0, indicating the 0th
     *        (1st) item in this array.
     * @param details
     *        Identifiers for messages which provide detail on the error. This
     *        may empty or null.
     * @param cause
     *        The underlying exception if known, null meaning unknown or none.
     */
    public ResourceIndexException(String bundleName,
                                  String code,
                                  String[] values,
                                  String[] details,
                                  Throwable cause) {
        super(bundleName, code, values, details, cause);
    }

}
