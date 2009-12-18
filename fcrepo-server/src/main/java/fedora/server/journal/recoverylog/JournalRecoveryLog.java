/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.recoverylog;

import java.io.IOException;
import java.io.Writer;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import fedora.server.MultiValueMap;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.journal.JournalConstants;
import fedora.server.journal.JournalException;
import fedora.server.journal.ServerInterface;
import fedora.server.journal.entry.ConsumerJournalEntry;
import fedora.server.journal.entry.JournalEntryContext;
import fedora.server.journal.helpers.JournalHelper;

/**
 * The abstract base for all JournalRecoveryLog classes.
 * 
 * @author Jim Blake
 */
public abstract class JournalRecoveryLog
        implements JournalConstants {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(JournalRecoveryLog.class.getName());

    private static final int LEVEL_LOW = 0;

    private static final int LEVEL_MEDIUM = 1;

    private static final int LEVEL_HIGH = 2;

    protected final ServerInterface server;

    private final int logLevel;

    /**
     * Create an instance of the proper JournalRecoveryLog child class, as
     * determined by the server parameters.
     */
    public static JournalRecoveryLog getInstance(Map<String, String> parameters,
                                                 String role,
                                                 ServerInterface server)
            throws ModuleInitializationException {

        try {
            Object recoveryLog =
                    JournalHelper
                            .createInstanceAccordingToParameter(PARAMETER_JOURNAL_RECOVERY_LOG_CLASSNAME,
                                                                new Class[] {
                                                                        Map.class,
                                                                        String.class,
                                                                        ServerInterface.class},
                                                                new Object[] {
                                                                        parameters,
                                                                        role,
                                                                        server},
                                                                parameters);
            LOG.info("JournalRecoveryLog is " + recoveryLog.toString());
            return (JournalRecoveryLog) recoveryLog;
        } catch (JournalException e) {
            throw new ModuleInitializationException("Can't create JournalRecoveryLog",
                                                    role,
                                                    e);
        }
    }

    /**
     * Concrete sub-classes must implement this constructor. Checks the server
     * parameters to find out what Logging Level to use - default is Low.
     */
    protected JournalRecoveryLog(Map<String, String> parameters,
                                 String role,
                                 ServerInterface server)
            throws ModuleInitializationException {
        this.server = server;

        String level = parameters.get(PARAMETER_RECOVERY_LOG_LEVEL);
        if (level == null) {
            logLevel = LEVEL_LOW;
        } else if (VALUE_RECOVERY_LOG_LEVEL_HIGH.equals(level)) {
            logLevel = LEVEL_HIGH;
        } else if (VALUE_RECOVERY_LOG_LEVEL_MEDIUM.equals(level)) {
            logLevel = LEVEL_MEDIUM;
        } else if (VALUE_RECOVERY_LOG_LEVEL_LOW.equals(level)) {
            logLevel = LEVEL_LOW;
        } else {
            throw new ModuleInitializationException("'"
                    + PARAMETER_RECOVERY_LOG_LEVEL + "' parameter must be '"
                    + VALUE_RECOVERY_LOG_LEVEL_LOW + "'(default), '"
                    + VALUE_RECOVERY_LOG_LEVEL_MEDIUM + "' or '"
                    + VALUE_RECOVERY_LOG_LEVEL_HIGH + "'", role);
        }
    }

    /**
     * Concrete sub-classes should probably synchronize this method, since it
     * can be called either from the JournalConsumerThread or from the Server.
     */
    public abstract void log(String message);

    /**
     * Concrete sub-classes should probably synchronize this method, since it
     * can be called either from the JournalConsumerThread or from the Server.
     * For the same reason, they should also provide for the possibility that it
     * will be called multiple times, or that a call to log() will happen after
     * the call to shutdown().
     */
    public abstract void shutdown();

    public void shutdown(String message) {
        log(message);
        shutdown();
    }

    /**
     * Concrete sub-classes should call this method from their constructor, or
     * as soon as the log is ready for writing.
     */
    public void logHeaderInfo(Map<String, String> parameters) {
        StringBuffer buffer = new StringBuffer("Recovery parameters:");
        for (Iterator<String> keys = parameters.keySet().iterator(); keys
                .hasNext();) {
            Object key = keys.next();
            Object value = parameters.get(key);
            buffer.append("\n    ").append(key).append("=").append(value);
        }
        log(buffer.toString());
    }

    /**
     * Format a journal entry for writing to the log. Take logging level into
     * account.
     */
    public void log(ConsumerJournalEntry journalEntry) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Event: method='").append(journalEntry.getMethodName())
                .append("', ").append(journalEntry.getIdentifier())
                .append("\n");
        if (logLevel == LEVEL_HIGH) {
            JournalEntryContext context = journalEntry.getContext();
            buffer.append("    context=").append(context.getClass().getName())
                    .append("\n");
            buffer.append(writeMapValues("environmentAttributes", context
                    .getEnvironmentAttributes()));
            buffer.append(writeMapValues("subjectAttributes", context
                    .getSubjectAttributes()));
            buffer.append(writeMapValues("actionAttributes", context
                    .getActionAttributes()));
            buffer.append(writeMapValues("resourceAttributes", context
                    .getResourceAttributes()));
            buffer.append(writeMapValues("recoveryAttributes", context
                    .getRecoveryAttributes()));
            buffer.append("        password='*********'\n");
            buffer.append("        noOp=").append(context.getNoOp())
                    .append("\n");
        }
        if (logLevel == LEVEL_HIGH || logLevel == LEVEL_MEDIUM) {
            buffer.append("        now=" + journalEntry.getContext().getNoOp()
                    + "\n");
            buffer.append("    arguments\n");
            Map argumentsMap = journalEntry.getArgumentsMap();
            for (Iterator names = argumentsMap.keySet().iterator(); names
                    .hasNext();) {
                String name = (String) names.next();
                Object value = argumentsMap.get(name);
                if (value instanceof String[]) {
                    buffer.append(writeStringArray(name, (String[]) value));
                } else {
                    buffer.append("        " + name + "='" + value + "'\n");
                }
            }
        }
        log(buffer.toString());
    }

    /**
     * Helper for the {@link #log(ConsumerJournalEntry)} method. Writes the
     * values of a string array
     */
    private String writeStringArray(String name, String[] values) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("        ").append(name).append("=[");
        for (int i = 0; i < values.length; i++) {
            buffer.append("'").append(values[i]).append("'");
            if (i < values.length - 1) {
                buffer.append(", ");
            }
        }
        buffer.append("]\n");
        return buffer.toString();
    }

    /**
     * Helper for the {@link #log(ConsumerJournalEntry)} method. Writes the
     * values of a context multi-map.
     */
    private String writeMapValues(String mapName, MultiValueMap map) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("        " + mapName + "\n");
        for (Iterator names = map.names(); names.hasNext();) {
            String name = (String) names.next();
            buffer.append("            ").append(name).append("\n");
            String[] values = map.getStringArray(name);
            for (String element : values) {
                buffer.append("                ").append(element).append("\n");
            }
        }
        return buffer.toString();
    }

    /**
     * Concrete sub-classes call this method to perform the final formatting if
     * a log entry.
     */
    protected void log(String message, Writer writer) {
        try {
            writer.write(JournalHelper.formatDate(new Date()) + ": " + message
                    + "\n");
        } catch (IOException e) {
            LOG.error("Error writing journal log entry", e);
        }
    }

}
