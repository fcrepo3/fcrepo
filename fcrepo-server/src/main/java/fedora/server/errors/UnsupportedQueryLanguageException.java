/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * @author eddie
 */
public class UnsupportedQueryLanguageException
        extends ResourceIndexException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public UnsupportedQueryLanguageException(String message) {
        super(message);
    }
}
