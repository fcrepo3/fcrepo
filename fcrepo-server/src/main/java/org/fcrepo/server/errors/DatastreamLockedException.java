/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.errors;

/**
 * Signals that a datastream was locked.
 *
 * @author Edwin Shin
 * @version $Id$
 */
public class DatastreamLockedException
        extends StorageException implements ResourceLockedError {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a DatastreamLockedException.
     *
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public DatastreamLockedException(String message) {
        super(message);
    }

}
