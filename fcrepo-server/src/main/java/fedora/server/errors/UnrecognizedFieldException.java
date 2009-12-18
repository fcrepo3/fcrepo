/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Thrown when a field is not recognized.
 * 
 * @author Chris Wilper
 */
public class UnrecognizedFieldException
        extends ServerException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an UnrecognizedFieldException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public UnrecognizedFieldException(String message) {
        super(null, message, null, null, null);
    }

    public UnrecognizedFieldException(String bundleName,
                                      String code,
                                      String[] replacements,
                                      String[] details,
                                      Throwable cause) {
        super(bundleName, code, replacements, details, cause);
    }

}
