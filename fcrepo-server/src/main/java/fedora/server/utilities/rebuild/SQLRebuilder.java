/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities.rebuild;

import fedora.common.Constants;
import fedora.common.Models;
import fedora.server.Context;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.config.DatastoreConfiguration;
import fedora.server.config.ModuleConfiguration;
import fedora.server.config.ServerConfiguration;
import fedora.server.errors.InitializationException;
import fedora.server.errors.LowlevelStorageException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ObjectNotFoundException;
import fedora.server.errors.ServerException;
import fedora.server.errors.StorageDeviceException;
import fedora.server.management.PIDGenerator;
import fedora.server.search.FieldSearch;
import fedora.server.storage.ConnectionPool;
import fedora.server.storage.ConnectionPoolManager;
import fedora.server.storage.DOManager;
import fedora.server.storage.DOReader;
import fedora.server.storage.DOWriter;
import fedora.server.storage.lowlevel.ILowlevelStorage;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.RelationshipTuple;
import fedora.server.utilities.SQLUtility;
import fedora.server.utilities.TableSpec;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Rebuilder for the SQL database.
 */
public class SQLRebuilder
        implements Rebuilder {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(Rebuilder.class.getName());

    private ServerConfiguration m_serverConfig;

    private Server m_server;

    private ConnectionPool m_connectionPool;

    private Context m_context;

    /**
     * Get a short phrase describing what the user can do with this rebuilder.
     */
    public String getAction() {
        return "Rebuild SQL database.";
    }

    /**
     * Returns true is the server _must_ be shut down for this rebuilder to
     * safely operate.
     */
    public boolean shouldStopServer() {
        return true;
    }

    /**
     * Initialize the rebuilder, given the server configuration.
     *
     * @returns a map of option names to plaintext descriptions.
     */
    public Map<String, String> init(File serverDir,
                                    ServerConfiguration serverConfig) {
        m_serverConfig = serverConfig;
        Map<String, String> m = new HashMap<String, String>();
        return m;
    }

    /**
     * Validate the provided options and perform any necessary startup tasks.
     */
    public void start(Map<String, String> options) throws Exception {
        // This must be done before starting "RebuildServer"
        // rather than after, so any application caches
        // (in particular the hash map held by PIDGenerator)
        // don't get out of sync with the database.
        blankExistingTables();

        try {
            m_server = Rebuild.getServer();
            // now get the connectionpool
            ConnectionPoolManager cpm =
                    (ConnectionPoolManager) m_server
                            .getModule("fedora.server.storage.ConnectionPoolManager");
            if (cpm == null) {
                throw new ModuleInitializationException("ConnectionPoolManager not loaded.",
                                                        "ConnectionPoolManager");
            }
            m_connectionPool = cpm.getPool();
            m_context =
                    ReadOnlyContext.getContext("utility", "fedoraAdmin", "", /* null, */
                    ReadOnlyContext.DO_OP);
            String registryClassTemp = m_server.getParameter("registry");
            ILowlevelStorage llstore =
                    (ILowlevelStorage) m_server
                            .getModule("fedora.server.storage.lowlevel.ILowlevelStorage");
            try {
                llstore.rebuildObject();
                llstore.rebuildDatastream();
            } catch (LowlevelStorageException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } catch (InitializationException ie) {
            LOG.error("Error initializing", ie);
            throw ie;
        }
    }

    public static List<String> getExistingTables(Connection conn)
            throws SQLException {

        ArrayList<String> existing = new ArrayList<String>();
        DatabaseMetaData dbMeta = conn.getMetaData();
        ResultSet r = null;
        // Get a list of tables that don't exist, if any
        try {
            r = dbMeta.getTables(null, null, "%", null);
            while (r.next()) {
                existing.add(r.getString("TABLE_NAME"));
            }
            r.close();
            r = null;
        } catch (SQLException sqle) {
            throw new SQLException(sqle.getMessage());
        } finally {
            try {
                if (r != null) {
                    r.close();
                }
            } catch (SQLException sqle2) {
                throw sqle2;
            } finally {
                r = null;
            }
        }
        return existing;
    }

    /**
     * Delete all rows from all Fedora-related tables (except the resource index
     * ones) that exist in the database.
     */
    private void blankExistingTables() {
        Connection connection = null;
        try {
            connection = getDefaultConnection();
            List<String> existingTables = getExistingTables(connection);
            List<String> fedoraTables = getFedoraTables();
            for (int i = 0; i < existingTables.size(); i++) {
                String origTableName = existingTables.get(i);
                String tableName = origTableName.toUpperCase();
                if (fedoraTables.contains(tableName)
                        && !tableName.startsWith("RI")) {
                    System.out.println("Cleaning up table: " + origTableName);
                    try {
                        executeSql(connection, "DELETE FROM " + origTableName);
                    } catch (LowlevelStorageException lle) {
                        System.err.println(lle.getMessage());
                        System.err.flush();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error while blanking existing tables",
                                       e);
        } finally {
            try {
                connection.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Get the names of all Fedora tables listed in the server's dbSpec file.
     * Names will be returned in ALL CAPS so that case-insensitive comparisons
     * can be done.
     */
    private List<String> getFedoraTables() {
        try {
            String dbSpecLocation =
                    "fedora/server/storage/resources/DefaultDOManager.dbspec";
            InputStream in =
                    getClass().getClassLoader()
                            .getResourceAsStream(dbSpecLocation);
            List<TableSpec> specs = TableSpec.getTableSpecs(in);
            ArrayList<String> names = new ArrayList<String>();
            for (int i = 0; i < specs.size(); i++) {
                TableSpec spec = specs.get(i);
                names.add(spec.getName().toUpperCase());
            }
            return names;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unexpected error reading dbspec file",
                                       e);
        }
    }

    public void executeSql(Connection connection, String sql)
            throws LowlevelStorageException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            if (statement.execute(sql)) {
                throw new LowlevelStorageException(true,
                                                   "sql returned query results for a nonquery");
            }
            int updateCount = statement.getUpdateCount();
        } catch (SQLException e1) {
            throw new LowlevelStorageException(true, "sql failurex (exec)", e1);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e2) { // purposely general to include
                // uninstantiated statement, connection
                throw new LowlevelStorageException(true,
                                                   "sql failure closing statement, connection, pool (exec)",
                                                   e2);
            } finally {
                statement = null;
            }
        }
    }

    /**
     * Add the data of interest for the given object.
     */
    public void addObject(DigitalObject obj) {
        // CURRENT TIME:
        // Get the current time to use for created dates on object
        // and object components (if they are not already there).
        Date nowUTC = new Date();

        // DOReplicator replicator=(DOReplicator)
        // m_server.getModule("fedora.server.storage.replication.DOReplicator");
        DOManager manager =
                (DOManager) m_server
                        .getModule("fedora.server.storage.DOManager");
        FieldSearch fieldSearch =
                (FieldSearch) m_server
                        .getModule("fedora.server.search.FieldSearch");
        PIDGenerator pidGenerator =
                (PIDGenerator) m_server
                        .getModule("fedora.server.management.PIDGenerator");

        // SET OBJECT PROPERTIES:
        LOG
                .debug("Rebuild: Setting object/component states and create dates if unset...");
        // set object state to "A" (Active) if not already set
        if (obj.getState() == null || obj.getState().equals("")) {
            obj.setState("A");
        }
        // set object create date to UTC if not already set
        if (obj.getCreateDate() == null || obj.getCreateDate().equals("")) {
            obj.setCreateDate(nowUTC);
        }
        // set object last modified date to UTC
        obj.setLastModDate(nowUTC);

        // SET OBJECT PROPERTIES:
        LOG
                .debug("Rebuild: Setting object/component states and create dates if unset...");
        // set object state to "A" (Active) if not already set
        if (obj.getState() == null || obj.getState().equals("")) {
            obj.setState("A");
        }
        // set object create date to UTC if not already set
        if (obj.getCreateDate() == null || obj.getCreateDate().equals("")) {
            obj.setCreateDate(nowUTC);
        }
        // set object last modified date to UTC
        obj.setLastModDate(nowUTC);

        // SET DATASTREAM PROPERTIES...
        Iterator<String> dsIter = obj.datastreamIdIterator();
        while (dsIter.hasNext()) {
            for (Datastream ds : obj.datastreams(dsIter.next())) {
                // Set create date to UTC if not already set
                if (ds.DSCreateDT == null || ds.DSCreateDT.equals("")) {
                    ds.DSCreateDT = nowUTC;
                }
                // Set state to "A" (Active) if not already set
                if (ds.DSState == null || ds.DSState.equals("")) {
                    ds.DSState = "A";
                }
            }
        }

        // GET DIGITAL OBJECT WRITER:
        // get an object writer configured with the DEFAULT export format
        LOG.debug("INGEST: Instantiating a SimpleDOWriter...");
        try {
            DOWriter w =
                    manager.getWriter(Server.USE_DEFINITIVE_STORE,
                                      m_context,
                                      obj.getPid());
        } catch (ServerException se) {
        }

        // PID GENERATION:
        // have the system generate a PID if one was not provided
        LOG
                .debug("INGEST: Stream contained PID with retainable namespace-id... will use PID from stream.");
        try {
            pidGenerator.neverGeneratePID(obj.getPid());
        } catch (IOException e) {
            throw new RuntimeException("Error calling pidGenerator.neverGeneratePID(): "
                                               + e.getMessage(),
                                       e);
        }

        // REGISTRY:
        // at this point the object is valid, so make a record
        // of it in the digital object registry
        try {
            registerObject(obj);
        } catch (StorageDeviceException e) {
        }

        try {
            LOG.info("COMMIT: Attempting replication: " + obj.getPid());
            DOReader reader =
                    manager.getReader(Server.USE_DEFINITIVE_STORE,
                                      m_context,
                                      obj.getPid());
            LOG.info("COMMIT: Updating FieldSearch indexes...");
            fieldSearch.update(reader);

        } catch (ServerException se) {
            System.out.println("Error while replicating: "
                    + se.getClass().getName() + ": " + se.getMessage());
            se.printStackTrace();
        } catch (Throwable th) {
            System.out.println("Error while replicating: "
                    + th.getClass().getName() + ": " + th.getMessage());
            th.printStackTrace();
        }
    }

    /**
     * Adds a new object.
     */
    private void registerObject(DigitalObject obj)
            throws StorageDeviceException {
        String pid = obj.getPid();
        String userId = obj.getOwnerId();
        String label = obj.getLabel();

        // label or contentModelId may be null...set to blank if so
        String theLabel = label;
        if (theLabel == null) {
            theLabel = "";
        }
        Connection conn = null;
        Statement s1 = null;
        try {
            String query =
                    "INSERT INTO doRegistry (doPID, " + "ownerId, label) "
                            + "VALUES ('" + pid + "', '" + userId + "', '"
                            + SQLUtility.aposEscape(theLabel) + "')";
            conn = m_connectionPool.getConnection();
            s1 = conn.createStatement();
            s1.executeUpdate(query);

            if (obj.hasContentModel(Models.SERVICE_DEPLOYMENT_3_0)){
                updateDeploymentMap(obj, conn);
            }
        } catch (SQLException sqle) {
            throw new StorageDeviceException("Unexpected error from SQL database while registering object: "
                    + sqle.getMessage());
        } finally {
            try {
                if (s1 != null) {
                    s1.close();
                }
            } catch (Exception sqle) {
                throw new StorageDeviceException("Unexpected error from SQL database while registering object: "
                        + sqle.getMessage());
            } finally {
                s1 = null;
            }
        }

        Statement s2 = null;
        ResultSet results = null;
        try {
            // REGISTRY:
            // update systemVersion in doRegistry (add one)
            LOG.debug("COMMIT: Updating registry...");
            String query =
                    "SELECT systemVersion " + "FROM doRegistry "
                            + "WHERE doPID='" + pid + "'";
            s2 = conn.createStatement();
            results = s2.executeQuery(query);
            if (!results.next()) {
                throw new ObjectNotFoundException("Error creating replication job: The requested object doesn't exist in the registry.");
            }
            int systemVersion = results.getInt("systemVersion");
            systemVersion++;
            s2.executeUpdate("UPDATE doRegistry SET systemVersion="
                    + systemVersion + " " + "WHERE doPID='" + pid + "'");
        } catch (SQLException sqle) {
            throw new StorageDeviceException("Error creating replication job: "
                    + sqle.getMessage());
        } catch (ObjectNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
                if (s2 != null) {
                    s2.close();
                }
                if (conn != null) {
                    m_connectionPool.free(conn);
                }
            } catch (SQLException sqle) {
                throw new StorageDeviceException("Unexpected error from SQL database: "
                        + sqle.getMessage());
            } finally {
                results = null;
                s2 = null;
            }
        }
    }

    /**
     * Free up any system resources associated with rebuilding.
     */
    public void finish() {
        // nothing to do
    }

    /**
     * Gets a connection to the database specified in connection pool module's
     * "defaultPoolName" config value. This allows us to the connect to the
     * database without the server running.
     */
    private Connection getDefaultConnection() {
        ModuleConfiguration poolConfig =
                m_serverConfig
                        .getModuleConfiguration("fedora.server.storage.ConnectionPoolManager");
        String datastoreID =
                poolConfig.getParameter("defaultPoolName").getValue();
        DatastoreConfiguration dbConfig =
                m_serverConfig.getDatastoreConfiguration(datastoreID);
        return getConnection(dbConfig.getParameter("jdbcDriverClass")
                                     .getValue(),
                             dbConfig.getParameter("jdbcURL").getValue(),
                             dbConfig.getParameter("dbUsername").getValue(),
                             dbConfig.getParameter("dbPassword").getValue());
    }

    private static Connection getConnection(String driverClass,
                                            String url,
                                            String username,
                                            String password) {
        try {
            Class.forName(driverClass);
            return DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            throw new RuntimeException("Error getting database connection", e);
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
                                                  Connection c)
            throws SQLException {

        Set<RelationshipTuple> sDefs =
                obj.getRelationships(Constants.MODEL.IS_DEPLOYMENT_OF, null);
        Set<RelationshipTuple> models =
                obj.getRelationships(Constants.MODEL.IS_CONTRACTOR_OF, null);

        for (RelationshipTuple sDefTuple : sDefs) {
            String sDef = sDefTuple.getObjectPID();
            for (RelationshipTuple cModelTuple : models) {
                String cModel = cModelTuple.getObjectPID();
                addDeployment(cModel, sDef, obj, c);
            }
        }
    }

    private void addDeployment(String cModel,
                               String sDef,
                               DigitalObject sDep,
                               Connection c) throws SQLException {

        Statement s = c.createStatement();

        try {
            s
                    .executeUpdate("INSERT INTO modelDeploymentMap (cModel, sDef, sDep) VALUES ('"
                            + cModel
                            + "' , '"
                            + sDef
                            + "', '"
                            + sDep.getPid()
                            + "')");
        } finally {
            if (s != null) {
                s.close();
            }
        }
    }
}
