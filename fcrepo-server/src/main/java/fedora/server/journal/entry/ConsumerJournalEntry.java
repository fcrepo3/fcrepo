/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.entry;

import fedora.server.errors.ServerException;
import fedora.server.journal.JournalException;
import fedora.server.journal.recoverylog.JournalRecoveryLog;
import fedora.server.management.ManagementDelegate;

/**
 * The JournalEntry to use when consuming a journal file.
 * <p>
 * Before invoking a method, write the entry to the recovery log. After invoking
 * the method, log a completion message.
 * 
 * @author Jim Blake
 */
public class ConsumerJournalEntry
        extends JournalEntry {

    private String identifier = "no identifier";

    public ConsumerJournalEntry(String methodName, JournalEntryContext context) {
        super(methodName, context);
    }

    public void invokeMethod(ManagementDelegate delegate,
                             JournalRecoveryLog recoveryLog)
            throws ServerException, JournalException {
        recoveryLog.log(this);
        super.getMethod().invoke(delegate);
        recoveryLog.log("Call complete:" + super.getMethodName());
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return "ConsumerJournalEntry[identifier=" + identifier
                + ", methodName=" + getMethodName() + ", context="
                + getContext() + "]";
    }

}
