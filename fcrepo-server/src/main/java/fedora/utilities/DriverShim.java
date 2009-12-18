/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.io.File;

import java.net.URL;
import java.net.URLClassLoader;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Properties;

/**
 * Allows one to load a JDBC driver at runtime. java.sql.DriverManager will
 * refuse to use a driver not loaded by the system ClassLoader. The workaround
 * for this is to use a shim class that implements java.sql.Driver. This shim
 * class will do nothing but call the methods of an instance of a JDBC driver
 * that is loaded dynamically. This works because DriverShim is loaded by the
 * system class loader, and DriverManager doesn't care that it invokes a class
 * that wasn't. Note that we must perform the registration on the instance
 * ourselves. See the utility method, loadAndRegister and the command-line test
 * below. Adapted from http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
 */
public class DriverShim
        implements Driver {

    /** The JDBC driver we're wrapping. */
    private final Driver _driver;

    /**
     * Constructs a DriverShim over the given driver in order to make it look
     * like it came from this classloader.
     */
    public DriverShim(Driver d) {
        _driver = d;
    }

    /**
     * Loads the driver from the given jar file and registers it with the driver
     * manager.
     */
    public static final void loadAndRegister(File driverJarFile,
                                             String driverClassName)
            throws Exception {
        loadAndRegister(new URL("jar:" + driverJarFile.toURI() + "!/"),
                        driverClassName);
    }

    public static final void loadAndRegister(URL driverURL,
                                             String driverClassName)
            throws Exception {
        URLClassLoader urlCL = new URLClassLoader(new URL[] {driverURL});
        Driver driver =
                (Driver) Class.forName(driverClassName, true, urlCL)
                        .newInstance();
        DriverManager.registerDriver(new DriverShim(driver));
    }

    //
    // Driver implementation
    //

    /**
     * {@inheritDoc}
     */
    public boolean acceptsURL(String u) throws SQLException {
        return _driver.acceptsURL(u);
    }

    /**
     * {@inheritDoc}
     */
    public Connection connect(String u, Properties p) throws SQLException {
        return _driver.connect(u, p);
    }

    /**
     * {@inheritDoc}
     */
    public int getMajorVersion() {
        return _driver.getMajorVersion();
    }

    /**
     * {@inheritDoc}
     */
    public int getMinorVersion() {
        return _driver.getMinorVersion();
    }

    /**
     * {@inheritDoc}
     */
    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p)
            throws SQLException {
        return _driver.getPropertyInfo(u, p);
    }

    /**
     * {@inheritDoc}
     */
    public boolean jdbcCompliant() {
        return _driver.jdbcCompliant();
    }

    /**
     * Command-line test. Dynamically loads the driver given at the command
     * line, opens a connection, and get the row count of the given table to
     * prove it worked. Usage: java -cp . DriverShim someDriver.jar
     * org.SomeDriver jdbcURL name pwd tbl
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 6) {
            DriverShim.loadAndRegister(new File(args[0]), args[1]);
            Connection conn =
                    DriverManager.getConnection(args[2], args[3], args[4]);
            String tbl = args[5];
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tbl);
            rs.next();
            System.out.println("The " + tbl + " table has " + rs.getInt(1)
                    + " rows.");
            rs.close();
            st.close();
            conn.close();
        } else {
            System.out.println("Usage: java -cp . DriverShim someDriver.jar"
                    + " org.SomeDriver jdbcURL name pwd tbl");
            System.exit(1);
        }
    }
}