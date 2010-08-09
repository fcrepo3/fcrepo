/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */


package org.fcrepo.server.security.xacml.pdp.data;

/**
 * Exception for use in PolicyIndex
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class PolicyIndexException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public PolicyIndexException() {
        super();
    }

    public PolicyIndexException(String message) {
        super(message);
    }

    public PolicyIndexException(Throwable cause) {
        super(cause);
    }

    public PolicyIndexException(String message, Throwable cause) {
        super(message, cause);
    }
}
