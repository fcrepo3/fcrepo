/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import fedora.client.utility.validate.ValidationResult;
import fedora.client.utility.validate.ValidationResultNotation;
import fedora.client.utility.validate.ValidationResults;

/**
 * An implementation of {@link ValidationResults} for use with the
 * {@link ValidatorProcess}. When {@link #record(ValidationResult)} is called,
 * the result is evaluated against the current Log4J configuration. If any of
 * the notes qualify for logging, they will be preceded by an overall log
 * record.
 * 
 * @author Jim Blake
 */
public class Log4jValidationResults
        implements ValidationResults {

    public static final String LOGGING_CATEGORY_PREFIX = "Validator";

    /** These Log4J settings will be used if no others are provided. */
    private static final Properties DEFAULT_CONFIG_PROPERTIES =
            initDefaultProperties();

    /** Initialize the {@link #DEFAULT_CONFIG_PROPERTIES}. */
    private static Properties initDefaultProperties() {
        Properties props = new Properties();

        // Create an appender for the root logger.
        props.put("log4j.appender.STDOUT", "org.apache.log4j.ConsoleAppender");
        props.put("log4j.appender.STDOUT.layout",
                  "org.apache.log4j.PatternLayout");
        props.put("log4j.appender.STDOUT.layout.ConversionPattern",
                  "%d{yyyy-MM-dd' 'HH:mm:ss.SSS} %p [%c] %m%n");

        // Assign the appender to the root logger.
        props.put("log4j.rootLogger", "INFO, STDOUT");

        // Create an appender for the validation messages.
        props.put("log4j.appender.VALIDATOR",
                  "org.apache.log4j.ConsoleAppender");
        props.put("log4j.appender.VALIDATOR.layout",
                  "org.apache.log4j.PatternLayout");
        props.put("log4j.appender.VALIDATOR.layout.ConversionPattern",
                  "%p [%c] %m%n");

        // Assign the appender to the validator, with no pass-through.
        props.put("log4j.logger.Validator=INFO", "INFO, VALIDATOR");
        props.put("log4j.additivity.Validator", "false");

        return props;
    }

    /** How many notations were at error level? */
    private int numberOfErrors;

    /** How many notations were at error level and printed to the log? */
    private int numberOfFilteredErrors;

    /** How many notations were at warning level? */
    private int numberOfWarnings;

    /** How many notations were at warning level and printed to the log? */
    private int numberOfFilteredWarnings;

    /** How many object had no errors and no warnings? */
    private int numberOfValidObjects;

    /** How many objects had errors? */
    private int numberOfInvalidObjects;

    /** How many objects had warnings but no errors? */
    private int numberOfIndeterminateObjects;

    public Log4jValidationResults(Properties configProperties) {
        LogManager.resetConfiguration();

        if (configProperties == null || configProperties.isEmpty()) {
            PropertyConfigurator.configure(DEFAULT_CONFIG_PROPERTIES);
        } else {
            PropertyConfigurator.configure(configProperties);
        }
    }

    /**
     * This class does not maintain a collection of the {@link ValidationResult}
     * objects. Instead, it logs each as it arrives, if it is severe enough.
     */
    public void record(ValidationResult rawResult) {
        // If the overall log level is set to debug, dump the result to the log.
        getBaseLogger().debug(rawResult.toString());

        // Process the result to get the info we will need.
        Log4jValidationResult result = new Log4jValidationResult(rawResult);

        // Analyze this result and update the statistics accordingly.
        incrementTallys(result);

        // Any notes that are not filtered out get written to the log.
        for (Log4jNote note : result.getNotes()) {
            note.log();
        }
    }

    /**
     * Each {@link ValidationResult} was logged (or not) when it arrived, so
     * just log a summary message at the end.
     */
    public void closeResults() {
        Logger logger = getBaseLogger();

        logger.info("Validated " + numberOfObjects() + " objects: "
                + numberOfValidObjects + " valid, " + numberOfInvalidObjects
                + " invalid, " + numberOfIndeterminateObjects
                + " indeterminate.");

        if (numberOfErrors == numberOfFilteredErrors
                && numberOfWarnings == numberOfFilteredWarnings) {
            logger.info(numberOfErrors + " errors, " + numberOfWarnings
                    + " warnings.");
        } else {
            logger.info(numberOfFilteredErrors + " filtered errors ("
                    + numberOfErrors + " unfiltered), "
                    + numberOfFilteredWarnings + " filtered warnings ("
                    + numberOfWarnings + " unfiltered)");
        }
    }

    /**
     * How many object were validated?
     */
    private int numberOfObjects() {
        return numberOfValidObjects + numberOfIndeterminateObjects
                + numberOfInvalidObjects;
    }

    /**
     * Adjust the running tallys to include this result.
     */
    private void incrementTallys(Log4jValidationResult result) {
        // For the whole record: valid? invalid? indeterminate?
        switch (result.getSeverityLevel()) {
            case ERROR:
                numberOfInvalidObjects++;
                break;
            case WARN:
                numberOfIndeterminateObjects++;
                break;
            default:
                numberOfValidObjects++;
                break;
        }

        for (Log4jNote note : result.getNotes()) {
            // For each note: error? warning?
            switch (note.getLevel()) {
                case ERROR:
                    numberOfErrors++;
                    break;
                case WARN:
                    numberOfWarnings++;
                    break;
                default:
                    break;
            }
            if (note.isLoggable()) {
                // For each loggable note: error? warning?
                switch (note.getLevel()) {
                    case ERROR:
                        numberOfFilteredErrors++;
                        break;
                    case WARN:
                        numberOfFilteredWarnings++;
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private Logger getBaseLogger() {
        return Logger.getLogger(LOGGING_CATEGORY_PREFIX);
    }

    /**
     * Extracts information from a {@link ValidationResult} and then serves it
     * up when we need it.
     */
    private static class Log4jValidationResult {

        private final String pid;

        private final ValidationResult.Level severityLevel;

        private final List<Log4jNote> notes;

        public Log4jValidationResult(ValidationResult rawResult) {
            pid = rawResult.getObject().getPid();
            severityLevel = rawResult.getSeverityLevel();
            List<Log4jNote> notes = new ArrayList<Log4jNote>();
            for (ValidationResultNotation rawNote : rawResult.getNotes()) {
                notes.add(new Log4jNote(rawNote, pid));
            }
            this.notes = Collections.unmodifiableList(notes);
        }

        public String getPid() {
            return pid;
        }

        public ValidationResult.Level getSeverityLevel() {
            return severityLevel;
        }

        public List<Log4jNote> getNotes() {
            return notes;
        }
    }

    /**
     * Extracts information from a {@link ValidationResultNotation} and then
     * serves it up when we need it.
     */
    private static class Log4jNote {

        private final ValidationResult.Level level;

        private final Level loggingLevel;

        private final Logger logger;

        private final boolean loggable;

        private final String message;

        public Log4jNote(ValidationResultNotation rawNote, String objectPid) {
            level = rawNote.getLevel();
            loggingLevel = Level.toLevel(rawNote.getLevel().toString());

            String category =
                    LOGGING_CATEGORY_PREFIX + "." + rawNote.getCategory();
            logger = Logger.getLogger(category);

            loggable =
                    loggingLevel.isGreaterOrEqual(logger.getEffectiveLevel());

            message = "pid='" + objectPid + "'  " + rawNote.getMessage();
        }

        public ValidationResult.Level getLevel() {
            return level;
        }

        public boolean isLoggable() {
            return loggable;
        }

        public void log() {
            if (loggable) {
                logger.log(loggingLevel, message);
            }
        }
    }
}
