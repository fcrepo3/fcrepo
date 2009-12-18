/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.managementmethods;

import fedora.server.errors.ServerException;
import fedora.server.journal.entry.JournalEntry;
import fedora.server.management.ManagementDelegate;

/**
 * Adapter class for Management.modifyObject().
 * 
 * @author Jim Blake
 */
public class ModifyObjectMethod
        extends ManagementMethod {

    public ModifyObjectMethod(JournalEntry parent) {
        super(parent);
    }

    @Override
    public Object invoke(ManagementDelegate delegate) throws ServerException {
        return delegate.modifyObject(parent.getContext(), parent
                .getStringArgument(ARGUMENT_NAME_PID), parent
                .getStringArgument(ARGUMENT_NAME_STATE), parent
                .getStringArgument(ARGUMENT_NAME_LABEL), parent
                .getStringArgument(ARGUMENT_NAME_OWNERID), parent
                .getStringArgument(ARGUMENT_NAME_LOG_MESSAGE));
    }

}
