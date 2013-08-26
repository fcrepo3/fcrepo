/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import java.net.URI;
import java.util.Iterator;

/**
 * A <code>Context</code> used for recovery. This Context provides access to
 * attributes in the recovery namespace.
 * 
 * @see org.fcrepo.common.Constants#RECOVERY
 */
public interface RecoveryContext
        extends Context {

    /**
     * Get the names of all <em>recovery</em> attributes whose values are
     * defined in this context.
     */
    public Iterator<URI> getRecoveryNames();

    /**
     * Get the first value for a <em>recovery</em> attribute, or
     * <code>null</code> if no such value exists in this context.
     */
    public String getRecoveryValue(URI attribute);

    /**
     * Get all values for a <em>recovery</em> attribute, or an empty array if
     * no values exist for the attribute in this context.
     */
    public String[] getRecoveryValues(URI attribute);

}
