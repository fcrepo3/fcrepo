/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal;

import java.util.Map;

import org.apache.log4j.Logger;

import fedora.server.journal.entry.ConsumerJournalEntry;
import fedora.server.journal.helpers.JournalHelper;
import fedora.server.journal.recoverylog.JournalRecoveryLog;
import fedora.server.management.ManagementDelegate;

/**
 * Process the journal entries as a separate Thread, while the JournalConsumer
 * is blocking all calls from outside.
 * 
 * @author Jim Blake
 */
public class JournalConsumerThread
        extends Thread {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(JournalConsumerThread.class.getName());

    private final ServerInterface server;

    private final JournalReader reader;

    private final JournalRecoveryLog recoveryLog;

    private ManagementDelegate delegate;

    private boolean shutdown = false;

    /**
     * Store references to all of this stuff, but we can't start work without a
     * ManagementDelegate is provided, and we won't get that until the
     * post-initialization stage.
     */
    public JournalConsumerThread(Map<String, String> parameters,
                                 String role,
                                 ServerInterface server,
                                 JournalReader reader,
                                 JournalRecoveryLog recoveryLog) {
        this.server = server;
        this.reader = reader;
        this.recoveryLog = recoveryLog;
    }

    /**
     * Now that we have a ManagementDelegate to perform the operations, we can
     * start working.
     */
    public void setManagementDelegate(ManagementDelegate delegate) {
        this.delegate = delegate;
        start();
    }

    /**
     * Wait until the server completes its initialization, then process journal
     * entries until the reader says there are no more, or until a shutdown is
     * requested.
     */
    @Override
    public void run() {
        try {
            waitUntilServerIsInitialized();

            recoveryLog.log("Start recovery.");

            while (true) {
                if (shutdown) {
                    break;
                }
                ConsumerJournalEntry cje = reader.readJournalEntry();
                if (cje == null) {
                    break;
                }
                cje.invokeMethod(delegate, recoveryLog);
                cje.close();
            }
            reader.shutdown();

            recoveryLog.log("Recovery complete.");
        } catch (Throwable e) {
            /*
             * It makes sense to catch Exception here, because any uncaught
             * exception will not be reported - there is no console to print the
             * stack trace! It might not be appropriate to catch Throwable, but
             * it's the only way we can know about missing class files and such.
             * Of course, if we catch an OutOfMemoryError or a
             * VirtualMachineError, all bets are off.
             */
            LOG.fatal("Error during Journal recovery", e);
            String stackTrace = JournalHelper.captureStackTrace(e);
            recoveryLog.log("PROBLEM: " + stackTrace);
            recoveryLog.log("Recovery terminated prematurely.");
        } finally {
            recoveryLog.shutdown();
        }
    }

    /**
     * Wait for the server to initialize. If we wait too long, give up and shut
     * down the thread.
     */
    private void waitUntilServerIsInitialized() {
        int i = 0;
        for (; i < 60; i++) {
            if (server.hasInitialized() || shutdown) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOG.warn("Thread was interrupted");
            }
        }
        LOG.fatal("Can't recover from the Journal - "
                + "the server hasn't initialized after " + i + " seconds.");
        shutdown = true;
    }

    /**
     * Set the flag saying that it's time to quit.
     */
    public void shutdown() {
        recoveryLog.log("Shutdown requested by server");
        shutdown = true;
    }
}
