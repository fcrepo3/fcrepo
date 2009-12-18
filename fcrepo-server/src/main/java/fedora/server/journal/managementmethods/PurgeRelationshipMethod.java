/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.managementmethods;

import fedora.server.errors.ServerException;
import fedora.server.journal.entry.JournalEntry;
import fedora.server.management.ManagementDelegate;

/**
 * Adapter class for Management.purgeRelationship().
 * 
 * @author Jim Blake
 */
public class PurgeRelationshipMethod
        extends ManagementMethod {

    public PurgeRelationshipMethod(JournalEntry parent) {
        super(parent);
    }

    @Override
    public Object invoke(ManagementDelegate delegate) throws ServerException {
        return delegate.purgeRelationship(parent.getContext(), parent
                .getStringArgument(ARGUMENT_NAME_PID), parent
                .getStringArgument(ARGUMENT_NAME_RELATIONSHIP), parent
                .getStringArgument(ARGUMENT_NAME_OBJECT), parent
                .getBooleanArgument(ARGUMENT_NAME_IS_LITERAL), parent
                .getStringArgument(ARGUMENT_NAME_DATATYPE));
    }

}
