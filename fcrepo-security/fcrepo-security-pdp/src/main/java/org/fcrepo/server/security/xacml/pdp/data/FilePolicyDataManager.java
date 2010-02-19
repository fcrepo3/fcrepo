/*
 * File: FilePolicyDataManager.java
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

package org.fcrepo.server.security.xacml.pdp.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.sun.xacml.EvaluationCtx;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.fcrepo.server.security.xacml.pdp.MelcoePDP;
import org.fcrepo.server.security.xacml.util.AttributeBean;
import org.fcrepo.server.security.xacml.util.DataFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class FilePolicyDataManager
        implements PolicyDataManager {

    private static final Logger logger =
            LoggerFactory.getLogger(FilePolicyDataManager.class);

    private static final String XACML20_POLICY_NS =
            "urn:oasis:names:tc:xacml:2.0:policy:schema:os";

    private String DB_HOME = null;

    private String DB_RCYL = null;

    private Schema validatorSchema = null;

    private DocumentBuilderFactory dbFactory = null;

    private SimpleDateFormat timestampFormat = null;

    private Map<String, byte[]> policies = null;

    private final Map<String, String> policyFiles = null;

    private Map<String, Map<String, String>> indexMap = null;

    private long lastUpdate;

    /**
     * The default constructor for DbXmlPolicyDataManager. This constructor
     * reads the configuration file, 'config-dbxml.xml' and initialises/creates
     * the database as required based on that configuration. Any required
     * indexes are automatically created.
     *
     * @throws PolicyDataManagerException
     */
    public FilePolicyDataManager()
            throws PolicyDataManagerException {
        initConfig();
        loadPolicies(DB_HOME);

        dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy(java.io.File)
     */
    public String addPolicy(File f) throws PolicyDataManagerException {
        return addPolicy(f, null);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy(java.io.File,
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

        return addPolicy(name, out.toString());
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy(java.lang.String)
     */
    public String addPolicy(String document) throws PolicyDataManagerException {
        return addPolicy(document, null);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy(java.lang.String,
     * java.lang.String)
     */
    public String addPolicy(String document, String name)
            throws PolicyDataManagerException {
        InputStream dis = new ByteArrayInputStream(document.getBytes());
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("validating document: " + name);
            }
            Validator validator = validatorSchema.newValidator();
            validator.validate(new StreamSource(dis));
        } catch (Exception e) {
            throw new PolicyDataManagerException("Could not validate policy: "
                    + name, e);
        }

        Map<String, String> dm = getDocumentMetadata(document.getBytes());
        String docName = dm.get("PolicyId");

        String filename = DB_HOME + "/" + docName + ".xml";
        filename = filename.replaceAll("[\\\\\\/\\*\\?\\:\\\"\\<\\>\\|]", "-");

        try {
            DataFileUtils.saveDocument(filename, document.getBytes());
            policies.put(docName, document.getBytes());
        } catch (Exception e) {
            throw new PolicyDataManagerException("Unable to save file: "
                    + filename + " " + e.getMessage());
        }

        setLastUpdate(System.currentTimeMillis());

        return docName;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#deletePolicy(java.lang.String)
     */
    public boolean deletePolicy(String name) throws PolicyDataManagerException {
        File db_rcyl = new File(DB_RCYL);
        if (!db_rcyl.exists()) {
            db_rcyl.mkdirs();
        }

        File db_home = new File(DB_HOME);

        String filename = policyFiles.get(name);
        File policy = new File(db_home.getAbsolutePath() + "/" + filename);
        String filenameTo = filename + "-" + timestampFormat.format(new Date());
        File policyTo = new File(db_rcyl.getAbsolutePath() + "/" + filenameTo);
        if (!policy.renameTo(policyTo)) {
            return false;
        }

        policies.remove(name);
        policyFiles.remove(name);

        return true;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#updatePolicy(java.lang.String,
     * java.lang.String)
     */
    public boolean updatePolicy(String name, String newDocument)
            throws PolicyDataManagerException {
        deletePolicy(name);
        addPolicy(newDocument, name);

        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#getPolicy(java.lang.String)
     */
    public byte[] getPolicy(String name) throws PolicyDataManagerException {
        return policies.get(name);
    }

    /*
     * (non-Javadoc)
     * @seemelcoe.xacml.pdp.data.PolicyDataManager#getPolicies(com.sun.xacml.
     * EvaluationCtx)
     */
    public Map<String, byte[]> getPolicies(EvaluationCtx eval)
            throws PolicyDataManagerException {
        Map<String, byte[]> documents = new HashMap<String, byte[]>();

        /*
         * try { // Get the query (query gets prepared if necesary) a =
         * System.nanoTime(); Map<String, Set<AttributeBean>> attributeMap =
         * getAttributeMap(eval); XmlQueryContext context =
         * manager.createQueryContext();
         * context.setDefaultCollection(CONTAINER); context.setNamespace("p",
         * XACML20_POLICY_NS); context.setNamespace("m", METADATA_POLICY_NS); //
         * Set all the bind variables in the query context String[] types = new
         * String[] { "Subject", "Resource", "Action", "Environment" }; int
         * resourceComponentCount = 0; for (int x = 0; x < types.length; x++) {
         * String t = types[x]; int count = 0; for (AttributeBean bean :
         * attributeMap.get(t.toLowerCase() + "Attributes")) { if
         * (bean.getId().equals(XACML_RESOURCE_ID)) {
         * context.setVariableValue("XacmlResourceId", new
         * XmlValue(bean.getId())); // removed type to reduce query parsing
         * time. // context.setVariableValue("XacmlResourceType", new
         * XmlValue(bean.getType())); for (String value : bean.getValues()) {
         * String[] components = makeComponents(value); if (components != null)
         * { int resourceComponents = components.length; for (int c = 0; c <
         * resourceComponents; c++, resourceComponentCount++) { XmlValue
         * component = new XmlValue(components[c]);
         * context.setVariableValue("XacmlResourceIdValue" +
         * resourceComponentCount, component); if (logger.isDebugEnabled())
         * logger.debug("XacmlResourceIdValue" + resourceComponentCount + ": " +
         * components[c]); } } else {
         * context.setVariableValue("XacmlResourceIdValue" +
         * resourceComponentCount, new XmlValue(value));
         * resourceComponentCount++; if (logger.isDebugEnabled())
         * logger.debug("XacmlResourceIdValue" + resourceComponentCount + ": " +
         * value); } } } else { context.setVariableValue(t + "Id" + count, new
         * XmlValue(bean.getId())); // removed type to reduce query parsing time
         * // context.setVariableValue(t + "Type" + count, new
         * XmlValue(bean.getType())); if (logger.isDebugEnabled()) logger.debug(t +
         * "Id" + count + " = '" + bean.getId() + "'"); int valueCount = 0; for
         * (String value : bean.getValues()) { context.setVariableValue(t + "Id"
         * + count + "-Value" + valueCount, new XmlValue(value)); if
         * (logger.isDebugEnabled()) logger.debug(t + "Id" + count + "-Value" +
         * valueCount + " = '" + value + "'"); valueCount++; } count++; } } }
         * XmlQueryExpression qe = getQuery(attributeMap, context,
         * resourceComponentCount); b = System.nanoTime(); total += (b - a); if
         * (logger.isDebugEnabled()) logger.debug("Query prep. time: " + (b - a) +
         * "ns"); // execute the query a = System.nanoTime(); XmlResults results
         * = qe.execute(context); b = System.nanoTime(); total += (b - a); if
         * (logger.isDebugEnabled()) logger.debug("Query exec. time: " + (b - a) +
         * "ns"); // process results while (results.hasNext()) { XmlValue value
         * = results.next(); if (logger.isDebugEnabled())
         * logger.debug("Retrieved Document: " + value.asDocument().getName());
         * documents.put(value.asDocument().getName(),
         * value.asDocument().getContent()); } results.delete(); if
         * (logger.isDebugEnabled()) logger.debug("Total exec. time: " + total +
         * "ns"); } catch (XmlException xe) { throw new
         * PolicyDataManagerException
         * ("Error getting policies from PolicyDataManager.", xe); } catch
         * (URISyntaxException use) { throw new
         * PolicyDataManagerException("Error building query.", use); }
         */

        return documents;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#listPolicies()
     */
    public List<String> listPolicies() throws PolicyDataManagerException {
        return new ArrayList<String>(policies.keySet());
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#findPolicies(org.fcrepo.server.security.xacml.util
     * .AttributeBean[])
     */
    public Map<String, byte[]> findPolicies(AttributeBean[] attributes)
            throws PolicyDataManagerException {
        if (attributes == null || attributes.length == 0) {
            throw new PolicyDataManagerException("attribute array cannot be null or zero length");
        }

        Map<String, byte[]> documents = new TreeMap<String, byte[]>();
        return documents;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#getLastUpdate()
     */
    public long getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @param lastUpdate
     *        the lastUpdate to set
     */
    private void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    // TODO: maybe use this to create the indexes...?

    /**
     * Obtains the metadata for the given document.
     *
     * @param docIS
     *        the document as an InputStream
     * @return the document metadata as a Map
     */
    private Map<String, String> getDocumentMetadata(byte[] docData) {
        Map<String, String> metadata = new HashMap<String, String>();

        InputStream docIS = new ByteArrayInputStream(docData);
        try {
            DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
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
            logger.error(e.getMessage());
        }

        return metadata;
    }

    /**
     * Reads a configuration file and initialises the instance based on that
     * information.
     *
     * @throws PolicyDataManagerException
     */
    private void initConfig() throws PolicyDataManagerException {
        if (logger.isDebugEnabled()) {
            Runtime runtime = Runtime.getRuntime();
            logger.debug("Total memory: " + runtime.totalMemory() / 1024);
            logger.debug("Free memory: " + runtime.freeMemory() / 1024);
            logger.debug("Max memory: " + runtime.maxMemory() / 1024);
        }

        try {
            String home = MelcoePDP.PDP_HOME.getAbsolutePath();

            String filename = home + "/conf/config-pdm-file.xml";
            File f = new File(filename);
            if (!f.exists()) {
                throw new PolicyDataManagerException("Could not locate config file: "
                        + f.getAbsolutePath());
            }

            logger.info("Loading config file: " + f.getAbsolutePath());

            DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new FileInputStream(f));

            NodeList nodes = null;

            // get config information
            nodes = doc.getChildNodes();
            for (int x = 0; x < nodes.getLength(); x++) {
                Node node = nodes.item(x);
                if (node.getNodeName().equals("directory")) {
                    DB_HOME =
                            MelcoePDP.PDP_HOME.getAbsolutePath()
                                    + node.getAttributes().getNamedItem("name")
                                            .getNodeValue();
                    DB_RCYL = DB_HOME + "recycle";
                    File db_home = new File(DB_HOME);
                    File db_rcyl = new File(DB_RCYL);
                    if (!db_home.exists()) {
                        try {
                            db_home.mkdirs();
                        } catch (Exception e) {
                            throw new PolicyDataManagerException("Could not create DB directory: "
                                    + db_home.getAbsolutePath());
                        }
                    }

                    if (!db_rcyl.exists()) {
                        try {
                            db_home.mkdirs();
                        } catch (Exception e) {
                            throw new PolicyDataManagerException("Could not create DB recycle directory: "
                                    + db_rcyl.getAbsolutePath());
                        }
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("[config] " + node.getNodeName() + ": "
                                + db_home.getAbsolutePath());
                        logger.debug("[config] " + node.getNodeName() + ": "
                                + db_rcyl.getAbsolutePath());
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
                    if (logger.isDebugEnabled()) {
                        logger.debug("Node name: " + node.getNodeName());
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
                logger.info("Initialising validation");

                for (int x = 0; x < nodes.getLength(); x++) {
                    Node schemaNode = nodes.item(x);
                    if (schemaNode.getNodeType() == Node.ELEMENT_NODE) {
                        String namespace =
                                schemaNode.getAttributes()
                                        .getNamedItem("namespace")
                                        .getNodeValue();
                        if (XACML20_POLICY_NS.equals(namespace)) {
                            logger.debug("found valid schema. Creating validator");

                            SchemaFactory schemaFactory =
                                    SchemaFactory
                                            .newInstance("http://www.w3.org/2001/XMLSchema");

                            String loc =
                                    schemaNode.getAttributes()
                                            .getNamedItem("location")
                                            .getNodeValue();
                            if (loc.startsWith("http://")) {
                                // web reference
                                validatorSchema =
                                        schemaFactory.newSchema(new URL(loc));
                            } else if (loc.startsWith("/")
                                    || loc.matches("[A-Za-z]:.*")) {
                                // absolute file reference
                                File schemaFile = new File(loc);
                                if (!schemaFile.exists()) {
                                    throw new PolicyDataManagerException("Cannot find schema file: "
                                            + schemaFile.getAbsolutePath());
                                }

                                validatorSchema =
                                        schemaFactory.newSchema(schemaFile);
                            } else {
                                // relative file reference
                                File schemaFile = new File(home + "/" + loc);
                                if (!schemaFile.exists()) {
                                    throw new PolicyDataManagerException("Cannot find schema file: "
                                            + schemaFile.getAbsolutePath());
                                }

                                validatorSchema =
                                        schemaFactory.newSchema(schemaFile);
                            }
                        }
                    }
                }
            }

            timestampFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        } catch (Exception e) {
            logger.error("Could not initialise DBXML: " + e.getMessage(), e);
            throw new PolicyDataManagerException("Could not initialise DBXML: "
                    + e.getMessage(), e);
        }
    }

    private void loadPolicies(String policyDir)
            throws PolicyDataManagerException {
        Map<String, byte[]> policiesTmp =
                new ConcurrentHashMap<String, byte[]>();

        File policyHome = new File(policyDir);
        if (!policyHome.exists()) {
            throw new PolicyDataManagerException("Policy directory does not exist: "
                    + policyHome.getAbsolutePath());
        }

        File[] pf = policyHome.listFiles();
        for (File f : pf) {
            if (!f.getName().endsWith(".xml")) {
                continue;
            }

            try {
                byte[] doc = DataFileUtils.loadFile(f);

                InputStream docIS = new ByteArrayInputStream(doc);
                Validator validator = validatorSchema.newValidator();
                validator.validate(new StreamSource(docIS));

                Map<String, String> dm = getDocumentMetadata(doc);
                policiesTmp.put(dm.get("PolicyId"), doc);
                policyFiles.put(dm.get("PolicyId"), f.getName());
            } catch (Exception e) {
                logger.error("Error loading document: " + f.getName(), e);
            }
        }

        synchronized (policies) {
            policies = policiesTmp;
        }
    }

}
