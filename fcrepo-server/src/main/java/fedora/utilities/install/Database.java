/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install;

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

import fedora.server.utilities.McKoiDDLConverter;
import fedora.server.utilities.TableCreatingConnection;
import fedora.server.utilities.TableSpec;

import fedora.utilities.DriverShim;
import fedora.utilities.FileUtils;

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
     * Postgresql, which is supported as of Fedora 2.2). McKoi unfortunately
     * doesn't support renaming tables, so we create the new dobj table, copy,
     * and then delete. For reference, the CREATE TABLE command for McKoi should
     * be: CREATE TABLE dobj ( doDbID int(11) default UNIQUEKEY('dobj') NOT
     * NULL, doPID varchar(64) default '' NOT NULL, doLabel varchar(255) default
     * '', doState varchar(1) default 'I' NOT NULL, PRIMARY KEY (doDbID), UNIQUE
     * (doPID) )
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
            ResultSet rs = dmd.getTables(null, null, "%", null);
            while (rs.next()) {
                if (rs.getString("TABLE_NAME").equals("do")) {
                    // McKoi doesn't support RENAME TABLE
                    if (_db.equals(InstallOptions.MCKOI)) {
                        // create table using DDL
                        List<TableSpec> specs =
                                TableSpec.getTableSpecs(_dist
                                        .get(Distribution.DBSPEC));
                        for (TableSpec spec : specs) {
                            if (spec.getName().equals("dobj")) {
                                TableCreatingConnection tcc =
                                        new TableCreatingConnection(conn,
                                                                    new McKoiDDLConverter());
                                tcc.createTable(spec);
                                Statement stmt = conn.createStatement();

                                // copy all from do to dobj
                                stmt
                                        .execute("INSERT INTO dobj SELECT * FROM do;");

                                // delete old table
                                stmt.execute("DROP TABLE do");
                                stmt.close();
                                break;
                            }
                        }
                    } else {
                        Statement stmt = conn.createStatement();
                        stmt.execute("ALTER TABLE do RENAME TO dobj");
                        System.out.println("Renamed table 'do' to 'dobj'.");
                        stmt.close();
                    }
                    break;
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
            } else if (_db.equals(InstallOptions.MCKOI)) {
                is = _dist.get(Distribution.JDBC_MCKOI);
                driver =
                        new File(System.getProperty("java.io.tmpdir"),
                                 Distribution.JDBC_MCKOI);
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

        DatabaseMetaData dmd = conn.getMetaData();
        dmd.getTables(null, null, "%", null);
        System.out.println("Successfully connected to "
                + dmd.getDatabaseProductName());
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
        ResultSet rs = dmd.getTables(null, null, "%", null);
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

    public static void main(String[] args) throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put(InstallOptions.DATABASE, InstallOptions.MCKOI);
        map.put(InstallOptions.DATABASE_DRIVER, InstallOptions.INCLUDED);
        map
                .put(InstallOptions.DATABASE_JDBCURL,
                     "jdbc:mckoi://localhost:9157/");
        map.put(InstallOptions.DATABASE_DRIVERCLASS, "com.mckoi.JDBCDriver");
        map.put(InstallOptions.DATABASE_USERNAME, "fedoraAdmin");
        map.put(InstallOptions.DATABASE_PASSWORD, "fedoraAdmin");

        Distribution dist = new ClassLoaderDistribution();
        InstallOptions opts = new InstallOptions(dist, map);
        Database db = new Database(dist, opts);
        db.test();
        db.close();
    }
}
