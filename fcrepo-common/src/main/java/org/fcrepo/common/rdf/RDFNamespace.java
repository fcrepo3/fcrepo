/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.common.rdf;

import java.io.Serializable;

/**
 * @author Chris Wilper
 */
public abstract class RDFNamespace
        implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public String uri;

    public String prefix;

}
