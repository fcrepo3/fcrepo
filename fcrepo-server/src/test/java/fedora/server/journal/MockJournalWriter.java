/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal;

import java.io.IOException;
import java.io.StringWriter;

import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import javanet.staxutils.IndentingXMLEventWriter;

import fedora.server.journal.entry.CreatorJournalEntry;

/**
 * <p>
 * A {@link JournalWriter} that writes to a {@link String} buffer.
 * </p>
 * <p>
 * Since the instance is created dynamically by the {@link JournalCreator}, the
 * buffer must be static and accessible at the class level. The buffer is set
 * when the writer is shut down. This means that the buffer contents would be
 * lost if not read before the next instance is created and shut down, but that
 * should not pose a problem in unit tests.
 * </p>
 * 
 * @author Jim Blake
 */
public class MockJournalWriter
        extends JournalWriter {

    private static String buffer;

    public static String getBuffer() {
        return buffer;
    }

    private final StringWriter stringWriter = new StringWriter();

    private final XMLEventWriter xmlWriter;

    private boolean firstEntry = true;

    public MockJournalWriter(Map<String, String> parameters,
                             String role,
                             ServerInterface server)
            throws XMLStreamException, FactoryConfigurationError {
        super(parameters, role, server);
        xmlWriter =
                new IndentingXMLEventWriter(XMLOutputFactory.newInstance()
                        .createXMLEventWriter(stringWriter));
    }

    @Override
    public void prepareToWriteJournalEntry() throws JournalException {
        if (firstEntry) {
            super.writeDocumentHeader(xmlWriter);
            firstEntry = false;
        }
    }

    @Override
    public void shutdown() throws JournalException {
        try {
            super.writeDocumentTrailer(xmlWriter);
            xmlWriter.close();
            stringWriter.close();
            buffer = stringWriter.toString();
        } catch (XMLStreamException e) {
            throw new JournalException(e);
        } catch (IOException e) {
            throw new JournalException(e);
        }
    }

    @Override
    public void writeJournalEntry(CreatorJournalEntry journalEntry)
            throws JournalException {
        super.writeJournalEntry(journalEntry, xmlWriter);
    }
}
