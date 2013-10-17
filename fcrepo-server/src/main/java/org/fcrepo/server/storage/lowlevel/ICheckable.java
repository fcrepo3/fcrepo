package org.fcrepo.server.storage.lowlevel;

import org.fcrepo.server.errors.LowlevelStorageException;

/**
 * Interface for {@link ILowlevelStorage} implementations that are
 * capable of checking whether a blob exists for a key. This
 * capability is required in order to minimize trust of external
 * databases.
 *
 * @author armintor@gmail.com
 * @since Fedora 3.7.1
 */
public interface ICheckable {
    public boolean objectExists(String objectKey) throws LowlevelStorageException;

}
