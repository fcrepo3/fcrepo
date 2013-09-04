/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.resourceIndex;

import java.util.Collections;
import java.util.Set;

import org.jrdf.graph.Triple;

import org.fcrepo.server.errors.ResourceIndexException;
import org.fcrepo.server.storage.DOReader;


/**
 * Get all triples for a 3.0 content model object.
 *
 * @author Aaron Birkland
 */
public class ContentModelTripleGenerator_3_0
        implements TripleGenerator {

    /**
     * {@inheritDoc}
     */
    public Set<Triple> getTriplesForObject(DOReader reader)
            throws ResourceIndexException {
        // no special triples for this content model
        return Collections.emptySet();
    }

}
