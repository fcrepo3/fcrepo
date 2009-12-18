/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.managementmethods;

import fedora.common.Constants;

import fedora.server.errors.ServerException;
import fedora.server.journal.entry.JournalEntry;
import fedora.server.management.ManagementDelegate;

/**
 * Adapter class for Management.addDatastream()
 * 
 * @author Jim Blake
 */
public class AddDatastreamMethod
        extends ManagementMethod {

    public AddDatastreamMethod(JournalEntry parent) {
        super(parent);
    }

    @Override
    public Object invoke(ManagementDelegate delegate) throws ServerException {
        String datastreamId =
                delegate.addDatastream(parent.getContext(), parent
                        .getStringArgument(ARGUMENT_NAME_PID), parent
                        .getStringArgument(ARGUMENT_NAME_DS_ID), parent
                        .getStringArrayArgument(ARGUMENT_NAME_ALT_IDS), parent
                        .getStringArgument(ARGUMENT_NAME_DS_LABEL), parent
                        .getBooleanArgument(ARGUMENT_NAME_VERSIONABLE), parent
                        .getStringArgument(ARGUMENT_NAME_MIME_TYPE), parent
                        .getStringArgument(ARGUMENT_NAME_FORMAT_URI), parent
                        .getStringArgument(ARGUMENT_NAME_LOCATION), parent
                        .getStringArgument(ARGUMENT_NAME_CONTROL_GROUP), parent
                        .getStringArgument(ARGUMENT_NAME_DS_STATE), parent
                        .getStringArgument(ARGUMENT_NAME_CHECKSUM_TYPE), parent
                        .getStringArgument(ARGUMENT_NAME_CHECKSUM), parent
                        .getStringArgument(ARGUMENT_NAME_LOG_MESSAGE));

        // Store the Datastream ID for writing to the journal.
        parent.setRecoveryValue(Constants.RECOVERY.DATASTREAM_ID.uri,
                                datastreamId);

        return datastreamId;
    }

}
