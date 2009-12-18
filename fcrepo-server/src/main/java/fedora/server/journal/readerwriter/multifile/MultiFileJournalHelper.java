/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.readerwriter.multifile;

import java.io.File;
import java.io.FileFilter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fedora.server.journal.JournalConstants;
import fedora.server.journal.JournalException;

/**
 * Utility methods for use by the Multi-file Journaling classes.
 * 
 * @author Jim Blake
 */
public class MultiFileJournalHelper
        implements JournalConstants, MultiFileJournalConstants {

    /**
     * Get the value of a parameter if there is one, or the default value if
     * there isn't.
     */

    /**
     * Get the requested parameter, or throw an exception if it is not found.
     */
    static String getRequiredParameter(Map<String, String> parameters,
                                       String parameterName)
            throws JournalException {
        String value = parameters.get(parameterName);
        if (value == null) {
            throw new JournalException("'" + parameterName + "' is required.");
        }
        return value;
    }

    /**
     * Find the polling interval that we will choose when checking for new
     * journal files to appear.
     */
    static long parseParametersForPollingInterval(Map<String, String> parameters)
            throws JournalException {
        String intervalString =
                parameters.get(PARAMETER_FOLLOW_POLLING_INTERVAL);
        if (intervalString == null) {
            intervalString = DEFAULT_FOLLOW_POLLING_INTERVAL;
        }
        Pattern p = Pattern.compile("([0-9]+)([HM]?)");
        Matcher m = p.matcher(intervalString);
        if (!m.matches()) {
            throw new JournalException("Parameter '"
                    + PARAMETER_FOLLOW_POLLING_INTERVAL
                    + "' must be an positive integer number of seconds, "
                    + "optionally followed by 'H'(hours), or 'M'(minutes)");
        }
        long interval = Long.parseLong(m.group(1)) * 1000;
        String factor = m.group(2);
        if ("H".equals(factor)) {
            interval *= 60 * 60;
        } else if ("M".equals(factor)) {
            interval *= 60;
        }
        return interval;
    }

    /**
     * Get the Journal Files that exist the Journal Directory, sorted by name.
     */
    static File[] getSortedArrayOfJournalFiles(File journalDirectory,
                                               String filenamePrefix) {
        JournalFileFilter filter = new JournalFileFilter(filenamePrefix);
        File[] journalFiles = journalDirectory.listFiles(filter);
        Arrays.sort(journalFiles, new FilenameComparator());
        return journalFiles;
    }

    /**
     * Allows us to search a directory for files that match the prefix.
     */
    private static class JournalFileFilter
            implements FileFilter {

        private final String filenamePrefix;

        JournalFileFilter(String filenamePrefix) {
            this.filenamePrefix = filenamePrefix;
        }

        public boolean accept(File file) {
            String filename = file.getName();
            return filename.startsWith(filenamePrefix);
        }
    }

    /**
     * A comparator that sorts files by their names.
     */
    private static class FilenameComparator
            implements Comparator<File> {

        public int compare(File first, File second) {
            return first.getName().compareTo(second.getName());
        }
    }

}
