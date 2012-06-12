/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.lowlevel.akubra;

import java.io.InputStream;

import java.util.Iterator;
import java.util.Map;

import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.storage.lowlevel.IListable;
import org.fcrepo.server.storage.lowlevel.ILowlevelStorage;
import org.fcrepo.server.storage.lowlevel.ISizable;
import org.springframework.beans.factory.annotation.Required;



/**
 * Wraps a Spring-configured {@link AkubraLowlevelStore} instance as a
 * {@link Module}.
 * <p>
 * To use this module, edit <code>$FEDORA_HOME/config/akubra-llstore.xml</code>
 * as appropriate and replace the existing <code>LowlevelStorage</code>
 * module in <code>fedora.fcfg</code> with the following:
 * <p>
 * <pre>
 * &lt;module role="org.fcrepo.server.storage.lowlevel.ILowlevelStorage"
 *   class="org.fcrepo.server.storage.lowlevel.akubra.AkubraLowlevelStorageModule"/>
 * </pre>
 *
 * @author Chris Wilper
 */
public class AkubraLowlevelStorageModule
        extends Module
        implements ILowlevelStorage, IListable, ISizable {

    private ILowlevelStorage m_impl;

    @Required
    public void setImpl(ILowlevelStorage store) {
        m_impl = store;
    }

    public AkubraLowlevelStorageModule(Map<String, String> moduleParameters,
                                       Server server,
                                       String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }
    
    public void setLLStoreImpl(ILowlevelStorage impl) {
    	m_impl = impl;
    }

    @Override
    public void postInitModule() throws ModuleInitializationException {
        if (m_impl == null) {
            throw new ModuleInitializationException("Error initializing: "
                    + "no ILowlevelStorage impl ", getRole());
        }
    }

    @Override
    public void addObject(String pid, InputStream content, Map<String, String> hints)
            throws LowlevelStorageException {
        m_impl.addObject(pid, content, hints);
    }
    
    @Override
    public void replaceObject(String pid, InputStream content, Map<String, String> hints)
            throws LowlevelStorageException {
        m_impl.replaceObject(pid, content, hints);
    }
    
    public InputStream retrieveObject(String pid)
            throws LowlevelStorageException {
        return m_impl.retrieveObject(pid);
    }

    public void removeObject(String pid) throws LowlevelStorageException {
        m_impl.removeObject(pid);
    }

    public void rebuildObject() throws LowlevelStorageException {
        m_impl.rebuildObject();
    }

    public void auditObject() throws LowlevelStorageException {
        m_impl.auditObject();
    }

    @Override
    public long addDatastream(String pid, InputStream content, Map<String, String> hints)
            throws LowlevelStorageException {
        return m_impl.addDatastream(pid, content, hints);
    }
    
    @Override
    public long replaceDatastream(String pid, InputStream content, Map<String, String> hints)
            throws LowlevelStorageException {
        return m_impl.replaceDatastream(pid, content, hints);
    }
    
    public InputStream retrieveDatastream(String pid)
            throws LowlevelStorageException {
        return m_impl.retrieveDatastream(pid);
    }

    public void removeDatastream(String pid) throws LowlevelStorageException {
        m_impl.removeDatastream(pid);
    }

    public void rebuildDatastream() throws LowlevelStorageException {
        m_impl.rebuildDatastream();
    }

    public void auditDatastream() throws LowlevelStorageException {
        m_impl.auditDatastream();
    }

    // IListable methods

    public Iterator<String> listObjects() {
        return ((IListable) m_impl).listObjects();
    }

    public Iterator<String> listDatastreams() {
        return ((IListable) m_impl).listDatastreams();
    }

    // ISizable methods

    public long getDatastreamSize(String dsKey) throws LowlevelStorageException {
        return ((ISizable) m_impl).getDatastreamSize(dsKey);
    }
}
