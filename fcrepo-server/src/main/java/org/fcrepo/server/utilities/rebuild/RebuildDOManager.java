/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities.rebuild;

import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ConnectionPoolNotFoundException;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.management.PIDGenerator;
import org.fcrepo.server.search.FieldSearch;
import org.fcrepo.server.storage.ConnectionPoolManager;
import org.fcrepo.server.storage.DefaultDOManager;
import org.fcrepo.server.storage.ExternalContentManager;
import org.fcrepo.server.storage.lowlevel.ILowlevelStorage;
import org.fcrepo.server.storage.translation.DOTranslator;
import org.fcrepo.server.utilities.SQLUtility;
import org.fcrepo.server.validation.DOValidator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;


/**
 * DefaultDOManager subclass for the rebuilder.
 *
 * @author Chris Wilper
 */
public class RebuildDOManager
        extends DefaultDOManager {

    /**
     * @param moduleParameters
     * @param server
     * @param role
     * @throws ModuleInitializationException
     */
    public RebuildDOManager(Map<String, String> moduleParameters, Server server, String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    @Override
    protected void initRetainPID() {
        // retainPIDs (optional, default=demo,test)
        // when m_retainPIDS is set to null, that means "all"
        m_retainPIDs = null;
    }

    @Override
    public void postInitModule() throws ModuleInitializationException {
        // does not use management module
//        m_management =
//                (Management) getServer()
//                        .getModule("org.fcrepo.server.management.Management");

        // get ref to contentmanager module
        m_contentManager =
                (ExternalContentManager) getServer()
                        .getModule("org.fcrepo.server.storage.ExternalContentManager");
        if (m_contentManager == null) {
            throw new ModuleInitializationException("ExternalContentManager not loaded.",
                                                    getRole());
        }
        // get ref to fieldsearch module
        m_fieldSearch =
                (FieldSearch) getServer()
                        .getModule("org.fcrepo.server.search.FieldSearch");
        // get ref to pidgenerator
        m_pidGenerator =
                (PIDGenerator) getServer()
                        .getModule("org.fcrepo.server.management.PIDGenerator");
        // note: permanent and temporary storage handles are lazily instantiated

        // get ref to translator and derive storageFormat default if not given
        m_translator =
                (DOTranslator) getServer()
                        .getModule("org.fcrepo.server.storage.translation.DOTranslator");
        //        // get ref to replicator
        //        m_replicator=(DOReplicator) getServer().
        //                getModule("org.fcrepo.server.storage.replication.DOReplicator");
        // get ref to digital object validator
        m_validator =
                (DOValidator) getServer()
                        .getModule("org.fcrepo.server.validation.DOValidator");
        if (m_validator == null) {
            throw new ModuleInitializationException("DOValidator not loaded.",
                                                    getRole());
        }
        // will not use ref to ResourceIndex (ok if it's not loaded)
//        m_resourceIndex =
//                (ResourceIndex) getServer()
//                        .getModule("org.fcrepo.server.resourceIndex.ResourceIndex");

        // now get the connectionpool
        ConnectionPoolManager cpm =
                (ConnectionPoolManager) getServer()
                        .getModule("org.fcrepo.server.storage.ConnectionPoolManager");
        if (cpm == null) {
            throw new ModuleInitializationException("ConnectionPoolManager not loaded.",
                                                    getRole());
        }
        try {
            if (m_storagePool == null) {
                m_connectionPool = cpm.getPool();
            } else {
                m_connectionPool = cpm.getPool(m_storagePool);
            }
        } catch (ConnectionPoolNotFoundException cpnfe) {
            String storagePool = (m_storagePool == null)?"[null]":m_storagePool;
            throw new ModuleInitializationException("Couldn't get required "
                    + "connection pool " + storagePool + " ...wasn't found", getRole());
        }
        try {
            String dbSpec =
                    "org/fcrepo/server/storage/resources/DefaultDOManager.dbspec";
            InputStream specIn =
                    this.getClass().getClassLoader()
                            .getResourceAsStream(dbSpec);
            if (specIn == null) {
                throw new IOException("Cannot find required " + "resource: "
                        + dbSpec);
            }
            SQLUtility.createNonExistingTables(m_connectionPool, specIn);
        } catch (Exception e) {
            throw new ModuleInitializationException("Error checking for and "
                    + "creating non-existing tables", getRole(), e);
        }

        // get ref to lowlevelstorage module
        m_permanentStore =
                (ILowlevelStorage) getServer()
                        .getModule("org.fcrepo.server.storage.lowlevel.ILowlevelStorage");
        if (m_permanentStore == null) {
            throw new ModuleInitializationException("LowlevelStorage not loaded",
                                                    getRole());
        }

    }

}
