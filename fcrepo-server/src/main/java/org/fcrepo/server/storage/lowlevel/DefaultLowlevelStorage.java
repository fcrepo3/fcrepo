/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.lowlevel;

import java.io.InputStream;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


import org.fcrepo.common.FaultException;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.storage.lowlevel.defaultstore.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author Bill Niebel
 */
public class DefaultLowlevelStorage
        implements ILowlevelStorage, IListable, ISizable, ICheckable {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultLowlevelStorage.class);
    public static final String REGISTRY_NAME = "registryName";

    public static final String OBJECT_REGISTRY_TABLE = "objectPaths";

    public static final String DATASTREAM_REGISTRY_TABLE = "datastreamPaths";

    public static final String OBJECT_STORE_BASE = "object_store_base";

    public static final String DATASTREAM_STORE_BASE = "datastream_store_base";

    public static final String FILESYSTEM = "file_system";

    public static final String PATH_ALGORITHM = "path_algorithm";

    public static final String PATH_REGISTRY = "path_registry";

    private final Store objectStore;

    private final Store datastreamStore;

    public DefaultLowlevelStorage(Map<String, Object> configuration)
            throws LowlevelStorageException {
        String objectStoreBase = (String) configuration.get(OBJECT_STORE_BASE);
        String datastreamStoreBase =
                (String) configuration.get(DATASTREAM_STORE_BASE);

        Map<String, Object> objConfig = new HashMap<String, Object>();
        objConfig.putAll(configuration);
        objConfig.put(REGISTRY_NAME, OBJECT_REGISTRY_TABLE);
        objConfig.put("storeBase", objectStoreBase);
        objConfig.put("storeBases", new String[] {objectStoreBase});
        objectStore = new Store(objConfig);

        Map<String, Object> dsConfig = new HashMap<String, Object>();
        dsConfig.putAll(configuration);
        dsConfig.put(REGISTRY_NAME, DATASTREAM_REGISTRY_TABLE);
        dsConfig.put("storeBase", datastreamStoreBase);
        dsConfig.put("storeBases", new String[] {datastreamStoreBase});
        datastreamStore = new Store(dsConfig);
    }

    @Override
    public void addObject(String pid, InputStream content, Map<String, String> hints)
            throws LowlevelStorageException {
        objectStore.add(pid, content);
    }

    @Override
    public void replaceObject(String pid, InputStream content, Map<String, String> hints)
            throws LowlevelStorageException {
        objectStore.replace(pid, content);
    }

    @Override
    public InputStream retrieveObject(String pid)
            throws LowlevelStorageException {
        return objectStore.retrieve(pid);
    }

    @Override
    public void removeObject(String pid) throws LowlevelStorageException {
        objectStore.remove(pid);
    }

    @Override
    public void rebuildObject() throws LowlevelStorageException {
        objectStore.rebuild();
    }

    @Override
    public void auditObject() throws LowlevelStorageException {
        objectStore.audit();
    }

    @Override
    public long addDatastream(String pid, InputStream content, Map<String, String> hints)
            throws LowlevelStorageException {
        return datastreamStore.add(pid, content);
    }

    @Override
    public long replaceDatastream(String pid, InputStream content, Map<String, String>hints)
            throws LowlevelStorageException {
        return datastreamStore.replace(pid, content);
    }

    @Override
    public InputStream retrieveDatastream(String pid)
            throws LowlevelStorageException {
        return datastreamStore.retrieve(pid);
    }

    @Override
    public void removeDatastream(String pid) throws LowlevelStorageException {
        datastreamStore.remove(pid);
    }

    @Override
    public void rebuildDatastream() throws LowlevelStorageException {
        datastreamStore.rebuild();
    }

    @Override
    public void auditDatastream() throws LowlevelStorageException {
        datastreamStore.audit();
    }

    //IListable methods
    @Override
    public Iterator<String> listObjects() {
        return objectStore.list();
    }

    @Override
    public Iterator<String> listDatastreams() {
        return datastreamStore.list();
    }
    
    //ISizable methods
    @Override
    public long getDatastreamSize(String dsKey) throws LowlevelStorageException {
        return datastreamStore.getSize(dsKey);
    }

    // ICheckable methods
    @Override
    public boolean objectExists(String objectKey) {
        try {
            return objectStore.exists(objectKey);
        } catch (LowlevelStorageException e) {
            logger.error(e.toString(),e);
            throw new FaultException(
                    "System error determining existence of blob", e);
        }
    }
}
