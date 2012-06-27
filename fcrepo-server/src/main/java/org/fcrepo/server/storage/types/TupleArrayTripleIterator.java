/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage.types;

import org.fcrepo.common.rdf.SimpleTriple;
import org.fcrepo.common.rdf.SimpleURIReference;
import org.jrdf.graph.Triple;
import org.trippi.TripleIterator;
import org.trippi.TrippiException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


public class TupleArrayTripleIterator
        extends TripleIterator {

    private static final HashMap<String, String> DEFAULT_NS = new HashMap<String, String>(2);
    static {
        DEFAULT_NS.put("rel", "info:fedora/fedora-system:def/relations-external#");
        DEFAULT_NS.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    }
    
    int size = 0;

    int index = 0;

    ArrayList<RelationshipTuple> m_TupleArray = null;

    Map<String, String> m_map = null;

    public TupleArrayTripleIterator(ArrayList<RelationshipTuple> array,
                                    Map<String, String> map) {
        m_TupleArray = array;
        size = array.size();
        m_map = map;
    }

    public TupleArrayTripleIterator(ArrayList<RelationshipTuple> array) {
        m_TupleArray = array;
        size = array.size();
        m_map = DEFAULT_NS;
    }

    @Override
    public boolean hasNext() throws TrippiException {
        return index < size;
    }

    @Override
    public Triple next() throws TrippiException {
        RelationshipTuple tuple = m_TupleArray.get(index++);
        try {
            return tuple.toTriple(m_map);
        } catch (URISyntaxException e) {
            throw new TrippiException("Invalid URI in Triple", e);
        }
    }

    @Override
    public void close() throws TrippiException {
        // TODO Auto-generated method stub

    }
}
