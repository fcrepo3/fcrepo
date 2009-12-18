/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.resourceIndex;

import java.io.IOException;
import java.io.OutputStream;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.GraphElementFactoryException;
import org.jrdf.graph.Literal;
import org.jrdf.graph.Node;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;

import org.trippi.FlushErrorHandler;
import org.trippi.RDFFormat;
import org.trippi.TripleIterator;
import org.trippi.TripleUpdate;
import org.trippi.TriplestoreConnector;
import org.trippi.TriplestoreWriter;
import org.trippi.TrippiException;
import org.trippi.TupleIterator;

import fedora.server.errors.ResourceIndexException;
import fedora.server.storage.DOReader;

/**
 * Implementation of the <code>ResourceIndex</code>.
 * 
 * @author Chris Wilper
 */
public class ResourceIndexImpl
        implements ResourceIndex {

    /** Interface to the underlying triplestore. */
    private final TriplestoreConnector _connector;

    /** Writer for the underlying triplestore. */
    private final TriplestoreWriter _writer;

    /** The TripleGenerator this instance will use. */
    private final TripleGenerator _generator;

    /** The current index level. */
    private final int _indexLevel;

    /**
     * Whether triples should be flushed to storage before returning from each
     * object modification method.
     */
    private final boolean _syncUpdates;

    ////////////////////
    // Initialization //
    ////////////////////

    public ResourceIndexImpl(TriplestoreConnector connector,
                             TripleGenerator generator,
                             int indexLevel,
                             boolean syncUpdates) {
        _connector = connector;
        _writer = _connector.getWriter();
        _generator = generator;
        _indexLevel = indexLevel;
        _syncUpdates = syncUpdates;
    }

    ///////////////////////////
    // ResourceIndex methods //
    ///////////////////////////

    /**
     * {@inheritDoc}
     */
    public int getIndexLevel() {
        return _indexLevel;
    }

    /**
     * {@inheritDoc}
     */
    public void addObject(DOReader reader) throws ResourceIndexException {
        if (_indexLevel > INDEX_LEVEL_OFF) {
            updateTriples(_generator.getTriplesForObject(reader), false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void modifyObject(DOReader oldReader, DOReader newReader)
            throws ResourceIndexException {
        if (_indexLevel > INDEX_LEVEL_OFF) {
            updateTripleDiffs(_generator.getTriplesForObject(oldReader),
                              _generator.getTriplesForObject(newReader));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteObject(DOReader oldReader) throws ResourceIndexException {
        if (_indexLevel > INDEX_LEVEL_OFF) {
            updateTriples(_generator.getTriplesForObject(oldReader), true);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void export(OutputStream out, RDFFormat format)
            throws ResourceIndexException {
        try {
            TripleIterator it = _writer.findTriples(null, null, null, 0);
            it.setAliasMap(_writer.getAliasMap());
            it.toStream(out, format);
        } catch (TrippiException e) {
            throw new ResourceIndexException("Unable to export RI", e);
        }
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    /**
     * Applies the given adds or deletes to the triplestore. If _syncUpdates is
     * true, changes will be flushed before returning.
     */
    private void updateTriples(Set<Triple> set, boolean delete)
            throws ResourceIndexException {
        try {
            if (delete) {
                _writer.delete(getTripleIterator(set), _syncUpdates);
            } else {
                _writer.add(getTripleIterator(set), _syncUpdates);
            }
        } catch (Exception e) {
            throw new ResourceIndexException("Error updating triples", e);
        }
    }

    /**
     * Computes the difference between the given sets and applies the
     * appropriate deletes and adds to the triplestore. If _syncUpdates is true,
     * changes will be flushed before returning.
     */
    private void updateTripleDiffs(Set<Triple> existing, Set<Triple> desired)
            throws ResourceIndexException {

        // Delete any existing triples that are no longer desired,
        // leaving the ones we want in place
        HashSet<Triple> obsoleteTriples = new HashSet<Triple>(existing);
        obsoleteTriples.removeAll(desired);
        updateTriples(obsoleteTriples, true);

        // Add only new desired triples
        HashSet<Triple> newTriples = new HashSet<Triple>(desired);
        newTriples.removeAll(existing);
        updateTriples(newTriples, false);

    }

    /**
     * Gets a Trippi TripleIterator for the given set.
     */
    private TripleIterator getTripleIterator(final Set<Triple> set) {
        return new TripleIterator() {

            private final Iterator<Triple> _iter = set.iterator();

            @Override
            public boolean hasNext() {
                return _iter.hasNext();
            }

            @Override
            public Triple next() {
                return getLocalizedTriple(_iter.next());
            }

            @Override
            public void close() {
            }
        };
    }
    
    /**
     * Gets a Triple appropriate for writing to the store.
     */
    private Triple getLocalizedTriple(Triple triple) {
        try {
            return _connector.getElementFactory().createTriple(
                    getLocalizedResource(triple.getSubject()),
                    getLocalizedResource(triple.getPredicate()),
                    getLocalizedObject(triple.getObject()));
        } catch (GraphElementFactoryException e) {
            throw new RuntimeException("Error localizing triple", e);
        }
    }

    /**
     * Gets a localized URIReference based on the given Node.
     */
    private URIReference getLocalizedResource(Node n)
            throws GraphElementFactoryException {
        if (n instanceof URIReference) {
            URIReference u = (URIReference) n;
            return _connector.getElementFactory().createResource(u.getURI());
        } else {
            throw new RuntimeException("Error localizing triple; "
                    + n.getClass().getName() + " is not a URIReference");
        }
    }

    /**
     * Gets a localized URIReference or Literal based on the given Node.
     */
    private ObjectNode getLocalizedObject(Node n)
            throws GraphElementFactoryException {
        if (n instanceof URIReference) {
            return getLocalizedResource(n);
        } else if (n instanceof Literal) {
            Literal l = (Literal) n;
            GraphElementFactory elementFactory = _connector.getElementFactory();
            if (l.getDatatypeURI() != null) {
                return elementFactory.createLiteral(l.getLexicalForm(),
                                                    l.getDatatypeURI());
            } else if (l.getLanguage() != null) {
                return elementFactory.createLiteral(l.getLexicalForm(),
                                                    l.getLanguage());
            } else {
                return elementFactory.createLiteral(l.getLexicalForm());
            }
        } else {
            throw new RuntimeException("Error localizing triple; "
                    + n.getClass().getName() + " is not a URIReference "
                    + "or a Literal");
        }
    }

    ///////////////////////////////
    // TriplestoreReader methods //
    ///////////////////////////////

    /**
     * {@inheritDoc}
     */
    public void setAliasMap(Map<String, String> aliasToPrefix)
            throws TrippiException {
        _writer.setAliasMap(aliasToPrefix);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getAliasMap() throws TrippiException {
        return _writer.getAliasMap();
    }

    /**
     * {@inheritDoc}
     */
    public TupleIterator findTuples(String queryLang,
                                    String tupleQuery,
                                    int limit,
                                    boolean distinct) throws TrippiException {
        return _writer.findTuples(queryLang, tupleQuery, limit, distinct);
    }

    /**
     * {@inheritDoc}
     */
    public int countTuples(String queryLang,
                           String tupleQuery,
                           int limit,
                           boolean distinct) throws TrippiException {
        return _writer.countTuples(queryLang, tupleQuery, limit, distinct);
    }

    /**
     * {@inheritDoc}
     */
    public TripleIterator findTriples(String queryLang,
                                      String tripleQuery,
                                      int limit,
                                      boolean distinct) throws TrippiException {
        return _writer.findTriples(queryLang, tripleQuery, limit, distinct);
    }

    /**
     * {@inheritDoc}
     */
    public int countTriples(String queryLang,
                            String tripleQuery,
                            int limit,
                            boolean distinct) throws TrippiException {
        return _writer.countTriples(queryLang, tripleQuery, limit, distinct);
    }

    /**
     * {@inheritDoc}
     */
    public TripleIterator findTriples(SubjectNode subject,
                                      PredicateNode predicate,
                                      ObjectNode object,
                                      int limit) throws TrippiException {
        return _writer.findTriples(subject, predicate, object, limit);
    }

    /**
     * {@inheritDoc}
     */
    public int countTriples(SubjectNode subject,
                            PredicateNode predicate,
                            ObjectNode object,
                            int limit) throws TrippiException {
        return _writer.countTriples(subject, predicate, object, limit);
    }

    /**
     * {@inheritDoc}
     */
    public TripleIterator findTriples(String queryLang,
                                      String tupleQuery,
                                      String tripleTemplate,
                                      int limit,
                                      boolean distinct) throws TrippiException {
        return _writer.findTriples(queryLang,
                                   tupleQuery,
                                   tripleTemplate,
                                   limit,
                                   distinct);
    }

    /**
     * {@inheritDoc}
     */
    public int countTriples(String queryLang,
                            String tupleQuery,
                            String tripleTemplate,
                            int limit,
                            boolean distinct) throws TrippiException {
        return _writer.countTriples(queryLang,
                                    tupleQuery,
                                    tripleTemplate,
                                    limit,
                                    distinct);
    }

    /**
     * {@inheritDoc}
     */
    public String[] listTupleLanguages() {
        return _writer.listTupleLanguages();
    }

    /**
     * {@inheritDoc}
     */
    public String[] listTripleLanguages() {
        return _writer.listTripleLanguages();
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws TrippiException {
        _connector.close();
    }

    ///////////////////////////////
    // TriplestoreWriter methods //
    ///////////////////////////////

    /**
     * {@inheritDoc}
     */
    public void add(List<Triple> triples, boolean flush) throws IOException,
            TrippiException {
        _writer.add(triples, flush);
    }

    /**
     * {@inheritDoc}
     */
    public void add(TripleIterator triples, boolean flush) throws IOException,
            TrippiException {
        _writer.add(triples, flush);
    }

    /**
     * {@inheritDoc}
     */
    public void add(Triple triple, boolean flush) throws IOException,
            TrippiException {
        _writer.add(triple, flush);
    }

    /**
     * {@inheritDoc}
     */
    public void delete(List<Triple> triples, boolean flush) throws IOException,
            TrippiException {
        _writer.delete(triples, flush);
    }

    /**
     * {@inheritDoc}
     */
    public void delete(TripleIterator triples, boolean flush)
            throws IOException, TrippiException {
        _writer.delete(triples, flush);
    }

    /**
     * {@inheritDoc}
     */
    public void delete(Triple triple, boolean flush) throws IOException,
            TrippiException {
        _writer.delete(triple, flush);
    }

    /**
     * {@inheritDoc}
     */
    public void flushBuffer() throws IOException, TrippiException {
        _writer.flushBuffer();
    }

    /**
     * {@inheritDoc}
     */
    public void setFlushErrorHandler(FlushErrorHandler h) {
        _writer.setFlushErrorHandler(h);
    }

    /**
     * {@inheritDoc}
     */
    public int getBufferSize() {
        return _writer.getBufferSize();
    }

    /**
     * {@inheritDoc}
     */
    public List<TripleUpdate> findBufferedUpdates(SubjectNode subject,
                                                  PredicateNode predicate,
                                                  ObjectNode object,
                                                  int updateType) {
        return _writer.findBufferedUpdates(subject,
                                           predicate,
                                           object,
                                           updateType);
    }

}
