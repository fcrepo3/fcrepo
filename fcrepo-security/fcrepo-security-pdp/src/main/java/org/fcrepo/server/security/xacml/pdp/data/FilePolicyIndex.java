/*
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
import java.io.File;
import java.io.FileInputStream;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.pdp.MelcoePDP;
import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;
import org.fcrepo.server.security.xacml.util.AttributeBean;
import org.fcrepo.server.security.xacml.util.DataFileUtils;

/**
 * Implements PolicyIndex for a filesystem policy store/cache
 *
 * @author nishen@melcoe.mq.edu.au
 */
class FilePolicyIndex
        implements PolicyIndex {

    private static final Logger log =
            LoggerFactory.getLogger(FilePolicyIndex.class.getName());

    private static final String XACML20_POLICY_NS =
            "urn:oasis:names:tc:xacml:2.0:policy:schema:os";

    private String DB_HOME = null;

    private String DB_RCYL = null;

    private final Schema validatorSchema = null;

    private DocumentBuilderFactory dbFactory = null;

    private PolicyUtils utils;

    private SimpleDateFormat timestampFormat = null;

    private Map<String, byte[]> policies = null;

    private final Map<String, String> policyFiles = null;

    private Map<String, Map<String, String>> indexMap = null;

    protected FilePolicyIndex()
            throws PolicyIndexException {
        initConfig();
        loadPolicies(DB_HOME);

        dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
    }

    /*
     * (non-Javadoc)
     * @seemelcoe.xacml.pdp.data.PolicyDataQuery#getPolicies(com.sun.xacml.
     * EvaluationCtx)
     */
    public Map<String, byte[]> getPolicies(EvaluationCtx eval)
            throws PolicyIndexException {
        Map<String, byte[]> documents = new HashMap<String, byte[]>();

        return documents;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataQuery#findPolicies(org.fcrepo.server.security.xacml.util
     * .AttributeBean[])
     */
    public Map<String, byte[]> findPolicies(AttributeBean[] attributes)
            throws PolicyIndexException {
        if (attributes == null || attributes.length == 0) {
            throw new PolicyIndexException("attribute array cannot be null or zero length");
        }

        Map<String, byte[]> documents = new TreeMap<String, byte[]>();

        return documents;
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



    private void loadPolicies(String policyDir)
            throws PolicyIndexException {
        Map<String, byte[]> policiesTmp =
                new ConcurrentHashMap<String, byte[]>();

        File policyHome = new File(policyDir);
        if (!policyHome.exists()) {
            throw new PolicyIndexException("Policy directory does not exist: "
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
                    log.debug(e.getMessage());
                }
            }
        }

        synchronized (policies) {
            policies = policiesTmp;
        }
    }
    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy(java.io.File)
     */
    public String addPolicy(File f) throws PolicyIndexException {
        return addPolicy(f, null);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy(java.io.File,
     * java.lang.String)
     */
    public String addPolicy(File f, String name)
            throws PolicyIndexException {
            try {
                return addPolicy(utils.fileToString(f));
            } catch (MelcoePDPException e) {
                throw new PolicyIndexException(e);
            }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy(java.lang.String)
     */
    public String addPolicy(String document) throws PolicyIndexException {
        return addPolicy(document, null);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy(java.lang.String,
     * java.lang.String)
     */
    public String addPolicy(String document, String name)
            throws PolicyIndexException {
        try {
            utils.validate(document, name);
        } catch (MelcoePDPException e1) {
            throw new PolicyIndexException(e1);
        }

        Map<String, String> dm = utils.getDocumentMetadata(document.getBytes());
        String docName = dm.get("PolicyId");

        String filename = DB_HOME + "/" + docName + ".xml";
        filename = filename.replaceAll("[\\\\\\/\\*\\?\\:\\\"\\<\\>\\|]", "-");

        try {
            DataFileUtils.saveDocument(filename, document.getBytes());
            policies.put(docName, document.getBytes());
        } catch (Exception e) {
            throw new PolicyIndexException("Unable to save file: "
                    + filename + " " + e.getMessage());
        }

        return docName;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#deletePolicy(java.lang.String)
     */
    public boolean deletePolicy(String name) throws PolicyIndexException {
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
            throws PolicyIndexException {
        deletePolicy(name);
        addPolicy(newDocument, name);

        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#getPolicy(java.lang.String)
     */
    public byte[] getPolicy(String name) throws PolicyIndexException {
        return policies.get(name);
    }


    /**
     * Check if the policy identified by policyName exists.
     *
     * @param policy
     * @return true iff the policy store contains a policy with the same
     *         PolicyId
     * @throws PolicyStoreException
     */
    public boolean contains(File policy) throws PolicyIndexException {
        try {
            return contains(utils.getPolicyName(policy));
        } catch (MelcoePDPException e) {
            throw new PolicyIndexException(e);
        }
    }


    public boolean contains(String name) throws PolicyIndexException {

         return policies.containsKey(name);

    }



    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#listPolicies()
     */
    public List<String> listPolicies() throws PolicyIndexException {
        return new ArrayList<String>(policies.keySet());
    }


    /**
     * Reads a configuration file and initialises the instance based on that
     * information.
     *
     * @throws PolicyStoreException
     */
    private void initConfig() throws PolicyIndexException {
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
                throw new PolicyIndexException("Could not locate config file: "
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
                            throw new PolicyIndexException("Could not create DB directory: "
                                    + db_home.getAbsolutePath());
                        }
                    }

                    if (!db_rcyl.exists()) {
                        try {
                            db_home.mkdirs();
                        } catch (Exception e) {
                            throw new PolicyIndexException("Could not create DB recycle directory: "
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
                                utils = new PolicyUtils(schemaFactory.newSchema(new URL(loc)).newValidator());
                            } else if (loc.startsWith("/")
                                    || loc.matches("[A-Za-z]:.*")) {
                                // absolute file reference
                                File schemaFile = new File(loc);
                                if (!schemaFile.exists()) {
                                    throw new PolicyIndexException("Cannot find schema file: "
                                            + schemaFile.getAbsolutePath());
                                }

                                utils =
                                        new PolicyUtils(schemaFactory.newSchema(schemaFile).newValidator());
                            } else {
                                // relative file reference
                                File schemaFile = new File(home + "/" + loc);
                                if (!schemaFile.exists()) {
                                    throw new PolicyIndexException("Cannot find schema file: "
                                            + schemaFile.getAbsolutePath());
                                }

                                utils =
                                        new PolicyUtils(schemaFactory.newSchema(schemaFile).newValidator());
                            }
                        }
                    }
                }
            }

            timestampFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        } catch (Exception e) {
            log.error("Could not initialise DBXML: " + e.getMessage(), e);
            throw new PolicyIndexException("Could not initialise DBXML: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public boolean clear() throws PolicyIndexException {
        // TODO:
        throw new RuntimeException("Method clear is not implemented");
    }


}
