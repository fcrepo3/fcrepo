/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.helpers;

import java.io.File;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fedora.server.journal.JournalConstants;
import fedora.server.journal.JournalException;

/**
 * A collection of utility methods to help the Journal classes to read parameter
 * values.
 * 
 * @author Jim Blake
 */
public class ParameterHelper
        implements JournalConstants {

    private ParameterHelper() {
        // no need to instantiate - all methods are static.
    }

    private static void validateParameters(Map<String, String> parameters) {
        if (parameters == null) {
            throw new NullPointerException("'parameters' may not be null.");
        }
    }

    private static void validateParameterName(String parameterName) {
        if (parameterName == null) {
            throw new NullPointerException("'parameterName' may not be null.");
        }
    }

    /**
     * Get an optional String parameter, If not found, use the default value.
     * 
     * @throws NullPointerException
     *         if either 'parameters' or 'parameterName' is null.
     */
    public static String getOptionalStringParameter(Map<String, String> parameters,
                                                    String parameterName,
                                                    String defaultValue) {
        validateParameters(parameters);
        validateParameterName(parameterName);

        String value = parameters.get(parameterName);
        return value == null ? defaultValue : value;
    }

    /**
     * Get an optional boolean parameter. If not found, use the default value.
     * 
     * @throws JournalException
     *         if a value is supplied that is neither "true" nor "false".
     * @throws NullPointerException
     *         if either 'parameters' or 'parameterName' is null.
     */
    public static boolean getOptionalBooleanParameter(Map<String, String> parameters,
                                                      String parameterName,
                                                      boolean defaultValue)
            throws JournalException {
        validateParameters(parameters);
        validateParameterName(parameterName);

        String string = parameters.get(parameterName);
        if (string == null) {
            return defaultValue;
        } else if (string.equals(VALUE_FALSE)) {
            return false;
        } else if (string.equals(VALUE_TRUE)) {
            return true;
        } else {
            throw new JournalException("'" + parameterName
                    + "' parameter must be '" + VALUE_FALSE + "'(default) or '"
                    + VALUE_TRUE + "'");
        }

    }

    /**
     * Look in the parameters for the path to a writable directory. The
     * parameter is required.
     */
    public static File parseParametersForWritableDirectory(Map<String, String> parameters,
                                                           String parameterName)
            throws JournalException {
        String directoryString = parameters.get(parameterName);
        if (directoryString == null) {
            throw new JournalException("'" + parameterName + "' is required.");
        }
        File directory = new File(directoryString);
        if (!directory.exists()) {
            throw new JournalException("Directory '" + directory
                    + "' does not exist.");
        }
        if (!directory.isDirectory()) {
            throw new JournalException("Directory '" + directory
                    + "' is not a directory.");
        }
        if (!directory.canWrite()) {
            throw new JournalException("Directory '" + directory
                    + "' is not writable.");
        }
        return directory;
    }

    /**
     * Look for a string to use as a prefix for the names of the journal files.
     * Default is "fedoraJournal"
     */
    public static String parseParametersForFilenamePrefix(Map<String, String> parameters) {
        return getOptionalStringParameter(parameters,
                                          PARAMETER_JOURNAL_FILENAME_PREFIX,
                                          DEFAULT_FILENAME_PREFIX);
    }

    /**
     * Get the size limit parameter (or let it default), and convert it to
     * bytes.
     */
    public static long parseParametersForSizeLimit(Map<String, String> parameters)
            throws JournalException {
        String sizeString =
                getOptionalStringParameter(parameters,
                                           PARAMETER_JOURNAL_FILE_SIZE_LIMIT,
                                           DEFAULT_SIZE_LIMIT);
        Pattern p = Pattern.compile("([0-9]+)([KMG]?)");
        Matcher m = p.matcher(sizeString);
        if (!m.matches()) {
            throw new JournalException("Parameter '"
                    + PARAMETER_JOURNAL_FILE_SIZE_LIMIT
                    + "' must be an integer number of bytes, "
                    + "optionally followed by 'K', 'M', or 'G', "
                    + "or a 0 to indicate no size limit");
        }
        long size = Long.parseLong(m.group(1));
        String factor = m.group(2);
        if ("K".equals(factor)) {
            size *= 1024;
        } else if ("M".equals(factor)) {
            size *= 1024 * 1024;
        } else if ("G".equals(factor)) {
            size *= 1024 * 1024 * 1024;
        }
        return size;
    }

    /**
     * Get the age limit parameter (or let it default), and convert it to
     * milliseconds.
     */
    public static long parseParametersForAgeLimit(Map<String, String> parameters)
            throws JournalException {
        String ageString =
                ParameterHelper
                        .getOptionalStringParameter(parameters,
                                                    PARAMETER_JOURNAL_FILE_AGE_LIMIT,
                                                    DEFAULT_AGE_LIMIT);
        Pattern p = Pattern.compile("([0-9]+)([DHM]?)");
        Matcher m = p.matcher(ageString);
        if (!m.matches()) {
            throw new JournalException("Parameter '"
                    + PARAMETER_JOURNAL_FILE_AGE_LIMIT
                    + "' must be an integer number of seconds, optionally "
                    + "followed by 'D'(days), 'H'(hours), or 'M'(minutes), "
                    + "or a 0 to indicate no age limit");
        }
        long age = Long.parseLong(m.group(1)) * 1000;
        String factor = m.group(2);
        if ("D".equals(factor)) {
            age *= 24 * 60 * 60;
        } else if ("H".equals(factor)) {
            age *= 60 * 60;
        } else if ("M".equals(factor)) {
            age *= 60;
        }
        return age;
    }

}
