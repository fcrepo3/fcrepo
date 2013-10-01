/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.search;

import java.util.Map;

import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ConnectionPoolNotFoundException;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.ConnectionPool;
import org.fcrepo.server.storage.ConnectionPoolManager;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DOReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module that wraps FieldSearchSQLImpl.
 *
 * @author Chris Wilper
 */
public class FieldSearchSQLModule
        extends Module
        implements FieldSearch {

    private static final Logger logger =
            LoggerFactory.getLogger(FieldSearchSQLModule.class);

    private FieldSearchSQLImpl m_wrappedFieldSearch;

    public FieldSearchSQLModule(Map<String, String> params, Server server, String role)
            throws ModuleInitializationException {
        super(params, server, role);
    }

    @Override
    public void postInitModule() throws ModuleInitializationException {

        //
        // get and validate maxResults
        //
        if (getParameter("maxResults") == null) {
            throw new ModuleInitializationException("maxResults parameter must be specified.",
                                                    getRole());
        }
        int maxResults = 0;
        try {
            maxResults = Integer.parseInt(getParameter("maxResults"));
            if (maxResults < 1) {
                throw new NumberFormatException("");
            }
        } catch (NumberFormatException nfe) {
            throw new ModuleInitializationException("maxResults must be a positive integer.",
                                                    getRole());
        }

        //
        // get and validate maxSecondsPerSession
        //
        if (getParameter("maxSecondsPerSession") == null) {
            throw new ModuleInitializationException("maxSecondsPerSession parameter must be specified.",
                                                    getRole());
        }
        int maxSecondsPerSession = 0;
        try {
            maxSecondsPerSession =
                    Integer.parseInt(getParameter("maxSecondsPerSession"));
            if (maxSecondsPerSession < 1) {
                throw new NumberFormatException("");
            }
        } catch (NumberFormatException nfe) {
            throw new ModuleInitializationException("maxSecondsPerSession must be a positive integer.",
                                                    getRole());
        }

        //
        // get indexDCFields parameter (default to true if unspecified)
        //
        boolean indexDCFields = true;
        String indexDCFieldsValue = getParameter("indexDCFields");
        if (indexDCFieldsValue != null) {
            String val = indexDCFieldsValue.trim().toLowerCase();
            if (val.equals("false") || val.equals("no")) {
                indexDCFields = false;
            } else if (!val.equals("true") && !val.equals("yes")) {
                throw new ModuleInitializationException("indexDCFields param "
                        + "was not a boolean", getRole());
            }
        }

        //
        // get connectionPool from ConnectionPoolManager
        //
        ConnectionPoolManager cpm =
                (ConnectionPoolManager) getServer()
                        .getModule("org.fcrepo.server.storage.ConnectionPoolManager");
        if (cpm == null) {
            throw new ModuleInitializationException("ConnectionPoolManager module was required, but apparently has "
                                                            + "not been loaded.",
                                                    getRole());
        }
        String cPoolName = getParameter("connectionPool");
        ConnectionPool cPool = null;
        try {
            if (cPoolName == null) {
                logger.debug("connectionPool unspecified; using default from "
                        + "ConnectionPoolManager.");
                cPool = cpm.getPool();
            } else {
                logger.debug("connectionPool specified: " + cPoolName);
                cPool = cpm.getPool(cPoolName);
            }
        } catch (ConnectionPoolNotFoundException cpnfe) {
            throw new ModuleInitializationException("Could not find requested "
                    + "connectionPool.", getRole());
        }
        //
        // get the doManager
        //
        DOManager doManager =
                (DOManager) getServer()
                        .getModule("org.fcrepo.server.storage.DOManager");
        if (doManager == null) {
            throw new ModuleInitializationException("DOManager module was required, but apparently has "
                                                            + "not been loaded.",
                                                    getRole());
        }
        //
        // things look ok...get the wrapped instance
        //
        m_wrappedFieldSearch =
                new FieldSearchSQLImpl(cPool,
                                       doManager,
                                       maxResults,
                                       maxSecondsPerSession,
                                       indexDCFields);
    }

    @Override
    public String[] getRequiredModuleRoles() {
        return new String[] {"org.fcrepo.server.storage.ConnectionPoolManager",
                "org.fcrepo.server.storage.DOManager"};
    }

    public void update(DOReader reader) throws ServerException {
        m_wrappedFieldSearch.update(reader);
    }

    public boolean delete(String pid) throws ServerException {
        return m_wrappedFieldSearch.delete(pid);
    }

    public FieldSearchResult findObjects(String[] resultFields,
                                         int maxResults,
                                         FieldSearchQuery query)
            throws ServerException {
        return m_wrappedFieldSearch
                .findObjects(resultFields, maxResults, query);
    }

    public FieldSearchResult resumeFindObjects(String sessionToken)
            throws ServerException {
        return m_wrappedFieldSearch.resumeFindObjects(sessionToken);
    }

}
