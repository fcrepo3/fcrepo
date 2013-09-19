/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage.types;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jrdf.graph.Triple;
import org.trippi.TripleIterator;
import org.trippi.TrippiException;


public class TupleArrayTripleIterator
        extends TripleIterator {

    private static final HashMap<String, String> DEFAULT_NS = new HashMap<String, String>(2);
    static {
        DEFAULT_NS.put("rel", "info:fedora/fedora-system:def/relations-external#");
        DEFAULT_NS.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    }
    
    Iterator<RelationshipTuple> m_tuples = null;

    Map<String, String> m_map = null;
    
    public TupleArrayTripleIterator(Iterator<RelationshipTuple> tuples,
                                    Map<String, String> map) {
        m_tuples = tuples;
        m_map = map;
    }

    public TupleArrayTripleIterator(List<RelationshipTuple> array,
                                    Map<String, String> map) {
        this(array.iterator(), map);
    }

    public TupleArrayTripleIterator(List<RelationshipTuple> array) {
        this(array, DEFAULT_NS);
    }

    @Override
    public boolean hasNext() throws TrippiException {
        return m_tuples.hasNext();
    }

    @Override
    public Triple next() throws TrippiException {
        RelationshipTuple tuple = m_tuples.next();
        try {
            return tuple.toTriple(m_map);
        } catch (URISyntaxException e) {
            throw new TrippiException("Invalid URI in Triple", e);
        }
    }

    @Override
    public void close() throws TrippiException {
        // no-op
    }
}
