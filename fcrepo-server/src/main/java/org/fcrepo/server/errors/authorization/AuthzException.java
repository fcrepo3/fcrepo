/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.errors.authorization;

import org.fcrepo.server.errors.ServerException;

/**
 * Thrown when functional processing should discontinue.
 * 
 * @author Bill Niebel
 */
public abstract class AuthzException
        extends ServerException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public static final String BRIEF_DESC = "Used for authorization signaling";

    public AuthzException(String message) {
        super(null, message, null, null, null);
    }

    public AuthzException(String bundleName,
                          String code,
                          String[] replacements,
                          String[] details,
                          Throwable cause) {
        super(bundleName, code, replacements, details, cause);
    }

}
