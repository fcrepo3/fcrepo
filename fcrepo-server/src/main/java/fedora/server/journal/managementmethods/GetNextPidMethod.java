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
 * Adapter class for Management.getNextPID()
 * 
 * @author Jim Blake
 */
public class GetNextPidMethod
        extends ManagementMethod {

    public GetNextPidMethod(JournalEntry parent) {
        super(parent);
    }

    @Override
    public Object invoke(ManagementDelegate delegate) throws ServerException {
        String[] pidList =
                delegate.getNextPID(parent.getContext(), parent
                        .getIntegerArgument(ARGUMENT_NAME_NUM_PIDS), parent
                        .getStringArgument(ARGUMENT_NAME_NAMESPACE));

        // Store the list of PIDs for writing to the journal.
        parent.setRecoveryValues(Constants.RECOVERY.PID_LIST.uri, pidList);

        return pidList;
    }

}
