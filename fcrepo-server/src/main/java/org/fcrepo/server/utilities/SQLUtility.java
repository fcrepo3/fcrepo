/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities;

import java.io.IOException;
import java.io.InputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;

import org.fcrepo.server.config.DatastoreConfiguration;
import org.fcrepo.server.config.ModuleConfiguration;
import org.fcrepo.server.config.Parameter;
import org.fcrepo.server.config.ServerConfiguration;
import org.fcrepo.server.errors.InconsistentTableSpecException;
import org.fcrepo.server.storage.ConnectionPool;


/**
 * <p>
 * SQL-related utility methods.
 * </p>
 * <p>
 * <b>Implementation note:</b> Many of these static methods use JDBC objects
 * which are difficult to mock out for unit tests. The methods now delegate to
 * an instance of {@link SQLUtilityImpl} instead. The instance is declared
 * <code>private</code>, but not <code>final</code>, so it can be
 * overridden for unit tests. For example, see
 * <code>TestFieldSearchSQLImpl.java</code>.
 * </p>
 * <p>
 * Some of the methods involve no JDBC objects, and so are not delegated to the
 * instance.
 * </p>
 *
 * @author Chris Wilper
 */
public abstract class SQLUtility {

    public static ConnectionPool getConnectionPool(ServerConfiguration fcfg)
            throws SQLException {
        ModuleConfiguration mcfg =
                fcfg
                        .getModuleConfiguration("org.fcrepo.server.storage.ConnectionPoolManager");
        String defaultPool = mcfg.getParameter("defaultPoolName",Parameter.class).getValue();
        DatastoreConfiguration dcfg =
                fcfg.getDatastoreConfiguration(defaultPool);
        return getConnectionPool(dcfg);
    }

    public static ConnectionPool getConnectionPool(DatastoreConfiguration cpDC)
            throws SQLException {
        return instance.i_getConnectionPool(cpDC);
    }

    public static void replaceInto(Connection conn,
                                   String tableName,
                                   String[] columns,
                                   String[] values,
                                   String uniqueColumn) throws SQLException {
        replaceInto(conn, tableName, columns, values, uniqueColumn, null);
    }

    /**
     * Adds or replaces a row in the given table.
     *
     * @param conn
     *        the connection to use
     * @param table
     *        the name of the table
     * @param columns
     *        the names of the columns whose values we're setting.
     * @param values
     *        associated values
     * @param uniqueColumn
     *        which column name is unique? The value of this column will be used
     *        in the where clause. It must be a column which is not numeric.
     * @param numeric
     *        for each associated column, is it numeric? if null, all columns
     *        are assumed to be strings.
     */
    public static void replaceInto(Connection conn,
                                   String table,
                                   String[] columns,
                                   String[] values,
                                   String uniqueColumn,
                                   boolean[] numeric) throws SQLException {
        instance.i_replaceInto(conn,
                               table,
                               columns,
                               values,
                               uniqueColumn,
                               numeric);
    }

    /**
     * Updates an existing row.
     *
     * @return false if the row did not previously exist and therefore was not
     *         updated.
     */
    public static boolean updateRow(Connection conn,
                                    String table,
                                    String[] columns,
                                    String[] values,
                                    String uniqueColumn,
                                    boolean[] numeric) throws SQLException {
        return instance.i_updateRow(conn,
                                    table,
                                    columns,
                                    values,
                                    uniqueColumn,
                                    numeric);
    }

    /**
     * Adds a new row.
     *
     * @throws SQLException
     *         if the row could not be added.
     */
    public static void addRow(Connection conn,
                              String table,
                              String[] columns,
                              String[] values,
                              boolean[] numeric) throws SQLException {
        instance.i_addRow(conn, table, columns, values, numeric);
    }

    /**
     * Get a long string, which could be a TEXT or CLOB type. (CLOBs require
     * special handling -- this method normalizes the reading of them)
     */
    public static String getLongString(ResultSet rs, int pos)
            throws SQLException {
        return instance.i_getLongString(rs, pos);
    }

    public static void createNonExistingTables(ConnectionPool cPool,
                                               InputStream dbSpec)
            throws IOException, InconsistentTableSpecException, SQLException {
        instance.i_createNonExistingTables(cPool, dbSpec);
    }
    
    public static long getMostRecentRebuild(Connection conn)
            throws SQLException {
        return instance.i_getMostRecentRebuild(conn);
    }
    
    public static boolean getRebuildStatus(Connection conn, long rebuildDate)
            throws SQLException {
        return instance.i_getRebuildStatus(conn, rebuildDate);
    }
    
    public static void recordSuccessfulRebuild(Connection conn, long rebuildDate)
            throws SQLException {
        instance.i_recordSuccessfulRebuild(conn, rebuildDate);
    }

    /**
     * Gets a connection to the database specified in connection pool module's
     * "defaultPoolName" config value. This allows us to the connect to the
     * database without the server running.
     */
    public static Connection getDefaultConnection(ServerConfiguration serverConfig) {
        ModuleConfiguration poolConfig =
                serverConfig
                        .getModuleConfiguration("org.fcrepo.server.storage.ConnectionPoolManager");
        String datastoreID =
                poolConfig.getParameter("defaultPoolName",Parameter.class).getValue();
        DatastoreConfiguration dbConfig =
                serverConfig.getDatastoreConfiguration(datastoreID);
        return getConnection(dbConfig.getParameter("jdbcDriverClass",Parameter.class)
                                     .getValue(),
                             dbConfig.getParameter("jdbcURL",Parameter.class).getValue(),
                             dbConfig.getParameter("dbUsername",Parameter.class).getValue(),
                             dbConfig.getParameter("dbPassword",Parameter.class).getValue());
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

    /*
     * ------------------------------------------------------------------------
     * The instance that handles the JDBC operations, and the method stubs.
     * ------------------------------------------------------------------------
     */

    private static SQLUtility instance = new SQLUtilityImpl();

    protected abstract ConnectionPool i_getConnectionPool(DatastoreConfiguration cpDC)
            throws SQLException;

    protected abstract void i_replaceInto(Connection conn,
                                          String table,
                                          String[] columns,
                                          String[] values,
                                          String uniqueColumn,
                                          boolean[] numeric)
            throws SQLException;

    protected abstract boolean i_updateRow(Connection conn,
                                           String table,
                                           String[] columns,
                                           String[] values,
                                           String uniqueColumn,
                                           boolean[] numeric)
            throws SQLException;

    protected abstract void i_addRow(Connection conn,
                                     String table,
                                     String[] columns,
                                     String[] values,
                                     boolean[] numeric) throws SQLException;

    protected abstract void i_createNonExistingTables(ConnectionPool pool,
                                                      InputStream dbSpec)
            throws IOException, InconsistentTableSpecException, SQLException;

    protected abstract List<TableSpec> i_getNonExistingTables(Connection conn,
                                                              List<TableSpec> specs)
            throws SQLException;

    protected abstract void i_createTables(TableCreatingConnection tcConn,
                                           List<TableSpec> specs)
            throws SQLException;

    protected abstract String i_getLongString(ResultSet rs, int pos)
            throws SQLException;
    
    protected abstract long i_getMostRecentRebuild(Connection conn)
            throws SQLException;

    protected abstract boolean i_getRebuildStatus(Connection conn, long rebuildDate)
            throws SQLException;
    
    protected abstract void i_recordSuccessfulRebuild(Connection conn, long rebuildDate)
            throws SQLException;
}
