/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.managementmethods;

import fedora.server.errors.ServerException;
import fedora.server.journal.JournalConstants;
import fedora.server.journal.JournalException;
import fedora.server.journal.entry.JournalEntry;
import fedora.server.management.ManagementDelegate;

/**
 * <p>
 * Abstract base class for the classes that act as adapters to the Management
 * methods.
 * </p>
 * <p>
 * An adapter is needed for each method that modifies the contents of the
 * repository in any way. If a method is read-only, it will not be written to
 * the journal, and so doesn't require an adapter class.
 * </p>
 * 
 * @author Jim Blake
 */
public abstract class ManagementMethod
        implements JournalConstants {

    /**
     * Get an instance of the proper class, based on the method name.
     */
    public static ManagementMethod getInstance(String methodName,
                                               JournalEntry parent) {
        if (METHOD_INGEST.equals(methodName)) {
            return new IngestMethod(parent);
        } else if (METHOD_MODIFY_OBJECT.equals(methodName)) {
            return new ModifyObjectMethod(parent);
        } else if (METHOD_PURGE_OBJECT.equals(methodName)) {
            return new PurgeObjectMethod(parent);
        } else if (METHOD_ADD_DATASTREAM.equals(methodName)) {
            return new AddDatastreamMethod(parent);
        } else if (METHOD_MODIFY_DATASTREAM_BY_REFERENCE.equals(methodName)) {
            return new ModifyDatastreamByReferenceMethod(parent);
        } else if (METHOD_MODIFY_DATASTREAM_BY_VALUE.equals(methodName)) {
            return new ModifyDatastreamByValueMethod(parent);
        } else if (METHOD_SET_DATASTREAM_STATE.equals(methodName)) {
            return new SetDatastreamStateMethod(parent);
        } else if (METHOD_SET_DATASTREAM_VERSIONABLE.equals(methodName)) {
            return new SetDatastreamVersionableMethod(parent);
        } else if (METHOD_PURGE_DATASTREAM.equals(methodName)) {
            return new PurgeDatastreamMethod(parent);
        } else if (METHOD_PURGE_RELATIONSHIP.equals(methodName)) {
            return new PurgeRelationshipMethod(parent);
        } else if (METHOD_PUT_TEMP_STREAM.equals(methodName)) {
            return new PutTempStreamMethod(parent);
        } else if (METHOD_GET_NEXT_PID.equals(methodName)) {
            return new GetNextPidMethod(parent);
        } else if (METHOD_ADD_RELATIONSHIP.equals(methodName)) {
            return new AddRelationshipMethod(parent);
        } else {
            throw new IllegalArgumentException("Unrecognized method name: '"
                    + methodName + "'");
        }
    }

    protected final JournalEntry parent;

    protected ManagementMethod(JournalEntry parent) {
        this.parent = parent;
    }

    /**
     * Each concrete sub-class should use this method to pull the necessary
     * arguments from the map of the parent JournalEntry, call the appropriate
     * method on the ManagementDelegate, and perhaps store the result in the
     * context of the parent JournalEntry (depends on the sub-class).
     */
    public abstract Object invoke(ManagementDelegate delegate)
            throws ServerException, JournalException;
}
