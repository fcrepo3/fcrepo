/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.managementmethods;

import fedora.common.Constants;

import fedora.server.errors.ServerException;
import fedora.server.journal.JournalException;
import fedora.server.journal.entry.JournalEntry;
import fedora.server.management.ManagementDelegate;

/**
 * Adapter class for Management.putTempStream().
 * 
 * @author Jim Blake
 */
public class PutTempStreamMethod
        extends ManagementMethod {

    public PutTempStreamMethod(JournalEntry parent) {
        super(parent);
    }

    @Override
    public Object invoke(ManagementDelegate delegate) throws ServerException,
            JournalException {
        String uploadId =
                delegate.putTempStream(parent.getContext(), parent
                        .getStreamArgument(ARGUMENT_NAME_IN));

        // Store the Upload ID for writing to the journal.
        parent.setRecoveryValue(Constants.RECOVERY.UPLOAD_ID.uri, uploadId);

        return uploadId;
    }

}
