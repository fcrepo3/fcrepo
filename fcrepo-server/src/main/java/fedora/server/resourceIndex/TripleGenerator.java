/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.resourceIndex;

import java.util.Set;

import org.jrdf.graph.Triple;

import fedora.server.errors.ResourceIndexException;
import fedora.server.storage.DOReader;

/**
 * Generates RDF triples for Fedora objects.
 * 
 * @author Chris Wilper
 */
public interface TripleGenerator {

    /**
     * Get triples implied by the given object.
     * 
     * @param reader
     *        Current object from which to determine triples
     * @return Set of triples implied by the objects contents.
     * @throws ResourceIndexException
     */
    public Set<Triple> getTriplesForObject(DOReader reader)
            throws ResourceIndexException;

}
