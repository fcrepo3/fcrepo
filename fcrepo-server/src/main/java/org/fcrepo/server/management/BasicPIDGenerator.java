/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.management;

import java.io.File;
import java.io.IOException;

import java.util.Map;

import org.fcrepo.common.PID;
import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.storage.ConnectionPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around the DBPIDGenerator class that casts it as a Module.
 *
 * @author Chris Wilper
 */
public class BasicPIDGenerator
        extends Module
        implements PIDGenerator {

    private static final Logger logger =
            LoggerFactory.getLogger(BasicPIDGenerator.class);

    private DBPIDGenerator m_pidGenerator;

    private File m_oldPidGenDir;

    /**
     * Constructs a BasicPIDGenerator.
     *
     * @param moduleParameters
     *        A pre-loaded Map of name-value pairs comprising the intended
     *        configuration of this Module.
     * @param server
     *        The <code>Server</code> instance.
     * @param role
     *        The role this module fulfills, a java class name.
     * @throws ModuleInitializationException
     *         If initilization values are invalid or initialization fails for
     *         some other reason.
     */
    public BasicPIDGenerator(Map moduleParameters, Server server, String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    @Override
    public void initModule() {
        // this parameter is no longer required; but if it's specified,
        // we can automatically upgrade from a pre-1.2 version of Fedora by
        // making sure the old "last pid generated" value is respected later.
        String dir = getParameter("pidgen_log_dir");
        if (dir != null && !dir.isEmpty()) {
            if (dir.startsWith("/") || dir.startsWith("\\")
                    || dir.substring(1).startsWith(":\\")) {
                m_oldPidGenDir = new File(dir);
            } else {
                m_oldPidGenDir = new File(getServer().getHomeDir(), dir);
            }
        }
    }

    /**
     * Get a reference to the ConnectionPoolManager so we can give the instance
     * constructor a ConnectionPool later in initializeIfNeeded().
     */
    @Override
    public void postInitModule() throws ModuleInitializationException {
        ConnectionPoolManager mgr =
                (ConnectionPoolManager) getServer()
                        .getModule("org.fcrepo.server.storage.ConnectionPoolManager");
        if (mgr == null) {
            throw new ModuleInitializationException("ConnectionPoolManager module not loaded.",
                                                    getRole());
        }
        try {
            m_pidGenerator = new DBPIDGenerator(mgr.getPool(), m_oldPidGenDir);
        } catch (Exception e) {
            String msg = "Can't get default connection pool";
            logger.error(msg, e);
            throw new ModuleInitializationException(msg, getRole());
        }
    }

    public PID generatePID(String namespaceID) throws IOException {
        return m_pidGenerator.generatePID(namespaceID);
    }

    public PID getLastPID() throws IOException {
        return m_pidGenerator.getLastPID();
    }

    public void neverGeneratePID(String pid) throws IOException {
        m_pidGenerator.neverGeneratePID(pid);
    }

}
