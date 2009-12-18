/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast;

import java.io.StringWriter;

import java.util.Date;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import javanet.staxutils.IndentingXMLEventWriter;

import fedora.server.journal.JournalException;

public class MockTransport
        extends Transport {

    private int howManyOpenFileRequests;

    private int howManyGetWriterRequests;

    private int howManyCloseFileRequests;

    private int howManyShutdownRequests;

    private String repositoryHash;

    private String filename;

    private Date currentDate;

    private StringWriter stringWriter;

    private XMLEventWriter xmlWriter;

    private boolean throwExceptionOnGetWriter;

    // -------------------------------------------------------------------------
    // Mocking infrastructure.
    // -------------------------------------------------------------------------

    public MockTransport(Map<String, String> parameters,
                         boolean crucial,
                         TransportParent parent)
            throws JournalException {
        super(parameters, crucial, parent);
    };

    public int getHowManyCloseFileRequests() {
        return howManyCloseFileRequests;
    }

    public int getHowManyGetWriterRequests() {
        return howManyGetWriterRequests;
    }

    public int getHowManyOpenFileRequests() {
        return howManyOpenFileRequests;
    }

    public int getHowManyShutdownRequests() {
        return howManyShutdownRequests;
    }

    public String getFilename() {
        return filename;
    }

    public Date getCurrentDate() {
        return currentDate;
    }

    public String getRepositoryHash() {
        return repositoryHash;
    }

    public String getFileContents() {
        return stringWriter.getBuffer().toString();
    }

    public void setThrowExceptionOnGetWriter(boolean throwExceptionOnGetWriter) {
        this.throwExceptionOnGetWriter = throwExceptionOnGetWriter;
    }

    // -------------------------------------------------------------------------
    // Mocked methods.
    // -------------------------------------------------------------------------

    @Override
    public void openFile(String repositoryHash,
                         String filename,
                         Date currentDate) throws JournalException {
        howManyOpenFileRequests++;
        this.repositoryHash = repositoryHash;
        this.filename = filename;
        this.currentDate = currentDate;
        try {
            stringWriter = new StringWriter();
            xmlWriter =
                    new IndentingXMLEventWriter(XMLOutputFactory.newInstance()
                            .createXMLEventWriter(stringWriter));
        } catch (XMLStreamException e) {
            throw new JournalException(e);
        } catch (FactoryConfigurationError e) {
            throw new JournalException(e);
        }
    }

    @Override
    public XMLEventWriter getWriter() throws JournalException {
        howManyGetWriterRequests++;
        if (throwExceptionOnGetWriter) {
            throw new JournalException("forced Exception on getWriter()");
        }
        return xmlWriter;
    }

    @Override
    public void closeFile() throws JournalException {
        howManyCloseFileRequests++;
    }

    @Override
    public void shutdown() throws JournalException {
        howManyShutdownRequests++;
    }

}