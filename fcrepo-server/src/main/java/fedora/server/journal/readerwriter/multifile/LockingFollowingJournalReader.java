/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.readerwriter.multifile;

import java.io.File;
import java.io.IOException;

import java.util.Map;

import fedora.server.journal.JournalException;
import fedora.server.journal.ServerInterface;
import fedora.server.journal.helpers.ParameterHelper;
import fedora.server.journal.recoverylog.JournalRecoveryLog;

/**
 * A Following Journal Reader that can be made quiescent by the presence of a
 * lock file.
 * <p>
 * This works like the MultiFileFollowingJournalReader, except that when looking
 * for the next Journal file to be processed, it will take a moment to check the
 * locking protocol. If a lock has been requested, it will accept the lock, and
 * ignore any additional Journal files if present.
 * <p>
 * Polling continues, however. At each polling interval, the reader will check
 * for the lock request. If the request has been removed, the lock acceptance
 * will be removed also, and the reader will process the next Journal file, if
 * one is found.
 * 
 * @author Jim Blake
 */
public class LockingFollowingJournalReader
        extends MultiFileJournalReader {

    /** How many milliseconds between polls? */
    private final long pollingIntervalMillis;

    /**
     * The name of the file that signals a request to go quiescent after the
     * current journal file.
     */
    private final File lockRequestedFile;

    /**
     * The name of the file that acknowledges a request to go quiescent after
     * the current journal file.
     */
    private final File lockAcceptedFile;

    /** Poll immediately for the next file, or pause first? */
    private final boolean pauseBeforePolling;

    /** Currently quiescent? */
    private boolean wasLocked = false;

    /**
     * Require parameters for polling interval, lock request filename and lock
     * acceptance filename.
     */
    public LockingFollowingJournalReader(Map<String, String> parameters,
                                         String role,
                                         JournalRecoveryLog recoveryLog,
                                         ServerInterface server)
            throws JournalException {
        super(parameters, role, recoveryLog, server);
        pollingIntervalMillis =
                MultiFileJournalHelper
                        .parseParametersForPollingInterval(parameters);
        lockRequestedFile =
                new File(MultiFileJournalHelper
                        .getRequiredParameter(parameters,
                                              PARAMETER_LOCK_REQUESTED_FILENAME));
        lockAcceptedFile =
                new File(MultiFileJournalHelper
                        .getRequiredParameter(parameters,
                                              PARAMETER_LOCK_ACCEPTED_FILENAME));
        pauseBeforePolling =
                ParameterHelper
                        .getOptionalBooleanParameter(parameters,
                                                     PARAMETER_PAUSE_BEFORE_POLLING,
                                                     false);
    }

    /**
     * Process the locking mechanism. If we are not locked, we should look for
     * another journal file to process. Ask for a new file, using the superclass
     * method, but if none is found, wait for a while and repeat. This will
     * continue until we get a server shutdown signal.
     */
    @Override
    protected synchronized JournalInputFile openNextFile()
            throws JournalException {
        while (open) {
            boolean locked = processLockingMechanism();

            if (pauseBeforePolling) {
                try {
                    wait(pollingIntervalMillis);
                } catch (InterruptedException e) {
                    // no special action on interrupt.
                }
            }

            if (!locked) {
                JournalInputFile nextFile = super.openNextFile();
                if (nextFile != null) {
                    return nextFile;
                }
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

    /**
     * If we see a lock request, issue an acceptance. If we do not, withdraw the
     * acceptance.
     * 
     * @return true if locked, false if not locked.
     * @throws JournalException
     *         if we fail to create the 'lock accepted' file.
     */
    private boolean processLockingMechanism() throws JournalException {
        boolean locked;
        if (lockRequestedFile.exists()) {
            try {
                if (!lockAcceptedFile.exists()) {
                    lockAcceptedFile.createNewFile();
                }
            } catch (IOException e) {
                throw new JournalException("Unable to create 'Lock Accepted' file at '"
                        + lockAcceptedFile.getPath() + "'");
            }
            locked = true;
        } else {
            if (lockAcceptedFile.exists()) {
                lockAcceptedFile.delete();
            }
            locked = false;
        }

        if (locked && !wasLocked) {
            recoveryLog.log("Lock request detected: "
                    + lockRequestedFile.getPath() + ", Lock accepted: "
                    + lockAcceptedFile.getPath());
        } else if (wasLocked && !locked) {
            recoveryLog.log("Lock request removed: "
                    + lockRequestedFile.getPath());
        }

        wasLocked = locked;
        return locked;
    }
}
