/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that an error occurred during a module's initialization.
 * 
 * @author Chris Wilper
 */
public class ModuleInitializationException
        extends InitializationException {

    private static final long serialVersionUID = 1L;

    /** The role of the module in which the error occurred */
    private final String m_role;

    /**
     * Creates a ModuleInitializationException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     * @param role
     *        The role of the module.
     */
    public ModuleInitializationException(String message, String role) {
        super(message);
        m_role = role;
    }

    public ModuleInitializationException(String message,
                                         String role,
                                         Throwable cause) {
        super(null, message, null, null, cause);
        m_role = role;
    }

    /**
     * Gets the role of the module in which the error occurred.
     * 
     * @return The role.
     */
    public String getRole() {
        return m_role;
    }
}
