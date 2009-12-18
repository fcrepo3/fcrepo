/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.lowlevel;

import java.io.File;
import java.io.InputStream;

import java.util.Map;

import fedora.server.errors.LowlevelStorageException;

/**
 * @author Bill Niebel
 * @version $Id$
 */
public abstract class FileSystem {

    public FileSystem(Map<String, ?> configuration) {
    }

    public abstract InputStream read(File file) throws LowlevelStorageException;

    public abstract void write(File file, InputStream content)
            throws LowlevelStorageException;

    public abstract void rewrite(File file, InputStream content)
            throws LowlevelStorageException;

    public abstract void delete(File file) throws LowlevelStorageException;

    public abstract String[] list(File directory);

    public abstract boolean isDirectory(File file);
}
