/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.fcrepo.common.Constants;
import org.fcrepo.common.Models;
import org.fcrepo.server.Context;
import org.fcrepo.server.Module;
import org.fcrepo.server.RecoveryContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ConnectionPoolNotFoundException;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.InvalidContextException;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ObjectAlreadyInLowlevelStorageException;
import org.fcrepo.server.errors.ObjectExistsException;
import org.fcrepo.server.errors.ObjectLockedException;
import org.fcrepo.server.errors.ObjectNotFoundException;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.StorageDeviceException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.management.Management;
import org.fcrepo.server.management.PIDGenerator;
import org.fcrepo.server.resourceIndex.ResourceIndex;
import org.fcrepo.server.search.FieldSearch;
import org.fcrepo.server.search.FieldSearchQuery;
import org.fcrepo.server.search.FieldSearchResult;
import org.fcrepo.server.storage.lowlevel.ICheckable;
import org.fcrepo.server.storage.lowlevel.ILowlevelStorage;
import org.fcrepo.server.storage.translation.DOTranslationUtility;
import org.fcrepo.server.storage.translation.DOTranslator;
import org.fcrepo.server.storage.types.BasicDigitalObject;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamManagedContent;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.storage.types.DigitalObjectUtil;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.fcrepo.server.storage.types.RelationshipTuple;
import org.fcrepo.server.storage.types.XMLDatastreamProcessor;
import org.fcrepo.server.utilities.DCField;
import org.fcrepo.server.utilities.DCFields;
import org.fcrepo.server.utilities.SQLUtility;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.server.validation.DOObjectValidator;
import org.fcrepo.server.validation.DOValidator;
import org.fcrepo.server.validation.ValidationUtility;
import org.fcrepo.utilities.ReadableByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the reading and writing of digital objects by instantiating an
 * appropriate object reader or writer. Also, manages the object ingest process
 * and the object replication process.
 *
 * @author Chris Wilper
 * @version $Id$
 */
public class DefaultDOManager extends Module
implements DOManager {

    private static final Logger logger = LoggerFactory
            .getLogger(DefaultDOManager.class);
    
    private static final String[] STRING_TYPE = new String[0];

    private static final Pattern URL_PROTOCOL = Pattern.compile("^\\w+:\\/.*$");
    
    public static String CMODEL_QUERY =
        "SELECT cModel, sDef, sDep, mDate " +
        " FROM modelDeploymentMap, doFields " +
        " WHERE doFields.pid = modelDeploymentMap.sDep";


    public static String REGISTERED_PID_QUERY =
            "SELECT doPID FROM doRegistry WHERE doPID=?";
    
    public static String INSERT_PID_QUERY =
            "INSERT INTO doRegistry (doPID, ownerId, label) VALUES (?, ?, ?)";

    public static String PID_VERSION_QUERY =
            "SELECT systemVersion FROM doRegistry WHERE doPID=?";

    public static String PID_VERSION_UPDATE =
            "UPDATE doRegistry SET systemVersion=? WHERE doPID=?";
    
    private static String INSERT_MODEL_DEPLOYMENT =
            "INSERT INTO modelDeploymentMap (cModel, sDef, sDep) VALUES (?, ?, ?)";

    private static String DELETE_MODEL_DEPLOYMENT =
            "DELETE FROM modelDeploymentMap "
                    + "WHERE cModel = ? AND sDef =? AND sDep = ?";

    private String m_pidNamespace;

    protected String m_storagePool;

    private String m_defaultStorageFormat;

    private String m_defaultExportFormat;

    private String m_storageCharacterEncoding;

    protected PIDGenerator m_pidGenerator;

    protected DOTranslator m_translator;
    
    protected ILowlevelStorage m_permanentStore;
    
    protected boolean m_checkableStore;

    protected FedoraStorageHintProvider m_hintProvider;

    protected DOValidator m_validator;

    private DOObjectValidator m_objectValidator;

    protected FieldSearch m_fieldSearch;

    protected ExternalContentManager m_contentManager;

    protected Management m_management;

    protected Set<String> m_retainPIDs;

    protected ResourceIndex m_resourceIndex;

    private DOReaderCache m_readerCache;

    protected ConnectionPool m_connectionPool;

    protected Connection m_connection;

    private ModelDeploymentMap m_cModelDeploymentMap;

    private int m_ingestValidationLevel;

    private Map<String, ReentrantLock> m_pidLocks;

    /**
     * Creates a new DefaultDOManager.
     */
    public DefaultDOManager(Map<String, String> moduleParameters,
            Server server, String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
        m_pidLocks = new HashMap< String, ReentrantLock >();
    }

    /**
     * Gets initial param values.
     */
    @Override
    public void initModule() throws ModuleInitializationException {
        // pidNamespace (required, 1-17 chars, a-z, A-Z, 0-9 '-' '.')
        m_pidNamespace = getParameter("pidNamespace");
        if (m_pidNamespace == null) {
            throw new ModuleInitializationException(
                    "pidNamespace parameter must be specified.", getRole());
        }
        if (m_pidNamespace.length() > 17 || m_pidNamespace.length() < 1) {
            throw new ModuleInitializationException(
                    "pidNamespace parameter must be 1-17 chars long", getRole());
        }
        StringBuffer badChars = new StringBuffer();
        for (int i = 0; i < m_pidNamespace.length(); i++) {
            char c = m_pidNamespace.charAt(i);
            boolean invalid = true;
            if (c >= '0' && c <= '9') {
                invalid = false;
            } else if (c >= 'a' && c <= 'z') {
                invalid = false;
            } else if (c >= 'A' && c <= 'Z') {
                invalid = false;
            } else if (c == '-') {
                invalid = false;
            } else if (c == '.') {
                invalid = false;
            }
            if (invalid) {
                badChars.append(c);
            }
        }
        if (badChars.length() > 0) {
            throw new ModuleInitializationException("pidNamespace contains " +
                    "invalid character(s) '" + badChars.toString() + "'",
                    getRole());
        }
        // storagePool (optional, default=ConnectionPoolManager's default pool)
        m_storagePool = getParameter("storagePool");
        if (m_storagePool == null) {
            logger.debug("Parameter storagePool "
                    + "not given, will defer to ConnectionPoolManager's "
                    + "default pool.");
        }
        // internal storage format (required)
        logger.debug("Server property format.storage= " + Server.STORAGE_FORMAT);
        m_defaultStorageFormat = Server.STORAGE_FORMAT;
        if (m_defaultStorageFormat == null) {
            throw new ModuleInitializationException(
                    "System property format.storage "
                            + "not given, but it's required.", getRole());
        }
        // default export format (required)
        m_defaultExportFormat = getParameter("defaultExportFormat");
        if (m_defaultExportFormat == null) {
            throw new ModuleInitializationException(
                    "Parameter defaultExportFormat "
                            + "not given, but it's required.", getRole());
        }
        // storageCharacterEncoding (optional, default=UTF-8)
        m_storageCharacterEncoding = getParameter("storageCharacterEncoding");
        if (m_storageCharacterEncoding == null) {
            logger.debug("Parameter storage_character_encoding "
                    + "not given, using UTF-8");
            m_storageCharacterEncoding = "UTF-8";
        }
        initRetainPID();

        // readerCacheSize and readerCacheSeconds (optional, defaults = 20, 5)
        String rcSize = getParameter("readerCacheSize");
        if (rcSize == null) {
            logger.debug("Parameter readerCacheSize not given, using 20");
            rcSize = "20";
        }
        int readerCacheSize;
        try {
            readerCacheSize = Integer.parseInt(rcSize);
            if (readerCacheSize < 0) {
                throw new Exception("Cannot be less than zero");
            }
        } catch (Exception e) {
            throw new ModuleInitializationException(
                    "Bad value for readerCacheSize parameter: " +
                            e.getMessage(), getRole());
        }

        String rcSeconds = getParameter("readerCacheSeconds");
        if (rcSeconds == null) {
            logger.debug("Parameter readerCacheSeconds not given, using 5");
            rcSeconds = "5";
        }
        int readerCacheSeconds;
        try {
            readerCacheSeconds = Integer.parseInt(rcSeconds);
            if (readerCacheSeconds < 1) {
                throw new Exception("Cannot be less than one");
            }
        } catch (Exception e) {
            throw new ModuleInitializationException(
                    "Bad value for readerCacheSeconds parameter: " +
                            e.getMessage(), getRole());
        }

        // configuration of ingest validation
        String ingestValidationLevel = getParameter("ingestValidationLevel");
        if (ingestValidationLevel == null) {
            logger.debug("Ingest validation level not specified, using default of all");
            m_ingestValidationLevel = DOValidator.VALIDATE_ALL;
        } else {
            m_ingestValidationLevel = Integer.parseInt(ingestValidationLevel);
            // check the values.  better to declare the levels as enums, but this
            // would require DOValidator interface change
            if (m_ingestValidationLevel < -1 || m_ingestValidationLevel > 2) {
                throw new ModuleInitializationException(
                        "Bad value for ingestValidationLevel", getRole());
            }
        }
    }

    protected void initRetainPID() {
        m_retainPIDs = new HashSet<String>();
        String retainPIDs = getParameter("retainPIDs");
        if (retainPIDs == null || retainPIDs.equals("*")) {
            // when m_retainPIDS is set to null, that means "all"
            m_retainPIDs = null;
        } else {
            // add to list (accept space and/or comma-separated)
            String[] ns =
                    retainPIDs.trim().replaceAll(" +", ",").replaceAll(",+",
                            ",").split(",");
            for (String element : ns) {
                if (element.length() > 0) {
                    m_retainPIDs.add(element);
                }
            }

            // fedora-system PIDs must be ingestable as-is
            m_retainPIDs.add("fedora-system");
        }
    }

    @Override
    public void postInitModule() throws ModuleInitializationException {
        // get ref to management module
        m_management =
                (Management) getServer().getModule(
                        "org.fcrepo.server.management.Management");
        if (m_management == null) {
            throw new ModuleInitializationException(
                    "Management module not loaded.", getRole());
        }

        // get ref to contentmanager module
        m_contentManager =
                (ExternalContentManager) getServer().getModule(
                        "org.fcrepo.server.storage.ExternalContentManager");
        if (m_contentManager == null) {
            throw new ModuleInitializationException(
                    "ExternalContentManager not loaded.", getRole());
        }
        // get ref to fieldsearch module
        m_fieldSearch =
                getServer().getBean(
                        "org.fcrepo.server.search.FieldSearch", FieldSearch.class);
        // get ref to pidgenerator
        m_pidGenerator =
                (PIDGenerator) getServer().getModule(
                        "org.fcrepo.server.management.PIDGenerator");
        // note: permanent and temporary storage handles are lazily instantiated

        // get ref to translator and derive storageFormat default if not given
        m_translator =
                (DOTranslator) getServer().getModule(
                        "org.fcrepo.server.storage.translation.DOTranslator");
        // get ref to digital object xml validator
        m_validator =
                (DOValidator) getServer().getModule(
                        "org.fcrepo.server.validation.DOValidator");
        if (m_validator == null) {
            throw new ModuleInitializationException("DOValidator not loaded.",
                    getRole());
        }
        // get ref to digital object validator
        m_objectValidator =
                (DOObjectValidator) getServer().getModule(
                        "org.fcrepo.server.validation.DOObjectValidator");
        if (m_objectValidator == null) {
            throw new ModuleInitializationException(
                    "DOObjectValidator not loaded.", getRole());
        }

        // get ref to ResourceIndex
        m_resourceIndex =
                (ResourceIndex) getServer().getModule(
                        "org.fcrepo.server.resourceIndex.ResourceIndex");
        if (m_resourceIndex == null) {
            logger.error("ResourceIndex not loaded");
            throw new ModuleInitializationException("ResourceIndex not loaded",
                    getRole());
        }

        // now get the connectionpool
        ConnectionPoolManager cpm =
                (ConnectionPoolManager) getServer().getModule(
                        "org.fcrepo.server.storage.ConnectionPoolManager");
        if (cpm == null) {
            throw new ModuleInitializationException(
                    "ConnectionPoolManager not loaded.", getRole());
        }
        try {
            if (m_storagePool == null) {
                m_connectionPool = cpm.getPool();
            } else {
                m_connectionPool = cpm.getPool(m_storagePool);
            }
        } catch (ConnectionPoolNotFoundException cpnfe) {
            throw new ModuleInitializationException("Couldn't get required "
                    + "connection pool; wasn't found", getRole());
        }
        try {
            String dbSpec =
                    "org/fcrepo/server/storage/resources/DefaultDOManager.dbspec";
            InputStream specIn =
                    this.getClass().getClassLoader()
                            .getResourceAsStream(dbSpec);
            if (specIn == null) {
                throw new IOException("Cannot find required " + "resource: " +
                        dbSpec);
            }
            SQLUtility.createNonExistingTables(m_connectionPool, specIn);
        } catch (Exception e) {
            throw new ModuleInitializationException(
                    "Error while attempting to " +
                            "check for and create non-existing table(s): " +
                            e.getClass().getName() + ": " + e.getMessage(),
                    getRole(), e);
        }

        // get ref to lowlevelstorage module
        m_permanentStore =
                (ILowlevelStorage) getServer().getModule(
                        "org.fcrepo.server.storage.lowlevel.ILowlevelStorage");
        if (m_permanentStore == null) {
            logger.error("LowlevelStorage not loaded");
            throw new ModuleInitializationException(
                    "LowlevelStorage not loaded", getRole());
        }
        m_checkableStore = (m_permanentStore instanceof ICheckable);
        // get ref to DOReaderCache module
        m_readerCache = (DOReaderCache) getServer().getBean("org.fcrepo.server.readerCache");
        
        // get ref to FedoraStorageHintProvider
        try {
            m_hintProvider =
                    (FedoraStorageHintProvider) getServer().getBean(
                            "fedoraStorageHintProvider");
        } catch (Throwable t) {
            logger.warn("Could not load the specified hint provider class (as specified in spring bean definition), using default nullprovider");
            m_hintProvider = new NullStorageHintsProvider();
        }

        /* Load the service deployment cache from the registry */
        initializeCModelDeploymentCache();
    }
    
    @Override
    public String lookupDeploymentForCModel(String cModelPid, String sDefPid) {

        return m_cModelDeploymentMap.getDeployment(ServiceContext.getInstance(
                cModelPid, sDefPid));
    }

    private void initializeCModelDeploymentCache() {
        // Initialize Map containing links from Content Models to the Service
        // Deployments.
        m_cModelDeploymentMap = new ModelDeploymentMap();

        logger.debug("Initializing content model deployment map");

        Connection c = null;
        PreparedStatement s = null;
        ResultSet r = null;
        try {
            c = m_connectionPool.getReadOnlyConnection();
            s = c.prepareStatement(CMODEL_QUERY, ResultSet.TYPE_FORWARD_ONLY,
                            ResultSet.CONCUR_READ_ONLY);
            ResultSet results = s.executeQuery();

            while (results.next()) {
                String cModel = results.getString(1);
                String sDef = results.getString(2);
                String sDep = results.getString(3);
                long lastMod = results.getLong(4);

                m_cModelDeploymentMap.putDeployment(ServiceContext.getInstance(
                        cModel, sDef), sDep, lastMod);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error loading cModel deployment cach",
                    e);
        } finally {
            try {
                if (r != null) {
                    r.close();
                }
                if (s != null) {
                    s.close();
                }
                if (c != null) {
                    m_connectionPool.free(c);
                }
            } catch (SQLException e) {
                throw new RuntimeException(
                        "Error loading cModel deployment cach", e);
            }
        }

    }

    /**
     * Update the registry and deployment cache to reflect the latest state of
     * reality.
     *
     * @param obj
     *        DOReader of a service deployment object
     */
    private synchronized void updateDeploymentMap(DigitalObject obj,
            Connection c, boolean isPurge) throws SQLException {

        String sDep = obj.getPid();
        Set<RelationshipTuple> sDefs =
                obj.getRelationships(Constants.MODEL.IS_DEPLOYMENT_OF, null);
        Set<RelationshipTuple> models =
                obj.getRelationships(Constants.MODEL.IS_CONTRACTOR_OF, null);

        /* Read in the new deployment map from the object */
        Set<ServiceContext> newContext = new HashSet<ServiceContext>();

        if (!isPurge) {
            for (RelationshipTuple sDefTuple : sDefs) {
                String sDef = sDefTuple.getObjectPID();
                for (RelationshipTuple cModelTuple : models) {
                    String cModel = cModelTuple.getObjectPID();
                    newContext.add(ServiceContext.getInstance(cModel, sDef));
                }
            }
        }

        /* Read in the old deployment map from the cache */
        Set<ServiceContext> oldContext =
                m_cModelDeploymentMap.getContextFor(sDep);

        /* Remove any obsolete deployments from the registry/cache */
        for (ServiceContext o : oldContext) {
            if (!newContext.contains(o)) {
                removeDeployment(o, obj, c);
            }
        }

        /* Add any new deployments from the registry/cache */
        for (ServiceContext n : newContext) {
            if (!oldContext.contains(n)) {
                addDeployment(n, obj, c);
            } else {
                updateDeployment(n, obj, c);
            }
        }
    }

    private void addDeployment(ServiceContext context, DigitalObject sDep,
            Connection c) throws SQLException {

        PreparedStatement s = c.prepareStatement(INSERT_MODEL_DEPLOYMENT);

        try {
            s.setString(1, context.cModel);
            s.setString(2, context.sDef);
            s.setString(3, sDep.getPid());
            s.executeUpdate();
        } finally {
            if (s != null) {
                s.close();
            }
        }

        m_cModelDeploymentMap.putDeployment(context, sDep.getPid(), sDep
                .getLastModDate().getTime());

    }

    private void updateDeployment(ServiceContext context, DigitalObject sDep,
            Connection c) throws SQLException {

        m_cModelDeploymentMap.putDeployment(context, sDep.getPid(), sDep
                .getLastModDate().getTime());

    }

    private void removeDeployment(ServiceContext context, DigitalObject sDep,
            Connection c) throws SQLException {
        PreparedStatement s = c.prepareStatement(DELETE_MODEL_DEPLOYMENT);
        s.setString(1, context.cModel);
        s.setString(2, context.sDef);
        s.setString(3, sDep.getPid());

        try {
            s.executeUpdate();
        } finally {
            if (s != null) {
                s.close();
            }
        }
        m_cModelDeploymentMap.removeDeployment(context, sDep.getPid());

    }

    @Override
    public void shutdownModule() {
    }

    @Override
    public void releaseWriter(DOWriter writer) {

        // If this is a new object, but object was not successfully committed
        // need to backout object registration.
        if (writer.isNew() && !writer.isCommitted()) {
            try {
                unregisterObject(writer.getObject());
            } catch (Exception e) {
                try {
                    logger.warn("Error unregistering object: " +
                            writer.GetObjectPID(), e);
                } catch (Exception e2) {
                    logger.warn(
                            "Error unregistering object; Unable to obtain PID from writer.",
                            e2);
                }
            }
        }

        writer.invalidate();
        
        if (writer.isCommitted()) {
            try{
                m_readerCache.remove(writer.GetObjectPID());
            } catch (ServerException e) {
                logger.warn("Error invalidating reader cache; Unable to obtain pid from writer.");
            }
        }

        try {
            releaseWriteLock(writer.GetObjectPID());
        } catch (ServerException e) {
            logger.warn("Error releasing object lock; Unable to obtain pid from writer.");
        }
    }

    private void releaseWriteLock(String pid) {
	    synchronized(m_pidLocks) {
	    	ReentrantLock lock = m_pidLocks.get( pid );
		    if( lock == null ) {
			    throw new IllegalMonitorStateException( String.format( "Unlock called and no LockAdmin corresponding to the pid: '%s' found in the lockMap", pid ) );
		    }
	
		    if( !lock.hasQueuedThreads() && lock.getHoldCount() == 1) {
		    	m_pidLocks.remove( pid );
	        }
		    lock.unlock();
	    }
    }

    private void getWriteLock(String pid) {
		if( pid == null ) {
		    throw new IllegalArgumentException("pid cannot be null");
		}
	
		ReentrantLock lock = null;
		synchronized(m_pidLocks) {
		    lock = m_pidLocks.get( pid );
		    if( lock == null ) {
			    lock = new ReentrantLock();
			    m_pidLocks.put( pid, lock );
		    }
		}
		lock.lock();
    }

    public ConnectionPool getConnectionPool() {
        return m_connectionPool;
    }

    public DOValidator getDOValidator() {
        return m_validator;
    }

    @Override
    public String[] getRequiredModuleRoles() {
        return new String[] {"org.fcrepo.server.management.PIDGenerator",
                "org.fcrepo.server.search.FieldSearch",
                "org.fcrepo.server.storage.ConnectionPoolManager",
                "org.fcrepo.server.storage.lowlevel.ILowlevelStorage",
                "org.fcrepo.server.storage.ExternalContentManager",
                "org.fcrepo.server.storage.translation.DOTranslator",
                "org.fcrepo.server.validation.DOValidator"};
    }

    public String getStorageFormat() {
        return m_defaultStorageFormat;
    }

    public String getDefaultExportFormat() {
        return m_defaultExportFormat;
    }

    public String getStorageCharacterEncoding() {
        return m_storageCharacterEncoding;
    }

    public DOTranslator getTranslator() {
        return m_translator;
    }
    
    /**
     * Gets a reader on an an existing digital object.
     */
    @Override
    public DOReader getReader(boolean cachedObjectRequired, Context context,
            String pid) throws ServerException {
        long getReaderStartTime = logger.isDebugEnabled() ?
                System.currentTimeMillis() : -1;
        String source = null;
        try {
            {
                DOReader reader = null;
                if (m_readerCache != null) {
                    reader = m_readerCache.get(pid);
                }
                if (reader == null) {
                    reader =
                            new SimpleDOReader(context, this, m_translator,
                                    m_defaultExportFormat,
                                    m_defaultStorageFormat,
                                    m_storageCharacterEncoding,
                                    m_permanentStore.retrieveObject(pid));
                    source = "filesystem";
                    if (m_readerCache != null) {
                        m_readerCache.put(reader, getReaderStartTime);
                    }
                } else {
                    source = "memory";
                }
                return reader;
            }
        } finally {
            if (logger.isDebugEnabled()) {
                long dur = System.currentTimeMillis() - getReaderStartTime;
                logger.debug("Got DOReader (source={}) for {} in {}ms.",
                        source, pid, dur);
            }
        }
    }

    /**
     * Gets a reader on an an existing service deployment object.
     */
    @Override
    public ServiceDeploymentReader getServiceDeploymentReader(
            boolean cachedObjectRequired, Context context, String pid)
            throws ServerException {
        {
            return new SimpleServiceDeploymentReader(context, this,
                    m_translator, m_defaultExportFormat,
                    m_defaultStorageFormat, m_storageCharacterEncoding,
                    m_permanentStore.retrieveObject(pid));
        }
    }

    /**
     * Gets a reader on an an existing service definition object.
     */
    @Override
    public ServiceDefinitionReader getServiceDefinitionReader(
            boolean cachedObjectRequired, Context context, String pid)
            throws ServerException {
        {
            return new SimpleServiceDefinitionReader(context, this,
                    m_translator, m_defaultExportFormat,
                    m_defaultStorageFormat, m_storageCharacterEncoding,
                    m_permanentStore.retrieveObject(pid));
        }
    }

    /**
     * Gets a writer on an an existing object.
     */
    @Override
    public DOWriter getWriter(boolean cachedObjectRequired, Context context,
            String pid) throws ServerException, ObjectLockedException {
        if (cachedObjectRequired) {
            throw new InvalidContextException(
                    "A DOWriter is unavailable in a cached context.");
        } else {
            BasicDigitalObject obj = new BasicDigitalObject();
            m_translator.deserialize(m_permanentStore.retrieveObject(pid), obj,
                    m_defaultStorageFormat, m_storageCharacterEncoding,
                    DOTranslationUtility.DESERIALIZE_INSTANCE);
            DOWriter w =
                    new SimpleDOWriter(context, this, m_translator,
                            m_defaultStorageFormat, m_storageCharacterEncoding,
                            obj);
            getWriteLock(obj.getPid());
            return w;
        }
    }

    /**
     * Manages the INGEST process which includes validation of the ingest XML
     * file, deserialization of the XML into a Digital Object instance, setting
     * of properties on the object by the system (dates and states), PID
     * validation or generation, object registry functions, getting a writer for
     * the digital object, and ultimately writing the object to persistent
     * storage via the writer.
     *
     * @param context
     * @param in
     *            the input stream that is the XML ingest file for a digital
     *            object
     * @param format
     *        the format of the XML ingest file (e.g., FOXML, Fedora METS)
     * @param encoding
     *        the character encoding of the XML ingest file (e.g., UTF-8)
     * @param pid
     *            "new" if the system should generate a new PID for the object,
     *            otherwise the value of the additional pid parameter for
     *            ingests (may be null or any valid pid)
     */
    @Override
    public DOWriter getIngestWriter(boolean cachedObjectRequired,
            Context context, InputStream in, String format, String encoding,
            String pid) throws ServerException {
        logger.debug("Entered getIngestWriter");

        DOWriter w = null;
        BasicDigitalObject obj = null;

        File tempFile = null;
        if (cachedObjectRequired) {
            throw new InvalidContextException(
                    "A DOWriter is unavailable in a cached context.");
        } else {
            try {
                // CURRENT TIME:
                // Get the current time to use for created dates on object
                // and object components (if they are not already there).
                Date nowUTC = Server.getCurrentDate(context);

                // TEMP STORAGE:
                // write ingest input stream to a temporary file
                tempFile = File.createTempFile("fedora-ingest-temp", ".xml");
                logger.debug("Creating temporary file for ingest: {}",
                        tempFile.toString());
                StreamUtility.pipeStream(in, new FileOutputStream(tempFile),
                        4096);

                // VALIDATION:
                // perform initial validation of the ingest submission file
                logger.debug("Validation (ingest phase)");
                m_validator.validate(tempFile, format, m_ingestValidationLevel,
                        DOValidator.PHASE_INGEST);

                // DESERIALIZE:
                // deserialize the ingest input stream into a digital object
                // instance
                obj = new BasicDigitalObject();
                obj.setNew(true);
                logger.debug("Deserializing from format: {}", format);
                m_translator.deserialize(new FileInputStream(tempFile), obj,
                        format, encoding,
                        DOTranslationUtility.DESERIALIZE_INSTANCE);

                // SET OBJECT PROPERTIES:
                logger.debug("Setting object/component states and create dates if unset");
                // set object state to "A" (Active) if not already set
                if (obj.getState() == null || obj.getState().isEmpty()) {
                    obj.setState("A");
                }
                // set object create date to UTC if not already set
                if (obj.getCreateDate() == null) {
                    obj.setCreateDate(nowUTC);
                }
                // set object last modified date to UTC
                obj.setLastModDate(nowUTC);

                // SET DATASTREAM PROPERTIES...
                Iterator<String> dsIter = obj.datastreamIdIterator();
                while (dsIter.hasNext()) {
                    for (Datastream ds : obj.datastreams(dsIter.next())) {
                        // Set create date to UTC if not already set
                        if (ds.DSCreateDT == null) {
                            ds.DSCreateDT = nowUTC;
                        }
                        // Set state to "A" (Active) if not already set
                        if (ds.DSState == null || ds.DSState.isEmpty()) {
                            ds.DSState = "A";
                        }
                        ds.DSChecksumType =
                                Datastream
                                        .validateChecksumType(ds.DSChecksumType);
                    }
                }

                // SET MIMETYPE AND FORMAT_URIS FOR LEGACY OBJECTS' DATASTREAMS
                if (FOXML1_0.uri.equals(format) ||
                        FOXML1_0_LEGACY.equals(format) ||
                        METS_EXT1_0.uri.equals(format) ||
                        METS_EXT1_0_LEGACY.equals(format)) {
                    DigitalObjectUtil.updateLegacyDatastreams(obj);
                }

                // If the PID was supplied as additional parameter (see REST
                // API), make sure it doesn't conflict with the (optional) PID
                // of the digital object
                if (pid != null && pid.length() > 0 && !pid.equals("new")) {
                    if (obj.getPid() != null && obj.getPid().length() > 0) {
                        if (!pid.equals(obj.getPid())) {
                            throw new GeneralException(
                                    "The PID of the digital object and the PID provided as parameter are different. Digital object: " +
                                            obj.getPid() + " parameter: " + pid);
                        }
                    } else {
                        obj.setPid(pid);
                    }

                }
                // PID VALIDATION:
                // validate and normalized the provided pid, if any
                if (obj.getPid() != null && obj.getPid().length() > 0) {
                    obj.setPid(Server.getPID(obj.getPid()).toString());
                }

                // PID GENERATION:
                // have the system generate a PID if one was not provided
                if (obj.getPid() != null &&
                        obj.getPid().indexOf(":") != -1 &&
                        (m_retainPIDs == null || m_retainPIDs.contains(obj
                                .getPid().split(":")[0]))) {
                    logger.debug("Stream contained PID with retainable namespace-id; will use PID from stream");
                    try {
                        m_pidGenerator.neverGeneratePID(obj.getPid());
                    } catch (IOException e) {
                        throw new GeneralException(
                                "Error calling pidGenerator.neverGeneratePID(): " +
                                        e.getMessage());
                    }
                } else {
                    if (pid.equals("new")) {
                        logger.debug("Client wants a new PID");
                        // yes... so do that, then set it in the obj.
                        String p = null;
                        try {
                            // If the context contains a recovery PID, use that.
                            // Otherwise, generate a new PID as usual.
                            if (context instanceof RecoveryContext) {
                                RecoveryContext rContext =
                                        (RecoveryContext) context;
                                p =
                                        rContext.getRecoveryValue(Constants.RECOVERY.PID.attributeId);
                            }
                            if (p == null) {
                                p =
                                        m_pidGenerator.generatePID(
                                                m_pidNamespace).toString();
                            } else {
                                logger.debug("Using new PID from recovery context");
                                m_pidGenerator.neverGeneratePID(p);
                            }
                        } catch (Exception e) {
                            throw new GeneralException("Error generating PID",
                                    e);
                        }
                        logger.info("Generated new PID: {}", p);
                        obj.setPid(p);
                    } else {
                        logger.debug("Client wants to use existing PID.");
                    }
                }

                logger.debug("New object PID is {}", obj.getPid());

                // WRITE LOCK:
                // ensure no one else can modify the object now
                getWriteLock(obj.getPid());

                // CHECK REGISTRY:
                // ensure the object doesn't already exist
                if (objectExists(obj.getPid())) {
                    releaseWriteLock(obj.getPid());
                    throw new ObjectExistsException("The PID '" + obj.getPid() +
                            "' already exists in the registry; the object can't be re-created.");
                }

                // GET DIGITAL OBJECT WRITER:
                // get an object writer configured with the DEFAULT export
                // format
                logger.debug("Getting new writer with default export format: {}",
                        m_defaultExportFormat);
                logger.debug("Instantiating a SimpleDOWriter");
                w =
                        new SimpleDOWriter(context, this, m_translator,
                                m_defaultExportFormat,
                                m_storageCharacterEncoding, obj);

                // DEFAULT DATASTREAMS:
                populateDC(context, obj, w, nowUTC);

                // DATASTREAM VALIDATION
                ValidationUtility.validateReservedDatastreams(w);

                // REGISTRY:
                // at this point the object is valid, so make a record
                // of it in the digital object registry
                registerObject(obj);
                return w;
            } catch (IOException e) {

                if (w != null) {
                    releaseWriteLock(obj.getPid());
                }

                throw new GeneralException("Error reading/writing temporary "
                        + "ingest file", e);
            } catch (Exception e) {

                if (w != null) {
                    releaseWriteLock(obj.getPid());
                }

                if (e instanceof ServerException) {
                    ServerException se = (ServerException) e;
                    throw se;
                }
                throw new GeneralException("Ingest failed: " +
                        e.getClass().getName(), e);
            } finally {
                if (tempFile != null) {
                    logger.debug("Finally, removing temp file");
                    try {
                        tempFile.delete();
                    } catch (Exception e) {
                        // don't worry if it doesn't exist
                    }
                }
            }
        }
    }

    /**
     * Adds a minimal DC datastream if one isn't already present.
     *
     * If there is already a DC datastream, ensure one of the dc:identifier
     * values is the PID of the object.
     */
    private static void populateDC(Context ctx, DigitalObject obj, DOWriter w,
            Date nowUTC) throws IOException, ServerException {
        logger.debug("Adding/Checking default DC datastream");
        Datastream dc = w.GetDatastream("DC", null);
        DCFields dcf;
        XMLDatastreamProcessor dcxml = null;

        if (dc == null) {
            dcxml = new XMLDatastreamProcessor("DC");
            dc = dcxml.getDatastream();
            //dc.DSMDClass=DatastreamXMLMetadata.DESCRIPTIVE;
            dc.DatastreamID = "DC";
            dc.DSVersionID = "DC1.0";
            //dc.DSControlGrp = "X"; set by XMLDatastreamProcessor instead
            dc.DSCreateDT = nowUTC;
            dc.DSLabel = "Dublin Core Record for this object";
            dc.DSMIME = "text/xml";
            dc.DSFormatURI = OAI_DC2_0.uri;
            dc.DSSize = 0;
            dc.DSState = "A";
            dc.DSVersionable = true;
            dcf = new DCFields();
            if (obj.getLabel() != null && !obj.getLabel().isEmpty()) {
                dcf.titles().add(new DCField(obj.getLabel()));
            }
            w.addDatastream(dc, dc.DSVersionable);
        } else {
            dcxml = new XMLDatastreamProcessor(dc);
            // note: context may be required to get through authz as content
            // could be filesystem file (or URL)
            dcf = new DCFields(dc.getContentStream(ctx));
        }
        // set the value of the dc datastream according to what's in the
        // DCFields object
        // ensure one of the dc:identifiers is the pid
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(512);
        PrintWriter out = new PrintWriter(new OutputStreamWriter(bytes, Charset.forName("UTF-8")));
        dcf.getAsXML(obj.getPid(), out);
        out.close();
        dcxml.setXMLContent(bytes.toByteArray());
    }

    /**
     * The doCommit method finalizes an ingest/update/remove of a digital
     * object. The process makes updates the object modified date, stores
     * managed content datastreams, creates the final XML serialization of the
     * digital object, saves the object to persistent storage, updates the
     * object registry, and replicates the object's current version information
     * to the relational db. In the case where it is not a deletion, the session
     * lock (TODO) is released, too. This happens as the result of a
     * writer.commit() call.
     */
    public void doCommit(boolean cachedObjectRequired, Context context,
            DigitalObject obj, String logMessage, boolean remove)
            throws ServerException {

        String pid = obj.getPid();

        // OBJECT REMOVAL...
        if (remove) {
            removeObject(obj, false);

            // OBJECT INGEST (ADD) OR MODIFY...
        } else {
            if (obj.isNew()) {
                logger.info("Committing addition of {}", obj.getPid());
            } else {
                logger.info("Committing modification of {}", obj.getPid());
            }

            // Object validation
            m_objectValidator.validate(context, new SimpleDOReader(null, null,
                    null, null, null, obj));

            try { // for cleanup catch

                // DATASTREAM STORAGE:
                // copy and store any datastreams of type Managed Content
                Iterator<String> dsIDIter = obj.datastreamIdIterator();
                while (dsIDIter.hasNext()) {
                    String dsID = dsIDIter.next();
                    Datastream dStream =
                            obj.datastreams(dsID).iterator().next();
                    String controlGroupType = dStream.DSControlGrp;
                    // if it's managed, we might need to grab content
                    if (controlGroupType.equalsIgnoreCase("M")) {
                        // iterate over all versions of this dsID
                        for (Datastream dmc : obj.datastreams(dsID)) {
                            String internalId =
                                    pid + "+" + dmc.DatastreamID + "+" +
                                            dmc.DSVersionID;
                            // if it's a url, we need to grab content for this
                            // version
                            if (URL_PROTOCOL.matcher(dmc.DSLocation).matches()) {
                                MIMETypedStream mimeTypedStream;
                                if (dmc.DSLocation
                                        .startsWith(DatastreamManagedContent.UPLOADED_SCHEME)) {
                                    mimeTypedStream =
                                            new MIMETypedStream(
                                                    null,
                                                    m_management
                                                            .getTempStream(dmc.DSLocation),
                                                    null, dmc.DSSize);
                                    logger.info("Getting managed datastream from internal uploaded " +
                                            "location: {} for ",
                                            dmc.DSLocation, pid);
                                } else if (dmc.DSLocation
                                        .startsWith(DatastreamManagedContent.COPY_SCHEME)) {
                                    // make a copy of the pre-existing content
                                    mimeTypedStream =
                                            new MIMETypedStream(
                                                    null,
                                                    m_permanentStore.retrieveDatastream(dmc.DSLocation
                                                                    .substring(7)),
                                                    null, dmc.DSSize);
                                } else if (dmc.DSLocation
                                        .startsWith(DatastreamManagedContent.TEMP_SCHEME)) {
                                    File file =
                                            new File(dmc.DSLocation
                                                    .substring(7));
                                    logger.info("Getting base64 decoded datastream spooled from archive for datastream {} ({})",
                                            dsID, pid);
                                    try {
                                        InputStream str =
                                                new FileInputStream(file);
                                        mimeTypedStream =
                                                new MIMETypedStream(dmc.DSMIME,
                                                        str, null, file
                                                                .length());
                                    } catch (FileNotFoundException fnfe) {
                                        logger.error(
                                                "Unable to read temp file created for datastream from archive for " +
                                                        pid + " / " + dsID,
                                                fnfe);
                                        throw new StreamIOException(
                                                "Error reading from temporary file created for binary content for " +
                                                        pid + " / " + dsID);
                                    }
                                } else {
                                    ContentManagerParams params =
                                            new ContentManagerParams(
                                                    DOTranslationUtility
                                                            .makeAbsoluteURLs(dmc.DSLocation
                                                                    .toString()),
                                                    dmc.DSMIME, null, null);
                                    params.setContext(context);
                                    mimeTypedStream =
                                            m_contentManager
                                                    .getExternalContent(params);
                                    logger.info("Getting managed datastream from remote location: {} ({} / {})",
                                            dmc.DSLocation, pid, dsID);
                                }
                                Map<String, String> dsHints =
                                        m_hintProvider
                                                .getHintsForAboutToBeStoredDatastream(
                                                        obj, dmc.DatastreamID);
                                if (obj.isNew()) {
                                    dmc.DSSize =
                                            m_permanentStore.addDatastream(
                                                    internalId, mimeTypedStream
                                                            .getStream(),
                                                    dsHints);
                                } else {
                                    // object already existed...so we may need
                                    // to call
                                    // replace if "add" indicates that it was
                                    // already there
                                    try {
                                        dmc.DSSize =
                                                m_permanentStore.addDatastream(
                                                        internalId,
                                                        mimeTypedStream
                                                                .getStream(),
                                                        dsHints);
                                    } catch (ObjectAlreadyInLowlevelStorageException oailse) {
                                        dmc.DSSize =
                                                m_permanentStore.replaceDatastream(
                                                                internalId,
                                                                mimeTypedStream
                                                                        .getStream(),
                                                                dsHints);
                                    }
                                }
                                if (mimeTypedStream != null) {
                                    mimeTypedStream.close();
                                    if (dmc.DSLocation
                                            .startsWith(DatastreamManagedContent.TEMP_SCHEME)) {
                                        // delete the temp file created to store
                                        // the binary content from archive
                                        File file =
                                                new File(dmc.DSLocation
                                                        .substring(7));
                                        if (file.exists()) {
                                            if (!file.delete()) {
                                                logger.warn("Failed to remove temp file, marked for deletion when VM closes: " +
                                                        file.toString());
                                                file.deleteOnExit();
                                            }
                                        } else {
                                            logger.warn("Cannot delete temp file as it no longer exists: " +
                                                    file.getAbsolutePath());
                                        }
                                    }
                                    // Reset dsLocation in object to new
                                    // internal location.
                                    dmc.DSLocation = internalId;
                                    dmc.DSLocationType =
                                            Datastream.DS_LOCATION_TYPE_INTERNAL;
                                    logger.info("Replaced managed datastream location with internal id: {}",
                                            internalId);
                                }
                            } else if (!internalId.equals(dmc.DSLocation)) {
                                logger.error("Unrecognized DSLocation \"" +
                                        dmc.DSLocation +
                                        "\" given for datastream " +
                                        dmc.DatastreamID + " of object " + pid);
                            }
                        }
                    }
                }

                // MANAGED DATASTREAM PURGE:
                // find out which, if any, managed datastreams were purged,
                // then remove them from low level datastream storage
                // this was moved because in the case of modifying a datastream
                // with versioning turned off, if a modification didn't involve
                // new
                // content a special url of the form copy:... would be used to
                // indicate the content for the new datastream version, which
                // would
                // point to the content of the most recent version. Which (if
                // this code
                // had been executed earlier) would no longer exist in the
                // low-level store.

                if (!obj.isNew()) {
                    deletePurgedDatastreams(obj, context);
                }

                // MODIFIED DATE:
                // set digital object last modified date, in UTC
                obj.setLastModDate(Server.getCurrentDate(context));
                ByteArrayInputStream serialized;

                // block-scoping the ByteArrayOutputStream to ensure toArray
                // is only called once
                // initial capacity is just a guess to prevent copying up from 32 bytes
                {
                    ReadableByteArrayOutputStream out = new ReadableByteArrayOutputStream(4096);

                    // FINAL XML SERIALIZATION:
                    // serialize the object in its final form for persistent storage
                    logger.debug("Serializing digital object for persistent storage {}",
                            pid);
                    m_translator.serialize(obj, out, m_defaultStorageFormat,
                            m_storageCharacterEncoding,
                            DOTranslationUtility.SERIALIZE_STORAGE_INTERNAL);

                    
                    serialized = out.toInputStream();
                }

                // FINAL VALIDATION:
                // As of version 2.0, final validation is only performed in
                // DEBUG mode.
                // This is to help performance during the ingest process since
                // validation
                // is a large amount of the overhead of ingest. Instead of a
                // second run
                // of the validation module, we depend on the integrity of our
                // code to
                // create valid XML files for persistent storage of digital
                // objects. As
                // a sanity check, we check that we can deserialize the object
                // we just serialized
                if (logger.isDebugEnabled()) {
                    logger.debug("Final Validation (storage phase)");
                    m_validator.validate(serialized, m_defaultStorageFormat,
                            DOValidator.VALIDATE_XML_SCHEMA, DOValidator.PHASE_STORE);
                    serialized.reset();
                    m_validator.validate(serialized, m_defaultStorageFormat,
                            DOValidator.VALIDATE_SCHEMATRON, DOValidator.PHASE_STORE);
                    serialized.reset();
                }
                /* Verify that we can deserialize our object. */
                m_translator.deserialize(serialized, new BasicDigitalObject(),
                        m_defaultStorageFormat, m_storageCharacterEncoding,
                        DOTranslationUtility.SERIALIZE_STORAGE_INTERNAL);
                serialized.reset();

                // RESOURCE INDEX:
                if (m_resourceIndex != null &&
                        m_resourceIndex.getIndexLevel() != ResourceIndex.INDEX_LEVEL_OFF) {
                    logger.info("Adding to ResourceIndex");
                    if (obj.isNew()) {
                        m_resourceIndex.addObject(new SimpleDOReader(null,
                                null, null, null, null, obj));
                    } else {
                        m_resourceIndex.modifyObject(getReader(false, null, obj
                                .getPid()), new SimpleDOReader(null, null,
                                null, null, null, obj));

                    }
                    logger.debug("Finished adding {} to ResourceIndex.", pid);
                }

                // STORAGE:
                // write XML serialization of object to persistent storage
                logger.debug("Storing digital object");
                Map<String, String> objectHints =
                        m_hintProvider.getHintsForAboutToBeStoredObject(obj);
                if (obj.isNew()) {
                    m_permanentStore.addObject(obj.getPid(),
                            serialized,
                            objectHints);
                } else {
                    m_permanentStore.replaceObject(obj.getPid(),
                            serialized,
                            objectHints);
                }

                // INVALIDATE DOREADER CACHE:
                // now that the object xml is stored, make sure future DOReaders
                // will get the latest copy
                if (m_readerCache != null) {
                    m_readerCache.remove(pid);
                }

                // REGISTRY:
                /*
                 * update systemVersion in doRegistry (add one), and update
                 * deployment maps if necessary.
                 */
                logger.debug("Updating registry for {}", pid);
                Connection conn = null;
                PreparedStatement s = null;
                ResultSet results = null;
                try {
                    conn = m_connectionPool.getReadWriteConnection();
                    s = conn.prepareStatement(PID_VERSION_QUERY);
                    s.setString(1, obj.getPid());
                    results = s.executeQuery();
                    if (!results.next()) {
                        throw new ObjectNotFoundException(
                                "Error creating replication job: The requested object " +
                                        pid + " doesn't exist in the registry.");
                    }
                    int systemVersion = results.getInt("systemVersion");
                    systemVersion++;
                    s = conn.prepareStatement(PID_VERSION_UPDATE);
                    s.setInt(1, systemVersion);
                    s.setString(2, obj.getPid());
                    s.executeUpdate();

                    //TODO hasModel
                    if (obj.hasContentModel(Models.SERVICE_DEPLOYMENT_3_0)) {
                        updateDeploymentMap(obj, conn, false);
                    }
                } catch (SQLException sqle) {
                    throw new StorageDeviceException(
                            "Error creating replication job for " + pid + ": " +
                                    sqle.getMessage(), sqle);
                } finally {
                    try {
                        if (results != null) {
                            results.close();
                        }
                        if (s != null) {
                            s.close();
                        }
                        if (conn != null) {
                            m_connectionPool.free(conn);
                        }
                    } catch (SQLException sqle) {
                        throw new StorageDeviceException(
                                "Unexpected error from SQL database for " +
                                        pid + ": " + sqle.getMessage(), sqle);
                    } finally {
                        results = null;
                        s = null;
                    }
                }

                // REPLICATE:
                // add to replication jobs table and do replication to db
                logger.info("Updating dissemination index for {}", pid);
                String whichIndex = "FieldSearch";

                try {
                    logger.info("Updating FieldSearch index");
                    m_fieldSearch.update(new SimpleDOReader(null, null, null,
                            null, null, obj));

                    // FIXME: also remove from temp storage if this is
                    // successful
                    //                    removeReplicationJob(obj.getPid());
                } catch (ServerException se) {
                    logger.error("Error updating " + whichIndex +
                            " index for " + pid, se);
                    throw se;
                } catch (Throwable th) {
                    String msg =
                            "Error updating " + whichIndex + " index for " +
                                    pid;
                    logger.error(msg, th);
                    throw new GeneralException(msg, th);
                }
            } catch (Throwable th) {
                if (obj.isNew()) {
                    // Clean up after a failed attempt to add
                    try {
                        removeObject(obj, true);
                    } catch (Exception e) {
                        logger.warn(
                                "Error while cleaning up after failed add for " +
                                        pid, e);
                    }
                }
                if (th instanceof ServerException) {
                    throw (ServerException) th;
                } else {
                    throw new GeneralException(
                            "Unable to add or modify object " + pid +
                                    " (commit canceled)", th);
                }
            }
        }
    }

    /*
     * Remove the object from permanent storage. Currently this is used both for
     * ingest failures
     * and for purging an object. failSafe indicates failure in removal of an
     * underlying artefact
     * is considered safe; ie part of a tidy-up operation where the artefact may
     * not in fact exist.
     * On a purge failSafe should be false so errors are logged, as all
     * artefacts should be present for deletion
     */
    private void removeObject(DigitalObject obj, boolean failSafe)
            throws ServerException {

        final String pid = obj.getPid();
        logger.info("Committing removal of {}", pid);

        // RESOURCE INDEX:
        // remove digital object from the resourceIndex
        // (nb: must happen before datastream storage removal - as
        // relationships might be in managed datastreams)
        if (m_resourceIndex.getIndexLevel() != ResourceIndex.INDEX_LEVEL_OFF) {
            try {
                logger.info("Deleting {} from ResourceIndex", pid);
                m_resourceIndex.deleteObject(new SimpleDOReader(null, null,
                        null, null, null, obj));
                logger.debug("Finished deleting {} from ResourceIndex", pid);
            } catch (ServerException se) {
                if (failSafe) {
                    logger.warn("Object " + pid +
                            " couldn't be removed from ResourceIndex (" +
                            se.getMessage() +
                            "), but that might be ok; continuing with purge");
                } else {
                    logger.error("Object " + pid +
                            " couldn't be removed from ResourceIndex (" +
                            se.getMessage() + ")");

                }

            }
        }

        // DATASTREAM STORAGE:
        // remove any managed content datastreams associated with object
        // from persistent storage.
        Iterator<String> dsIDIter = obj.datastreamIdIterator();
        while (dsIDIter.hasNext()) {
            String dsID = dsIDIter.next();
            String controlGroupType =
                    obj.datastreams(dsID).iterator().next().DSControlGrp;
            if (controlGroupType.equalsIgnoreCase("M")) {
                // iterate over all versions of this dsID
                for (Datastream dmc : obj.datastreams(dsID)) {
                    String id =
                            pid + "+" + dmc.DatastreamID + "+" +
                                    dmc.DSVersionID;
                    logger.info("Deleting managed datastream: {} for {}",
                            id, pid);
                    try {
                        m_permanentStore.removeDatastream(id);
                    } catch (LowlevelStorageException llse) {
                        if (failSafe) {
                            logger.warn(
                                    "Error attempting removal of managed " +
                                            "content datastream " + id +
                                            " for " + pid +
                                            " (but that might be ok during a clean-up): ",
                                    llse);
                        } else {
                            logger.error(
                                    "Error attempting removal of managed " +
                                            "content datastream " + id +
                                            " for " + pid + ": ", llse);
                        }
                    }
                }
            }
        }

        // STORAGE:
        // remove digital object from persistent storage
        try {
            m_permanentStore.removeObject(pid);
        } catch (ObjectNotInLowlevelStorageException onilse) {
            if (failSafe) {
                logger.warn("Object " + pid +
                        " wasn't found in permanent low level " +
                        "store, but that might be ok; continuing with purge");
            } else {
                logger.error("Object " + pid +
                        " wasn't found in permanent low level " + "store");
            }
        }

        // INVALIDATE DOREADER CACHE:
        // now that the object xml is removed, make sure future requests
        // for the object will not use a stale copy
        if (m_readerCache != null) {
            m_readerCache.remove(pid);
        }

        // REGISTRY:
        // Remove digital object from the registry
        try {
            unregisterObject(obj);
        } catch (ServerException se) {
            if (failSafe) {
                logger.warn("Object " +
                        pid +
                        " couldn't be removed from registry, but that might be ok; continuing with purge");
            } else {
                logger.error("Object " + pid +
                        " couldn't be removed from registry");
            }
        }
        // FIELD SEARCH INDEX:
        // remove digital object from the default search index
        try {
            logger.info("Deleting {} from FieldSearch index", pid);
            m_fieldSearch.delete(obj.getPid());
        } catch (ServerException se) {
            if (failSafe) {
                logger.warn("Object " + pid +
                        " couldn't be removed from FieldSearch index (" +
                        se.getMessage() +
                        "), but that might be ok; continuing with purge");
            } else {
                logger.error("Object " + pid +
                        " couldn't be removed from FieldSearch index (" +
                        se.getMessage() + ")");
            }
        }

    }

    private Set<Long> getDatastreamDates(Iterable<Datastream> ds) {
        Set<Long> dates = new HashSet<Long>();
        for (Datastream d : ds) {
            dates.add(d.DSCreateDT.getTime());
        }

        return dates;
    }

    private void deletePurgedDatastreams(DigitalObject obj, Context context) {
        try {
            // for each datastream that existed before the change:
            DOReader reader = getReader(false, context, obj.getPid());
            Datastream[] datastreams = reader.GetDatastreams(null, null);
            for (Datastream element : datastreams) {
                // if it's a managed datastream...
                if (element.DSControlGrp.equals("M")) {
                    String dsID = element.DatastreamID;

                    /*
                     * find out which versions were purged by comparing creation
                     * dates. If a datastream is to be purged, then there won't
                     * be one in the newest version of the object matching its
                     * creation date and dsID.
                     */
                    Set<Long> newVersionDates =
                            getDatastreamDates(obj.datastreams(dsID));
                    Date[] dates = reader.getDatastreamVersions(dsID);
                    for (Date dt : dates) {
                        if (!newVersionDates.contains(dt.getTime())) {
                            // ... and delete them from low level storage
                            String token =
                                    obj.getPid() +
                                            "+" +
                                            dsID +
                                            "+" +
                                            reader.GetDatastream(dsID, dt).DSVersionID;
                            try {
                                m_permanentStore.removeDatastream(token);
                                logger.info("Removed purged datastream version " +
                                        "from low level storage (token = {})",
                                        token);
                            } catch (Exception e) {
                                logger.error(
                                        "Error removing purged datastream " +
                                                "version from low level storage " +
                                                "(token = " + token + ")", e);
                            }
                        }
                    }

                }
            }
        } catch (ServerException e) {
            logger.error("Error reading " + obj.getPid() + "; if any" +
                    " managed datastreams were purged, they were not removed " +
                    " from low level storage.", e);
        }
    }

    /**
     * Checks the object registry for the given object.
     */
    @Override
    public boolean objectExists(String pid) throws StorageDeviceException {
        boolean registered = objectExistsInRegistry(pid);
        boolean exists = false;
        if (!registered && m_checkableStore) {
            try {
                exists = ((ICheckable)m_permanentStore).objectExists(pid);
            } catch (LowlevelStorageException e) {
                throw new StorageDeviceException(e.getMessage(), e);
            }
        }
        if (exists && !registered) {
            logger.warn("{} was not in the registry, but appears to be in store." +
                        " Registry db may be in inconsistent state.", pid);
        }
        return registered || exists;
    }
    
    protected boolean objectExistsInRegistry(String pid)
        throws StorageDeviceException {
        logger.debug("Checking if {} already exists", pid);
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet results = null;
        try {
            conn = m_connectionPool.getReadOnlyConnection();
            s = conn.prepareStatement(REGISTERED_PID_QUERY);
            s.setString(1, pid);
            results = s.executeQuery();
            return results.next(); // 'true' if match found, else 'false'
        } catch (SQLException sqle) {
            throw new StorageDeviceException(
                    "Unexpected error from SQL database: " + sqle.getMessage(),
                    sqle);
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    m_connectionPool.free(conn);
                }
            } catch (SQLException sqle) {
                throw new StorageDeviceException(
                        "Unexpected error from SQL database: " +
                                sqle.getMessage(), sqle);
            } finally {
                results = null;
                s = null;
            }
        }
    }

    /**
     * Adds a new object. The caller *must* ensure the object does not already
     * exist in the registry before calling this method.
     */
    private void registerObject(DigitalObject obj)
            throws StorageDeviceException {
        String theLabel = "the label field is no longer used";
        String ownerID = "the ownerID field is no longer used";
        String pid = obj.getPid();

        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = m_connectionPool.getReadWriteConnection();
            st = conn.prepareStatement(INSERT_PID_QUERY);
            st.setString(1, pid);
            st.setString(2, ownerID);
            st.setString(3, theLabel);
            st.executeUpdate();
        } catch (SQLException sqle) {
            // clean up if the INSERT didn't succeeed
            try {
                unregisterObject(obj);
            } catch (Throwable th) {
            }
            // ...then notify the caller with the original exception
            throw new StorageDeviceException(
                    "Unexpected error from SQL database while registering object: " +
                            sqle.getMessage(), sqle);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (conn != null) {
                    m_connectionPool.free(conn);
                }
            } catch (Exception sqle) {
                throw new StorageDeviceException(
                        "Unexpected error from SQL database while registering object: " +
                                sqle.getMessage(), sqle);
            } finally {
                st = null;
            }
        }
    }

    /**
     * Removes an object from the object registry.
     */
    private void unregisterObject(DigitalObject obj)
            throws StorageDeviceException {
        String pid = obj.getPid();
        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = m_connectionPool.getReadWriteConnection();
            String query = "DELETE FROM doRegistry WHERE doPID=?";
            st = conn.prepareStatement(query);
            st.setString(1, pid);
            st.executeUpdate();

            //TODO hasModel
            if (obj.hasContentModel(Models.SERVICE_DEPLOYMENT_3_0)) {
                updateDeploymentMap(obj, conn, true);
            }
        } catch (SQLException sqle) {
            throw new StorageDeviceException(
                    "Unexpected error from SQL database while unregistering object: " +
                            sqle.getMessage(), sqle);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (conn != null) {
                    m_connectionPool.free(conn);
                }
            } catch (Exception sqle) {
                throw new StorageDeviceException(
                        "Unexpected error from SQL database while unregistering object: " +
                                sqle.getMessage(), sqle);
            } finally {
                st = null;
            }
        }
    }

    @Override
    public String[] listObjectPIDs(Context context)
            throws StorageDeviceException {
        return getPIDs("WHERE systemVersion > 0");
    }

    // translates simple wildcard string to sql-appropriate.
    // the first character is a " " if it needs an escape
    public static String toSql(String name, String in) {
        if (in.indexOf('\\') != -1) {
            // has one or more escapes, un-escape and translate
            StringBuffer out = new StringBuffer();
            out.append('\'');
            boolean needLike = false;
            boolean needEscape = false;
            boolean lastWasEscape = false;
            for (int i = 0; i < in.length(); i++) {
                char c = in.charAt(i);
                if (!lastWasEscape && c == '\\') {
                    lastWasEscape = true;
                } else {
                    char nextChar = '!';
                    boolean useNextChar = false;
                    if (!lastWasEscape) {
                        if (c == '?') {
                            out.append('_');
                            needLike = true;
                        } else if (c == '*') {
                            out.append('%');
                            needLike = true;
                        } else {
                            nextChar = c;
                            useNextChar = true;
                        }
                    } else {
                        nextChar = c;
                        useNextChar = true;
                    }
                    if (useNextChar) {
                        if (nextChar == '\"') {
                            out.append("\\\"");
                            needEscape = true;
                        } else if (nextChar == '\'') {
                            out.append("\\\'");
                            needEscape = true;
                        } else if (nextChar == '%') {
                            out.append("\\%");
                            needEscape = true;
                        } else if (nextChar == '_') {
                            out.append("\\_");
                            needEscape = true;
                        } else {
                            out.append(nextChar);
                        }
                    }
                    lastWasEscape = false;
                }
            }
            out.append('\'');
            if (needLike) {
                out.insert(0, " LIKE ");
            } else {
                out.insert(0, " = ");
            }
            out.insert(0, name);
            if (needEscape) {
                out.insert(0, ' ');
            }
            return out.toString();
        } else {
            // no escapes, just translate if needed
            StringBuffer out = new StringBuffer();
            out.append('\'');
            boolean needLike = false;
            boolean needEscape = false;
            for (int i = 0; i < in.length(); i++) {
                char c = in.charAt(i);
                if (c == '?') {
                    out.append('_');
                    needLike = true;
                } else if (c == '*') {
                    out.append('%');
                    needLike = true;
                } else if (c == '\"') {
                    out.append("\\\"");
                    needEscape = true;
                } else if (c == '\'') {
                    out.append("\\\'");
                    needEscape = true;
                } else if (c == '%') {
                    out.append("\\%");
                    needEscape = true;
                } else if (c == '_') {
                    out.append("\\_");
                    needEscape = true;
                } else {
                    out.append(c);
                }
            }
            out.append('\'');
            if (needLike) {
                out.insert(0, " LIKE ");
            } else {
                out.insert(0, " = ");
            }
            out.insert(0, name);
            if (needEscape) {
                out.insert(0, ' ');
            }
            return out.toString();
        }
    }

    /** whereClause is a WHERE clause, starting with "where" */
    private String[] getPIDs(String whereClause) throws StorageDeviceException {
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet results = null;
        try {
            conn = m_connectionPool.getReadOnlyConnection();
            String query = "SELECT doPID FROM doRegistry " + whereClause;
            s = conn.prepareStatement(query);
            logger.debug("Executing db query: {}", query);
            results = s.executeQuery();
            if (results.next()){
                ArrayList<String> pidList = new ArrayList<String>();
                do {
                    pidList.add(results.getString("doPID"));
                } while (results.next()); 
                return pidList.toArray(STRING_TYPE);
            } else {
                return STRING_TYPE;
            }
        } catch (SQLException sqle) {
            throw new StorageDeviceException(
                    "Unexpected error from SQL database: " + sqle.getMessage(),
                    sqle);

        } finally {
            try {
                if (results != null) {
                    results.close();
                }
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    m_connectionPool.free(conn);
                }
            } catch (SQLException sqle) {
                throw new StorageDeviceException(
                        "Unexpected error from SQL database: " +
                                sqle.getMessage(), sqle);
            } finally {
                results = null;
                s = null;
            }
        }
    }

    @Override
    public FieldSearchResult findObjects(Context context,
            String[] resultFields, int maxResults, FieldSearchQuery query)
            throws ServerException {
        return m_fieldSearch.findObjects(resultFields, maxResults, query);
    }

    @Override
    public FieldSearchResult resumeFindObjects(Context context,
            String sessionToken) throws ServerException {
        return m_fieldSearch.resumeFindObjects(sessionToken);
    }

    /**
     * <p>
     * Gets a list of the requested next available PIDs. the number of PIDs.
     * </p>
     *
     * @param numPIDs
     *            The number of PIDs to generate. Defaults to 1 if the number is
     *            not a positive integer.
     * @param namespace
     *            The namespace to be used when generating the PIDs. If null,
     *            the namespace defined by the <i>pidNamespace</i> parameter in
     *            the fedora.fcfg configuration file is used.
     * @return An array of PIDs.
     * @throws ServerException
     *         If an error occurs in generating the PIDs.
     */
    @Override
    public String[] getNextPID(int numPIDs, String namespace)
            throws ServerException {

        if (numPIDs < 1) {
            numPIDs = 1;
        }
        String[] pidList = new String[numPIDs];
        if (namespace == null || namespace.isEmpty()) {
            namespace = m_pidNamespace;
        }
        try {
            for (int i = 0; i < numPIDs; i++) {
                pidList[i] = m_pidGenerator.generatePID(namespace).toString();
            }
            return pidList;
        } catch (IOException ioe) {
            throw new GeneralException(
                    "DefaultDOManager.getNextPID: Error " +
                            "generating PID, PIDGenerator returned unexpected error: (" +
                            ioe.getClass().getName() + ") - " +
                            ioe.getMessage());
        }
    }

    @Override
    public void reservePIDs(String[] pidList) throws ServerException {

        try {
            for (String element : pidList) {
                m_pidGenerator.neverGeneratePID(element);
            }
        } catch (IOException e) {
            throw new GeneralException("Error reserving PIDs", e);
        }
    }

    @Override
    public String getRepositoryHash() throws ServerException {

        // This implementation returns a string containing the
        // total number of objects in the repository, followed by the
        // latest object's modification date (utc millis)
        // in the format: "10|194861293462"

        Connection conn = null;
        try {
            conn = m_connectionPool.getReadOnlyConnection();

            StringBuffer hash = new StringBuffer();
            hash.append(getNumObjectsWithVersion(conn, 0));
            hash.append("|");

            hash.append(getLatestModificationDate(conn));

            return hash.toString();

        } catch (SQLException e) {
            throw new GeneralException("SQL error encountered while computing "
                    + "repository hash", e);
        } finally {
            if (conn != null) {
                m_connectionPool.free(conn);
            }
        }
    }

    /**
     * Get the number of objects in the registry whose system version is equal
     * to the given value. If n is less than one, return the total number of
     * objects in the registry.
     */
    private int getNumObjectsWithVersion(Connection conn, int n)
            throws SQLException {

        PreparedStatement st = null;
        try {
            String query;
            // Because we are dealing with only two Strings, one of which is fixed,
            // take advantage of String.concat
            if (n > 0) {
                query = "SELECT COUNT(*) FROM doRegistry WHERE systemVersion = ".concat(Integer.toString(n));
            } else {
                query = "SELECT COUNT(*) FROM doRegistry";
            }
            st = conn.prepareStatement(query);
            ResultSet results = st.executeQuery();
            results.next();
            return results.getInt(1);
        } finally {
            if (st != null) {
                st.close();
            }
        }
    }

    private long getLatestModificationDate(Connection conn) throws SQLException {
        PreparedStatement st = null;
        try {
            st = conn.prepareStatement("SELECT MAX(mDate) FROM doFields ");
            ResultSet results = st.executeQuery();
            if (results.next()) {
                return results.getLong(1);
            } else {
                return 0L;
            }
        } finally {
            if (st != null) {
                st.close();
            }
        }
    }

    private class ModelDeploymentMap {

        private final Map<ServiceContext, Map<String, Long>> map =
                new ConcurrentHashMap<ServiceContext, Map<String, Long>>();

        public String putDeployment(ServiceContext cxt, String sDep,
                long lastModDate) {

            if (!map.containsKey(cxt)) {
                map.put(cxt, new HashMap<String, Long>());
            }

            map.get(cxt).put(sDep, lastModDate);

            return getDeployment(cxt);

        }

        /** Removes a deployment from a particular (cModel, sDef) context */
        public String removeDeployment(ServiceContext cxt, String sDep) {

            Map<String, Long> deployments = map.get(cxt);

            if (deployments != null) {
                deployments.remove(sDep);
            }

            return getDeployment(cxt);
        }

        /** Return the OLDEST deployment for a given (cModel, sDef) context */
        public String getDeployment(ServiceContext cxt) {

            if (map.containsKey(cxt)) {
                String sDep = null;
                int count = 0;
                long first = -1;
                for (Map.Entry<String, Long> dep : map.get(cxt).entrySet()) {
                    if (dep.getValue() < first || first < 0) {
                        first = dep.getValue();
                        sDep = dep.getKey();
                        count++;
                    }
                }

                if (count > 1) {
                    logger.info("More than one service deployment specified for sDef {}" +
                            " in model {}.  Using the one with the EARLIEST modification date.",
                            cxt.sDef, cxt.cModel);
                }
                return sDep;
            } else {
                return null;
            }
        }

        /**
         * Return all the (cModel, sDef) contexts a serviceDeployment deploys
         * for
         */
        public Set<ServiceContext> getContextFor(String sDep) {
            Set<ServiceContext> cxt = new HashSet<ServiceContext>();

            for (Entry<ServiceContext, Map<String, Long>> dep : map.entrySet()) {
                if (dep.getValue().keySet().contains(sDep)) {
                    cxt.add(dep.getKey());
                }
            }
            return cxt;
        }
    }

    private static class ServiceContext {

        public final String cModel;

        public final String sDef;

        private static final String VAL_TEMPLATE = "(%1$s,%2$s)";
        /* Internal string value for calculating hash code, equality */
        private final String _val;

        private ServiceContext(String cModelPid, String sDefPid) {
            cModel = cModelPid;
            sDef = sDefPid;

            _val = String.format(VAL_TEMPLATE, cModelPid, sDefPid);
        }

        public static ServiceContext getInstance(String cModel, String sDef) {
            return new ServiceContext(cModel, sDef);
        }

        @Override
        public String toString() {
            return _val;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof ServiceContext)) {
                return false;
            }
            return _val.equals(((ServiceContext) o)._val);
        }

        @Override
        public int hashCode() {
            return _val.hashCode();
        }
    }
}
