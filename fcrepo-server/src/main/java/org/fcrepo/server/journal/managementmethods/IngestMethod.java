/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.journal.managementmethods;

import org.fcrepo.common.Constants;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.journal.JournalException;
import org.fcrepo.server.journal.entry.JournalEntry;
import org.fcrepo.server.management.ManagementDelegate;



/**
 * Adapter class for Management.ingest()
 * 
 * @author Jim Blake
 */
public class IngestMethod
        extends ManagementMethod {

    public IngestMethod(JournalEntry parent) {
        super(parent);
    }

    @Override
    public Object invoke(ManagementDelegate delegate) throws ServerException,
            JournalException {
        String pid =
                delegate.ingest(parent.getContext(), parent
                        .getStreamArgument(ARGUMENT_NAME_SERIALIZATION), parent
                        .getStringArgument(ARGUMENT_NAME_LOG_MESSAGE), parent
                        .getStringArgument(ARGUMENT_NAME_FORMAT), parent
                        .getStringArgument(ARGUMENT_NAME_ENCODING), parent
                        .getStringArgument(ARGUMENT_NAME_NEW_PID));

        // Store the PID for writing to the journal.
        parent.setRecoveryValue(Constants.RECOVERY.PID.attributeId, pid);

        return pid;
    }

}
