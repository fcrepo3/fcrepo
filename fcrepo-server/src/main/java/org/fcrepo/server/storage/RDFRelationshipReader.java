/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.RelationshipTuple;
import org.jrdf.graph.Triple;
import org.trippi.RDFFormat;
import org.trippi.TrippiException;
import org.trippi.io.TripleIteratorFactory;


public abstract class RDFRelationshipReader {

    public static Set<RelationshipTuple> readRelationships(Datastream ds)
            throws ServerException {

        if (ds == null) {
            return new HashSet<RelationshipTuple>();
        }

        try {
            return readRelationships(ds.getContentStream());
        } catch (TrippiException e) {
            throw new GeneralException(e.getMessage(), e);
        }
    }

    public static Set<RelationshipTuple> readRelationships(InputStream dsContent)
            throws TrippiException {

        Set<Triple> triples =
                TripleIteratorFactory.defaultInstance().allAsSet(dsContent, null, RDFFormat.RDF_XML);
        Set<RelationshipTuple> tuples =
                new HashSet<RelationshipTuple>(triples.size());
        for (Triple triple: triples) {
            tuples.add(RelationshipTuple.fromTriple(triple));
        }
        return tuples;
    }
}
