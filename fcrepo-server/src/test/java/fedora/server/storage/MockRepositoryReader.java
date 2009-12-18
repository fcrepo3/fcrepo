/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import fedora.common.Models;
import fedora.server.Context;
import fedora.server.errors.GeneralException;
import fedora.server.errors.ObjectNotFoundException;
import fedora.server.errors.ServerException;
import fedora.server.storage.types.DigitalObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Mock implementation of <code>RepositoryReader</code> for testing. This
 * works by simply keeping a map of <code>DigitalObject</code> instances in
 * memory.
 *
 * @author Chris Wilper
 */
public class MockRepositoryReader
        implements RepositoryReader {

    /**
     * The <code>DigitalObject</code>s in the "repository", keyed by PID.
     */
    private final Map<String, DigitalObject> _objects =
            new HashMap<String, DigitalObject>();

    /**
     * Construct with an empty "repository".
     */
    public MockRepositoryReader() {
    }

    /**
     * Adds/replaces the object into the "repository".
     */
    public synchronized void putObject(DigitalObject obj) {
        _objects.put(obj.getPid(), obj);
    }

    /**
     * Removes the object from the "repository" and returns it (or null if it
     * didn't exist in the first place).
     */
    public synchronized DigitalObject deleteObject(String pid) {
        return _objects.remove(pid);
    }

    /**
     * Get a <code>DigitalObject</code> if it's in the "repository".
     *
     * @throws ObjectNotFoundException
     *         if it's not in the "repository".
     */
    public synchronized DigitalObject getObject(String pid)
            throws ObjectNotFoundException {
        DigitalObject obj = _objects.get(pid);
        if (obj == null) {
            throw new ObjectNotFoundException("No such object: " + pid);
        } else {
            return obj;
        }
    }

    // Mock methods from RepositoryReader interface.

    /**
     * {@inheritDoc}
     */
    public synchronized DOReader getReader(boolean cachedObjectRequired,
                                           Context context,
                                           String pid) throws ServerException {
        DigitalObject obj = getObject(pid);
        return new SimpleDOReader(null, this, null, null, null, obj);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized ServiceDeploymentReader getServiceDeploymentReader(boolean cachedObjectRequired,
                                                                           Context context,
                                                                           String pid)
            throws ServerException {
        DigitalObject obj = getObject(pid);
        //TODO hasModel
        if (!obj.hasContentModel(
                                 Models.SERVICE_DEPLOYMENT_3_0)) {
            throw new GeneralException("Not a service deployment: " + pid);
        } else {
            return new SimpleServiceDeploymentReader(null,
                                                     this,
                                                     null,
                                                     null,
                                                     null,
                                                     obj);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized ServiceDefinitionReader getServiceDefinitionReader(boolean cachedObjectRequired,
                                                                           Context context,
                                                                           String pid)
            throws ServerException {
        DigitalObject obj = getObject(pid);
        //TODO hasModel
        if (!obj.hasContentModel(
                                 Models.SERVICE_DEFINITION_3_0)) {
            throw new GeneralException("Not a service definition object: "
                    + pid);
        } else {
            return new SimpleServiceDefinitionReader(null,
                                                     this,
                                                     null,
                                                     null,
                                                     null,
                                                     obj);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized String[] listObjectPIDs(Context context)
            throws ServerException {
        String[] pids = new String[_objects.keySet().size()];
        Iterator<String> iter = _objects.keySet().iterator();
        int i = 0;
        while (iter.hasNext()) {
            pids[i++] = iter.next();
        }
        return pids;
    }

}
