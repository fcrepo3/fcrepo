/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import java.io.InputStream;

import fedora.server.Context;
import fedora.server.errors.ServerException;
import fedora.server.errors.StorageDeviceException;
import fedora.server.search.FieldSearchQuery;
import fedora.server.search.FieldSearchResult;

/**
 * A RepositoryReader that provides facilities for creating and modifying 
 * objects within the repository, as well as a query facility.
 * 
 * @author Chris Wilper
 */
public interface DOManager
        extends RepositoryReader {

    /**
     * Relinquishes control of a DOWriter back to the DOManager.
     * 
     * <p>When a DOManager provides a DOWriter, it creates a session lock. This
     * is used to guarantee that there will never be concurrent changes to the 
     * same object. To release the session lock, a DOWriter user calls this 
     * method.
     * 
     * @param writer
     *        an instance of a digital object writer.
     * @throws ServerException
     *         if an error occurs in obtaining a writer.
     */
    public abstract void releaseWriter(DOWriter writer) throws ServerException;

    /**
     * Gets a DOWriter for an existing digital object.
     * 
     * @param context
     *        The context of this request.
     * @param pid
     *        The PID of the object.
     * @return A writer, or null if the pid didn't point to an accessible
     *         object.
     * @throws ServerException
     *         If anything went wrong.
     */
    public abstract DOWriter getWriter(boolean cachedObjectRequired,
                                       Context context,
                                       String pid) throws ServerException;

    /**
     * Creates a copy of the digital object given by the InputStream, with
     * either a new PID or the PID indicated by the InputStream.
     * 
     * @param context
     *        The context of this request.
     * @param in
     *        A serialization of the digital object.
     * @param format
     *        The format of the serialization.
     * @param encoding
     *        The character encoding.
     * @param newPid
     *        Whether a new PID should be generated or the one indicated by the
     *        InputStream should be used.
     * @return a writer.
     * @throws ServerException
     *         If anything went wrong.
     */
    public abstract DOWriter getIngestWriter(boolean cachedObjectRequired,
                                             Context context,
                                             InputStream in,
                                             String format,
                                             String encoding,
                                             boolean newPid)
            throws ServerException;
    
    public boolean objectExists(String pid)
            throws StorageDeviceException;

    public FieldSearchResult findObjects(Context context,
                                         String[] resultFields,
                                         int maxResults,
                                         FieldSearchQuery query)
            throws ServerException;

    public FieldSearchResult resumeFindObjects(Context context,
                                               String sessionToken)
            throws ServerException;

    public String[] getNextPID(int numPIDs, String namespace)
            throws ServerException;

    public String lookupDeploymentForCModel(String cModelPid, String sDefPid);

    /**
     * Reserve a series of PIDs so that they are never used for subsequent PID
     * generations.
     */
    public void reservePIDs(String[] pidList) throws ServerException;

    /**
     * Get a "hash" of the repository. This value can be compared to a previous
     * value to determine whether the content of the repository has changed. It
     * is not necessary for this value to precisely reflect the state of the
     * repository, but if the repository hasn't changed, subsequent calls should
     * return the same value.
     */
    public String getRepositoryHash() throws ServerException;

}
