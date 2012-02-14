/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.errors;

import org.fcrepo.server.storage.types.Validation;

/**
 * Signals that an object is not valid.
 * 
 * @author Sandy Payette
 */
public class ObjectValidityException
        extends ServerException {

    private static final long serialVersionUID = 1L;
    
    protected Validation m_validation = null;

    /**
     * Creates an ObjectValidityException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public ObjectValidityException(String message, Validation validation) {
        super(null, message, null, null, null);
        m_validation = validation;
    }

    public ObjectValidityException(String message) {
        super(null, message, null, null, null);
    }
    public ObjectValidityException(String message, Validation validation, Throwable cause) {
        super(null, message, null, null, cause);
        m_validation = validation;
    }
    public ObjectValidityException(String message, Throwable cause) {
        super(null, message, null, null, cause);
    }

    public ObjectValidityException(String a,
                                   String message,
                                   String[] b,
                                   String[] c,
                                   Throwable th) {
        super(a, message, b, c, th);
    }
    
    public Validation getValidation() {
    	return m_validation;
    }

}
