/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */


package org.fcrepo.server.security.xacml.pdp.data;

/**
 * Exception for use in Policy config
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class PolicyConfigException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public PolicyConfigException() {
        super();
    }

    public PolicyConfigException(String message) {
        super(message);
    }

    public PolicyConfigException(Throwable cause) {
        super(cause);
    }

    public PolicyConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
