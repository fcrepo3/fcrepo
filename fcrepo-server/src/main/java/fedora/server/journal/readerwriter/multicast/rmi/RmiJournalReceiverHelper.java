/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast.rmi;

/**
 * <p>
 * RmiJournalReceiverHelper.java
 * </p>
 * <p>
 * Utility methods that are used by multiple RMI-related classes.
 * </p>
 *
 * @author jblake
 * @version $Id: RmiJournalReceiverHelper.java,v 1.3 2007/06/01 17:21:32 jblake
 *          Exp $
 */
public class RmiJournalReceiverHelper {

    /**
     * The writer and the receiver each use this method to figure the hash on
     * each journal entry. If the receiver calculates a different hash from the
     * one that appears on the entry, it will throw an exception.
     */
    public static String figureIndexedHash(String repositoryHash,
                                           long entryIndex) {
        return String.valueOf((entryIndex + repositoryHash).hashCode());
    }

}
