/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.managementmethods;

import fedora.server.errors.ServerException;
import fedora.server.journal.JournalException;
import fedora.server.journal.entry.JournalEntry;
import fedora.server.management.ManagementDelegate;

/**
 * Adapter class for Management.modifyDatastreamByValue().
 * 
 * @author Jim Blake
 */
public class ModifyDatastreamByValueMethod
        extends ManagementMethod {

    public ModifyDatastreamByValueMethod(JournalEntry parent) {
        super(parent);
    }

    @Override
    public Object invoke(ManagementDelegate delegate) throws ServerException,
            JournalException {
        return delegate.modifyDatastreamByValue(parent.getContext(), parent
                .getStringArgument(ARGUMENT_NAME_PID), parent
                .getStringArgument(ARGUMENT_NAME_DS_ID), parent
                .getStringArrayArgument(ARGUMENT_NAME_ALT_IDS), parent
                .getStringArgument(ARGUMENT_NAME_DS_LABEL), parent
                .getStringArgument(ARGUMENT_NAME_MIME_TYPE), parent
                .getStringArgument(ARGUMENT_NAME_FORMAT_URI), parent
                .getStreamArgument(ARGUMENT_NAME_DS_CONTENT), parent
                .getStringArgument(ARGUMENT_NAME_CHECKSUM_TYPE), parent
                .getStringArgument(ARGUMENT_NAME_CHECKSUM), parent
                .getStringArgument(ARGUMENT_NAME_LOG_MESSAGE), parent
                .getBooleanArgument(ARGUMENT_NAME_FORCE));
    }

}
