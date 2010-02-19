/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.journal.recoverylog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import java.util.Map;

import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.journal.ServerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple implementation of JournalRecoveryLog that keeps the entire log in a
 * StringBuffer, and writes it to a file on shutdown().
 * <p>
 * This is memory-intensive, so it should only be used for System Tests, where
 * the presence of the log file can be treated as a signal that the recovery is
 * complete.
 *
 * @author Jim Blake
 */
public class BufferedJournalRecoveryLog
        extends JournalRecoveryLog {

    private static final Logger logger =
            LoggerFactory.getLogger(BufferedJournalRecoveryLog.class);

    private final File logFile;

    private final StringWriter buffer;

    private boolean open = true;

    /**
     * Get the name of the log file from the server parameters, but don't create
     * the file yet. Just create the StringWriter that we will use as a buffer.
     */
    public BufferedJournalRecoveryLog(Map<String, String> parameters,
                                      String role,
                                      ServerInterface server)
            throws ModuleInitializationException {
        super(parameters, role, server);

        buffer = new StringWriter();

        if (!parameters.containsKey(PARAMETER_RECOVERY_LOG_FILENAME)) {
            throw new ModuleInitializationException("Parameter '"
                    + PARAMETER_RECOVERY_LOG_FILENAME + "' is not set.", role);
        }
        String fileName = parameters.get(PARAMETER_RECOVERY_LOG_FILENAME);
        logFile = new File(fileName);

        super.logHeaderInfo(parameters);
    }

    /**
     * Any request to log a message just adds it to the buffer in the
     * StringWriter (if shutdown has not been called).
     */
    @Override
    public synchronized void log(String message) {
        if (open) {
            log(message, buffer);
        }
    }

    /**
     * On the first call to this method, write the buffer to the log file. Set
     * the flag so no more logging calls will be accepted.
     */
    @Override
    public synchronized void shutdown() {
        try {
            if (open) {
                open = false;
                FileWriter logWriter = new FileWriter(logFile);
                logWriter.write(buffer.toString());
                logWriter.close();
            }
        } catch (IOException e) {
            logger.error("Error shutting down", e);
        }
    }

    @Override
    public String toString() {
        return super.toString() + ", logFile='" + logFile.getPath() + "'";
    }

}
