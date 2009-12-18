/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * An exception indicating a low-level configuration or related problem 
 * with the repository software.
 * 
 * <p>This is likely due to a bad installation.
 * 
 * @author Chris Wilper
 */
public class RepositoryConfigurationException
        extends ServerException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a RepositoryConfiguration Exception.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public RepositoryConfigurationException(String message) {
        super(null, message, null, null, null);
    }

    public RepositoryConfigurationException(String bundleName,
                                            String code,
                                            String[] values,
                                            String[] details,
                                            Throwable cause) {
        super(bundleName, code, values, details, cause);
    }

}
