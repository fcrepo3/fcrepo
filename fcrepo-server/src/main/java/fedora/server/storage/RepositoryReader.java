/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import fedora.server.Context;
import fedora.server.errors.ServerException;

/**
 * Provides context-appropriate digital object readers and the ability to 
 * list all objects (accessible in the given context) within the repository.
 * 
 * @author Chris Wilper
 */
public interface RepositoryReader {

    /**
     * Gets a digital object reader.
     * 
     * @param context
     *        The context of this request.
     * @param pid
     *        The PID of the object.
     * @return A reader.
     * @throws ServerException
     *         If anything went wrong.
     */
    public abstract DOReader getReader(boolean cachedObjectRequired,
                                       Context context,
                                       String pid) throws ServerException;

    public abstract ServiceDeploymentReader getServiceDeploymentReader(boolean cachedObjectRequired,
                                               Context context,
                                               String pid)
            throws ServerException;

    public abstract ServiceDefinitionReader getServiceDefinitionReader(boolean cachedObjectRequired,
                                             Context context,
                                             String pid) throws ServerException;

    /**
     * Gets a list of PIDs (accessible in the given context) of all objects in
     * the repository.
     */
    public String[] listObjectPIDs(Context context) throws ServerException;

}
