/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities.install;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fcrepo.server.utilities.TableCreatingConnection;
import org.fcrepo.server.utilities.TableSpec;
import org.fcrepo.utilities.DriverShim;
import org.fcrepo.utilities.FileUtils;



public class Database {

    private final Distribution _dist;

    private final InstallOptions _opts;

    private final String _db;

    private Connection _conn;

    public Database(Distribution dist, InstallOptions opts) {
        _dist = dist;
        _opts = opts;
        _db = _opts.getValue(InstallOptions.DATABASE);
        _conn = null;
    }

    public void install() throws InstallationFailedException {
        if (_opts.getBooleanValue(InstallOptions.DATABASE_UPDATE, false)) {
            updateDOTable();
        }
    }

    /**
     * Fedora 2.2 renamed the 'do' table to 'dobj' (because 'do' is reserved in
     * Postgresql, which is supported as of Fedora 2.2). 
     *
     * @throws InstallationFailedException
     */
    private void updateDOTable() throws InstallationFailedException {
        if (_db.equals(InstallOptions.INCLUDED)) {
            return; // no need to update embedded
        }
        try {
            Connection conn = getConnection();
            DatabaseMetaData dmd = conn.getMetaData();
            ResultSet rs = dmd.getTables(null, null, "do%", null);
            while (rs.next()) {
                if (rs.getString("TABLE_NAME").equals("do")) {
                    Statement stmt = conn.createStatement();
                    stmt.execute("ALTER TABLE do RENAME TO dobj");
                    System.out.println("Renamed table 'do' to 'dobj'.");
                    stmt.close();
                }
            }
            rs.close();
        } catch (Exception e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }
    }

    protected File getDriver() throws IOException {
        File driver = null;
        if (_opts.getValue(InstallOptions.DATABASE_DRIVER)
                .equals(InstallOptions.INCLUDED)) {
            InputStream is;
            boolean success = false;
            // INCLUDED driver with INCLUDED database, uses embedded driver.
            if (_db.equals(InstallOptions.INCLUDED)) {
                is = _dist.get(Distribution.JDBC_DERBY);
                driver =
                        new File(System.getProperty("java.io.tmpdir"),
                                 Distribution.JDBC_DERBY);
                success = FileUtils.copy(is, new FileOutputStream(driver));
            } // INCLUDED driver with DERBY database, uses network driver.
            else if (_db.equals(InstallOptions.DERBY)) {
                is = _dist.get(Distribution.JDBC_DERBY_NETWORK);
                driver =
                        new File(System.getProperty("java.io.tmpdir"),
                                 Distribution.JDBC_DERBY_NETWORK);
                success = FileUtils.copy(is, new FileOutputStream(driver));
            } else if (_db.equals(InstallOptions.MYSQL)) {
                is = _dist.get(Distribution.JDBC_MYSQL);
                driver =
                        new File(System.getProperty("java.io.tmpdir"),
                                 Distribution.JDBC_MYSQL);
                success = FileUtils.copy(is, new FileOutputStream(driver));
            } else if (_db.equals(InstallOptions.POSTGRESQL)) {
                is = _dist.get(Distribution.JDBC_POSTGRESQL);
                driver =
                        new File(System.getProperty("java.io.tmpdir"),
                                 Distribution.JDBC_POSTGRESQL);
                success = FileUtils.copy(is, new FileOutputStream(driver));
            }
            if (!success) {
                throw new IOException("Extraction of included JDBC driver failed.");
            }
        } else {
            driver = new File(_opts.getValue(InstallOptions.DATABASE_DRIVER));
        }
        return driver;
    }

    /**
     * Simple sanity check of user-supplied database options. Tries to establish
     * a database connection and issue a Connection.getMetaData() using the
     * supplied InstallOptions values for DATABASE_DRIVER, DATABASE_DRIVERCLASS,
     * DATABASE_JDBCURL, DATABASE_USERNAME, and DATABASE_PASSWORD.
     *
     * @throws Exception
     */
    protected void test() throws Exception {
        Connection conn = getConnection();
        if (!conn.isValid(10)) {
            throw new Exception("Unable to connect to database");
        }
        System.out.println("Successfully connected to "
                + conn.getMetaData().getDatabaseProductName());
    }

    /**
     * Determines whether or not the database has a table named "do".
     *
     * @return true if the database contains a table with the name "do".
     * @throws Exception
     */
    protected boolean usesDOTable() throws Exception {
        Connection conn = getConnection();
        DatabaseMetaData dmd = conn.getMetaData();

        // check if we need to update old table
        ResultSet rs = dmd.getTables(null, null, "do%", null);
        while (rs.next()) {
            if (rs.getString("TABLE_NAME").equals("do")) {
                rs.close();
                return true;
            }
        }
        rs.close();
        return false;
    }

    private Connection getConnection() throws Exception {
        if (_conn == null) {
            DriverShim.loadAndRegister(getDriver(), _opts
                    .getValue(InstallOptions.DATABASE_DRIVERCLASS));
            _conn =
                    DriverManager.getConnection(_opts
                            .getValue(InstallOptions.DATABASE_JDBCURL), _opts
                            .getValue(InstallOptions.DATABASE_USERNAME), _opts
                            .getValue(InstallOptions.DATABASE_PASSWORD));
        }
        return _conn;
    }

    /**
     * Closes any underlying connection with the database if necessary.
     *
     * @throws SQLException
     */
    public void close() throws SQLException {
        if (_conn != null) {
            _conn.close();
        }
    }

}
