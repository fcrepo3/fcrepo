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

    private ILowlevelStorage impl;

    @Required
    public void setImpl(ILowlevelStorage store) {
        impl = store;
    }

    public AkubraLowlevelStorageModule(Map<String, String> moduleParameters,
                                       Server server,
                                       String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    public void addObject(String pid, InputStream content)
            throws LowlevelStorageException {
        impl.addObject(pid, content);
    }

    public void replaceObject(String pid, InputStream content)
            throws LowlevelStorageException {
        impl.replaceObject(pid, content);
    }

    public InputStream retrieveObject(String pid)
            throws LowlevelStorageException {
        return impl.retrieveObject(pid);
    }

    public void removeObject(String pid) throws LowlevelStorageException {
        impl.removeObject(pid);
    }

    public void rebuildObject() throws LowlevelStorageException {
        impl.rebuildObject();
    }

    public void auditObject() throws LowlevelStorageException {
        impl.auditObject();
    }

    public long addDatastream(String pid, InputStream content)
            throws LowlevelStorageException {
        return impl.addDatastream(pid, content);
    }

    public long replaceDatastream(String pid, InputStream content)
            throws LowlevelStorageException {
        return impl.replaceDatastream(pid, content);
    }

    public InputStream retrieveDatastream(String pid)
            throws LowlevelStorageException {
        return impl.retrieveDatastream(pid);
    }

    public void removeDatastream(String pid) throws LowlevelStorageException {
        impl.removeDatastream(pid);
    }

    public void rebuildDatastream() throws LowlevelStorageException {
        impl.rebuildDatastream();
    }

    public void auditDatastream() throws LowlevelStorageException {
        impl.auditDatastream();
    }

    // IListable methods

    public Iterator<String> listObjects() {
        return ((IListable) impl).listObjects();
    }

    public Iterator<String> listDatastreams() {
        return ((IListable) impl).listDatastreams();
    }

    // ISizable methods

    public long getDatastreamSize(String dsKey) throws LowlevelStorageException {
        return ((ISizable) impl).getDatastreamSize(dsKey);
    }
}
