/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.readerwriter.multifile;

import java.util.Map;

import fedora.server.journal.JournalException;
import fedora.server.journal.ServerInterface;
import fedora.server.journal.recoverylog.JournalRecoveryLog;

/**
 * A JournalReader implementation for "following" a leading server, when the
 * leading server is using a {@link MultiFileJournalWriter}, or the equivalent.
 * <p>
 * The recovery is never complete, as the reader continues to poll for
 * recently-created files, until the server shuts down.
 * <p>
 * This class should likely be superceded by
 * {@link LockingFollowingJournalReader}.
 * 
 * @author Jim Blake
 */
public class MultiFileFollowingJournalReader
        extends MultiFileJournalReader {

    private final long pollingIntervalMillis;

    /**
     * Do the super-class constructor, and then find the polling interval.
     */
    public MultiFileFollowingJournalReader(Map<String, String> parameters,
                                           String role,
                                           JournalRecoveryLog recoveryLog,
                                           ServerInterface server)
            throws JournalException {
        super(parameters, role, recoveryLog, server);
        pollingIntervalMillis =
                MultiFileJournalHelper
                        .parseParametersForPollingInterval(parameters);
    }

    /**
     * Ask for a new file, using the superclass method, but if none is found,
     * wait for a while and ask again. This will continue until we get a server
     * shutdown signal.
     */
    @Override
    protected synchronized JournalInputFile openNextFile()
            throws JournalException {
        while (open) {
            JournalInputFile nextFile = super.openNextFile();
            if (nextFile != null) {
                return nextFile;
            }
            try {
                wait(pollingIntervalMillis);
            } catch (InterruptedException e) {
                // no special action on interrupt.
            }
        }
        return null;
    }

    /**
     * If the server requests a shutdown, stop waiting the next file to come in.
     */
    @Override
    public synchronized void shutdown() throws JournalException {
        super.shutdown();
        notifyAll();
    }
}
