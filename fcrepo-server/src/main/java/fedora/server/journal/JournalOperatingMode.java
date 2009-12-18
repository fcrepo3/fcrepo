/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal;

import fedora.server.errors.InvalidStateException;
import fedora.server.errors.ServerException;
import fedora.server.journal.entry.CreatorJournalEntry;

/**
 * <p>
 * <b>Title:</b> JournalOperatingMode.java
 * </p>
 * <p>
 * <b>Description:</b> A mechanism for kicking a server from normal
 * (Journal-Creating) mode, to disabled (Read-Only) mode. Any
 * {@link CreatorJournalEntry} must call
 * {@link JournalOperatingMode#enforceCurrentMode} before performing an
 * operation that might modify the repository.
 * </p>
 *
 * @author jblake
 * @version $Id: JournalOperatingMode.java,v 1.3 2007/06/01 17:21:31 jblake Exp $
 */
public enum JournalOperatingMode {
    NORMAL, READ_ONLY;

    private static JournalOperatingMode currentMode = NORMAL;

    /**
     * Set the current mode.
     */
    public static void setMode(JournalOperatingMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Journal operating mode may not be null");
        }
        JournalOperatingMode.currentMode = mode;
    }

    /**
     * Get the current mode.
     */
    public static Object getMode() {
        return currentMode;
    }

    /**
     * If a modifying operation is attempted while we are in Read-Only mode,
     * throw an exception to prevent it. In Normal mode, do nothing.
     *
     * @throws ServerException
     *         to prevent a modifying operation in Read-Only mode.
     */
    public static void enforceCurrentMode() throws ServerException {
        switch (currentMode) {
            case READ_ONLY:
                throw new InvalidStateException("Server is in Read-Only mode, pursuant to a Journaling error.");
            default:
        }
    }

}
