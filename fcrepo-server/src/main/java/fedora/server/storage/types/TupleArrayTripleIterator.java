/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.types;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.Triple;

import org.trippi.TripleIterator;
import org.trippi.TrippiException;

import fedora.common.rdf.SimpleLiteral;
import fedora.common.rdf.SimpleTriple;
import fedora.common.rdf.SimpleURIReference;

import fedora.server.storage.types.RelationshipTuple;

public class TupleArrayTripleIterator
        extends TripleIterator {

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
        m_map = new HashMap<String, String>();
        m_map.put("rel", "info:fedora/fedora-system:def/relations-external#");
        m_map.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    }

    @Override
    public boolean hasNext() throws TrippiException {
        return index < size;
    }

    @Override
    public Triple next() throws TrippiException {
        RelationshipTuple tuple = m_TupleArray.get(index++);
        try {
            Triple triple = new SimpleTriple(
                    new SimpleURIReference(new URI(tuple.subject)),
                    makePredicateResourceFromRel(tuple.predicate,
                                                 m_map),
                    makeObjectFromURIandLiteral(tuple.object,
                                                tuple.isLiteral,
                                                tuple.datatype));
            return triple;
        } catch (URISyntaxException e) {
            throw new TrippiException("Invalid URI in Triple", e);
        }
    }

    public static ObjectNode makeObjectFromURIandLiteral(String objURI,
                                                         boolean isLiteral,
                                                         String literalType)
            throws URISyntaxException {
        ObjectNode obj = null;
        if (isLiteral) {
            if (literalType == null || literalType.length() == 0) {
                obj = new SimpleLiteral(objURI);
            } else {
                obj = new SimpleLiteral(objURI, new URI(literalType));
            }
        } else {
            obj = new SimpleURIReference(new URI(objURI));
        }
        return obj;
    }

    public static PredicateNode makePredicateResourceFromRel(String predicate,
                                                             Map<String, String> map)
            throws URISyntaxException {
        URI predURI = makePredicateFromRel(predicate, map);
        PredicateNode node = new SimpleURIReference(predURI);
        return node;
    }

    public static URI makePredicateFromRel(String relationship, Map map)
            throws URISyntaxException {
        String predicate = relationship;
        Set keys = map.keySet();
        Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            if (predicate.startsWith(key + ":")) {
                predicate = predicate.replaceFirst(key + ":",
                                                   (String) map.get(key));
            }
        }

        URI retVal = null;
        retVal = new URI(predicate);
        return retVal;
    }

    @Override
    public void close() throws TrippiException {
        // TODO Auto-generated method stub

    }
}
