/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.lowlevel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Map;
import java.util.NoSuchElementException;

import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.LowlevelStorageInconsistencyException;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.storage.ConnectionPool;
import org.fcrepo.server.utilities.SQLUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bill Niebel
 */
public class DBPathRegistry
        extends PathRegistry {

    private static final Logger logger =
            LoggerFactory.getLogger(DBPathRegistry.class);

    private static final String escapedBackslash = "\\\\"; //Java quotes will interpolate these as 2 backslashes

    private ConnectionPool connectionPool = null;

    private final boolean backslashIsEscape;

    public DBPathRegistry(Map<String, ?> configuration) {
        super(configuration);
        connectionPool = (ConnectionPool) configuration.get("connectionPool");
        backslashIsEscape =
                Boolean
                        .valueOf((String) configuration
                                .get("backslashIsEscape")).booleanValue();
    }

    @Override
    public String get(String pid) throws ObjectNotInLowlevelStorageException,
            LowlevelStorageInconsistencyException, LowlevelStorageException {
        String path = null;
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            int paths = 0;
            connection = connectionPool.getReadOnlyConnection();
            String query = "SELECT path FROM " + getRegistryName() + " WHERE token=?";
            statement = connection.prepareStatement(query);
            statement.setString(1,pid);
            rs =
                    statement.executeQuery();
            for (; rs.next(); paths++) {
                path = rs.getString(1);
            }
            if (paths == 0) {
                throw new ObjectNotInLowlevelStorageException("no path in db registry for ["
                        + pid + "]");
            }
            if (paths > 1) {
                throw new LowlevelStorageInconsistencyException("[" + pid
                        + "] in db registry -multiple- times");
            }
            if (path == null || path.length() == 0) {
                throw new LowlevelStorageInconsistencyException("[" + pid
                        + "] has -null- path in db registry");
            }
        } catch (SQLException e1) {
            throw new LowlevelStorageException(true, "sql failure (get)", e1);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connectionPool.free(connection);
                }
            } catch (Exception e2) { // purposely general to include uninstantiated statement, connection
                throw new LowlevelStorageException(true,
                                                   "sql failure closing statement, connection, pool (get)",
                                                   e2);
            } finally {
                rs = null;
                statement = null;
            }
        }
        return path;
    }

    private void ensureSingleUpdate(Statement statement)
        throws ObjectNotInLowlevelStorageException,
        LowlevelStorageInconsistencyException, LowlevelStorageException {
    	try{
    		int updateCount = statement.getUpdateCount();
    		if (updateCount == 0) {
    			throw new ObjectNotInLowlevelStorageException("Object not found in low-level storage: -no- rows updated in db registry");
    		}
    		if (updateCount > 1) {
    			throw new LowlevelStorageInconsistencyException("-multiple- rows updated in db registry");
    		}
        } catch (SQLException e1) {
            throw new LowlevelStorageException(true, "sql failurex (exec)", e1);
        }
    }

    @Deprecated
    public void executeSql(String sql)
            throws ObjectNotInLowlevelStorageException,
            LowlevelStorageInconsistencyException, LowlevelStorageException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = connectionPool.getReadWriteConnection();
            statement = connection.createStatement();
            if (statement.execute(sql)) {
                throw new LowlevelStorageException(true,
                                                   "sql returned query results for a nonquery");
            }
            ensureSingleUpdate(statement);
        } catch (SQLException e1) {
            throw new LowlevelStorageException(true, "sql failurex (exec)", e1);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connectionPool.free(connection);
                }
            } catch (Exception e2) { // purposely general to include uninstantiated statement, connection
                throw new LowlevelStorageException(true,
                                                   "sql failure closing statement, connection, pool (exec)",
                                                   e2);
            } finally {
                statement = null;
            }
        }
    }
    private void executeUpdate(String sql, String pid)
        throws ObjectNotInLowlevelStorageException,
        LowlevelStorageInconsistencyException, LowlevelStorageException {
    	Connection connection = null;
    	PreparedStatement statement = null;
    	try {
    		connection = connectionPool.getReadWriteConnection();
    		statement = connection.prepareStatement(sql);
    		if (pid != null){
    			statement.setString(1,pid);
    		}
    		if (statement.execute()) {
    			throw new LowlevelStorageException(true,
    			"sql returned query results for a nonquery");
    		}
    		ensureSingleUpdate(statement);
    	} catch (SQLException e1) {
    		throw new LowlevelStorageException(true, "sql failurex (exec)", e1);
    	} finally {
    		try {
    			if (statement != null) {
    				statement.close();
    			}
    			if (connection != null) {
    				connectionPool.free(connection);
    			}
    		} catch (Exception e2) { // purposely general to include uninstantiated statement, connection
    			throw new LowlevelStorageException(true,
    					"sql failure closing statement, connection, pool (exec)",
    					e2);
    		} finally {
    			statement = null;
    		}
    	}
    }
    @Override
    public void put(String pid, String path)
            throws ObjectNotInLowlevelStorageException,
            LowlevelStorageInconsistencyException, LowlevelStorageException {
        if (backslashIsEscape) {
            StringBuffer buffer = new StringBuffer();
            /*
             * Escape each backspace so that DB will correctly record a single
             * backspace, instead of incorrectly escaping the following
             * character.
             */
            for (int i = 0; i < path.length(); i++) {
                char s = path.charAt(i);
                buffer.append(s == '\\' ? escapedBackslash : s);
            }
            path = buffer.toString();
        }
        Connection conn = null;
        try {
            conn = connectionPool.getReadWriteConnection();
            SQLUtility.replaceInto(conn, getRegistryName(), new String[] {
                    "token", "path"}, new String[] {pid, path}, "token");
        } catch (SQLException e1) {
            throw new ObjectNotInLowlevelStorageException("put into db registry failed for ["
                                                                  + pid + "]",
                                                          e1);
        } finally {
            if (conn != null) {
                connectionPool.free(conn);
            }
        }
    }

    @Override
    public void remove(String pid) throws ObjectNotInLowlevelStorageException,
            LowlevelStorageInconsistencyException, LowlevelStorageException {
        try {
            String query = "DELETE FROM " + getRegistryName() + " WHERE "
            + getRegistryName() + ".token=?";
            executeUpdate(query, pid);

        } catch (ObjectNotInLowlevelStorageException e1) {
            throw new ObjectNotInLowlevelStorageException("[" + pid
                    + "] not in db registry to delete", e1);
        } catch (LowlevelStorageInconsistencyException e2) {
            throw new LowlevelStorageInconsistencyException("[" + pid
                    + "] deleted from db registry -multiple- times", e2);
        }
    }

    @Override
    public void rebuild() throws LowlevelStorageException {
        int report = FULL_REPORT;
        try {
        	executeUpdate("DELETE FROM " + getRegistryName(), null);
        } catch (ObjectNotInLowlevelStorageException e1) {
        } catch (LowlevelStorageInconsistencyException e2) {
        }
        try {
            logger.info("begin rebuilding registry from files");
            traverseFiles(storeBases, REBUILD, false, report); // continues, ignoring bad files
            logger.info("end rebuilding registry from files (ending normally)");
        } catch (Exception e) {
            if (report != NO_REPORT) {
                logger.error("ending rebuild unsuccessfully", e);
            }
            throw new LowlevelStorageException(true,
                                               "ending rebuild unsuccessfully",
                                               e); //<<====
        }
    }

    @Override
    public void auditFiles() throws LowlevelStorageException {
        logger.info("begin audit: files-against-registry");
        traverseFiles(storeBases, AUDIT_FILES, false, FULL_REPORT);
        logger.info("end audit: files-against-registry (ending normally)");
    }

    @Override
    protected Enumeration<String> keys() throws LowlevelStorageException,
            LowlevelStorageInconsistencyException {
        File tempFile = null;
        PrintWriter writer = null;
        ResultSet rs = null;
        Connection connection = null;
        Statement statement = null;
        try {
            tempFile = File.createTempFile("fedora-keys", ".tmp");
            writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(tempFile)));
            connection = connectionPool.getReadOnlyConnection();
            statement = connection.createStatement();
            rs = statement.executeQuery("SELECT token FROM "
                    + getRegistryName());
            while (rs.next()) {
                String key = rs.getString(1);
                if (null == key || 0 == key.length()) {
                    throw new LowlevelStorageInconsistencyException(
                        "Null token found in " + getRegistryName());
                }
                writer.println(key);
            }
            writer.close();
            return new KeyEnumeration(tempFile);
        } catch (Exception e) {
            throw new LowlevelStorageException(true, "Unexpected error", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connectionPool.free(connection);
                }
            } catch (Exception e) {
                throw new LowlevelStorageException(true, "Unexpected error", e);
            } finally {
                if (writer != null) {
                    writer.close();
                    writer = null;
                }
                rs = null;
                statement = null;
            }
        }
    }

    /**
     * Iterates over each non-empty line in a temporary file.
     * When iteration is complete, or garbage collection occurs, the
     * file will be deleted.
     */
    private class KeyEnumeration
            implements Enumeration<String> {

        private final File file;
        private final BufferedReader reader;

        private boolean closed;
        private String nextKey;

        public KeyEnumeration(File file) throws FileNotFoundException {
            this.file = file;
            this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            setNextKey();
        }

        private void setNextKey() {
            try {
                nextKey = reader.readLine();
                if (nextKey == null) {
                    close();
                } else if (nextKey.length() == 0) {
                    setNextKey();
                }
            } catch (IOException e) {
                throw new Error(e);
            }
        }

        @Override
        public boolean hasMoreElements() {
            return nextKey != null;
        }

        @Override
        public String nextElement() {
            if (nextKey != null) {
                try {
                    return nextKey;
                } finally {
                    setNextKey();
                }
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        protected void finalize() {
            if (!closed) {
                close();
            }
        }

        private void close() {
            try {
                reader.close();
                file.delete();
            } catch (IOException e) {
                throw new Error(e);
            } finally {
                closed = true;
            }
        }

    }
}
