/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal;

import java.io.StringReader;

import java.util.Date;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import fedora.server.journal.entry.ConsumerJournalEntry;
import fedora.server.journal.recoverylog.JournalRecoveryLog;

/**
 * Read Journal XML from a String buffer. If the buffer is null or empty, treat
 * it as an empty Journal.
 *
 * @author Jim Blake
 */
public class MockJournalReader
        extends JournalReader {

    private static String buffer;

    public static void setBuffer(String buffer) {
        MockJournalReader.buffer = buffer;
    }

    private StringReader stringReader;

    private XMLEventReader xmlReader;

    public MockJournalReader(Map<String, String> parameters,
                             String role,
                             JournalRecoveryLog recoveryLog,
                             ServerInterface server)
            throws JournalException {
        super(parameters, role, recoveryLog, server);
    }

    @Override
    public ConsumerJournalEntry readJournalEntry() throws JournalException,
            XMLStreamException {
        prepareXmlReader();

        if (xmlReader == null) {
            return null;
        }

        advancePastWhitespace(xmlReader);

        XMLEvent next = xmlReader.peek();
        if (isStartTagEvent(next, QNAME_TAG_JOURNAL_ENTRY)) {
            ConsumerJournalEntry journalEntry =
                    super.readJournalEntry(xmlReader);
            journalEntry.setIdentifier(new Date().toString());
            return journalEntry;
        } else if (isEndTagEvent(next, QNAME_TAG_JOURNAL)) {
            return null;
        } else {
            throw getNotNextMemberOrEndOfGroupException(QNAME_TAG_JOURNAL,
                                                        QNAME_TAG_JOURNAL_ENTRY,
                                                        next);
        }
    }

    @Override
    public void shutdown() throws JournalException {
        try {
            if (xmlReader != null) {
                xmlReader.close();
            }
            if (stringReader != null) {
                stringReader.close();
            }
        } catch (XMLStreamException e) {
            throw new JournalException(e);
        }
    }

    /**
     * Advance past the document header to the first JournalEntry.
     */
    private void advanceIntoFile() throws XMLStreamException, JournalException {
        XMLEvent event = xmlReader.nextEvent();
        if (!event.isStartDocument()) {
            throw new JournalException("Expecting XML document header, but event was '"
                    + event + "'");
        }

        event = xmlReader.nextTag();
        if (!isStartTagEvent(event, QNAME_TAG_JOURNAL)) {
            throw new JournalException("Expecting FedoraJournal start tag, but event was '"
                    + event + "'");
        }

        String hash =
                getOptionalAttributeValue(event.asStartElement(),
                                          QNAME_ATTR_REPOSITORY_HASH);
        checkRepositoryHash(hash);
    }

    private void prepareXmlReader() throws JournalException {
        if (xmlReader != null) {
            return;
        }
        if (buffer == null || buffer.length() == 0) {
            return;
        }

        stringReader = new StringReader(buffer);

        try {
            xmlReader =
                    XMLInputFactory.newInstance()
                            .createXMLEventReader(stringReader);
            advanceIntoFile();
        } catch (XMLStreamException e) {
            throw new JournalException(e);
        } catch (FactoryConfigurationError e) {
            throw new JournalException(e);
        }
    }
}
