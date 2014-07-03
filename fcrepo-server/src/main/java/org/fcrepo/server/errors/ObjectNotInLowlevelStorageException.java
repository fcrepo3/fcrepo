/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.errors;

/**
 * @author Bill Niebel
 */
public class ObjectNotInLowlevelStorageException
        extends LowlevelStorageException implements ResourceNotFoundError {

    private static final long serialVersionUID = 1L;

    public ObjectNotInLowlevelStorageException(String message, Throwable cause) {
        super(false, message, cause);
    }

    public ObjectNotInLowlevelStorageException(String message) {
        this(message, null);
    }
}
