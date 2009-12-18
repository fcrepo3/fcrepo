/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Thrown when some format+encoding pair isn't supported by a translator.
 * 
 * @author Chris Wilper
 */
public class UnsupportedTranslationException
        extends ServerException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an UnsupportedTranslationException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public UnsupportedTranslationException(String message) {
        super(null, message, null, null, null);
    }

    public UnsupportedTranslationException(String bundleName,
                                           String code,
                                           String[] replacements,
                                           String[] details,
                                           Throwable cause) {
        super(bundleName, code, replacements, details, cause);
    }

}
