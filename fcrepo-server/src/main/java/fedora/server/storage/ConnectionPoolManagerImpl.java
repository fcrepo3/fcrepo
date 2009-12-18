/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import java.sql.SQLException;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;

import fedora.server.DatastoreConfig;
import fedora.server.Module;
import fedora.server.Server;
import fedora.server.errors.ConnectionPoolNotFoundException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ModuleShutdownException;
import fedora.server.utilities.DDLConverter;

/**
 * Implements <code>ConnectionPoolManager</code> to facilitate obtaining
 * database connection pools. This class initializes the connection pools
 * specified by parameters in the Fedora <code>fedora.fcfg</code> configuration
 * file. The Fedora server must be instantiated in order for this class to
 * function properly.
 *
 * @author Ross Wayland
 */
public class ConnectionPoolManagerImpl
        extends Module
        implements ConnectionPoolManager {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(ConnectionPoolManagerImpl.class.getName());

    private static Hashtable<String, ConnectionPool> h_ConnectionPools =
            new Hashtable<String, ConnectionPool>();

    private static String defaultPoolName = null;

    private String jdbcDriverClass = null;

    private String dbUsername = null;

    private String dbPassword = null;

    private String jdbcURL = null;

    private int maxActive = 0;

    private int maxIdle = 0;

    private long maxWait = 0;

    private long minEvictableIdleTimeMillis = 0;

    private int minIdle = 0;

    private int numTestsPerEvictionRun = 0;

    private String validationQuery;

    private boolean testOnBorrow = false;

    private boolean testOnReturn = false;

    private boolean testWhileIdle = false;

    private long timeBetweenEvictionRunsMillis = 0;

    private byte whenExhaustedAction = 0;

    /**
     * <p>
     * Constructs a new ConnectionPoolManagerImpl
     * </p>
     *
     * @param moduleParameters
     *        The name/value pair map of module parameters.
     * @param server
     *        The server instance.
     * @param role
     *        The module role name.
     * @throws ModuleInitializationException
     *         If initialization values are invalid or initialization fails for
     *         some other reason.
     */
    public ConnectionPoolManagerImpl(Map<String, String> moduleParameters,
                                     Server server,
                                     String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    /**
     * Initializes the Module based on configuration parameters. The
     * implementation of this method is dependent on the schema used to define
     * the parameter names for the role of
     * <code>fedora.server.storage.ConnectionPoolManager</code>.
     *
     * @throws ModuleInitializationException
     *         If initialization values are invalid or initialization fails for
     *         some other reason.
     */
    @Override
    public void initModule() throws ModuleInitializationException {
        try {
            Server s_server = getServer();
            defaultPoolName = this.getParameter("defaultPoolName");
            if (defaultPoolName == null || defaultPoolName.equalsIgnoreCase("")) {
                throw new ModuleInitializationException("Default Connection Pool "
                                                                + "Name Not Specified",
                                                        getRole());
            }
            LOG.debug("DefaultPoolName: " + defaultPoolName);
            String poolList = this.getParameter("poolNames");

            // Pool names should be comma delimited
            String[] poolNames = poolList.split(",");

            // Initialize each connection pool
            for (int i = 0; i < poolNames.length; i++) {
                DatastoreConfig config =
                        s_server.getDatastoreConfig(poolNames[i]);
                jdbcDriverClass = config.getParameter("jdbcDriverClass");
                dbUsername = config.getParameter("dbUsername");
                dbPassword = config.getParameter("dbPassword");
                jdbcURL = config.getParameter("jdbcURL");
                maxActive =
                        new Integer(config.getParameter("maxActive"))
                                .intValue();
                maxIdle =
                        new Integer(config.getParameter("maxIdle")).intValue();
                maxWait =
                        new Integer(config.getParameter("maxWait")).intValue();
                minIdle =
                        new Integer(config.getParameter("minIdle")).intValue();
                numTestsPerEvictionRun =
                        new Integer(config
                                .getParameter("numTestsPerEvictionRun"))
                                .intValue();
                minEvictableIdleTimeMillis =
                        new Long(config
                                .getParameter("minEvictableIdleTimeMillis"))
                                .longValue();
                timeBetweenEvictionRunsMillis =
                        new Long(config
                                .getParameter("timeBetweenEvictionRunsMillis"))
                                .longValue();
                validationQuery = config.getParameter("validationQuery");
                testOnBorrow =
                        new Boolean(config.getParameter("testOnBorrow"))
                                .booleanValue();
                testOnReturn =
                        new Boolean(config.getParameter("testOnReturn"))
                                .booleanValue();
                testWhileIdle =
                        new Boolean(config.getParameter("testWhileIdle"))
                                .booleanValue();
                whenExhaustedAction =
                        new Byte(config.getParameter("whenExhaustedAction"))
                                .byteValue();
                if (whenExhaustedAction != 0 && whenExhaustedAction != 1
                        && whenExhaustedAction != 2) {
                    LOG
                            .debug("Valid values for whenExhaustedAction are: 0 - (fail), 1 - (block), or 2 - (grow)");
                    throw new ModuleInitializationException("A connection pool could "
                                                                    + "not be instantiated. The underlying error was an "
                                                                    + "invalid value for the whenExhaustedAction parameter."
                                                                    + "Valid values are 0 - (fail), 1 - (block), or 2 - (grow). Value specified"
                                                                    + "was \""
                                                                    + whenExhaustedAction
                                                                    + "\".",
                                                            getRole());
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("poolName[" + i + "] = " + poolNames[i]);
                    LOG.debug("JDBC driver: " + jdbcDriverClass);
                    LOG.debug("Database username: " + dbUsername);
                    LOG.debug("Database password: " + dbPassword);
                    LOG.debug("JDBC connection URL: " + jdbcURL);
                    LOG.debug("Maximum active connections: " + maxActive);
                    LOG.debug("Maximum idle connections: " + maxIdle);
                    LOG.debug("Maximum wait time: " + maxWait);
                    LOG.debug("Minimum idle time: " + minIdle);
                    LOG.debug("Number of tests per eviction run: "
                            + numTestsPerEvictionRun);
                    LOG.debug("Minimum Evictable Idle time: "
                            + minEvictableIdleTimeMillis);
                    LOG.debug("Minimum Evictable Idle time: "
                            + timeBetweenEvictionRunsMillis);
                    LOG.debug("Validation query: " + validationQuery);
                    LOG.debug("Test on borrow: " + testOnBorrow);
                    LOG.debug("Test on return: " + testOnReturn);
                    LOG.debug("Test while idle: " + testWhileIdle);
                    LOG.debug("whenExhaustedAction: " + whenExhaustedAction);
                }

                // Treat any parameters whose names start with "connection."
                // as connection parameters
                Map<String, String> cProps = new HashMap<String, String>();
                for (String name : config.getParameters().keySet()) {
                    if (name.startsWith("connection.")) {
                        String realName = name.substring(11);
                        LOG.debug("Connection property " + realName + " = "
                                + config.getParameter(name));
                        cProps.put(realName, config.getParameter(name));
                    }
                }

                // If a ddlConverter has been specified for the pool,
                // try to instantiate it so the ConnectionPool can use
                // it when it provides a TableCreatingConnection.
                // If a ddlConverter has been specified, it is assumed
                // that a failure to initialize (construct) it should
                // trigger a ModuleInitializationException (a fatal startup error).
                DDLConverter ddlConverter = null;
                String ddlConverterClassName =
                        getServer().getDatastoreConfig(poolNames[i])
                                .getParameter("ddlConverter");
                if (ddlConverterClassName != null) {
                    try {
                        ddlConverter =
                                (DDLConverter) Class
                                        .forName(ddlConverterClassName)
                                        .newInstance();
                    } catch (Throwable th) {
                        throw new ModuleInitializationException("A DDLConverter was "
                                                                        + "specified for the pool \""
                                                                        + poolNames[i]
                                                                        + "\", but it couldn't be instantiated.",
                                                                getRole(),
                                                                th);
                    }
                }

                // Create connection pool
                try {
                    ConnectionPool connectionPool =
                            new ConnectionPool(jdbcDriverClass,
                                               jdbcURL,
                                               dbUsername,
                                               dbPassword,
                                               ddlConverter,
                                               maxActive,
                                               maxIdle,
                                               maxWait,
                                               minIdle,
                                               minEvictableIdleTimeMillis,
                                               numTestsPerEvictionRun,
                                               timeBetweenEvictionRunsMillis,
                                               validationQuery,
                                               testOnBorrow,
                                               testOnReturn,
                                               testWhileIdle,
                                               whenExhaustedAction);
                    connectionPool.setConnectionProperties(cProps);
                    LOG.debug("Initialized Pool: " + connectionPool);
                    h_ConnectionPools.put(poolNames[i], connectionPool);
                    LOG.debug("putPoolInHash: " + h_ConnectionPools.size());
                } catch (SQLException sqle) {
                    LOG.error("Unable to initialize connection pool: "
                            + poolNames[i] + ": " + sqle.getMessage());
                }
            }

        } catch (Throwable th) {
            th.printStackTrace();
            throw new ModuleInitializationException("A connection pool could "
                    + "not be instantiated. The underlying error was a "
                    + th.getClass().getName() + "The message was \""
                    + th.getMessage() + "\".", getRole());
        }
    }

    /**
     * <p>
     * Gets a named connection pool.
     * </p>
     *
     * @param poolName
     *        The name of the connection pool.
     * @return The named connection pool.
     * @throws ConnectionPoolNotFoundException
     *         If the specified connection pool cannot be found.
     */
    public ConnectionPool getPool(String poolName)
            throws ConnectionPoolNotFoundException {
        ConnectionPool connectionPool = null;

        try {
            if (h_ConnectionPools.containsKey(poolName)) {
                connectionPool = h_ConnectionPools.get(poolName);
            } else {
                // Error: pool was never initialized or name could not be found
                throw new ConnectionPoolNotFoundException("Connection pool "
                        + "not found: " + poolName);
            }
        } catch (Throwable th) {
            throw new ConnectionPoolNotFoundException("The specified connection "
                    + "pool \""
                    + poolName
                    + "\" could not be found. The underlying "
                    + "error was a "
                    + th.getClass().getName()
                    + "The message was \""
                    + th.getMessage() + "\".");
        }

        return connectionPool;
    }

    /**
     * <p>
     * Gets the default Connection Pool. This method overrides <code>
     * getPool(String poolName)</code>
     * .
     * </p>
     *
     * @return The default connection pool.
     * @throws ConnectionPoolNotFoundException
     *         If the default connection pool cannot be found.
     */
    public ConnectionPool getPool() throws ConnectionPoolNotFoundException {
        ConnectionPool connectionPool = null;

        try {
            if (h_ConnectionPools.containsKey(defaultPoolName)) {
                connectionPool = h_ConnectionPools.get(defaultPoolName);
            } else {
                // Error: default pool was never initialized or could not be found
                throw new ConnectionPoolNotFoundException("Default connection pool "
                        + "not found: " + defaultPoolName);
            }

        } catch (Throwable th) {
            throw new ConnectionPoolNotFoundException("The default connection "
                    + "pool \"" + defaultPoolName
                    + "\" could not be found. The " + "underlying error was a "
                    + th.getClass().getName() + "The message was \""
                    + th.getMessage() + "\".");
        }

        return connectionPool;
    }

    /**
     * <p>
     * Closes all connection pools. This method overrides
     * <code> shutdownModule()</code> .
     * </p>
     *
     * @throws ModuleShutdownException
     *         If the close operation for the connection pool(s) fails.
     */
    @Override
    public void shutdownModule() throws ModuleShutdownException {

        for (Map.Entry<String, ConnectionPool> e : h_ConnectionPools.entrySet()) {
            e.getValue().close();
        }

        super.shutdownModule();
    }

}
