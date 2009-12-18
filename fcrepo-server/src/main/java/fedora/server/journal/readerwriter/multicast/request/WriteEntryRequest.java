/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast.request;

import fedora.server.journal.JournalException;
import fedora.server.journal.entry.CreatorJournalEntry;
import fedora.server.journal.readerwriter.multicast.MulticastJournalWriter;
import fedora.server.journal.readerwriter.multicast.Transport;

/**
 * TransportRequest that writes a journalEntry to each Transport.
 */
public class WriteEntryRequest
        extends TransportRequest {

    private final MulticastJournalWriter journalWriter;

    private final CreatorJournalEntry journalEntry;

    public WriteEntryRequest(MulticastJournalWriter journalWriter,
                             CreatorJournalEntry journalEntry) {
        this.journalWriter = journalWriter;
        this.journalEntry = journalEntry;
    }

    @Override
    public void performRequest(Transport transport) throws JournalException {
        journalWriter.writeJournalEntry(journalEntry, transport.getWriter());
    }
}