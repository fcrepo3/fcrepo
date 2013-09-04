/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.fcrepo.server.utilities.DDLConverter;
import org.fcrepo.server.utilities.TableCreatingConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides a dispenser for database Connection Pools.
 *
 * @author Ross Wayland
 * @author Chris Wilper
 */
public class ConnectionPool {

    private static final Logger logger =
            LoggerFactory.getLogger(ConnectionPool.class);

    private DDLConverter ddlConverter;

    private BasicDataSource dataSource;

    private boolean supportsReadOnly = true;

    /**
     * <p>
     * Constructs a ConnectionPool based on the calling arguments.
     * </p>
     *
     * @param driver
     *        The JDBC driver class name.
     * @param url
     *        The JDBC connection URL.
     * @param username
     *        The database user name.
     * @param password
     *        The database password.
     * @param maxActive
     *        Maximum number of active instances in pool.
     * @param maxIdle
     *        Maximum number of idle instances in pool.
     * @param maxWait
     *        Maximum amount of time in milliseconds the borrowObject() method
     *        should wait when whenExhaustedAction is set to
     *        WHEN_EXHAUSTED_BLOCK.
     * @param minIdle
     *        Minimum of idle instances in pool.
     * @param minEvictableIdleTimeMillis
     *        Minimum amount of time in milliseconds an object can be idle in
     *        pool before eligible for eviction (if applicable).
     * @param numTestsPerEvictionRun
     *        Number of objects to be examined on each run of idle evictor
     *        thread (if applicable).
     * @param timeBetweenEvictionRunsMillis
     *        Time in milliseconds to sleep between runs of the idle object
     *        evictor thread.
     * @param validationQuery
     *        Query to run when validation connections, e.g. SELECT 1.
     * @param testOnBorrow
     *        When true objects are validated before borrowed from the pool.
     * @param testOnReturn
     *        When true, objects are validated before returned to hte pool.
     * @param testWhileIdle
     *        When true, objects are validated by the idle object evictor
     *        thread.
     * @param whenExhaustedAction
     *        Action to take when a new object is requested and the the pool has
     *        reached maximum number of active objects.
     * @throws SQLException
     *         If the connection pool cannot be established for any reason.
     */
    public ConnectionPool(String driver,
                          String url,
                          String username,
                          String password,
                          int maxActive,
                          int maxIdle,
                          long maxWait,
                          int minIdle,
                          long minEvictableIdleTimeMillis,
                          int numTestsPerEvictionRun,
                          long timeBetweenEvictionRunsMillis,
                          String validationQuery,
                          boolean testOnBorrow,
                          boolean testOnReturn,
                          boolean testWhileIdle,
                          byte whenExhaustedAction)
            throws SQLException {

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC class not found: " + driver
                    + "; make sure " + "the JDBC driver is in the classpath");
        }

        // // http://jakarta.apache.org/commons/dbcp/configuration.html
        Properties props = new Properties();
        props.setProperty("url", url);
        props.setProperty("username", username);
        props.setProperty("password", password);
        props.setProperty("maxActive", "" + maxActive);
        props.setProperty("maxIdle", "" + maxIdle);
        props.setProperty("maxWait", "" + maxWait);
        props.setProperty("minIdle", "" + minIdle);
        props.setProperty("minEvictableIdleTimeMillis", ""
                + minEvictableIdleTimeMillis);
        props
                .setProperty("numTestsPerEvictionRun", ""
                        + numTestsPerEvictionRun);
        props.setProperty("timeBetweenEvictionRunsMillis", ""
                + timeBetweenEvictionRunsMillis);
        if (validationQuery != null && validationQuery.length() > 0) {
            props.setProperty("validationQuery", validationQuery);
        }
        props.setProperty("testOnBorrow", "" + testOnBorrow);
        props.setProperty("testOnReturn", "" + testOnReturn);
        props.setProperty("testWhileIdle", "" + testWhileIdle);

        if (whenExhaustedAction == 0) {
            // fail (don't wait, just fail)
            props.setProperty("maxWait", "0");
        } else if (whenExhaustedAction == 1) {
            // block (wait indefinitely)
            props.setProperty("maxWait", "-1");
        } else if (whenExhaustedAction == 2) {
            // grow (override the maxActive value with -1, unlimited)
            props.setProperty("maxActive", "-1");
        }

        try {
            dataSource =
                    (BasicDataSource) BasicDataSourceFactory
                            .createDataSource(props);
            dataSource.setDriverClassName(driver);

            // not setting the default:
            // potentially this could mean that exceptions are thrown when the connnections are created
            // where read-only is not supported.  We don't know the behaviour of all drivers in this instance
            // (eg the type of exception thrown)
            // so instead we explicitly setReadOnly() on the connection when (1) get...()ing and (2) free()ing
            // and catch any exceptions there

            // dataSource.setDefaultReadOnly(true);

        } catch (Exception e) {
            throw new SQLException("Error initializing connection pool", e);
        }
    }

    protected void setConnectionProperties(Map<String, String> props) {
        for (String name : props.keySet()) {
            dataSource.addConnectionProperty(name, props.get(name));
        }
    }

    /**
     * Constructs a ConnectionPool that can provide TableCreatingConnections.
     *
     * @param driver
     *        The JDBC driver class name.
     * @param url
     *        The JDBC connection URL.
     * @param username
     *        The database user name.
     * @param password
     *        The he database password.
     * @param ddlConverter
     *        The DDLConverter that the TableCreatingConnections should use when
     *        createTable(TableSpec) is called.
     * @param maxActive
     *        Maximum number of active instances in pool.
     * @param maxIdle
     *        Maximum number of idle instances in pool.
     * @param maxWait
     *        Maximum amount of time in milliseconds the borrowObject() method
     *        should wait when whenExhaustedAction is set to
     *        WHEN_EXHAUSTED_BLOCK.
     * @param minIdle
     *        Minimum of idle instances in pool.
     * @param minEvictableIdleTimeMillis
     *        Minimum amount of time in milliseconds an object can be idle in
     *        pool before eligible for eviction (if applicable).
     * @param numTestsPerEvictionRun
     *        Number of objects to be examined on each run of idle evictor
     *        thread (if applicable).
     * @param timeBetweenEvictionRunsMillis
     *        Time in milliseconds to sleep between runs of the idle object
     *        evictor thread.
     * @param validationQuery
     *        Query to run when validation connections, e.g. SELECT 1.
     * @param testOnBorrow
     *        When true objects are validated before borrowed from the pool.
     * @param testOnReturn
     *        When true, objects are validated before returned to hte pool.
     * @param testWhileIdle
     *        When true, objects are validated by the idle object evictor
     *        thread.
     * @param whenExhaustedAction
     *        Action to take when a new object is requested and the the pool has
     *        reached maximum number of active objects.
     * @throws SQLException
     *         If the connection pool cannot be established for any reason.
     */
    public ConnectionPool(String driver,
                          String url,
                          String username,
                          String password,
                          DDLConverter ddlConverter,
                          int maxActive,
                          int maxIdle,
                          long maxWait,
                          int minIdle,
                          long minEvictableIdleTimeMillis,
                          int numTestsPerEvictionRun,
                          long timeBetweenEvictionRunsMillis,
                          String validationQuery,
                          boolean testOnBorrow,
                          boolean testOnReturn,
                          boolean testWhileIdle,
                          byte whenExhaustedAction)
            throws SQLException {
        this(driver,
             url,
             username,
             password,
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

        this.ddlConverter = ddlConverter;
    }

    /**
     * Gets a TableCreatingConnection.
     * <p>
     * </p>
     * This derives from the same pool, but wraps the Connection in an
     * appropriate TableCreatingConnection before returning it.
     *
     * @return The next available Connection from the pool, wrapped as a
     *         TableCreatingException, or null if this ConnectionPool hasn't
     *         been configured with a DDLConverter (see constructor).
     * @throws SQLException
     *         If there is any propblem in getting the SQL connection.
     */
    public TableCreatingConnection getTableCreatingConnection()
            throws SQLException {
        if (ddlConverter == null) {
            return null;
        } else {
            Connection c = getReadWriteConnection();
            return new TableCreatingConnection(c, ddlConverter);
        }
    }

    /**
     * <p>
     * Gets the next available connection.  Connection is read-only, see
     * getReadWriteConnection() for performing updates
     * </p>
     *
     * @return The next available connection.
     * @throws SQLException
     *         If the maximum number of connections has been reached or there is
     *         some other problem in obtaining the connection.
     */
    public Connection getReadOnlyConnection() throws SQLException {
        try {
            Connection conn = dataSource.getConnection();
            setConnectionReadOnly(conn, true);
            return conn;
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("Got connection from pool (" + toString() + ")");
            }
        }
    }

    /**
     * <p>
     * Gets the next available connection.  Connection is read-write, only
     * use if updates are to be performed, otherwise use getReadOnlyConnection()
     * </p>
     *
     * @return The next available connection.
     * @throws SQLException
     *         If the maximum number of connections has been reached or there is
     *         some other problem in obtaining the connection.
     */
    public Connection getReadWriteConnection() throws SQLException {
        try {
            Connection conn = dataSource.getConnection();
            setConnectionReadOnly(conn, false);
            return conn;
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("Got connection from pool (" + toString() + ")");
            }
        }
    }
    /**
     * <p>
     * Releases the specified connection and returns it to the pool.
     * </p>
     *
     * @param connection
     *        A JDBC connection.
     */
    public void free(Connection connection) {
        try {
            // ensure connections returned to pool as read-only
            setConnectionReadOnly(connection, true);
            if (!connection.isClosed()) connection.close();
        } catch (SQLException sqle) {
            logger.warn("Unable to close connection", sqle);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("Returned connection to pool (" + toString() + ")");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return dataSource.getUsername() + "@" + dataSource.getUrl()
                + ", numIdle=" + dataSource.getNumIdle() + ", numActive="
                + dataSource.getNumActive() + ", maxActive="
                + dataSource.getMaxActive();
    }

    /**
     * <p>
     * Closes the underlying data source
     * </p>
     */
    public void close() {
        try {
            String username = dataSource.getUsername();
            String password = dataSource.getPassword();
            dataSource.close();

            if (isEmbeddedDB()) {
                shutdownEmbeddedDB(username, password);
            }
        } catch (SQLException sqle) {
            logger.warn("Unable to close pool", sqle);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("Closed pool (" + toString() + ")");
            }
        }
    }

    /*
     * Set the read-only state of the connection.  If the connection throws an
     * exception, record this, and don't attempt to change the state in future
     */
    private void setConnectionReadOnly(Connection connection, boolean readOnly) {
        if (supportsReadOnly) {
            try {
                connection.setReadOnly(readOnly);
            } catch (Throwable th) {
                logger.info("Read-only connections not supported ({})", th.getMessage());
                supportsReadOnly = false;
            }
        }

    }

    private boolean isEmbeddedDB() {
        return dataSource.getDriverClassName().equals(
                org.apache.derby.jdbc.EmbeddedDriver.class.getName());
    }

    private void shutdownEmbeddedDB(String username, String password) {
        logger.info("Shutting down embedded derby database.");
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true",
                                        username,
                                        password);
        } catch (SQLException e) {
            // Shutdown throws the XJ015 exception to confirm success.
            if (!e.getSQLState().equals("XJ015")) {
                logger.error("Embedded Derby DB did not shut down normally.");
            }
        }
    }

}
