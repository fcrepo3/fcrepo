/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.errors;

/**
 * Signals that the value of the Range request header cannot be satisfied by
 * the bytestream requested. Corresponds to HTTP Status Code 416.
 * @author armintor@gmail.com
 */
public class RangeNotSatisfiableException
        extends DisseminationException {

    private static final long serialVersionUID = 1L;

    public static final int STATUS_CODE = 416;
    /**
     * Creates an RangeNotSatisfiableException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public RangeNotSatisfiableException(String message) {
        super(message);
    }

}
