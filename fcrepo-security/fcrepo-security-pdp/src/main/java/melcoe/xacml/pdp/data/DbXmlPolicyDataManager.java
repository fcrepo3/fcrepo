/*
 * File: DbXmlPolicyDataManager.java
 *
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package melcoe.xacml.pdp.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import melcoe.xacml.pdp.MelcoePDP;
import melcoe.xacml.util.AttributeBean;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sleepycat.db.Environment;
import com.sleepycat.db.EnvironmentConfig;
import com.sleepycat.dbxml.XmlContainer;
import com.sleepycat.dbxml.XmlContainerConfig;
import com.sleepycat.dbxml.XmlDocument;
import com.sleepycat.dbxml.XmlDocumentConfig;
import com.sleepycat.dbxml.XmlException;
import com.sleepycat.dbxml.XmlIndexSpecification;
import com.sleepycat.dbxml.XmlManager;
import com.sleepycat.dbxml.XmlManagerConfig;
import com.sleepycat.dbxml.XmlQueryContext;
import com.sleepycat.dbxml.XmlQueryExpression;
import com.sleepycat.dbxml.XmlResults;
import com.sleepycat.dbxml.XmlTransaction;
import com.sleepycat.dbxml.XmlUpdateContext;
import com.sleepycat.dbxml.XmlValue;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.cond.EvaluationResult;

/**
 * <p>
 * DbXmlPolicyDataManager provides policy management capabilities for the
 * PolicyFinder. This class implements the PolicyDataManager interface.
 * </p>
 * <p>
 * This class is used by the PolicyFinder to produce a subset of policies for
 * matching. For the PolicyFinder to match every policy would be extremely time
 * consuming and inefficient, so this class uses an index and the XACML
 * EvaluationCtx to generate a much smaller subset of policies that can then be
 * matched with a much greater efficiency.
 * </p>
 * <p>
 * The DbXmlPolicyDataManager is designed to handle large numbers of policies
 * with extremely fast search times. This is mainly achieved through the use of
 * DBXML's indexing engine which is extremely quick.
 * </p>
 * <p>
 * DBXML is an embedded database requiring minimal to no administration. All
 * indexes are created and maintained within this application.
 * </p>
 * <p>
 * To be able to use this class there are some software and configuration
 * requirements that need to be met:
 * <ul>
 * <li>All the requirements for installation of the MelcoePDP are met.</li>
 * <li>DBXML 2.5.13+ is installed.</li>
 * <li>The config-dbxml.xml configuration file located in $FEDORA_HOME/pdp/conf</li>
 * </ul>
 * </p>
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class DbXmlPolicyDataManager
        implements PolicyDataManager {

    private static final Logger log =
            Logger.getLogger(DbXmlPolicyDataManager.class.getName());

    private static final String XACML20_POLICY_NS =
            "urn:oasis:names:tc:xacml:2.0:policy:schema:os";

    private static final String METADATA_POLICY_NS = "metadata";

    private static final String XACML_RESOURCE_ID =
            "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    private String DB_HOME = null;

    private String CONTAINER = null;

    private Validator validator = null;

    private Map<String, Map<String, String>> indexMap = null;

    private XmlManager manager = null;

    private XmlUpdateContext updateContext = null;

    private XmlContainer container = null;

    private Environment env = null;

    private Map<String, XmlQueryExpression> queries = null;

    private XmlQueryExpression[] searchQueries = null;

    private long lastUpdate;

    /**
     * The default constructor for DbXmlPolicyDataManager. This constructor
     * reads the configuration file, 'config-dbxml.xml' and initialises/creates
     * the database as required based on that configuration. Any required
     * indexes are automatically created.
     * 
     * @throws PolicyDataManagerException
     */
    public DbXmlPolicyDataManager()
            throws PolicyDataManagerException {
        initConfig();

        File envHome = new File(DB_HOME);
        XmlTransaction txn = null;
        try {
            try {
                EnvironmentConfig envCfg = new EnvironmentConfig();
                if (log.isDebugEnabled()) {
                    log.debug("Lockers: " + envCfg.getMaxLockers());
                    log.debug("LockObjects: " + envCfg.getMaxLockObjects());
                    log.debug("Locks: " + envCfg.getMaxLocks());
                }

                // envCfg.setRunRecovery(true);
                envCfg.setAllowCreate(true);
                envCfg.setInitializeCache(true);
                envCfg.setInitializeLocking(true);
                envCfg.setInitializeLogging(true);
                envCfg.setTransactional(true);
                envCfg.setMaxLockers(10000);
                envCfg.setMaxLockObjects(10000);
                envCfg.setMaxLocks(10000);

                XmlManagerConfig managerCfg = new XmlManagerConfig();
                managerCfg.setAdoptEnvironment(true);
                managerCfg.setAllowExternalAccess(true);

                XmlContainerConfig containerCfg = new XmlContainerConfig();
                containerCfg.setAllowCreate(true);
                containerCfg.setTransactional(true);

                env = new Environment(envHome, envCfg);
                manager = new XmlManager(env, managerCfg);
                // XmlManager.setLogCategory(XmlManager.CATEGORY_NONE, true);
                // XmlManager.setLogLevel(XmlManager.LEVEL_WARNING, true);

                updateContext = manager.createUpdateContext();

                txn = manager.createTransaction();
                if (manager.existsContainer(CONTAINER) == 0) {
                    container =
                            manager.createContainer(txn,
                                                    CONTAINER,
                                                    containerCfg);

                    // if we just created a container we also need to add an
                    // index
                    XmlIndexSpecification is =
                            container.getIndexSpecification(txn);

                    int idxType = 0;
                    int syntaxType = 0;

                    // Add the attribute value index
                    idxType |= XmlIndexSpecification.PATH_EDGE;
                    idxType |= XmlIndexSpecification.NODE_ELEMENT;
                    idxType |= XmlIndexSpecification.KEY_EQUALITY;
                    syntaxType = XmlValue.STRING;
                    is.addIndex(XACML20_POLICY_NS,
                                "AttributeValue",
                                idxType,
                                syntaxType);

                    // Add the metadata default index
                    idxType = 0;
                    idxType |= XmlIndexSpecification.PATH_NODE;
                    idxType |= XmlIndexSpecification.NODE_METADATA;
                    idxType |= XmlIndexSpecification.KEY_PRESENCE;
                    syntaxType = XmlValue.NONE;
                    is.addDefaultIndex(idxType, syntaxType);

                    container.setIndexSpecification(txn, is, updateContext);
                } else {
                    container =
                            manager.openContainer(txn, CONTAINER, containerCfg);
                }

                txn.commit();
                log.info("Opened Container: " + CONTAINER);

                queries = new HashMap<String, XmlQueryExpression>();

                searchQueries = new XmlQueryExpression[10];

                setLastUpdate(System.currentTimeMillis());
            } catch (Exception e) {
                log.fatal("Could not start database subsystem.", e);
                txn.abort();
                close();
                throw new PolicyDataManagerException(e.getMessage(), e);
            }
        } catch (XmlException xe) {
            throw new PolicyDataManagerException("Error aborting transaction: "
                    + xe.getMessage(), xe);
        }
    }

    /*
     * (non-Javadoc)
     * @see melcoe.xacml.pdp.data.PolicyDataManager#addPolicy(java.io.File)
     */
    public String addPolicy(File f) throws PolicyDataManagerException {
        return addPolicy(f, null);
    }

    /*
     * (non-Javadoc)
     * @see melcoe.xacml.pdp.data.PolicyDataManager#addPolicy(java.io.File,
     * java.lang.String)
     */
    public String addPolicy(File f, String name)
            throws PolicyDataManagerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes = new byte[1024];

        try {
            FileInputStream fis = new FileInputStream(f);
            int count = fis.read(bytes);
            while (count > -1) {
                out.write(bytes, 0, count);
                count = fis.read(bytes);
            }
        } catch (IOException e) {
            throw new PolicyDataManagerException("Error reading file: "
                    + f.getName(), e);
        }

        return addPolicy(out.toString(), name);
    }

    /*
     * (non-Javadoc)
     * @see melcoe.xacml.pdp.data.PolicyDataManager#addPolicy(java.lang.String)
     */
    public String addPolicy(String document) throws PolicyDataManagerException {
        return addPolicy(document, null);
    }

    /*
     * (non-Javadoc)
     * @see melcoe.xacml.pdp.data.PolicyDataManager#addPolicy(java.lang.String,
     * java.lang.String)
     */
    public String addPolicy(String document, String name)
            throws PolicyDataManagerException {
        try {
            if (validator != null) {
                if (log.isDebugEnabled()) {
                    log.debug("validating document: " + name);
                }
                validator
                        .validate(new StreamSource(new ByteArrayInputStream(document
                                .getBytes())));
            }
        } catch (Exception e) {
            throw new PolicyDataManagerException("Could not validate policy: "
                    + name, e);
        }

        String docName = null;
        XmlTransaction txn = null;
        try {
            try {
                XmlDocument doc = makeDocument(name, document);
                docName = doc.getName();
                txn = manager.createTransaction();
                container.putDocument(txn, doc, updateContext);
                txn.commit();
                setLastUpdate(System.currentTimeMillis());
            } catch (XmlException xe) {
                if (xe.getErrorCode() == XmlException.UNIQUE_ERROR) {
                    throw new PolicyDataManagerException("Document already exists: "
                            + docName);
                }
                txn.abort();
                throw new PolicyDataManagerException("Error adding policy: "
                        + xe.getMessage(), xe);
            }
        } catch (XmlException xe) {
            throw new PolicyDataManagerException("Error aborting transaction: "
                    + xe.getMessage(), xe);
        }

        return docName;
    }

    /*
     * (non-Javadoc)
     * @see
     * melcoe.xacml.pdp.data.PolicyDataManager#deletePolicy(java.lang.String)
     */
    public boolean deletePolicy(String name) throws PolicyDataManagerException {
        XmlTransaction txn = null;
        try {
            try {
                txn = manager.createTransaction();
                container.deleteDocument(txn, name, updateContext);
                txn.commit();
                setLastUpdate(System.currentTimeMillis());
            } catch (XmlException xe) {
                txn.abort();
                throw new PolicyDataManagerException("Error deleting document: "
                                                             + name,
                                                     xe);
            }
        } catch (XmlException xe) {
            throw new PolicyDataManagerException("Error aborting transaction: "
                    + xe.getMessage(), xe);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * @see
     * melcoe.xacml.pdp.data.PolicyDataManager#updatePolicy(java.lang.String,
     * java.lang.String)
     */
    public boolean updatePolicy(String name, String newDocument)
            throws PolicyDataManagerException {
        try {
            if (validator != null) {
                if (log.isDebugEnabled()) {
                    log.debug("validating document: " + name);
                }
                validator
                        .validate(new StreamSource(new ByteArrayInputStream(newDocument
                                .getBytes())));
            }
        } catch (Exception e) {
            throw new PolicyDataManagerException("Could not validate policy: "
                    + name, e);
        }

        XmlTransaction txn = null;

        try {
            try {
                txn = manager.createTransaction();
                XmlDocument doc = makeDocument(name, newDocument);
                container.updateDocument(txn, doc, updateContext);
                txn.commit();
                setLastUpdate(System.currentTimeMillis());
            } catch (XmlException xe) {
                txn.abort();
                throw new PolicyDataManagerException("Error updating document: "
                                                             + name,
                                                     xe);
            }
        } catch (XmlException xe) {
            throw new PolicyDataManagerException("Error aborting transaction: "
                    + xe.getMessage(), xe);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * @see melcoe.xacml.pdp.data.PolicyDataManager#getPolicy(java.lang.String)
     */
    public byte[] getPolicy(String name) throws PolicyDataManagerException {
        try {
            XmlDocument doc = container.getDocument(name);
            return doc.getContent();
        } catch (XmlException xe) {
            throw new PolicyDataManagerException("Error retrieving document: "
                    + name, xe);
        }
    }

    /*
     * (non-Javadoc)
     * @seemelcoe.xacml.pdp.data.PolicyDataManager#getPolicies(com.sun.xacml.
     * EvaluationCtx)
     */
    public Map<String, byte[]> getPolicies(EvaluationCtx eval)
            throws PolicyDataManagerException {
        long a = 0;
        long b = 0;
        long total = 0;

        Map<String, byte[]> documents = new HashMap<String, byte[]>();

        try {
            // Get the query (query gets prepared if necesary)
            a = System.nanoTime();

            Map<String, Set<AttributeBean>> attributeMap =
                    getAttributeMap(eval);

            XmlQueryContext context = manager.createQueryContext();
            context.setDefaultCollection(CONTAINER);
            context.setNamespace("p", XACML20_POLICY_NS);
            context.setNamespace("m", METADATA_POLICY_NS);

            // Set all the bind variables in the query context
            String[] types =
                    new String[] {"Subject", "Resource", "Action",
                            "Environment"};
            int resourceComponentCount = 0;

            for (String t : types) {
                int count = 0;
                for (AttributeBean bean : attributeMap.get(t.toLowerCase()
                        + "Attributes")) {
                    if (bean.getId().equals(XACML_RESOURCE_ID)) {
                        context.setVariableValue("XacmlResourceId",
                                                 new XmlValue(bean.getId()));

                        int c = 0;
                        for (String value : bean.getValues()) {
                            XmlValue component = new XmlValue(value);
                            context
                                    .setVariableValue("XacmlResourceIdValue"
                                            + c, component);

                            if (log.isDebugEnabled()) {
                                log
                                        .debug("XacmlResourceIdValue"
                                                + resourceComponentCount + ": "
                                                + value);
                            }

                            resourceComponentCount++;
                            c++;
                        }
                    } else {
                        context.setVariableValue(t + "Id" + count,
                                                 new XmlValue(bean.getId()));

                        if (log.isDebugEnabled()) {
                            log.debug(t + "Id" + count + " = '" + bean.getId()
                                    + "'");
                        }

                        int valueCount = 0;
                        for (String value : bean.getValues()) {
                            context.setVariableValue(t + "Id" + count
                                                             + "-Value"
                                                             + valueCount,
                                                     new XmlValue(value));
                            if (log.isDebugEnabled()) {
                                log.debug(t + "Id" + count + "-Value"
                                        + valueCount + " = '" + value + "'");
                            }

                            valueCount++;
                        }

                        count++;
                    }
                }
            }

            XmlQueryExpression qe =
                    getQuery(attributeMap, context, resourceComponentCount);

            b = System.nanoTime();
            total += b - a;
            if (log.isDebugEnabled()) {
                log.debug("Query prep. time: " + (b - a) + "ns");
            }

            // execute the query
            a = System.nanoTime();
            XmlResults results = qe.execute(context);
            b = System.nanoTime();
            total += b - a;
            if (log.isDebugEnabled()) {
                log.debug("Query exec. time: " + (b - a) + "ns");
            }

            // process results
            while (results.hasNext()) {
                XmlValue value = results.next();
                if (log.isDebugEnabled()) {
                    log.debug("Retrieved Document: "
                            + value.asDocument().getName());
                }
                documents.put(value.asDocument().getName(), value.asDocument()
                        .getContent());
            }
            results.delete();

            if (log.isDebugEnabled()) {
                log.debug("Total exec. time: " + total + "ns");
            }
        } catch (XmlException xe) {
            throw new PolicyDataManagerException("Error getting policies from PolicyDataManager.",
                                                 xe);
        } catch (URISyntaxException use) {
            throw new PolicyDataManagerException("Error building query.", use);
        }

        return documents;
    }

    /**
     * Check if the policy identified by policyName exists.
     * 
     * @param policyName
     * @return true iff the policy store contains a policy identified as
     *         policyName
     * @throws PolicyDataManagerException
     */
    public boolean contains(String policyName)
            throws PolicyDataManagerException {
        try {
            container.getDocument(policyName, new XmlDocumentConfig()
                    .setLazyDocs(true));
        } catch (XmlException e) {
            if (e.getErrorCode() == XmlException.DOCUMENT_NOT_FOUND) {
                return false;
            } else {
                throw new PolicyDataManagerException(e.getMessage(), e);
            }
        }
        return true;
    }

    /**
     * Check if the policy identified by policyName exists.
     * 
     * @param policy
     * @return true iff the policy store contains a policy with the same
     *         PolicyId
     * @throws PolicyDataManagerException
     */
    public boolean contains(File policy) throws PolicyDataManagerException {
        InputStream is;
        String policyName;
        try {
            is = new FileInputStream(policy);
            Map<String, String> metadata = getDocumentMetadata(is);
            is.close();
            policyName = metadata.get("PolicyId");
        } catch (IOException e) {
            throw new PolicyDataManagerException(e.getMessage(), e);
        }
        return contains(policyName);
    }

    /*
     * (non-Javadoc)
     * @see melcoe.xacml.pdp.data.PolicyDataManager#listPolicies()
     */
    public List<String> listPolicies() throws PolicyDataManagerException {
        List<String> documents = new ArrayList<String>();
        XmlTransaction txn = null;

        try {
            try {
                txn = manager.createTransaction();
                XmlDocumentConfig docConf = new XmlDocumentConfig();
                XmlResults results = container.getAllDocuments(txn, docConf);
                while (results.hasNext()) {
                    XmlValue value = results.next();
                    documents.add(value.asDocument().getName());
                }
                results.delete();
                txn.commit();
            } catch (XmlException xe) {
                txn.abort();
                throw new PolicyDataManagerException(xe);
            }
        } catch (XmlException xe) {
            throw new PolicyDataManagerException("Error aborting transaction: "
                    + xe.getMessage(), xe);
        }

        return documents;
    }

    /**
     * Closes the dbxml container and manager.
     */
    private void close() {
        try {
            if (container != null) {
                container.close();
                container = null;
                log.info("Closed container");
            }

            if (manager != null) {
                manager.close();
                manager = null;
                log.info("Closed manager");
            }
        } catch (Exception de) {
            log.warn(de.getMessage());
        }
    }

    /**
     * Obtains the metadata for the given document.
     * 
     * @param docIS
     *        the document as an InputStream
     * @return the document metadata as a Map
     */
    private Map<String, String> getDocumentMetadata(InputStream docIS) {
        Map<String, String> metadata = new HashMap<String, String>();

        try {
            // Create instance of DocumentBuilderFactory
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            // Get the DocumentBuilder
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            // Create blank DOM Document and parse contents of input stream
            Document doc = docBuilder.parse(docIS);

            NodeList nodes = null;

            metadata.put("PolicyId", doc.getDocumentElement()
                    .getAttribute("PolicyId"));

            nodes = doc.getElementsByTagName("Subjects");
            if (nodes.getLength() == 0) {
                metadata.put("anySubject", "T");
            }

            nodes = doc.getElementsByTagName("Resources");
            if (nodes.getLength() == 0) {
                metadata.put("anyResource", "T");
            }

            nodes = doc.getElementsByTagName("Actions");
            if (nodes.getLength() == 0) {
                metadata.put("anyAction", "T");
            }

            nodes = doc.getElementsByTagName("Environments");
            if (nodes.getLength() == 0) {
                metadata.put("anyEnvironment", "T");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return metadata;
    }

    /**
     * Creates an instance of an XmlDocument for storage in the database.
     * 
     * @param name
     *        the name of the document (policy)
     * @param document
     *        the document data as a String
     * @return the XmlDocument instance
     * @throws XmlException
     * @throws PolicyDataManagerException
     */
    private XmlDocument makeDocument(String name, String document)
            throws XmlException, PolicyDataManagerException {
        Map<String, String> metadata =
                getDocumentMetadata(new ByteArrayInputStream(document
                        .getBytes()));
        XmlDocument doc = manager.createDocument();
        String docName = name;

        if (docName == null || "".equals(docName)) {
            docName = metadata.get("PolicyId");
        }

        if (docName == null || "".equals(docName)) {
            throw new PolicyDataManagerException("Could not extract PolicyID from document.");
        }

        doc.setMetaData("metadata", "PolicyId", new XmlValue(XmlValue.STRING,
                                                             docName));
        doc.setContent(document);
        doc.setName(docName);

        String item = null;
        item = metadata.get("anySubject");
        if (item != null) {
            doc.setMetaData("metadata",
                            "anySubject",
                            new XmlValue(XmlValue.STRING, item));
        }

        item = metadata.get("anyResource");
        if (item != null) {
            doc.setMetaData("metadata",
                            "anyResource",
                            new XmlValue(XmlValue.STRING, item));
        }

        item = metadata.get("anyAction");
        if (item != null) {
            doc.setMetaData("metadata",
                            "anyAction",
                            new XmlValue(XmlValue.STRING, item));
        }

        item = metadata.get("anyEnvironment");
        if (item != null) {
            doc.setMetaData("metadata",
                            "anyEnvironment",
                            new XmlValue(XmlValue.STRING, item));
        }

        return doc;
    }

    /**
     * Either returns a query that has previously been generated, or generates a
     * new one if it has not.
     * 
     * @param attributeMap
     *        the Map of attributes, type and values upon which this query is
     *        based
     * @param context
     *        the context for the query
     * @return an XmlQueryExpression that can be executed
     * @throws XmlException
     */
    private XmlQueryExpression getQuery(Map<String, Set<AttributeBean>> attributeMap,
                                        XmlQueryContext context,
                                        int r) throws XmlException {
        // The dimensions for this query.
        StringBuilder sb = new StringBuilder();
        for (Set<AttributeBean> attributeBeans : attributeMap.values()) {
            sb.append(attributeBeans.size() + ":");
            for (AttributeBean bean : attributeBeans) {
                sb.append(bean.getValues().size() + "-");
            }
        }

        // If a query of these dimensions already exists, then just return it.
        String hash = sb.toString() + r;
        XmlQueryExpression result = queries.get(hash);
        if (result != null) {
            return result;
        }

        // We do not have a query of those dimensions. We must make one.
        String query = createQuery(attributeMap, r);

        if (log.isDebugEnabled()) {
            log.debug("Query [" + hash + "]:\n" + query);
        }

        // Once we have created a query, we can parse it and store the
        // execution plan. This is an expensive operation that we do
        // not want to have to do more than once for each dimension
        result = manager.prepare(query, context);
        queries.put(hash, result);

        return result;
    }

    /**
     * Given a set of attributes this method generates a DBXML XPath query based
     * on those attributes to extract a subset of policies from the database.
     * 
     * @param attributeMap
     *        the Map of Attributes from which to generate the query
     * @param r
     *        the number of components in the resource id
     * @return the query as a String
     */
    private String createQuery(Map<String, Set<AttributeBean>> attributeMap,
                               int r) {
        // The query contains these 4 sections.
        String[] types =
                new String[] {"Subject", "Resource", "Action", "Environment"};

        int sections = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("collection('" + CONTAINER + "')/p:Policy/p:Target[");
        for (String t : types) {
            if (attributeMap.get(t.toLowerCase() + "Attributes").size() == 0) {
                continue;
            }

            if (sections > 0) {
                sb.append(" and ");
            }

            sections++;

            sb.append("((exists(dbxml:metadata('m:any" + t + "')))");
            // sb.append("((not('p:" + t + "s'))");

            int count = 0;
            for (AttributeBean bean : attributeMap.get(t.toLowerCase()
                    + "Attributes")) {
                sb.append(" or ");
                sb.append("(");

                if (bean.getId().equals(XACML_RESOURCE_ID) && r > 0) {
                    sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                    sb.append("p:" + t + "AttributeDesignator/@AttributeId = ");
                    sb.append("$XacmlResourceId");
                    sb.append(" and ");

                    /*
                     * sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                     * sb.append("p:" + t + "AttributeDesignator/@DataType = ");
                     * sb.append("$XacmlResourceType"); sb.append(" and ");
                     */

                    sb.append("(");
                    for (int i = 0; i < bean.getValues().size(); i++) {
                        if (i > 0) {
                            sb.append(" or ");
                        }

                        sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                        sb.append("p:AttributeValue = ");
                        sb.append("$XacmlResourceIdValue" + i);
                    }
                    sb.append(")");
                } else {
                    sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                    sb.append("p:" + t + "AttributeDesignator/@AttributeId = ");
                    sb.append("$" + t + "Id" + count);
                    sb.append(" and ");
                    sb.append("(");
                    /*
                     * sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                     * sb.append("p:" + t + "AttributeDesignator/@DataType = ");
                     * sb.append("$" + t + "Type" + count); sb.append(" and ");
                     */

                    for (int valueCount = 0; valueCount < bean.getValues()
                            .size(); valueCount++) {
                        if (valueCount > 0) {
                            sb.append(" or ");
                        }

                        sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                        sb.append("p:AttributeValue = ");
                        sb.append("$" + t + "Id" + count + "-Value"
                                + valueCount);
                    }
                    sb.append(")");

                    count++;
                }
                sb.append(")");
            }
            sb.append(")");
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * This method extracts the attributes listed in the indexMap from the given
     * evaluation context.
     * 
     * @param eval
     *        the Evaluation Context from which to extract Attributes
     * @return a Map of Attributes for each category (Subject, Resource, Action,
     *         Environment)
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    private Map<String, Set<AttributeBean>> getAttributeMap(EvaluationCtx eval)
            throws URISyntaxException {
        URI defaultCategoryURI =
                new URI(AttributeDesignator.SUBJECT_CATEGORY_DEFAULT);

        Map<String, String> im = null;
        Map<String, Set<AttributeBean>> attributeMap =
                new HashMap<String, Set<AttributeBean>>();
        Map<String, AttributeBean> attributeBeans = null;

        im = indexMap.get("subjectAttributes");
        attributeBeans = new HashMap<String, AttributeBean>();
        for (String attributeId : im.keySet()) {
            EvaluationResult result =
                    eval.getSubjectAttribute(new URI(im.get(attributeId)),
                                             new URI(attributeId),
                                             defaultCategoryURI);
            if (result.getStatus() == null && !result.indeterminate()) {
                AttributeValue attr = result.getAttributeValue();
                if (attr.returnsBag()) {
                    Iterator<AttributeValue> i =
                            ((BagAttribute) attr).iterator();
                    if (i.hasNext()) {
                        while (i.hasNext()) {
                            AttributeValue value = i.next();
                            String attributeType = im.get(attributeId);

                            AttributeBean ab = attributeBeans.get(attributeId);
                            if (ab == null) {
                                ab = new AttributeBean();
                                ab.setId(attributeId);
                                ab.setType(attributeType);
                                attributeBeans.put(attributeId, ab);
                            }

                            ab.addValue(value.encode());
                        }
                    }
                }
            }
        }
        attributeMap.put("subjectAttributes", new HashSet(attributeBeans
                .values()));

        im = indexMap.get("resourceAttributes");
        attributeBeans = new HashMap<String, AttributeBean>();
        for (String attributeId : im.keySet()) {
            EvaluationResult result =
                    eval.getResourceAttribute(new URI(im.get(attributeId)),
                                              new URI(attributeId),
                                              null);
            if (result.getStatus() == null && !result.indeterminate()) {
                AttributeValue attr = result.getAttributeValue();
                if (attr.returnsBag()) {
                    Iterator<AttributeValue> i =
                            ((BagAttribute) attr).iterator();
                    if (i.hasNext()) {
                        while (i.hasNext()) {
                            AttributeValue value = i.next();
                            String attributeType = im.get(attributeId);

                            AttributeBean ab = attributeBeans.get(attributeId);
                            if (ab == null) {
                                ab = new AttributeBean();
                                ab.setId(attributeId);
                                ab.setType(attributeType);
                                attributeBeans.put(attributeId, ab);
                            }

                            if (attributeId.equals(XACML_RESOURCE_ID)
                                    && value.encode().startsWith("/")) {
                                String[] components =
                                        makeComponents(value.encode());
                                if (components != null && components.length > 0) {
                                    for (String c : components) {
                                        ab.addValue(c);
                                    }
                                } else {
                                    ab.addValue(value.encode());
                                }
                            } else {
                                ab.addValue(value.encode());
                            }
                        }
                    }
                }
            }
        }
        attributeMap.put("resourceAttributes", new HashSet(attributeBeans
                .values()));

        im = indexMap.get("actionAttributes");
        attributeBeans = new HashMap<String, AttributeBean>();
        for (String attributeId : im.keySet()) {
            EvaluationResult result =
                    eval.getActionAttribute(new URI(im.get(attributeId)),
                                            new URI(attributeId),
                                            null);
            if (result.getStatus() == null && !result.indeterminate()) {
                AttributeValue attr = result.getAttributeValue();
                if (attr.returnsBag()) {
                    Iterator<AttributeValue> i =
                            ((BagAttribute) attr).iterator();
                    if (i.hasNext()) {
                        while (i.hasNext()) {
                            AttributeValue value = i.next();
                            String attributeType = im.get(attributeId);

                            AttributeBean ab = attributeBeans.get(attributeId);
                            if (ab == null) {
                                ab = new AttributeBean();
                                ab.setId(attributeId);
                                ab.setType(attributeType);
                                attributeBeans.put(attributeId, ab);
                            }

                            ab.addValue(value.encode());
                        }
                    }
                }
            }
        }
        attributeMap.put("actionAttributes", new HashSet(attributeBeans
                .values()));

        im = indexMap.get("environmentAttributes");
        attributeBeans = new HashMap<String, AttributeBean>();
        for (String attributeId : im.keySet()) {
            URI imAttrId = new URI(im.get(attributeId));
            URI attrId = new URI(attributeId);
            EvaluationResult result =
                    eval.getEnvironmentAttribute(imAttrId, attrId, null);
            if (result.getStatus() == null && !result.indeterminate()) {
                AttributeValue attr = result.getAttributeValue();
                if (attr.returnsBag()) {
                    Iterator<AttributeValue> i =
                            ((BagAttribute) attr).iterator();
                    if (i.hasNext()) {
                        while (i.hasNext()) {
                            AttributeValue value = i.next();
                            String attributeType = im.get(attributeId);

                            AttributeBean ab = attributeBeans.get(attributeId);
                            if (ab == null) {
                                ab = new AttributeBean();
                                ab.setId(attributeId);
                                ab.setType(attributeType);
                                attributeBeans.put(attributeId, ab);
                            }

                            ab.addValue(value.encode());
                        }
                    }
                }
            }
        }
        attributeMap.put("environmentAttributes", new HashSet(attributeBeans
                .values()));

        return attributeMap;
    }

    /**
     * Reads a configuration file and initialises the instance based on that
     * information.
     * 
     * @throws PolicyDataManagerException
     */
    private void initConfig() throws PolicyDataManagerException {
        if (log.isDebugEnabled()) {
            Runtime runtime = Runtime.getRuntime();
            log.debug("Total memory: " + runtime.totalMemory() / 1024);
            log.debug("Free memory: " + runtime.freeMemory() / 1024);
            log.debug("Max memory: " + runtime.maxMemory() / 1024);
        }

        try {
            String home = MelcoePDP.PDP_HOME.getAbsolutePath();

            String filename = home + "/conf/config-dbxml.xml";
            File f = new File(filename);
            if (!f.exists()) {
                throw new PolicyDataManagerException("Could not locate config file: "
                        + f.getAbsolutePath());
            }

            log.info("Loading config file: " + f.getAbsolutePath());

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new FileInputStream(f));

            NodeList nodes = null;

            // get database information
            nodes =
                    doc.getElementsByTagName("database").item(0)
                            .getChildNodes();
            for (int x = 0; x < nodes.getLength(); x++) {
                Node node = nodes.item(x);
                if (node.getNodeName().equals("directory")) {
                    DB_HOME =
                            MelcoePDP.PDP_HOME.getAbsolutePath()
                                    + node.getAttributes().getNamedItem("name")
                                            .getNodeValue();
                    File db_home = new File(DB_HOME);
                    if (!db_home.exists()) {
                        try {
                            db_home.mkdirs();
                        } catch (Exception e) {
                            throw new PolicyDataManagerException("Could not create DB directory: "
                                    + db_home.getAbsolutePath());
                        }
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("[config] " + node.getNodeName() + ": "
                                + db_home.getAbsolutePath());
                    }
                }
                if (node.getNodeName().equals("container")) {
                    CONTAINER =
                            node.getAttributes().getNamedItem("name")
                                    .getNodeValue();
                    File conFile = new File(DB_HOME + "/" + CONTAINER);
                    if (log.isDebugEnabled()) {
                        log.debug("[config] " + node.getNodeName() + ": "
                                + conFile.getAbsolutePath());
                    }
                }
            }

            // get index map information
            String[] indexMapElements =
                    {"subjectAttributes", "resourceAttributes",
                            "actionAttributes", "environmentAttributes"};

            indexMap = new HashMap<String, Map<String, String>>();
            for (String s : indexMapElements) {
                indexMap.put(s, new HashMap<String, String>());
            }

            nodes =
                    doc.getElementsByTagName("indexMap").item(0)
                            .getChildNodes();
            for (int x = 0; x < nodes.getLength(); x++) {
                Node node = nodes.item(x);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    if (log.isDebugEnabled()) {
                        log.debug("Node name: " + node.getNodeName());
                    }

                    NodeList attrs = node.getChildNodes();
                    for (int y = 0; y < attrs.getLength(); y++) {
                        Node attr = attrs.item(y);
                        if (attr.getNodeType() == Node.ELEMENT_NODE) {
                            String name =
                                    attr.getAttributes().getNamedItem("name")
                                            .getNodeValue();
                            String type =
                                    attr.getAttributes().getNamedItem("type")
                                            .getNodeValue();
                            indexMap.get(node.getNodeName()).put(name, type);
                        }
                    }
                }
            }

            // get validation information
            Node schemaConfig =
                    doc.getElementsByTagName("schemaConfig").item(0);
            nodes = schemaConfig.getChildNodes();
            if ("true".equals(schemaConfig.getAttributes()
                    .getNamedItem("validation").getNodeValue())) {
                log.info("Initialising validation");

                for (int x = 0; x < nodes.getLength(); x++) {
                    Node schemaNode = nodes.item(x);
                    if (schemaNode.getNodeType() == Node.ELEMENT_NODE) {
                        if (XACML20_POLICY_NS.equals(schemaNode.getAttributes()
                                .getNamedItem("namespace").getNodeValue())) {
                            if (log.isDebugEnabled()) {
                                log
                                        .debug("found valid schema. Creating validator");
                            }
                            String loc =
                                    schemaNode.getAttributes()
                                            .getNamedItem("location")
                                            .getNodeValue();
                            SchemaFactory schemaFactory =
                                    SchemaFactory
                                            .newInstance("http://www.w3.org/2001/XMLSchema");
                            Schema schema =
                                    schemaFactory.newSchema(new URL(loc));
                            validator = schema.newValidator();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.fatal("Could not initialise DBXML: " + e.getMessage(), e);
            throw new PolicyDataManagerException("Could not initialise DBXML: "
                    + e.getMessage(), e);
        }
    }

    /*
     * (non-Javadoc)
     * @see melcoe.xacml.pdp.data.PolicyDataManager#getLastUpdate()
     */
    public long getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @param lastUpdate
     *        the lastUpdate to set
     */
    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /*
     * (non-Javadoc)
     * @see
     * melcoe.xacml.pdp.data.PolicyDataManager#findPolicies(melcoe.xacml.util
     * .AttributeBean[])
     */
    public Map<String, byte[]> findPolicies(AttributeBean[] attributes)
            throws PolicyDataManagerException {
        if (attributes == null || attributes.length == 0) {
            throw new PolicyDataManagerException("attribute array cannot be null or zero length");
        }

        long a, b, total = 0;
        Map<String, byte[]> documents = new TreeMap<String, byte[]>();
        try {
            a = System.nanoTime();

            XmlQueryContext context = manager.createQueryContext();
            context.setDefaultCollection(CONTAINER);
            context.setNamespace("p", XACML20_POLICY_NS);
            context.setNamespace("m", METADATA_POLICY_NS);

            for (int x = 0; attributes.length < 0; x++) {
                context.setVariableValue("id" + x, new XmlValue(attributes[x]
                        .getId()));
                // context.setVariableValue("type" + x, new
                // XmlValue(attributes[x].getType()));
                // context.setVariableValue("value" + x, new
                // XmlValue(attributes[x].getValue()));
            }

            if (searchQueries[attributes.length] == null) {
                StringBuilder sb = new StringBuilder();

                sb.append("for $doc in ");
                sb.append("collection('" + CONTAINER + "') ");
                sb.append("let $value := $doc//p:AttributeValue ");
                sb.append("let $id := $value/..//@AttributeId ");
                sb.append("where 1 = 1 ");
                for (int x = 0; x < attributes.length; x++) {
                    sb.append("and $value = $value" + x + " ");
                    sb.append("and $id = $id" + x + " ");
                }
                sb.append("return $doc");

                searchQueries[attributes.length] =
                        manager.prepare(sb.toString(), context);
            }

            b = System.nanoTime();
            total += b - a;
            if (log.isDebugEnabled()) {
                log.debug("Query prep. time: " + (b - a) + "ns");
            }

            a = System.nanoTime();
            XmlResults results =
                    searchQueries[attributes.length].execute(context);
            b = System.nanoTime();
            total += b - a;
            if (log.isDebugEnabled()) {
                log.debug("Search exec. time: " + (b - a) + "ns");
            }

            a = System.nanoTime();

            while (results.hasNext()) {
                XmlValue value = results.next();
                if (log.isDebugEnabled()) {
                    log.debug("Found search result: "
                            + value.asDocument().getName());
                }
                documents.put(value.asDocument().getName(), value.asDocument()
                        .getContent());
            }
            results.delete();

            b = System.nanoTime();
            total += b - a;
            if (log.isDebugEnabled()) {
                log.debug("Result proc. time: " + (b - a) + "ns");
            }

            log.info("Total time: " + total + "ns");
        } catch (XmlException xe) {
            log.error("Exception during findPolicies: " + xe.getMessage(), xe);
            throw new PolicyDataManagerException("Exception during findPolicies: "
                                                         + xe.getMessage(),
                                                 xe);
        }

        return documents;
    }

    private String[] makeComponents(String resourceId) {
        if (resourceId == null || resourceId.equals("")
                || !resourceId.startsWith("/")) {
            return null;
        }

        List<String> components = new ArrayList<String>();

        String[] parts = resourceId.split("\\/");

        for (int x = 1; x < parts.length; x++) {
            StringBuilder sb = new StringBuilder();
            for (int y = 0; y < x; y++) {
                sb.append("/");
                sb.append(parts[y + 1]);
            }

            components.add(sb.toString());

            if (x != parts.length - 1) {
                components.add(sb.toString() + "/.*");
            } else {
                components.add(sb.toString() + "$");
            }
        }

        return components.toArray(new String[components.size()]);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            close(); // close open files
        } finally {
            super.finalize();
        }
    }
}
