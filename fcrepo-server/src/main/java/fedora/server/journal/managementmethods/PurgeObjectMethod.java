/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.managementmethods;

import fedora.server.errors.ServerException;
import fedora.server.journal.entry.JournalEntry;
import fedora.server.management.ManagementDelegate;

/**
 * Adapter class for Management.purgeObject().
 * 
 * @author Jim Blake
 */
public class PurgeObjectMethod
        extends ManagementMethod {

    public PurgeObjectMethod(JournalEntry parent) {
        super(parent);
    }

    @Override
    public Object invoke(ManagementDelegate delegate) throws ServerException {
        return delegate.purgeObject(parent.getContext(), parent
                .getStringArgument(ARGUMENT_NAME_PID), parent
                .getStringArgument(ARGUMENT_NAME_LOG_MESSAGE), parent
                .getBooleanArgument(ARGUMENT_NAME_FORCE));
    }

}
