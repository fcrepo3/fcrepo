/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.validation;

import java.util.Collection;
import java.util.Iterator;

import org.jrdf.graph.Triple;

import org.trippi.TripleIterator;
import org.trippi.TrippiException;

public class MockTripleIterator
        extends TripleIterator {

    private final Iterator<Triple> triples;

    public MockTripleIterator(Collection<Triple> triples) {
        this.triples = triples.iterator();
    }

    @Override
    public void close() throws TrippiException {

    }

    @Override
    public boolean hasNext() throws TrippiException {
        return triples.hasNext();
    }

    @Override
    public Triple next() throws TrippiException {
        return triples.next();
    }
}
