/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.lowlevel;

import java.io.InputStream;

import fedora.server.errors.LowlevelStorageException;

/**
 * Provides read/write access to all serialized objects and managed datastream
 * content within Fedora.
 *
 * @author Bill Niebel
 */
public interface ILowlevelStorage {

    /**
     * Adds a new object.
     *
     * @param objectKey the pid of the object.
     * @param content the serialized object.
     * @throws LowlevelStorageException if the object already exists or
     *         cannot be added for any other reason.
     */
    public void addObject(String objectKey, InputStream content)
            throws LowlevelStorageException;

    /**
     * Replaces an existing object.
     *
     * @param objectKey the pid of the object.
     * @param content the serialized object.
     * @throws LowlevelStorageException if the object does not already exist
     *         or cannot be replaced for any other reason.
     */
    public void replaceObject(String objectKey, InputStream content)
            throws LowlevelStorageException;

    /**
     * Gets an existing object.
     *
     * @param objectKey the pid of the object.
     * @return the serialized form of the object, as stored.
     * @throws LowlevelStorageException if the object does not exist or
     *         cannot be read for any other reason.
     */
    public InputStream retrieveObject(String objectKey)
            throws LowlevelStorageException;

    /**
     * Removes an object.
     *
     * @param objectKey the pid of the object.
     * @throws LowlevelStorageException if the object does not exist or
     *         cannot be removed for any other reason.
     */
    public void removeObject(String objectKey) throws LowlevelStorageException;

    /**
     * Reconstructs the object index if such an index exists. The object index
     * associates an object with a stored location. If the implementation does
     * not use an index, this is a no-op.
     * <p>
     * <h2>Warning</h2>
     * Rebuilding the object index is not expected to be an atomic operation
     * and should only be run while the system is offline or reads and writes
     * are otherwise prevented.
     *
     * @throws LowlevelStorageException if an error occurs that prevents the
     *         index from being rebuilt.
     */
    public void rebuildObject() throws LowlevelStorageException;

    /**
     * Performs a consistency check against the object index if such an index
     * exists. The object index associates an object with a stored location.
     * If the implementation does not use an index, this is a no-op. If any
     * inconsistencies are found, they will be reported to the system log.
     *
     * @throws LowlevelStorageException if an error occurs that prevents the
     *         consistency check from taking place.
     */
    public void auditObject() throws LowlevelStorageException;

    /**
     * Sets the content of a new datastream version.
     *
     * @param dsKey the $pid "+" $dsId "+" $dsVersionId string that uniquely
     *        identifies the datastream version.
     * @param content the content.
     * @throws LowlevelStorageException if the datastream version already
     *         exists or cannot be added for any other reason.
     */
    public void addDatastream(String dsKey, InputStream content)
            throws LowlevelStorageException;

    /**
     * Sets the content of an existing datastream version.
     *
     * @param dsKey the $pid "+" $dsId "+" $dsVersionId string that uniquely
     *        identifies the datastream version.
     * @param content the content.
     * @throws LowlevelStorageException if the datastream version does not
     *         already exist or cannot be replaced for any other reason.
     */
    public void replaceDatastream(String dsKey, InputStream content)
            throws LowlevelStorageException;

    /**
     * Gets the content of an existing datastream version.
     *
     * @param dsKey the $pid "+" $dsId "+" $dsVersionId string that uniquely
     *        identifies the datastream version.
     * @return the content.
     * @throws LowlevelStorageException if the datastream version does not
     *         exist or cannot be read for any other reason.
     */
    public InputStream retrieveDatastream(String dsKey)
            throws LowlevelStorageException;

    /**
     * Removes the content of an existing datastream version.
     *
     * @param dsKey the $pid "+" $dsId "+" $dsVersionId string that uniquely
     *        identifies the datastream version.
     * @throws LowlevelStorageException if the datastream version does not
     *         exist or cannot be removed for any other reason.
     */
    public void removeDatastream(String dsKey) throws LowlevelStorageException;

    /**
     * Reconstructs the datastream index if such an index exists. The datastream
     * index associates a datastream version with a stored location. If the
     * implementation does not use an index, this is a no-op.
     * <p>
     * <h2>Warning</h2>
     * Rebuilding the datastream index is not expected to be an atomic operation
     * and should only be run while the system is offline or reads and writes
     * are otherwise prevented.
     *
     * @throws LowlevelStorageException if an error occurs that prevents the
     */
    public void rebuildDatastream() throws LowlevelStorageException;

    /**
     * Performs a consistency check against the datastream index if such an
     * index exists. The datastream index associates a datastream version with
     * a stored location. If the implementation does not use an index, this is
     * a no-op. If any inconsistencies are found, they will be reported to the
     * system log.
     *
     * @throws LowlevelStorageException if an error occurs that prevents the
     *         consistency check from taking place.
     */
    public void auditDatastream() throws LowlevelStorageException;
}
