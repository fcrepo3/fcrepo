/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.lowlevel;

import java.io.File;
import java.io.IOException;

import java.util.Enumeration;
import java.util.Map;

import org.apache.log4j.Logger;

import fedora.server.errors.LowlevelStorageException;
import fedora.server.errors.LowlevelStorageInconsistencyException;

/**
 * @author Bill Niebel
 */
public abstract class PathRegistry {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(PathRegistry.class.getName());

    protected static final int NO_REPORT = 0; //<=========????????

    protected static final int ERROR_REPORT = 1;

    protected static final int FULL_REPORT = 2;

    protected static final int REPORT_FILES = 0;

    protected static final int AUDIT_FILES = 1;

    protected static final int REBUILD = 2;

    protected final String registryName;

    protected final String[] storeBases;

    public PathRegistry(Map<String, ?> configuration) {
        registryName = (String) configuration.get("registryName");
        storeBases = (String[]) configuration.get("storeBases");
    }

    public abstract String get(String pid) throws LowlevelStorageException;

    public abstract void put(String pid, String path)
            throws LowlevelStorageException;

    public abstract void remove(String pid) throws LowlevelStorageException;

    public abstract void rebuild() throws LowlevelStorageException;

    public abstract void auditFiles() throws LowlevelStorageException;

    public void auditRegistry() throws LowlevelStorageException {
        LOG.info("begin audit:  registry-against-files");
        Enumeration<String> keys = keys();
        while (keys.hasMoreElements()) {
            String pid = keys.nextElement();
            try {
                String path = get(pid);
                File file = new File(path);
                boolean fileExists = file.exists();
                LOG.info((fileExists ? "" : "ERROR: ") + "registry has [" + pid
                        + "] => [" + path + "] " + (fileExists ? "and" : "BUT")
                        + " file does " + (fileExists ? "" : "NOT") + "exist");
            } catch (LowlevelStorageException e) {
                LOG.error("ERROR: registry has [" + pid + "] => []", e);
            }
        }
        LOG.info("end audit:  registry-against-files (ending normally)");
    }

    protected final String getRegistryName() {
        return registryName;
    }

    public static final boolean stringNull(String string) {
        return null == string || string.equals("");
    }

    private final void traverseFiles(File[] files,
                                     int operation,
                                     boolean stopOnError,
                                     int report)
            throws LowlevelStorageException {
        for (File element : files) {
            if (element.exists()) {
                if (element.isDirectory()) {
                    traverseFiles(element.listFiles(),
                                  operation,
                                  stopOnError,
                                  report);
                } else {
                    String filename = element.getName();
                    String path = null;
                    try {
                        path = element.getCanonicalPath();
                    } catch (IOException e) {
                        if (report != NO_REPORT) {
                            LOG.error("couldn't get File path", e);
                        }
                        if (stopOnError) {
                            throw new LowlevelStorageException(true,
                                                               "couldn't get File path",
                                                               e);
                        }
                    }
                    if (path != null) {
                        String pid = PathAlgorithm.decode(filename);
                        if (pid == null) {
                            if (report != NO_REPORT) {
                                LOG.error("unexpected file at [" + path + "]");
                            }
                            if (stopOnError) {
                                throw new LowlevelStorageException(true,
                                                                   "unexpected file traversing object store at ["
                                                                           + path
                                                                           + "]");
                            }
                        } else {
                            switch (operation) {
                                case REPORT_FILES: {
                                    if (report == FULL_REPORT) {
                                        LOG.info("file [" + path
                                                + "] would have pid [" + pid
                                                + "]");
                                    }
                                    break;
                                }
                                case REBUILD: {
                                    put(pid, path);
                                    if (report == FULL_REPORT) {
                                        LOG.info("added to registry: [" + pid
                                                + "] ==> [" + path + "]");
                                    }
                                    break;
                                }
                                case AUDIT_FILES: {
                                    String rpath = null;
                                    try {
                                        rpath = get(pid);
                                    } catch (LowlevelStorageException e) {
                                    }
                                    boolean matches = rpath.equals(path);
                                    if (report == FULL_REPORT || !matches) {
                                        LOG
                                                .info((matches ? "" : "ERROR: ")
                                                        + "["
                                                        + path
                                                        + "] "
                                                        + (matches ? ""
                                                                : "NOT ")
                                                        + "in registry"
                                                        + (matches ? ""
                                                                : "; pid ["
                                                                        + pid
                                                                        + "] instead registered as ["
                                                                        + (rpath == null ? "[OBJECT NOT IN STORE]"
                                                                                : rpath)
                                                                        + "]"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void traverseFiles(String[] storeBases,
                              int operation,
                              boolean stopOnError,
                              int report) throws LowlevelStorageException {
        File files[];
        try {
            files = new File[storeBases.length];
            for (int i = 0; i < storeBases.length; i++) {
                files[i] = new File(storeBases[i]);
            }
        } catch (Exception e) {
            throw new LowlevelStorageException(true,
                                               "couldn't rebuild VolatilePathRegistry",
                                               e);
        }
        traverseFiles(files, operation, stopOnError, report);
    }

    protected abstract Enumeration<String> keys() throws LowlevelStorageException,
            LowlevelStorageInconsistencyException;
}
