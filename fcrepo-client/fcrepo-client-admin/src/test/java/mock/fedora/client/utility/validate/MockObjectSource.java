/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package mock.fedora.client.utility.validate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import fedora.client.utility.validate.InvalidContentModelException;
import fedora.client.utility.validate.ObjectSource;
import fedora.client.utility.validate.ObjectSourceException;
import fedora.client.utility.validate.types.ContentModelInfo;
import fedora.client.utility.validate.types.ObjectInfo;

import fedora.server.search.FieldSearchQuery;

/**
 * @author Jim Blake
 */
public class MockObjectSource
        implements ObjectSource {

    // ----------------------------------------------------------------------
    // Mocking infrastructure
    // ----------------------------------------------------------------------

    /**
     * If they ask for one of these, give it to them.
     */
    private final Map<String, ObjectInfo> seedObjects =
            new HashMap<String, ObjectInfo>();

    /**
     * If they ask for one of these, give it to them.
     */
    private final Map<String, ContentModelInfo> seedModels =
            new HashMap<String, ContentModelInfo>();

    /**
     * Try to get info on any of these PIDs, and you will get an
     * {@link ObjectSourceException} instead.
     */
    private final Set<String> throwExceptionOnThesePids = new HashSet<String>();

    /**
     * Try to get a content model for any of these PIDs, and you will get an
     * {@link ObjectSourceException} instead.
     */
    private final Set<String> throwExceptionOnTheseModels =
            new HashSet<String>();

    public void addSeedObject(ObjectInfo seedObject) {
        seedObjects.put(seedObject.getPid(), seedObject);
    }

    public void addSeedModel(ObjectInfo seedModelBase,
                             ContentModelInfo seedModel) {
        addSeedObject(seedModelBase);
        seedModels.put(seedModel.getPid(), seedModel);
    }

    public void removeSeedModel(ContentModelInfo model) {
        seedObjects.remove(model.getPid());
        seedModels.remove(model.getPid());
    }

    public void throwObjectSourceExceptionOnPid(String pid) {
        throwExceptionOnThesePids.add(pid);
    }

    public void throwObjectSourceException(ContentModelInfo model) {
        throwExceptionOnThesePids.add(model.getPid());
    }

    public void throwInvalidContentModelException(ContentModelInfo model) {
        throwExceptionOnTheseModels.add(model.getPid());
    }

    // ----------------------------------------------------------------------
    // Mocked methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public ObjectInfo getValidationObject(String pid)
            throws ObjectSourceException {
        if (throwExceptionOnThesePids.contains(pid)) {
            throw createObjectSourceException(pid);
        }
        return seedObjects.get(pid);
    }

    /**
     * In order to have a content model, you must also have the object it is
     * based on.
     */
    public ContentModelInfo getContentModelInfo(String pid)
            throws ObjectSourceException, InvalidContentModelException {
        if (throwExceptionOnThesePids.contains(pid)) {
            throw createObjectSourceException(pid);
        }

        if (throwExceptionOnTheseModels.contains(pid)) {
            throw createInvalidContentModelException(pid);
        }

        ObjectInfo object = seedObjects.get(pid);
        if (object == null) {
            return null;
        }

        return seedModels.get(pid);
    }

    // ----------------------------------------------------------------------
    // Un-implemented methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public Iterator<String> findObjectPids(FieldSearchQuery query)
            throws ObjectSourceException {
        // KLUGE Auto-generated method stub
        throw new RuntimeException("MockObjectSource.findObjectPids() not implemented.");
    }

    /**
     * If we throw an {@link ObjectSourceException}, it will look like this.
     */
    public ObjectSourceException createObjectSourceException(String pid) {
        return new ObjectSourceException("forced exception on '" + pid + "'");
    }

    /**
     * If we throw an {@link InvalidContentModelException}, it will look like
     * this.
     */
    public InvalidContentModelException createInvalidContentModelException(String pid) {
        return new InvalidContentModelException(pid, "forced exception on "
                + "content model");
    }

}
