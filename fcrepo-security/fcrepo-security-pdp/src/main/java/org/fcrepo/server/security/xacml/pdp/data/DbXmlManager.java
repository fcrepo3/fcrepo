/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.security.xacml.pdp.data;

import java.io.File;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.fcrepo.server.security.xacml.pdp.MelcoePDP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import com.sleepycat.db.Environment;
import com.sleepycat.db.EnvironmentConfig;
import com.sleepycat.dbxml.XmlContainer;
import com.sleepycat.dbxml.XmlContainerConfig;
import com.sleepycat.dbxml.XmlException;
import com.sleepycat.dbxml.XmlIndexSpecification;
import com.sleepycat.dbxml.XmlManager;
import com.sleepycat.dbxml.XmlManagerConfig;
import com.sleepycat.dbxml.XmlUpdateContext;
import com.sleepycat.dbxml.XmlValue;


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
            Constants.XACML2_POLICY_SCHEMA.OS.uri;

    public String DB_HOME = null;

    public String CONTAINER = null;

    public Map<String, Map<String, String>> indexMap = null;

    public XmlManager manager = null;

    public XmlUpdateContext updateContext = null;

    public XmlContainer container = null;

    public Environment env = null;

    private static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    public static final Lock readLock = rwl.readLock();
    public static final Lock writeLock = rwl.writeLock();

    public DbXmlManager(String databaseDirectory, String container)
            throws PolicyStoreException {
      setDatabaseDirectory(databaseDirectory);
      setContainer(container);
    }

    /**
     * Closes the dbxml container and manager.
     */
    public void close() {
        // getting a read lock will ensure all writes have finished
        // if we are really closing assume that we don't care about reads
        readLock.lock();
        try {
            if (container != null) {
                try {
                    container.close();
                    container = null;
                    log.info("Closed container");
                } catch (XmlException e) {
                    log.warn("close failed: " + e.getMessage(), e);
                }
            }

            if (manager != null) {
                try {
                    manager.close();
                    manager = null;
                    log.info("Closed manager");
                } catch (XmlException e) {
                    log.warn("close failed: " + e.getMessage(), e);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    public void deleteDatabase() {
        readLock.lock();
        try {
            try {
                //XmlIndexSpecification is = container.getIndexSpecification();
                //container.setIndexSpecification(is);

                container.close();

                container = null;
            } catch (XmlException e) {
                log.warn("Error closing container " + e.getMessage());
            }
            //container.delete();
            try {
                manager.removeContainer(CONTAINER);
            } catch (XmlException e) {
                log.warn("Error removing container " + e.getMessage());
            }
        } finally {
            readLock.unlock();
        }
    }

    public void setDatabaseDirectory(String databaseDirectory) throws PolicyStoreException {
        DB_HOME = MelcoePDP.PDP_HOME.getAbsolutePath() + databaseDirectory;
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
           log.debug("[config] databaseDirectory : " + db_home.getAbsolutePath());
        }
    }

    public void setContainer(String container) {
        CONTAINER = container;
        if (log.isDebugEnabled()) {
            log.debug("[config] container: " + container);
        }
    }


    public void init() throws PolicyStoreException {
        if (log.isDebugEnabled()) {
            Runtime runtime = Runtime.getRuntime();
            log.debug("Total memory: " + runtime.totalMemory() / 1024);
            log.debug("Free memory: " + runtime.freeMemory() / 1024);
            log.debug("Max memory: " + runtime.maxMemory() / 1024);
        }

        File envHome = new File(DB_HOME);
        EnvironmentConfig envCfg = new EnvironmentConfig();

        //envCfg.setRunRecovery(true);
        envCfg.setAllowCreate(true);
        envCfg.setInitializeCache(true);
        envCfg.setInitializeLocking(false);
        envCfg.setInitializeLogging(true);
        envCfg.setTransactional(false);

        XmlManagerConfig managerCfg = new XmlManagerConfig();
        managerCfg.setAdoptEnvironment(true);
        managerCfg.setAllowExternalAccess(true);

        XmlContainerConfig containerCfg = new XmlContainerConfig();
        containerCfg.setAllowCreate(true);
        containerCfg.setTransactional(false);

        try {
            env = new Environment(envHome, envCfg);
            manager = new XmlManager(env, managerCfg);
            // XmlManager.setLogCategory(XmlManager.CATEGORY_NONE, true);
            // XmlManager.setLogLevel(XmlManager.LEVEL_WARNING, true);
            updateContext = manager.createUpdateContext();
        } catch (Exception e) {
            log.error("Error opening container: " + e.getMessage(), e);
            close();
            throw new PolicyStoreException(e.getMessage(), e);
        }

        try {
            if (manager.existsContainer(CONTAINER) == 0) {
                container =
                    manager.createContainer(CONTAINER,
                                            containerCfg);

                // if we just created a container we also need to add an
                // index
                XmlIndexSpecification is =
                    container.getIndexSpecification();

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

                container.setIndexSpecification(is, updateContext);
            } else {
                container =
                    manager.openContainer(CONTAINER, containerCfg);
            }

        } catch (XmlException e) {
            if(e.getDatabaseException() instanceof com.sleepycat.db.DeadlockException){
                log.error("Caught deadlock exception, create/open container", e);
            }
            log.error("Could not start database subsystem.", e);
            close();
            throw new PolicyStoreException(e.getMessage(), e);
        }


        log.info("Opened Container: " + CONTAINER);

    }

}