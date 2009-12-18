/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import fedora.server.errors.ConnectionPoolNotFoundException;

/**
 * Interface that defines a <code>Module</code> to facilitate the acquisition
 * of JDBC connection pools for database access.
 * 
 * @author Ross Wayland
 */
public interface ConnectionPoolManager {

    /**
     * Gets the specified connection pool.
     * 
     * @param poolName
     *        The name of the specified connection pool.
     * @return The named connection pool.
     * @throws ConnectionPoolNotFoundException
     *         If the specified connection pool cannot be found.
     */
    public ConnectionPool getPool(String poolName)
            throws ConnectionPoolNotFoundException;

    /**
     * Gets the default Connection Pool. Overrides
     * <code>getPool(String poolName)</code> to return the default connection
     * pool when no specific pool name is provided as an argument.
     * 
     * @return The default connection pool.
     * @throws ConnectionPoolNotFoundException
     *         If the default connection pool cannot be found.
     */
    public ConnectionPool getPool() throws ConnectionPoolNotFoundException;
}
