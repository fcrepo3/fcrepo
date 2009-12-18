/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

import java.net.URI;

import org.jrdf.graph.AbstractURIReference;

/**
 * A URIReference with convenient constructors.
 *
 * @author Chris Wilper
 */
public class SimpleURIReference
        extends AbstractURIReference {
    
    private static final long serialVersionUID = 1L;
    
    public SimpleURIReference(URI uri) {
        super(uri);
    }
    
    public SimpleURIReference(URI uri, boolean validate) {
        super(uri, validate);
    }
    
}
