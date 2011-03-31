package org.fcrepo.server.security.xacml.pdp.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.sun.xacml.EvaluationCtx;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XPathQueryService;

import org.fcrepo.server.security.xacml.pdp.MelcoePDP;
import org.fcrepo.server.security.xacml.util.AttributeBean;

/**
 * A PolicyIndex based on an XPath XML database.
 *
 * This implementation only tested on eXist, but as only generic xmldb API methods have
 * been used it should work with some customisation on other XML databases that support
 * the xmldb API.  Customisations will be required for driver configuration, indexing,
 * and potentially organisation of collections (eg root collection name).
 *
 * Concurrency handled with a ReentrantReadWriteLock (although eXist does natively have
 * some concurrency support).
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class ExistPolicyIndex extends XPathPolicyIndex implements PolicyIndex {

    private static final Logger log =
        LoggerFactory.getLogger(ExistPolicyIndex.class.getName());

    // path of eXist root collection (within which policies collection stored)
    private static final String m_rootCollectionPath = "/db";

    // eXist index config document name
    private static final String m_indexDocumentName = "collection.xconf";


    // string URI of database
    private static String m_databaseURI;

    // name of collection to store FeSL policies in
    private static String m_collectionName;

    // path of policies collection URI (nb: needs to include base collection - /db)
    private static String m_collectionPath ;

    // path to collection containing index config document
    private static String m_indexCollectionPath;

    // admin credentials
    private static String m_user;
    private static String m_password;

    // the main policies collection object used to perform queries, adds, updates etc
    protected Collection m_collection;

    // allow multiple read threads, block reading when writing, allow single write thread
    private static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private static final Lock readLock = rwl.readLock();
    private static final Lock writeLock = rwl.writeLock();


    protected ExistPolicyIndex() throws PolicyIndexException {
        super();

        // initialise from config, set up driver
        init();

        // create collection if needed
        try {
            writeLock.lock();
            initCollection();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public String addPolicy(String name, String document) throws PolicyIndexException {
        String Id = nameToId(name);
        XMLResource res;
        try {
            writeLock.lock();
            // check it doesn't already exist
            res = (XMLResource) m_collection.getResource(Id);
            if (res != null ) {
                throw new PolicyIndexException("Tried to add an already-existing resource " + name);
            }

            // create the new resource
            res = (XMLResource) m_collection.createResource(Id, XMLResource.RESOURCE_TYPE);

            // set the resource content
            res.setContentAsDOM(createDocument(document));

            // add it
            m_collection.storeResource(res);
        } catch (XMLDBException e) {
            log.error("Error adding resource " + name + " " + e.getMessage(), e);
            throw new PolicyIndexException("Error adding resource " + name + " " + e.getMessage(), e);
        } finally {
            writeLock.unlock();
        }
        return name;
    }

    @Override
    public boolean clear() throws PolicyIndexException {
        try {
            writeLock.lock();
            deleteCollection();
        } finally {
            writeLock.unlock();
        }
        return true;

    }

    @Override
    public boolean contains(String policyName) throws PolicyIndexException {
        try {
            readLock.lock();
            return (m_collection.getResource(nameToId(policyName)) != null);
        } catch (XMLDBException e) {
            log.error("Error determining if db contains " + policyName + " - " + e.getMessage(), e);
            throw new PolicyIndexException("Error determining if db contains " + policyName + " - " + e.getMessage(), e);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean deletePolicy(String name) throws PolicyIndexException {
        String Id = nameToId(name);
        try {
            writeLock.lock();
            Resource res = m_collection.getResource(Id);
            if (res == null) {
                log.warn("Attempted to delete non-existing resource " + name);
                return false;
            }
            m_collection.removeResource(res);
        } catch (XMLDBException e) {
            log.error("Error deleting resource " + name + " " + e.getMessage(), e);
            throw new PolicyIndexException("Error deleting resource " + name + " " + e.getMessage(), e);
        } finally {
            writeLock.unlock();
        }
        return true;
    }


    @Override
    public Map<String, byte[]> getPolicies(EvaluationCtx eval)
    throws PolicyIndexException {

        Map<String, Set<AttributeBean>> attributeMap;
        try {
            // get evaluation context attributes to query on
            attributeMap = getAttributeMap(eval);
        } catch (URISyntaxException e) {
            log.error("Error getting attribute map " + e.getMessage(), e);
            throw new PolicyIndexException("Error getting attribute map " + e.getMessage(), e);
        }

        Map<String, byte[]> documents = new HashMap<String, byte[]>();

        // generate the xpath query and variables
        String query = getXpath(attributeMap);
        Map<String, String> variables = getXpathVariables(attributeMap);

        try {
            readLock.lock();
            // do the query
            ResourceSet resources = doQuery(query, variables);

            try {
                // get each result
                ResourceIterator ri = resources.getIterator();
                while (ri.hasMoreResources()) {
                    XMLResource res = (XMLResource)ri.nextResource();
                    // get the result's document name (the query result just contains content as selected by the xpath query)
                    String id = res.getDocumentId();
                    log.trace("Query matched document: " + IdToName(id));
                    documents.put(IdToName(id), ((String)m_collection.getResource(id).getContent()).getBytes("UTF-8"));
                }
            } catch (XMLDBException e) {
                log.error("Error retrieving query results " + e.getMessage(), e);
                throw new PolicyIndexException("Error retrieving query results " + e.getMessage(), e);
            } catch (UnsupportedEncodingException e) {
                // Should never happen
                throw new RuntimeException("Unsupported encoding " + e.getMessage(), e);
            }
        } finally {
            readLock.unlock();
        }

        return documents;


    }


    @Override
    public byte[] getPolicy(String name) throws PolicyIndexException {
        try {
            readLock.lock();
            XMLResource resource = (XMLResource) m_collection.getResource(nameToId(name));
            if (resource == null) {
                log.error("Attempting to get non-existant resource " + name);
                throw new PolicyIndexException("Attempting to get non-existant resource " + name);
            }
            return ((String)resource.getContent()).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen
            throw new RuntimeException("Unsupported encoding " + e.getMessage(), e);
        } catch (XMLDBException e) {
            log.error("Error getting policy " + name + " " + e.getMessage(), e);
            throw new PolicyIndexException("Error getting policy " + name + " " + e.getMessage(), e);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean updatePolicy(String name, String newDocument) throws PolicyIndexException {
        String Id = nameToId(name);
        try {
            writeLock.lock();
            // get the resource
            XMLResource res = (XMLResource) m_collection.getResource(Id);
            if (res == null ) {
                log.error("Tried to update non-existing resource " + name);
                throw new PolicyIndexException("Tried to update non-existing resource " + name);
            }

            // set the resource content
            res.setContentAsDOM(createDocument(newDocument));

            // update it
            m_collection.storeResource(res);
        } catch (XMLDBException e) {
            log.error("Error updating resource " + name + " " + e.getMessage(), e);
            throw new PolicyIndexException("Error updating resource " + name + " " + e.getMessage(), e);
        } finally {
            writeLock.unlock();
        }
        return true;

    }

    /**
     * get XML document supplied as w3c dom Node as bytes
     *
     * @param node
     * @return
     * @throws PolicyIndexException
     */
    protected static byte[] nodeToByte(Node node) throws PolicyIndexException {
        OutputFormat format = new OutputFormat();
        format.setEncoding("UTF-8");
        format.setIndenting(true);
        format.setIndent(2);
        format.setOmitXMLDeclaration(false);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer output = new OutputStreamWriter(out);

        XMLSerializer serializer = new XMLSerializer(output, format);
        try {
            serializer.serialize(node);
        } catch (IOException e) {
            throw new PolicyIndexException("Failed to serialise node " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    // utility methods to convert between a document name and a form that eXist accepts (ie URL-encoded)
    protected static String nameToId(String name) {
        try {
            return URLEncoder.encode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should not happen
            throw new RuntimeException("Unsupported encoding", e);
        }
    }
    protected static String IdToName(String Id) {
        try {
            return URLDecoder.decode(Id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should not happen
            throw new RuntimeException("Unsupported encoding", e);
        }

    }

    /**
     * sorts a string array in descending order of length
     * @param s
     * @return
     */
    protected  static String[] sortDescending(String[] s) {
        Arrays.sort(s,
                    new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o1.length() < o2.length())
                    return 1;
                if (o1.length() > o2.length())
                    return -1;
                return 0;
            }
        }
        );
        return s;
    }

    protected ResourceSet doQuery(String query, Map<String, String> variables) throws PolicyIndexException {
        try {
            XPathQueryService queryService = (XPathQueryService) m_collection.getService("XPathQueryService", "1.0");

            // set namespaces
            for (String prefix : PolicyIndexBase.namespaces.keySet()) {
                queryService.setNamespace(prefix, PolicyIndexBase.namespaces.get(prefix));
            }

            // note: eXist extensions support "declareVariable", but the base API does not
            // so we substitute the variables here to avoid dependency on the eXist implementation (and libraries)

            // do in descending order as some variable names could be substrings of others
            String[] varNames = sortDescending((variables.keySet().toArray(new String[0])));

            for (String name : varNames) {
                // nb, treat as strings (variables currently does not specify type)
                query = query.replace("$" + name, "\"" + variables.get(name) + "\"");
            }
            log.trace("XPath query with variables substituted:\n" + query);


            /* eXist impl version, not used, to remove dependency on eXist API
            // set the xpath variables if supplied
            if (variables != null) {
                for (String name : variables.keySet()) {
                    String value = variables.get(name);
                    // query service needs to be eXist extension to xmldb
                    queryService.declareVariable(name, value);
                }
            }
             */
            long start = System.nanoTime();
            ResourceSet results = queryService.query(query);
            log.debug("XPath query time: " + (System.nanoTime() - start) + "ns");

            return results;

        } catch (XMLDBException e) {
            log.error("Error running query " + e.getMessage(), e);
            throw new PolicyIndexException("Error running query " + e.getMessage(), e);
        }
    }

    /**
     * Create a collection given a full path to the collection.  The collection path must include
     * the root collection.  Intermediate collections in the path are created if they do not
     * already exist.
     *
     * @param collectionPath
     * @param rootCollection
     * @return
     * @throws PolicyIndexException
     */
    protected Collection createCollectionPath(String collectionPath, Collection rootCollection) throws PolicyIndexException {
        try {
            if (rootCollection.getParentCollection()  != null) {
                throw new PolicyIndexException("Collection supplied is not a root collection");
            }
            String rootCollectionName = rootCollection.getName();
            if (!collectionPath.startsWith(rootCollectionName)) {
                throw new PolicyIndexException("Collection path " + collectionPath + " does not start from root collection - " + rootCollectionName );
            }

            // strip root collection from path, obtain each individual collection name in the path
            String pathToCreate = collectionPath.substring(rootCollectionName.length());
            String[] collections = pathToCreate.split("/");

            // iterate each and create as necessary
            Collection nextCollection = rootCollection;
            for (String collectionName : collections ) {
                if (nextCollection.getChildCollection(collectionName) != null) {
                    // child exists
                    nextCollection = nextCollection.getChildCollection(collectionName);
                } else {
                    // does not exist, create it
                    CollectionManagementService mgtService = (CollectionManagementService) nextCollection.getService("CollectionManagementService", "1.0");
                    nextCollection = mgtService.createCollection(collectionName);
                    log.debug("Created collection " + collectionName);
                }
            }
            return nextCollection;
        } catch (XMLDBException e) {
            log.error("Error creating collections from path " + e.getMessage(), e);
            throw new PolicyIndexException("Error creating collections from path " + e.getMessage(), e);
        }
    }

    protected void init() throws PolicyIndexException {
        String databaseImplClassName = "org.exist.xmldb.DatabaseImpl";
        Class<?> cl;
        try {
            cl = Class.forName(databaseImplClassName);
        } catch (ClassNotFoundException e) {
            log.error("Class not found - check xmldb driver classes are on classpath " + e.getMessage());
            throw new PolicyIndexException("Class not found - check xmldb driver classes are on classpath " + e.getMessage(), e);
        }


        String home = MelcoePDP.PDP_HOME.getAbsolutePath();

        String filename = home + "/conf/config-exist.xml";
        File f = new File(filename);
        if (!f.exists()) {
            throw new PolicyIndexException("Could not locate eXist config file: "
                                           + f.getAbsolutePath());
        }

        log.info("Loading config file: " + f.getAbsolutePath());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        Document doc;
        try {
            docBuilder = factory.newDocumentBuilder();
            doc = docBuilder.parse(new FileInputStream(f));
        } catch (ParserConfigurationException e) {
            throw new PolicyIndexException("Error parsing eXist config file " + e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new PolicyIndexException("Error reading eXist config file " + e.getMessage(), e);
        } catch (SAXException e) {
            throw new PolicyIndexException("Error parsing eXist config file " + e.getMessage(), e);
        } catch (IOException e) {
            throw new PolicyIndexException("Error reading eXist config file " + e.getMessage(), e);
        }

        NodeList nodes = null;

        // get database information
        nodes = doc.getElementsByTagName("database").item(0)
        .getChildNodes();

        for (int x = 0; x < nodes.getLength(); x++) {
            Node node = nodes.item(x);
            if (node.getNodeName().equals("uri")) {
                m_databaseURI = node.getAttributes().getNamedItem("name").getNodeValue();
            }
            if (node.getNodeName().equals("collection")) {
                m_collectionName = node.getAttributes().getNamedItem("name").getNodeValue();
            }
            if (node.getNodeName().equals("user")) {
                m_user = node.getAttributes().getNamedItem("name").getNodeValue();
            }
            if (node.getNodeName().equals("password")) {
                m_password = node.getAttributes().getNamedItem("name").getNodeValue();
            }
        }

        m_indexCollectionPath = m_rootCollectionPath +"/system/config/db/" + m_collectionName;

        // string URI form of collection URI (nb: needs to include base collection - /db)
        m_collectionPath = m_rootCollectionPath + "/" + m_collectionName;


        Database database;
        try {
            database = (Database)cl.newInstance();
        } catch (Exception e) {
            log.error("Error instantiating xmldb driver", e);
            throw new PolicyIndexException("Error instantiating xmldb driver", e);
        }
        try {
            database.setProperty("create-database", "true");
            DatabaseManager.registerDatabase(database);
        } catch (XMLDBException e) {
            throw new PolicyIndexException("Error registering xmldb driver " + e.getMessage(), e);
        }
    }

    protected void initCollection() throws PolicyIndexException {
        try {
            // try to get FeSL policy collection
            m_collection = DatabaseManager.getCollection(m_databaseURI + m_collectionPath, m_user, m_password);

            // if it doesn't exist, create it and create the index document if this doesn't already exist
            if (m_collection == null) {
                // get root collection
                Collection rootCol = DatabaseManager.getCollection(m_databaseURI + m_rootCollectionPath ,m_user, m_password);
                CollectionManagementService mgtService = (CollectionManagementService) rootCol.getService("CollectionManagementService", "1.0");

                // first create the index for the collection
                Collection indexCollection = DatabaseManager.getCollection(m_databaseURI + m_indexCollectionPath, m_user, m_password);
                if (indexCollection == null) {
                    log.debug("creating index collection");
                    indexCollection = createCollectionPath(m_indexCollectionPath, rootCol);
                }
                XMLResource ixd = (XMLResource) indexCollection.getResource(m_indexDocumentName);
                // get an already-existing index config document; or create if it doesn't exist
                if (ixd == null ) {
                    ixd = (XMLResource) indexCollection.createResource(m_indexDocumentName, XMLResource.RESOURCE_TYPE);
                }

                // Note: although eXist allows full path-based indexes, recommendation is to use
                // qname-based indexes for efficiency, therefore the following defines indexing
                // on resource attribute IDs and values throughout rather than following the
                // (dbxml-style) index definition.
                // Thus the index configuration in the config file is not followed.

                String configDoc =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">" +
                    "<index xmlns:p=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\">" +
                    "<create qname=\"p:AttributeValue\" type=\"xs:string\"/>" +
                    "<create qname=\"@AttributeId\" type=\"xs:string\"/>" +
                    "</index>" +
                    "</collection>";

                Document config = createDocument(configDoc);
                log.debug("Storing index document");
                ixd.setContentAsDOM(config);
                indexCollection.storeResource(ixd);

                // create the collection
                log.debug("Creating policy collection");
                m_collection = mgtService.createCollection(m_collectionName);
            }

        } catch (XMLDBException e) {
            throw new PolicyIndexException("Error getting/creating policy collection " + e.getMessage(), e);
        }
    }

    /**
     * delete the policy collection from the database
     * @throws PolicyIndexException
     */
    protected void deleteCollection() throws PolicyIndexException {

        // get root collection management service
        Collection rootCol;
        try {
            rootCol = DatabaseManager.getCollection(m_databaseURI + m_rootCollectionPath ,m_user, m_password);
            CollectionManagementService mgtService = (CollectionManagementService) rootCol.getService("CollectionManagementService", "1.0");

            // delete the collection
            mgtService.removeCollection(m_collectionName);

            log.debug("Policy collection deleted");
        } catch (XMLDBException e) {
            throw new PolicyIndexException("Error deleting collection " + e.getMessage(), e);
        }
    }

    // create an XML Document from the policy document
    protected static Document createDocument(String document) throws PolicyIndexException {

        // parse policy document and create dom
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(document)));
            return doc;
        } catch (ParserConfigurationException e) {
            throw new PolicyIndexException(e);
        } catch (SAXException e) {
            throw new PolicyIndexException(e);
        } catch (IOException e) {
            throw new PolicyIndexException(e);
        }
    }
}
