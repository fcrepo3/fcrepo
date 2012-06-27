/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage;

import java.io.InputStream;

import java.net.URI;

import java.util.HashSet;
import java.util.Set;

import org.jrdf.graph.Literal;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.Triple;

import org.trippi.RDFFormat;
import org.trippi.TripleIterator;
import org.trippi.TrippiException;
import org.trippi.io.TripleIteratorFactory;

import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.RelationshipTuple;


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
        Set<RelationshipTuple> tuples = new HashSet<RelationshipTuple>();

        TripleIterator iter = null;
        try {
            iter = TripleIteratorFactory.defaultInstance().fromStream(dsContent, RDFFormat.RDF_XML);
            Triple triple;
            while (iter.hasNext()) {
                triple = iter.next();
                tuples.add(RelationshipTuple.fromTriple(triple));
            }
        } finally {
            if (iter != null) {
                iter.close();
            }
        }
        return tuples;
    }
}
