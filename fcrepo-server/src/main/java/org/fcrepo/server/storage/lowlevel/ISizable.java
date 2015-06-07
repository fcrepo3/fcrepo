package org.fcrepo.server.storage.lowlevel;

import org.fcrepo.server.errors.LowlevelStorageException;

/**
 * Interface for {@link ILowlevelStorage} implementations that are
 * capable of retrieving the size of a datastream.
 *
 * @author Stephen Bayliss
 * @since Fedora 3.5
 */
public interface ISizable {

    /**
     * Return the size of a datastream in bytes
     * @param dsKey
     * @return long size of datastream contents
     * @throws LowlevelStorageException
     */
    public long getDatastreamSize(String dsKey) throws LowlevelStorageException;

}
