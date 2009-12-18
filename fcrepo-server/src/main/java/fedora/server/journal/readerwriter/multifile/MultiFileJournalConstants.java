/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.readerwriter.multifile;

/**
 * Parameters, formats and default values for use by the Multi-file journaling
 * classes.
 * 
 * @author Jim Blake
 */
public interface MultiFileJournalConstants {

    String PARAMETER_JOURNAL_DIRECTORY = "journalDirectory";

    String PARAMETER_ARCHIVE_DIRECTORY = "archiveDirectory";

    /** Used by following readers */
    String PARAMETER_FOLLOW_POLLING_INTERVAL = "followPollingInterval";

    /** Used by following readers */
    String DEFAULT_FOLLOW_POLLING_INTERVAL = "3";

    /** Used by locking readers like {@link LockingFollowingJournalReader} */
    String PARAMETER_LOCK_REQUESTED_FILENAME = "lockRequestedFilename";

    /** Used by locking readers like {@link LockingFollowingJournalReader} */
    String PARAMETER_LOCK_ACCEPTED_FILENAME = "lockAcceptedFilename";

    /** Used by locking readers like {@link LockingFollowingJournalReader} */
    String PARAMETER_PAUSE_BEFORE_POLLING = "pauseBeforePolling";
}
