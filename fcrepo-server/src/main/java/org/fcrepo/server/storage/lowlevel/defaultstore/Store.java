package org.fcrepo.server.storage.lowlevel.defaultstore;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import org.fcrepo.common.FaultException;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.ObjectAlreadyInLowlevelStorageException;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.storage.lowlevel.DefaultLowlevelStorage;
import org.fcrepo.server.storage.lowlevel.FileSystem;
import org.fcrepo.server.storage.lowlevel.PathAlgorithm;
import org.fcrepo.server.storage.lowlevel.PathRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Store {
    private static final Logger logger =
            LoggerFactory.getLogger(Store.class);

    private final PathAlgorithm pathAlgorithm;

    private final PathRegistry pathRegistry;

    private final FileSystem fileSystem;

    //private final String storeBase;

    public Store(Map<String, Object> configuration)
            throws LowlevelStorageException {
        String registryName = (String) configuration.get(DefaultLowlevelStorage.REGISTRY_NAME);
        String filesystem = (String) configuration.get(DefaultLowlevelStorage.FILESYSTEM);
        String pathAlgorithm = (String) configuration.get(DefaultLowlevelStorage.PATH_ALGORITHM);
        String pathRegistry = (String) configuration.get(DefaultLowlevelStorage.PATH_REGISTRY);
        //storeBase = (String)configuration.get("storeBase");

        Object[] parameters = new Object[] {configuration};
        Class<?>[] parameterTypes = new Class[] {Map.class};
        ClassLoader loader = getClass().getClassLoader();
        Class<?> cclass;
        Constructor<?> constructor;
        String failureReason = "";
        try {
            failureReason = DefaultLowlevelStorage.FILESYSTEM;
            cclass = loader.loadClass(filesystem);
            constructor = cclass.getConstructor(parameterTypes);
            fileSystem = (FileSystem) constructor.newInstance(parameters);

            failureReason = DefaultLowlevelStorage.PATH_ALGORITHM;
            cclass = loader.loadClass(pathAlgorithm);
            constructor = cclass.getConstructor(parameterTypes);
            this.pathAlgorithm =
                    (PathAlgorithm) constructor.newInstance(parameters);

            failureReason = DefaultLowlevelStorage.PATH_REGISTRY;
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
        //check that object is not already in store
        if (pathRegistry.exists(pid)){
            throw new ObjectAlreadyInLowlevelStorageException(pid);
        }

        filePath = pathAlgorithm.get(pid);
        if (filePath == null || filePath.equals("")) { //guard against algorithm implementation
            throw new LowlevelStorageException(true,
                    "null path from algorithm for pid " + pid);
        }

        try {
            file = new File(filePath);
        } catch (Exception eFile) { //purposefully general catch-all
            throw new LowlevelStorageException(true,
                    "couldn't make File for " + filePath, eFile);
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
        File file = getFile(pid);
        fileSystem.rewrite(file, content);
        return file.length();
    }

    /** get content of Fedora object from low-level store */
    public final InputStream retrieve(String pid)
            throws LowlevelStorageException {
        File file = getFile(pid);
        return fileSystem.read(file);
    }

    /** get size of datastream  */
    public final long getSize(String pid) throws LowlevelStorageException {
        File file = getFile(pid);
        return file.length();
    }


    /** remove Fedora object from low-level store */
    public final void remove(String pid) throws LowlevelStorageException {
        File file = getFile(pid);

        pathRegistry.remove(pid);
        fileSystem.delete(file);
    }
    
    public final boolean exists(String pid) throws LowlevelStorageException {
        if (pathRegistry.exists(pid)){
            if (!getFile(pid).exists()) {
                throw new LowlevelStorageException(true,
                        "file not at indexed path from registry for " + pid
                        + "at " + pathRegistry.get(pid));
            }
            return true;
        } else {
            return false;
        }
    }
    
    private File getFile(String pid) throws LowlevelStorageException {
        String filePath = pathRegistry.get(pid);
        if (filePath == null || filePath.equals("")) { //guard against registry implementation
            throw new LowlevelStorageException(true,
                    "null path from registry for pid " + pid);
        }
        try {
            return new File(filePath);
        } catch (Exception eFile) { //purposefully general catch-all
            throw new LowlevelStorageException(true,
                    "couldn't make File for " + filePath, eFile);
        }
    }

}