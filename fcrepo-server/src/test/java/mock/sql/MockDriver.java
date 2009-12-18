/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package mock.sql;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;

import java.util.Properties;

/**
 * A partial implementation of {@link Driver} for use in unit tests. Add more
 * mocking to this class as needed, or override methods in sub-classes.
 *
 * @author Jim Blake
 */
public class MockDriver
        implements Driver {

    // ----------------------------------------------------------------------
    // Mocking infrastructure
    // ----------------------------------------------------------------------

    public static final String PROTOCOL = "mock";

    // ----------------------------------------------------------------------
    // Mocked methods
    // ----------------------------------------------------------------------

    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith(PROTOCOL + "://");
    }

    public Connection connect(String url, Properties info) throws SQLException {
        return new MockConnection();
    }

    // ----------------------------------------------------------------------
    // Un-implemented methods
    // ----------------------------------------------------------------------

    public int getMajorVersion() {
        throw new RuntimeException("MockDriver.getMajorVersion not implemented");
    }

    public int getMinorVersion() {
        throw new RuntimeException("MockDriver.getMinorVersion not implemented");
    }

    public DriverPropertyInfo[] getPropertyInfo(String arg0, Properties arg1)
            throws SQLException {
        throw new RuntimeException("MockDriver.getPropertyInfo not implemented");
    }

    public boolean jdbcCompliant() {
        throw new RuntimeException("MockDriver.jdbcCompliant not implemented");
    }

}
