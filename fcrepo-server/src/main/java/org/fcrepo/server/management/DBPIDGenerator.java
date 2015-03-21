/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.management;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.fcrepo.common.MalformedPIDException;
import org.fcrepo.common.PID;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.storage.ConnectionPool;
import org.fcrepo.server.utilities.SQLUtility;

/**
 * A PIDGenerator that uses a database to keep track of the highest pid it knows
 * about for each namespace.
 *
 * @author Chris Wilper
 */
public class DBPIDGenerator
        implements PIDGenerator {

    private static final Logger logger =
            LoggerFactory.getLogger(DBPIDGenerator.class);

    private final HashMap<String, Integer> m_highestID;

    private PID m_lastPID;

    private final ConnectionPool m_connectionPool;

    /**
     * Initialize the DBPIDGenerator. This initializes the memory hash with
     * values in the database, if any. If oldPidGenDir is not null, the
     * constructor will then call neverGeneratePID on the most recently
     * generated PID as reported by the log files in that directory. This is to
     * support automatic upgrade of this functionality from versions of Fedora
     * prior to 1.2.
     * @throws ModuleInitializationException 
     * @throws  
     */
    public DBPIDGenerator(ConnectionPool cPool, File oldPidGenDir)
            throws IOException, ModuleInitializationException {
        m_connectionPool = cPool;
        try {
            String dbSpec =
                    "org/fcrepo/server/storage/resources/DBPIDGenerator.dbspec";
            InputStream specIn =
                    this.getClass().getClassLoader()
                            .getResourceAsStream(dbSpec);
            if (specIn == null) {
                throw new IOException("Cannot find required resource: " +
                        dbSpec);
            }
            SQLUtility.createNonExistingTables(m_connectionPool, specIn);
        } catch (Exception e) {
            throw new ModuleInitializationException(
                "Error while attempting to check for and create non-existing table(s): " +
                    e.getClass().getName() + ": " + e.getMessage(), getRole(), e);
        }
        // load the values from the database into the m_highestID hash
        // pidGen:  namespace  highestID
        Statement s = null;
        ResultSet results = null;
        Connection conn = null;
        try {
            conn = m_connectionPool.getReadOnlyConnection();
            String query = "SELECT namespace, highestID FROM pidGen";
            s = conn.createStatement();
            results = s.executeQuery(query);
            m_highestID = new HashMap<String, Integer>();
            while (results.next()) {
                m_highestID.put(results.getString("namespace"),
                                new Integer(results.getInt("highestID")));
            }
        } catch (SQLException e) {
            throw new ModuleInitializationException(
                    "Error while attempting to load highest existing pids and namespaces: " +
                        e.getClass().getName() + ": " + e.getMessage(), getRole(), e);
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    m_connectionPool.free(conn);
                }
            } catch (SQLException sqle2) {
                logger.warn("Error trying to free db "
                        + "resources in DBPIDGenerator", sqle2);
            } finally {
                results = null;
                s = null;
            }
        }
        upgradeIfNeeded(oldPidGenDir);
    }

    public String getRole() {
        return PIDGenerator.class.getName();
    }

    /**
     * Read the highest value from the old pidGen directory if it exists, and
     * ensure it is never used.
     */
    private void upgradeIfNeeded(File oldPidGenDir) throws IOException {
        if (oldPidGenDir != null && oldPidGenDir.isDirectory()) {
            String[] names = oldPidGenDir.list();
            Arrays.sort(names);
            if (names.length > 0) {
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(new FileInputStream(new File(oldPidGenDir,
                                                                                              names[names.length - 1]))));
                String lastLine = null;
                String line;
                while ((line = in.readLine()) != null) {
                    lastLine = line;
                }
                in.close();
                if (lastLine != null) {
                    String[] parts = lastLine.split("|");
                    if (parts.length == 2) {
                        neverGeneratePID(parts[0]);
                    }
                }
            }
        }
    }

    /**
     * Generate a new pid that is guaranteed to be unique, within the given
     * namespace.
     */
    public synchronized PID generatePID(String namespace) throws IOException {
        int i = getHighestID(namespace);
        i++;

        try {
            m_lastPID = new PID(namespace + ":" + i);
        } catch (MalformedPIDException e) {
            throw new IOException(e.getMessage());
        }

        setHighestID(namespace, i);

        return m_lastPID;
    }

    /**
     * Get the last pid that was generated.
     */
    public synchronized PID getLastPID() {
        return m_lastPID;
    }

    /**
     * Cause the given PID to never be generated by the PID generator.
     */
    public synchronized void neverGeneratePID(String pid) throws IOException {
        logger.debug("Never generating PID: {}", pid);
        try {
            PID p = new PID(pid);
            String ns = p.getNamespaceId();
            int id = Integer.parseInt(p.getObjectId());
            if (id > getHighestID(ns)) {
                setHighestID(ns, id);
            }
        } catch (MalformedPIDException mpe) {
            throw new IOException(mpe.getMessage());
        } catch (NumberFormatException nfe) {
            // if the id part is not numeric, we already know we'll
            // never generate that id because all generated ids are numeric.
        }
    }

    /**
     * Gets the highest id ever used for the given namespace.
     */
    private int getHighestID(String namespace) {
        Integer i = (Integer) m_highestID.get(namespace);
        if (i == null) {
            return 0;
        }
        return i.intValue();
    }

    /**
     * Sets the highest id ever used for the given namespace.
     */
    private void setHighestID(String namespace, int id) throws IOException {
        logger.debug("Setting highest ID for {} to {}", namespace, id);
        m_highestID.put(namespace, new Integer(id));
        // write the new highest id in the database, too
        Connection conn = null;
        try {
            conn = m_connectionPool.getReadWriteConnection();
            SQLUtility.replaceInto(conn,
                                   "pidGen",
                                   new String[] {"namespace", "highestID"},
                                   new String[] {namespace, "" + id},
                                   "namespace",
                                   new boolean[] {false, true});
        } catch (SQLException sqle) {
            throw new IOException("Error setting highest id for "
                    + "namespace in db: " + sqle.getMessage());
        } finally {
            if (conn != null) {
                m_connectionPool.free(conn);
            }
        }
    }

}
