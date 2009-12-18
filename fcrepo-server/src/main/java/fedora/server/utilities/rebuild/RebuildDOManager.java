/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities.rebuild;

import java.io.IOException;
import java.io.InputStream;

import java.util.Map;

import fedora.server.Server;
import fedora.server.errors.ConnectionPoolNotFoundException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.management.Management;
import fedora.server.management.PIDGenerator;
import fedora.server.resourceIndex.ResourceIndex;
import fedora.server.search.FieldSearch;
import fedora.server.storage.ConnectionPoolManager;
import fedora.server.storage.DefaultDOManager;
import fedora.server.storage.ExternalContentManager;
import fedora.server.storage.lowlevel.ILowlevelStorage;
import fedora.server.storage.translation.DOTranslator;
import fedora.server.utilities.SQLUtility;
import fedora.server.validation.DOValidator;

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
    public RebuildDOManager(Map moduleParameters, Server server, String role)
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
        // get ref to management module
        m_management =
                (Management) getServer()
                        .getModule("fedora.server.management.Management");
        if (m_management == null) {
            //   throw new ModuleInitializationException(
            //           "Management module not loaded.", getRole());
        }
        // get ref to contentmanager module
        m_contentManager =
                (ExternalContentManager) getServer()
                        .getModule("fedora.server.storage.ExternalContentManager");
        if (m_contentManager == null) {
            throw new ModuleInitializationException("ExternalContentManager not loaded.",
                                                    getRole());
        }
        // get ref to fieldsearch module
        m_fieldSearch =
                (FieldSearch) getServer()
                        .getModule("fedora.server.search.FieldSearch");
        // get ref to pidgenerator
        m_pidGenerator =
                (PIDGenerator) getServer()
                        .getModule("fedora.server.management.PIDGenerator");
        // note: permanent and temporary storage handles are lazily instantiated

        // get ref to translator and derive storageFormat default if not given
        m_translator =
                (DOTranslator) getServer()
                        .getModule("fedora.server.storage.translation.DOTranslator");
        //        // get ref to replicator
        //        m_replicator=(DOReplicator) getServer().
        //                getModule("fedora.server.storage.replication.DOReplicator");
        // get ref to digital object validator
        m_validator =
                (DOValidator) getServer()
                        .getModule("fedora.server.validation.DOValidator");
        if (m_validator == null) {
            throw new ModuleInitializationException("DOValidator not loaded.",
                                                    getRole());
        }
        // get ref to ResourceIndex (ok if it's not loaded)
        m_resourceIndex =
                (ResourceIndex) getServer()
                        .getModule("fedora.server.resourceIndex.ResourceIndex");

        // now get the connectionpool
        ConnectionPoolManager cpm =
                (ConnectionPoolManager) getServer()
                        .getModule("fedora.server.storage.ConnectionPoolManager");
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
            throw new ModuleInitializationException("Couldn't get required "
                    + "connection pool...wasn't found", getRole());
        }
        try {
            String dbSpec =
                    "fedora/server/storage/resources/DefaultDOManager.dbspec";
            InputStream specIn =
                    this.getClass().getClassLoader()
                            .getResourceAsStream(dbSpec);
            if (specIn == null) {
                throw new IOException("Cannot find required " + "resource: "
                        + dbSpec);
            }
            SQLUtility.createNonExistingTables(m_connectionPool, specIn);
        } catch (Exception e) {
            throw new ModuleInitializationException("Error while attempting to "
                                                            + "check for and create non-existing table(s): "
                                                            + e.getClass()
                                                                    .getName()
                                                            + ": "
                                                            + e.getMessage(),
                                                    getRole());
        }

        // get ref to lowlevelstorage module
        m_permanentStore =
                (ILowlevelStorage) getServer()
                        .getModule("fedora.server.storage.lowlevel.ILowlevelStorage");
        if (m_permanentStore == null) {
            throw new ModuleInitializationException("LowlevelStorage not loaded",
                                                    getRole());
        }

    }

}
