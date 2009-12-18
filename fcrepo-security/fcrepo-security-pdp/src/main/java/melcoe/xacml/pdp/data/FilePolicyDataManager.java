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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import melcoe.xacml.pdp.MelcoePDP;
import melcoe.xacml.util.AttributeBean;
import melcoe.xacml.util.DataFileUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.cond.EvaluationResult;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class FilePolicyDataManager
        implements PolicyDataManager {

    private static final Logger log =
            Logger.getLogger(FilePolicyDataManager.class.getName());

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

        return addPolicy(name, out.toString());
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
        InputStream dis = new ByteArrayInputStream(document.getBytes());
        try {
            if (log.isDebugEnabled()) {
                log.debug("validating document: " + name);
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
     * melcoe.xacml.pdp.data.PolicyDataManager#deletePolicy(java.lang.String)
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
     * melcoe.xacml.pdp.data.PolicyDataManager#updatePolicy(java.lang.String,
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
     * @see melcoe.xacml.pdp.data.PolicyDataManager#getPolicy(java.lang.String)
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
         * resourceComponentCount, component); if (log.isDebugEnabled())
         * log.debug("XacmlResourceIdValue" + resourceComponentCount + ": " +
         * components[c]); } } else {
         * context.setVariableValue("XacmlResourceIdValue" +
         * resourceComponentCount, new XmlValue(value));
         * resourceComponentCount++; if (log.isDebugEnabled())
         * log.debug("XacmlResourceIdValue" + resourceComponentCount + ": " +
         * value); } } } else { context.setVariableValue(t + "Id" + count, new
         * XmlValue(bean.getId())); // removed type to reduce query parsing time
         * // context.setVariableValue(t + "Type" + count, new
         * XmlValue(bean.getType())); if (log.isDebugEnabled()) log.debug(t +
         * "Id" + count + " = '" + bean.getId() + "'"); int valueCount = 0; for
         * (String value : bean.getValues()) { context.setVariableValue(t + "Id"
         * + count + "-Value" + valueCount, new XmlValue(value)); if
         * (log.isDebugEnabled()) log.debug(t + "Id" + count + "-Value" +
         * valueCount + " = '" + value + "'"); valueCount++; } count++; } } }
         * XmlQueryExpression qe = getQuery(attributeMap, context,
         * resourceComponentCount); b = System.nanoTime(); total += (b - a); if
         * (log.isDebugEnabled()) log.debug("Query prep. time: " + (b - a) +
         * "ns"); // execute the query a = System.nanoTime(); XmlResults results
         * = qe.execute(context); b = System.nanoTime(); total += (b - a); if
         * (log.isDebugEnabled()) log.debug("Query exec. time: " + (b - a) +
         * "ns"); // process results while (results.hasNext()) { XmlValue value
         * = results.next(); if (log.isDebugEnabled())
         * log.debug("Retrieved Document: " + value.asDocument().getName());
         * documents.put(value.asDocument().getName(),
         * value.asDocument().getContent()); } results.delete(); if
         * (log.isDebugEnabled()) log.debug("Total exec. time: " + total +
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
     * @see melcoe.xacml.pdp.data.PolicyDataManager#listPolicies()
     */
    public List<String> listPolicies() throws PolicyDataManagerException {
        return new ArrayList<String>(policies.keySet());
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

        Map<String, byte[]> documents = new TreeMap<String, byte[]>();
        /*
         * try { a = System.nanoTime(); XmlQueryContext context =
         * manager.createQueryContext();
         * context.setDefaultCollection(CONTAINER); context.setNamespace("p",
         * XACML20_POLICY_NS); context.setNamespace("m", METADATA_POLICY_NS);
         * for (int x = 0; attributes.length < 0; x++) {
         * context.setVariableValue("id" + x, new
         * XmlValue(attributes[x].getId())); // context.setVariableValue("type"
         * + x, new // XmlValue(attributes[x].getType()));
         * //context.setVariableValue("value" + x, new
         * XmlValue(attributes[x].getValue())); } if
         * (searchQueries[attributes.length] == null) { StringBuilder sb = new
         * StringBuilder(); sb.append("for $doc in "); sb.append("collection('"
         * + CONTAINER + "') ");
         * sb.append("let $value := $doc//p:AttributeValue ");
         * sb.append("let $id := $value/..//@AttributeId ");
         * sb.append("where 1 = 1 "); for (int x = 0; x < attributes.length;
         * x++) { sb.append("and $value = $value" + x + " ");
         * sb.append("and $id = $id" + x + " "); } sb.append("return $doc");
         * searchQueries[attributes.length] = manager.prepare(sb.toString(),
         * context); } b = System.nanoTime(); total += (b - a); if
         * (log.isDebugEnabled()) log.debug("Query prep. time: " + (b - a) +
         * "ns"); a = System.nanoTime(); XmlResults results =
         * searchQueries[attributes.length].execute(context); b =
         * System.nanoTime(); total += (b - a); if (log.isDebugEnabled())
         * log.debug("Search exec. time: " + (b - a) + "ns"); a =
         * System.nanoTime(); while (results.hasNext()) { XmlValue value =
         * results.next(); if (log.isDebugEnabled())
         * log.debug("Found search result: " + value.asDocument().getName());
         * documents.put(value.asDocument().getName(),
         * value.asDocument().getContent()); } results.delete(); b =
         * System.nanoTime(); total += (b - a); if (log.isDebugEnabled())
         * log.debug("Result proc. time: " + (b - a) + "ns");
         * log.info("Total time: " + total + "ns"); } catch (XmlException xe) {
         * log.error("Exception during findPolicies: " + xe.getMessage(), xe);
         * throw new
         * PolicyDataManagerException("Exception during findPolicies: " +
         * xe.getMessage(), xe); }
         */

        return documents;
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
            log.error(e.getMessage());
        }

        return metadata;
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

                            ab.addValue(value.encode());
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

            String filename = home + "/conf/config-pdm-file.xml";
            File f = new File(filename);
            if (!f.exists()) {
                throw new PolicyDataManagerException("Could not locate config file: "
                        + f.getAbsolutePath());
            }

            log.info("Loading config file: " + f.getAbsolutePath());

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

                    if (log.isDebugEnabled()) {
                        log.debug("[config] " + node.getNodeName() + ": "
                                + db_home.getAbsolutePath());
                        log.debug("[config] " + node.getNodeName() + ": "
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
                        String namespace =
                                schemaNode.getAttributes()
                                        .getNamedItem("namespace")
                                        .getNodeValue();
                        if (XACML20_POLICY_NS.equals(namespace)) {
                            if (log.isDebugEnabled()) {
                                log
                                        .debug("found valid schema. Creating validator");
                            }

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
            log.fatal("Could not initialise DBXML: " + e.getMessage(), e);
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
                log.error("Error loading document: " + f.getName());
                log.error(e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug(e);
                }
            }
        }

        synchronized (policies) {
            policies = policiesTmp;
        }
    }

    private Map<String, Map<String, String>> indexPolicy(byte[] policy) {
        Map<String, Map<String, String>> indexes =
                new HashMap<String, Map<String, String>>();

        InputStream docIS = new ByteArrayInputStream(policy);
        try {
            DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(docIS);

            NodeList nodes = doc.getElementsByTagNameNS(XACML20_POLICY_NS, "");

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
