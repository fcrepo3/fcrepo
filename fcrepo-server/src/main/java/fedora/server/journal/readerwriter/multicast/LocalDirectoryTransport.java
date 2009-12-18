/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast;

import java.io.File;
import java.io.IOException;

import java.util.Date;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import javanet.staxutils.IndentingXMLEventWriter;

import fedora.server.journal.JournalException;
import fedora.server.journal.helpers.ParameterHelper;

/**
 * <p>
 * LocalDirectoryTransport.java
 * </p>
 * <p>
 * Writes Journal files to a local disk directory. It requires these parameters:
 * <ul>
 * <li>directoryPath - full path to the directory where the Journals will be
 * stored.</li>
 * </ul>
 * </p>
 *
 * @author jblake
 * @version $Id: LocalDirectoryTransport.java,v 1.1 2007/03/06 15:02:58 jblake
 *          Exp $
 */
public class LocalDirectoryTransport
        extends Transport {

    public static final String PARAMETER_DIRECTORY_PATH = "directoryPath";

    /** The directory in which to create journal files. */
    private final File directory;

    /** The current journal file, if open, or most recent file if closed. */
    private TransportOutputFile journalFile;

    /** An XMLEventWriter that writes to the current journal file, if open. */
    private XMLEventWriter xmlWriter;

    public LocalDirectoryTransport(Map<String, String> parameters,
                                   boolean crucial,
                                   TransportParent parent)
            throws JournalException {
        super(parameters, crucial, parent);
        directory =
                ParameterHelper
                        .parseParametersForWritableDirectory(parameters,
                                                             PARAMETER_DIRECTORY_PATH);
    }

    /**
     * On a request to open the file,
     * <ul>
     * <li>check that we are in a valid state,</li>
     * <li>create the file,</li>
     * <li>create the {@link XMLEventWriter} for use on the file,</li>
     * <li>ask the parent to write the header to the file,</li>
     * <li>set the state.</li>
     * </ul>
     */
    @Override
    public void openFile(String repositoryHash,
                         String filename,
                         Date currentDate) throws JournalException {
        try {
            super.testStateChange(State.FILE_OPEN);

            journalFile = new TransportOutputFile(directory, filename);

            xmlWriter =
                    new IndentingXMLEventWriter(XMLOutputFactory.newInstance()
                            .createXMLEventWriter(journalFile.open()));

            parent.writeDocumentHeader(xmlWriter, repositoryHash, currentDate);

            super.setState(State.FILE_OPEN);
        } catch (FactoryConfigurationError e) {
            throw new JournalException(e);
        } catch (XMLStreamException e) {
            throw new JournalException(e);
        } catch (IOException e) {
            throw new JournalException(e);
        }
    }

    /**
     * Check that our current state is correct before filling a request for an
     * {@link XMLEventWriter}.
     */
    @Override
    public XMLEventWriter getWriter() throws JournalException {
        super.testWriterState();
        return xmlWriter;
    }

    /**
     * On a request to close the file,
     * <ul>
     * <li>check that we are in a valid state,</li>
     * <li>close the {@link XMLEventWriter} and the {@link TransportOutputFile},</li>
     * <li>set the state.</li>
     * </ul>
     */
    @Override
    public void closeFile() throws JournalException {
        try {
            super.testStateChange(State.FILE_CLOSED);
            parent.writeDocumentTrailer(xmlWriter);
            xmlWriter.close();
            journalFile.close();
            super.setState(State.FILE_CLOSED);
        } catch (XMLStreamException e) {
            throw new JournalException(e);
        } catch (IOException e) {
            throw new JournalException(e);
        }
    }

    /**
     * On a request to shut down,
     * <ul>
     * <li>check that we are in a valid state,</li>
     * <li>set the state.</li>
     * </ul>
     * If we have already shut down, a second call is not an error, but requires
     * no action.
     */
    @Override
    public void shutdown() throws JournalException {
        super.testStateChange(State.SHUTDOWN);
        if (super.getState() != State.SHUTDOWN) {
            super.setState(State.SHUTDOWN);
        }
    }

}
