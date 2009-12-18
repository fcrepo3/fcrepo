/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security;

import fedora.server.errors.BackendSecurityParserException;

/**
 * Interface for accessing backend service security info.
 * 
 * @author Ross Wayland
 */
public interface BackendSecurity {

    /**
     * Gets the current instance of BackendSecuritySpec.
     * 
     * @return Current instance of backendSecuritySpec.
     */
    public BackendSecuritySpec getBackendSecuritySpec();

    /**
     * Sets the current instance of BackendSecuritySpec by parsing the
     * beSecurity configuration file.
     * 
     * @throws BackendSecurityParserException
     *         If an error occurs in parsing the beSecurity configuration file.
     */
    public void setBackendSecuritySpec() throws BackendSecurityParserException;

    /**
     * Parses the beSecurity configuration file and stores the results in an
     * instance of BackendSecuritySpec.
     * 
     * @return An instance of BackendSecuritySpec.
     * @throws BackendSecurityParserException
     *         If an error occursin parsing the beSecurity configuration file.
     */
    public BackendSecuritySpec parseBeSecurity()
            throws BackendSecurityParserException;

    /**
     * Reloads the backend service security info by reparsing the beSecurity
     * configuration file and storing results in an instance of
     * BackendSecuritySpec.
     * 
     * @return An instance of BackendSecuritySpec.
     * @throws BackendSecurityParserException
     *         If an error occurs in trying to reparse the beSecurity
     *         configuration file.
     */
    public BackendSecuritySpec reloadBeSecurity()
            throws BackendSecurityParserException;

}
