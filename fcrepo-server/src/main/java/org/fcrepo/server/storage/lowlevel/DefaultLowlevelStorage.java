/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.lowlevel;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.Constructor;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.fcrepo.common.FaultException;

import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.ObjectAlreadyInLowlevelStorageException;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;



/**
 * @author Bill Niebel
 */
public class DefaultLowlevelStorage
        implements ILowlevelStorage, IListable, ISizable {

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

    public InputStream retrieveObject(String pid)
            throws LowlevelStorageException {
        return objectStore.retrieve(pid);
    }

    public void removeObject(String pid) throws LowlevelStorageException {
        objectStore.remove(pid);
    }

    public void rebuildObject() throws LowlevelStorageException {
        objectStore.rebuild();
    }

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

    public InputStream retrieveDatastream(String pid)
            throws LowlevelStorageException {
        return datastreamStore.retrieve(pid);
    }

    public void removeDatastream(String pid) throws LowlevelStorageException {
        datastreamStore.remove(pid);
    }

    public void rebuildDatastream() throws LowlevelStorageException {
        datastreamStore.rebuild();
    }

    public void auditDatastream() throws LowlevelStorageException {
        datastreamStore.audit();
    }

    public Iterator<String> listObjects() {
        return objectStore.list();
    }

    public Iterator<String> listDatastreams() {
        return datastreamStore.list();
    }
    public long getDatastreamSize(String dsKey) throws LowlevelStorageException {
        return datastreamStore.getSize(dsKey);
    }


    class Store {

        private final PathAlgorithm pathAlgorithm;

        private final PathRegistry pathRegistry;

        private final FileSystem fileSystem;

        //private final String storeBase;

        public Store(Map<String, Object> configuration)
                throws LowlevelStorageException {
            String registryName = (String) configuration.get(REGISTRY_NAME);
            String filesystem = (String) configuration.get(FILESYSTEM);
            String pathAlgorithm = (String) configuration.get(PATH_ALGORITHM);
            String pathRegistry = (String) configuration.get(PATH_REGISTRY);
            //storeBase = (String)configuration.get("storeBase");

            Object[] parameters = new Object[] {configuration};
            Class[] parameterTypes = new Class[] {Map.class};
            ClassLoader loader = getClass().getClassLoader();
            Class cclass;
            Constructor constructor;
            String failureReason = "";
            try {
                failureReason = FILESYSTEM;
                cclass = loader.loadClass(filesystem);
                constructor = cclass.getConstructor(parameterTypes);
                fileSystem = (FileSystem) constructor.newInstance(parameters);

                failureReason = PATH_ALGORITHM;
                cclass = loader.loadClass(pathAlgorithm);
                constructor = cclass.getConstructor(parameterTypes);
                this.pathAlgorithm =
                        (PathAlgorithm) constructor.newInstance(parameters);

                failureReason = PATH_REGISTRY;
                cclass = loader.loadClass(pathRegistry);
                constructor = cclass.getConstructor(parameterTypes);
                this.pathRegistry =
                        (PathRegistry) constructor.newInstance(parameters);
            } catch (Exception e) {
                LowlevelStorageException wrapper =
                        new LowlevelStorageException(true, "couldn't set up "
                                + failureReason + " for " + registryName, e);
                throw wrapper;
            }
        }

        /**
         * Gets the keys of all stored items.
         *
         * @return an iterator of all keys.
         */
        public Iterator<String> list() {
            try {
                final Enumeration<String> keys = pathRegistry.keys();
                return new Iterator<String>() {
                    public boolean hasNext() { return keys.hasMoreElements(); }
                    public String next() { return keys.nextElement(); }
                    public void remove() { throw new UnsupportedOperationException(); }
                };
            } catch (LowlevelStorageException e) {
                throw new FaultException(e);
            }
        }

        /**
         * compares a. path registry with OS files; and b. OS files with
         * registry
         */
        public void audit() throws LowlevelStorageException {
            pathRegistry.auditFiles();
            pathRegistry.auditRegistry();
        }

        /** recreates path registry from OS files */
        public void rebuild() throws LowlevelStorageException {
            pathRegistry.rebuild();
        }

        /**
         * add to lowlevel store content of Fedora object not already in
         * lowlevel store
         * @return size - size of the object stored
         */
        public final long add(String pid, InputStream content)
                throws LowlevelStorageException {
            String filePath;
            File file = null;
            try { //check that object is not already in store
                filePath = pathRegistry.get(pid);
                ObjectAlreadyInLowlevelStorageException already =
                        new ObjectAlreadyInLowlevelStorageException("" + pid);
                throw already;
            } catch (ObjectNotInLowlevelStorageException not) {
                // OK:  keep going
            }
            filePath = pathAlgorithm.get(pid);
            if (filePath == null || filePath.isEmpty()) { //guard against algorithm implementation
                LowlevelStorageException nullPath =
                        new LowlevelStorageException(true,
                                                     "null path from algorithm for pid "
                                                             + pid);
                throw nullPath;
            }

            try {
                file = new File(filePath);
            } catch (Exception eFile) { //purposefully general catch-all
                LowlevelStorageException newFile =
                        new LowlevelStorageException(true,
                                                     "couldn't make File for "
                                                             + filePath,
                                                     eFile);
                throw newFile;
            }
            fileSystem.write(file, content);
            pathRegistry.put(pid, filePath);
            return file.length();
        }

        /**
         * replace into low-level store content of Fedora object already in
         * lowlevel store
         */
        public final long replace(String pid, InputStream content)
                throws LowlevelStorageException {
            String filePath;
            File file = null;
            try {
                filePath = pathRegistry.get(pid);
            } catch (ObjectNotInLowlevelStorageException ffff) {
                LowlevelStorageException noPath =
                        new LowlevelStorageException(false, "pid " + pid
                                + " not in registry", ffff);
                throw noPath;
            }
            if (filePath == null || filePath.isEmpty()) { //guard against registry implementation
                LowlevelStorageException nullPath =
                        new LowlevelStorageException(true, "pid " + pid
                                + " not in registry");
                throw nullPath;
            }

            try {
                file = new File(filePath);
            } catch (Exception eFile) { //purposefully general catch-all
                LowlevelStorageException newFile =
                        new LowlevelStorageException(true,
                                                     "couldn't make new File for "
                                                             + filePath,
                                                     eFile);
                throw newFile;
            }
            fileSystem.rewrite(file, content);
            return file.length();
        }

        /** get content of Fedora object from low-level store */
        public final InputStream retrieve(String pid)
                throws LowlevelStorageException {
            String filePath;
            File file;

            try {
                filePath = pathRegistry.get(pid);
            } catch (ObjectNotInLowlevelStorageException eReg) {
                throw eReg;
            }

            if (filePath == null || filePath.isEmpty()) { //guard against registry implementation
                LowlevelStorageException nullPath =
                        new LowlevelStorageException(true,
                                                     "null path from registry for pid "
                                                             + pid);
                throw nullPath;
            }

            try {
                file = new File(filePath);
            } catch (Exception eFile) { //purposefully general catch-all
                LowlevelStorageException newFile =
                        new LowlevelStorageException(true,
                                                     "couldn't make File for "
                                                             + filePath,
                                                     eFile);
                throw newFile;
            }

            return fileSystem.read(file);
        }

        /** get size of datastream  */
        public final long getSize(String pid) throws LowlevelStorageException {
            String filePath;
            File file;

            try {
                filePath = pathRegistry.get(pid);
            } catch (ObjectNotInLowlevelStorageException eReg) {
                throw eReg;
            }

            if (filePath == null || filePath.isEmpty()) { //guard against registry implementation
                LowlevelStorageException nullPath =
                        new LowlevelStorageException(true,
                                                     "null path from registry for pid "
                                                             + pid);
                throw nullPath;
            }

            try {
                file = new File(filePath);
            } catch (Exception eFile) { //purposefully general catch-all
                LowlevelStorageException newFile =
                        new LowlevelStorageException(true,
                                                     "couldn't make File for "
                                                             + filePath,
                                                     eFile);
                throw newFile;
            }
            return file.length();

        }


        /** remove Fedora object from low-level store */
        public final void remove(String pid) throws LowlevelStorageException {
            String filePath;
            File file = null;

            try {
                filePath = pathRegistry.get(pid);
            } catch (ObjectNotInLowlevelStorageException eReg) {
                throw eReg;
            }
            if (filePath == null || filePath.isEmpty()) { //guard against registry implementation
                LowlevelStorageException nullPath =
                        new LowlevelStorageException(true,
                                                     "null path from registry for pid "
                                                             + pid);
                throw nullPath;
            }

            try {
                file = new File(filePath);
            } catch (Exception eFile) { //purposefully general catch-all
                LowlevelStorageException newFile =
                        new LowlevelStorageException(true,
                                                     "couldn't make File for "
                                                             + filePath,
                                                     eFile);
                throw newFile;
            }
            pathRegistry.remove(pid);
            fileSystem.delete(file);
        }

    }
}
