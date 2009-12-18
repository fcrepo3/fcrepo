/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors.authorization;

/**
 * Thrown when authorization cannot be completed.
 * 
 * @author Bill Niebel
 */
public class AuthzOperationalException
        extends AuthzDeniedException {

    private static final long serialVersionUID = 1L;

    public static final String BRIEF_DESC = "Authorization Failed";

    public AuthzOperationalException(String message) {
        super(null, message, null, null, null);
    }

    public AuthzOperationalException(String message, Throwable cause) {
        super(null, message, null, null, cause);
    }

    public AuthzOperationalException(String bundleName,
                                     String code,
                                     String[] replacements,
                                     String[] details,
                                     Throwable cause) {
        super(bundleName, code, replacements, details, cause);
    }

}
