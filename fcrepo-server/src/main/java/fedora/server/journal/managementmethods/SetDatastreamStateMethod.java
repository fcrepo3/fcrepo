/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.managementmethods;

import fedora.server.errors.ServerException;
import fedora.server.journal.entry.JournalEntry;
import fedora.server.management.ManagementDelegate;

/**
 * Adapter class for Management.setDatastreamState().
 * 
 * @author Jim Blake
 */
public class SetDatastreamStateMethod
        extends ManagementMethod {

    public SetDatastreamStateMethod(JournalEntry parent) {
        super(parent);
    }

    @Override
    public Object invoke(ManagementDelegate delegate) throws ServerException {
        return delegate.setDatastreamState(parent.getContext(), parent
                .getStringArgument(ARGUMENT_NAME_PID), parent
                .getStringArgument(ARGUMENT_NAME_DS_ID), parent
                .getStringArgument(ARGUMENT_NAME_DS_STATE), parent
                .getStringArgument(ARGUMENT_NAME_LOG_MESSAGE));
    }

}
