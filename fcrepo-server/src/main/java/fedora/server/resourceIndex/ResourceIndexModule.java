/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.resourceIndex;

import java.io.IOException;
import java.io.OutputStream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;

import org.trippi.FlushErrorHandler;
import org.trippi.RDFFormat;
import org.trippi.TripleIterator;
import org.trippi.TripleUpdate;
import org.trippi.TriplestoreConnector;
import org.trippi.TrippiException;
import org.trippi.TupleIterator;

import fedora.common.Constants;

import fedora.server.Module;
import fedora.server.Parameterized;
import fedora.server.Server;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ModuleShutdownException;
import fedora.server.errors.ResourceIndexException;
import fedora.server.storage.DOReader;
import fedora.server.utilities.status.ServerState;

/**
 * Fedora's <code>ResourceIndex</code> as a configurable module.
 * 
 * @author Chris Wilper
 */
public class ResourceIndexModule
        extends Module
        implements ResourceIndex {

    /**
     * The instance this module wraps.
     */
    private ResourceIndex _ri;

    /////////////////////////////////////
    // Initialization & Module Methods //
    /////////////////////////////////////

    /**
     * Instantiate the module.
     */
    public ResourceIndexModule(Map<String, String> parameters,
                               Server server,
                               String role)
            throws ModuleInitializationException {
        super(parameters, server, role);
    }

    /**
     * Perform post-initialization of this module. ResourceIndexModule takes the
     * following parameters:
     * <ul>
     * <li> level (required, integer between 0 and 1)<br/> The level of
     * indexing that should be performed. Values correspond to
     * <code>INDEX_LEVEL_OFF</code>, and <code>INDEX_LEVEL_ON</code>.
     * </li>
     * <li> datastore (required)<br/> The name of the datastore element that
     * contains the Trippi Connector configuration.
     * <li> connectionPool (optional, default is ConnectionPoolManager's
     * default)<br/> Which connection pool to use for updating the database of
     * method information. </li>
     * <li> syncUpdates (optional, default is false)<br/> Whether to flush the
     * triple buffer before returning from object modification operations.
     * Specifying this as true will ensure that RI queries always reflect the
     * latest triples. </li>
     * <li> alias:xyz (optional, uri)<br/> Any parameter starting with "alias:"
     * will be put into Trippi's alias map, and can be used for queries. For
     * example, alias:xyz with a value of urn:example:long:uri:x:y:z: will make
     * it possible to use "xyz:a" to mean "urn:example:long:uri:x:y:z:a" in
     * queries. </li>
     * </ul>
     */
    @Override
    public void postInitModule() throws ModuleInitializationException {
        int level = getRequiredInt("level", 0, 1);
        if (level == 0) {
            return;
        }
        boolean syncUpdates = getBoolean("syncUpdates", false);
        try {
            TriplestoreConnector connector =
                    getConnector(getServer()
                            .getDatastoreConfig(getRequired("datastore")));

            TripleGenerator generator = new ModelBasedTripleGenerator();

            _ri = new ResourceIndexImpl(connector,
                                        generator,
                                        level,
                                        syncUpdates);
            setAliasMap(getAliases());
        } catch (Exception e) {
            throw new ModuleInitializationException("Error initializing RI",
                                                    getRole(),
                                                    e);
        }
    }

    private TriplestoreConnector getConnector(Parameterized datastore)
            throws Exception {
        if (datastore == null) {
            throw new ModuleInitializationException("Specifed datastore "
                    + "does not exist in fedora.fcfg", getRole());
        }
        Map<String, String> config = datastore.getParameters();
        // make sure path, if specified and relative, is translated
        // to an absolute path based on the value of FEDORA_HOME
        String path = config.get("path");
        if (path != null) {
            config.put("path", datastore.getParameter("path", true));
        }
        String className = config.get("connectorClassName");
        if (className == null) {
            throw new ResourceIndexException("Required datastore parameter "
                    + "is missing: connectorClassName");
        }
        getServer().getStatusFile().append(ServerState.STARTING,
                                           "Initializing Triplestore");
        return TriplestoreConnector.init(className, config);
    }

    private Map<String, String> getAliases() {
        HashMap<String, String> map = new HashMap<String, String>();
        Iterator<String> iter = parameterNames();
        while (iter.hasNext()) {
            String pName = iter.next();
            String[] parts = pName.split(":");
            if (parts.length == 2 && parts[0].equals("alias")) {
                map.put(parts[1], getParameter(pName));
            }
        }
        map.put("fedora", Constants.FEDORA.uri);
        map.put("dc", Constants.DC.uri);
        map.put("fedora-model", Constants.MODEL.uri);
        map.put("fedora-rels-ext", Constants.RELS_EXT.uri);
        map.put("fedora-view", Constants.VIEW.uri);
        map.put("rdf", Constants.RDF.uri);
        map.put("mulgara", Constants.MULGARA.uri);
        map.put("xml-schema", Constants.RDF_XSD.uri);
        return map;
    }

    private int getRequiredInt(String name, int min, int max)
            throws ModuleInitializationException {
        try {
            int value = Integer.parseInt(getRequired(name));
            if (value < min || value > max) {
                throw new ModuleInitializationException(name
                        + " parameter is out of range, expected [" + min + "-"
                        + max + "]", getRole());
            }
            return value;
        } catch (NumberFormatException e) {
            throw new ModuleInitializationException(name
                    + " parameter must be " + "an integer", getRole());
        }
    }

    private boolean getBoolean(String name, boolean defaultValue)
            throws ModuleInitializationException {
        String value = getParameter(name);
        if (value == null) {
            return defaultValue;
        }
        value = value.toLowerCase();
        if (value.equals("true") || value.equals("yes") || value.equals("on")) {
            return true;
        } else if (value.equals("false") || value.equals("no")
                || value.equals("off")) {
            return false;
        } else {
            throw new ModuleInitializationException(name + " parameter, if "
                    + "specified, must be a boolean (true or false)", getRole());
        }
    }

    private String getRequired(String name)
            throws ModuleInitializationException {
        String value = getParameter(name);
        if (value != null) {
            return value;
        } else {
            throw new ModuleInitializationException(name + " parameter "
                    + "is required", getRole());
        }
    }

    /**
     * Shutdown the RI module by closing the wrapped ResourceIndex.
     * 
     * @throws ModuleShutdownException
     *         if any error occurs while closing.
     */
    @Override
    public void shutdownModule() throws ModuleShutdownException {
        if (_ri != null) {
            try {
                _ri.close();
            } catch (TrippiException e) {
                throw new ModuleShutdownException("Error closing RI",
                                                  getRole(),
                                                  e);
            }
        }
    }

    ///////////////////////////
    // ResourceIndex Methods //
    ///////////////////////////

    /**
     * {@inheritDoc}
     */
    public int getIndexLevel() {
        if (_ri == null) {
            return 0;
        } else {
            return _ri.getIndexLevel();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addObject(DOReader reader) throws ResourceIndexException {
        _ri.addObject(reader);
    }

    /**
     * {@inheritDoc}
     */
    public void modifyObject(DOReader oldReader, DOReader newReader)
            throws ResourceIndexException {
        _ri.modifyObject(oldReader, newReader);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteObject(DOReader oldReader) throws ResourceIndexException {
        _ri.deleteObject(oldReader);
    }

    /**
     * {@inheritDoc}
     */
    public void export(OutputStream out, RDFFormat format)
            throws ResourceIndexException {
        _ri.export(out, format);
    }

    ///////////////////////////////
    // TriplestoreReader methods //
    ///////////////////////////////

    /**
     * {@inheritDoc}
     */
    public void setAliasMap(Map<String, String> aliasToPrefix)
            throws TrippiException {
        _ri.setAliasMap(aliasToPrefix);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getAliasMap() throws TrippiException {
        return _ri.getAliasMap();
    }

    /**
     * {@inheritDoc}
     */
    public TupleIterator findTuples(String queryLang,
                                    String tupleQuery,
                                    int limit,
                                    boolean distinct) throws TrippiException {
        return _ri.findTuples(queryLang, tupleQuery, limit, distinct);
    }

    /**
     * {@inheritDoc}
     */
    public int countTuples(String queryLang,
                           String tupleQuery,
                           int limit,
                           boolean distinct) throws TrippiException {
        return _ri.countTuples(queryLang, tupleQuery, limit, distinct);
    }

    /**
     * {@inheritDoc}
     */
    public TripleIterator findTriples(String queryLang,
                                      String tripleQuery,
                                      int limit,
                                      boolean distinct) throws TrippiException {
        return _ri.findTriples(queryLang, tripleQuery, limit, distinct);
    }

    /**
     * {@inheritDoc}
     */
    public int countTriples(String queryLang,
                            String tripleQuery,
                            int limit,
                            boolean distinct) throws TrippiException {
        return _ri.countTriples(queryLang, tripleQuery, limit, distinct);
    }

    /**
     * {@inheritDoc}
     */
    public TripleIterator findTriples(SubjectNode subject,
                                      PredicateNode predicate,
                                      ObjectNode object,
                                      int limit) throws TrippiException {
        return _ri.findTriples(subject, predicate, object, limit);
    }

    /**
     * {@inheritDoc}
     */
    public int countTriples(SubjectNode subject,
                            PredicateNode predicate,
                            ObjectNode object,
                            int limit) throws TrippiException {
        return _ri.countTriples(subject, predicate, object, limit);
    }

    /**
     * {@inheritDoc}
     */
    public TripleIterator findTriples(String queryLang,
                                      String tupleQuery,
                                      String tripleTemplate,
                                      int limit,
                                      boolean distinct) throws TrippiException {
        return _ri.findTriples(queryLang,
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
        return _ri.countTriples(queryLang,
                                tupleQuery,
                                tripleTemplate,
                                limit,
                                distinct);
    }

    /**
     * {@inheritDoc}
     */
    public String[] listTupleLanguages() {
        return _ri.listTupleLanguages();
    }

    /**
     * {@inheritDoc}
     */
    public String[] listTripleLanguages() {
        return _ri.listTripleLanguages();
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws TrippiException {
        // no-op; closing must be done via Module.shutdownModule
    }

    ///////////////////////////////
    // TriplestoreWriter methods //
    ///////////////////////////////

    /**
     * {@inheritDoc}
     */
    public void add(List<Triple> triples, boolean flush) throws IOException,
            TrippiException {
        _ri.add(triples, flush);
    }

    /**
     * {@inheritDoc}
     */
    public void add(TripleIterator triples, boolean flush) throws IOException,
            TrippiException {
        _ri.add(triples, flush);
    }

    /**
     * {@inheritDoc}
     */
    public void add(Triple triple, boolean flush) throws IOException,
            TrippiException {
        _ri.add(triple, flush);
    }

    /**
     * {@inheritDoc}
     */
    public void delete(List<Triple> triples, boolean flush) throws IOException,
            TrippiException {
        _ri.delete(triples, flush);
    }

    /**
     * {@inheritDoc}
     */
    public void delete(TripleIterator triples, boolean flush)
            throws IOException, TrippiException {
        _ri.delete(triples, flush);
    }

    /**
     * {@inheritDoc}
     */
    public void delete(Triple triple, boolean flush) throws IOException,
            TrippiException {
        _ri.delete(triple, flush);
    }

    /**
     * {@inheritDoc}
     */
    public void flushBuffer() throws IOException, TrippiException {
        _ri.flushBuffer();
    }

    /**
     * {@inheritDoc}
     */
    public void setFlushErrorHandler(FlushErrorHandler h) {
        _ri.setFlushErrorHandler(h);
    }

    /**
     * {@inheritDoc}
     */
    public int getBufferSize() {
        return _ri.getBufferSize();
    }

    /**
     * {@inheritDoc}
     */
    public List<TripleUpdate> findBufferedUpdates(SubjectNode subject,
                                                  PredicateNode predicate,
                                                  ObjectNode object,
                                                  int updateType) {
        return _ri.findBufferedUpdates(subject, predicate, object, updateType);
    }

}
