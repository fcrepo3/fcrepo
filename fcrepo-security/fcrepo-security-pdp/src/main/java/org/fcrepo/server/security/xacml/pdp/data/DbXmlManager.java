/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.security.xacml.pdp.data;

import java.io.File;
import java.io.FileInputStream;

import java.net.URL;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.sleepycat.db.Environment;
import com.sleepycat.db.EnvironmentConfig;
import com.sleepycat.dbxml.XmlContainer;
import com.sleepycat.dbxml.XmlContainerConfig;
import com.sleepycat.dbxml.XmlException;
import com.sleepycat.dbxml.XmlIndexSpecification;
import com.sleepycat.dbxml.XmlManager;
import com.sleepycat.dbxml.XmlManagerConfig;
import com.sleepycat.dbxml.XmlTransaction;
import com.sleepycat.dbxml.XmlUpdateContext;
import com.sleepycat.dbxml.XmlValue;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.pdp.MelcoePDP;


/**
 * Encapsulates access to DbXml
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class DbXmlManager {

    private static final Logger log =
            LoggerFactory.getLogger(DbXmlManager.class.getName());

    private static final String XACML20_POLICY_NS =
            "urn:oasis:names:tc:xacml:2.0:policy:schema:os";

    public String DB_HOME = null;

    public String CONTAINER = null;

    public Map<String, Map<String, String>> indexMap = null;

    public XmlManager manager = null;

    public XmlUpdateContext updateContext = null;

    public XmlContainer container = null;

    public Environment env = null;

    public Validator validator = null;

    public DbXmlManager()
            throws PolicyStoreException {
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

            } catch (Exception e) {
                log.error("Could not start database subsystem.", e);
                txn.abort();
                close();
                throw new PolicyStoreException(e.getMessage(), e);
            }
        } catch (XmlException xe) {
            throw new PolicyStoreException("Error aborting transaction: "
                    + xe.getMessage(), xe);
        }
    }

    /**
     * Closes the dbxml container and manager.
     */
    public void close() {
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
     * Reads a configuration file and initialises the instance based on that
     * information.
     *
     * @throws PolicyStoreException
     */
    private void initConfig() throws PolicyStoreException {
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
                throw new PolicyStoreException("Could not locate config file: "
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
                            throw new PolicyStoreException("Could not create DB directory: "
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
                            validator =schema.newValidator();
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new PolicyStoreException("Could not initialise DBXML: "
                    + e.getMessage(), e);
        }
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
