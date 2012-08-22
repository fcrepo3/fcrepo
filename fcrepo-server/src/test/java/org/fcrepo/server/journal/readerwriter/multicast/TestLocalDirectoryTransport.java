/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.journal.readerwriter.multicast;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;

import org.fcrepo.server.journal.AbstractJournalTester;
import org.fcrepo.server.journal.JournalException;
import org.fcrepo.server.journal.MockServerForJournalTesting;
import org.fcrepo.server.management.MockManagementDelegate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestLocalDirectoryTransport
        extends AbstractJournalTester {

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestMulticastJournalWriterInitializations.class);
    }

    private static final String EXPECTED_JOURNAL_1_CONTENTS =
            "<?xml " + "version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<FedoraJournal repositoryHash=\"firstSillyHash\" "
                    + "timestamp=\"2007-03-05T16:49:21.392-0500\">\n"
                    + "  <junkElement1a></junkElement1a>\n"
                    + "  <junkElement1b></junkElement1b></FedoraJournal>\n";

    private static final String EXPECTED_JOURNAL_2_CONTENTS =
            "<?xml " + "version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<FedoraJournal repositoryHash=\"secondSillyHash\" "
                    + "timestamp=\"2007-03-05T16:49:21.392-0500\">\n"
                    + "  <junkElement2></junkElement2></FedoraJournal>\n";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File journalDirectory;

    // immaterial to the test - required by the constructor.
    private static final boolean CRUCIAL = true;

    private Map<String, String> parameters;

    private MockMulticastJournalWriter parent;

    @Before
    public void createJournalDirectory() throws IOException {
        journalDirectory = folder.newFolder("TestLocalDirectoryTransport");
    }

    @Before
    public void initalizeBasicParameters() {
        parameters = new HashMap<String, String>();
        parameters.put(LocalDirectoryTransport.PARAMETER_DIRECTORY_PATH,
                       journalDirectory.getAbsolutePath());
    }

    @Before
    public void initializeTransportParent() throws JournalException {
        MockServerForJournalTesting server =
                new MockServerForJournalTesting(new MockManagementDelegate(),
                                                "myHashValue");
        parent =
                new MockMulticastJournalWriter(new HashMap<String, String>(),
                                               null,
                                               server);
    }

    @Test
    public void testParameterNoDirectoryPath() {
        parameters.remove(LocalDirectoryTransport.PARAMETER_DIRECTORY_PATH);
        try {
            new LocalDirectoryTransport(parameters, CRUCIAL, null);
            fail("expected a JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

    @Test
    public void testParametersInvalidDirectory() {
        parameters.put(LocalDirectoryTransport.PARAMETER_DIRECTORY_PATH,
                       "BogusDirectoryName");
        try {
            new LocalDirectoryTransport(parameters, CRUCIAL, null);
            fail("expected a JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

    @Test
    public void testParametersSuccess() throws JournalException {
        new LocalDirectoryTransport(parameters, CRUCIAL, null);
    }

    @Test
    public void testOperations() throws JournalException, IOException,
            XMLStreamException, ParseException {
        XMLEventFactory factory = XMLEventFactory.newInstance();
        QName name1a = new QName("junkElement1a");
        QName name1b = new QName("junkElement1b");
        QName name2 = new QName("junkElement2");
        SimpleDateFormat parser =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        Transport transport =
                new LocalDirectoryTransport(parameters, CRUCIAL, parent);

        // open creates the temp file
        parent.setCurrentDate(parser.parse("2007-03-05T16:49:21.392-0500"));
        transport
                .openFile("firstSillyHash", "fileOne", parent.getCurrentDate());
        File tempfile1 = new File(journalDirectory, "_fileOne");
        assertFileExists(tempfile1);

        // write to the file
        transport.getWriter().add(factory
                .createStartElement(name1a, null, null));
        transport.getWriter().add(factory.createEndElement(name1a, null));
        transport.getWriter().add(factory
                .createStartElement(name1b, null, null));
        transport.getWriter().add(factory.createEndElement(name1b, null));

        // closing renames the file
        transport.closeFile();
        File file1 = new File(journalDirectory, "fileOne");
        assertFileExists(file1);
        assertFileDoesNotExist(tempfile1);

        // open creates another temp file
        transport.openFile("secondSillyHash", "fileTwo", parent
                .getCurrentDate());
        File tempfile2 = new File(journalDirectory, "_fileTwo");
        assertFileExists(tempfile2);

        // write to the file
        transport.getWriter()
                .add(factory.createStartElement(name2, null, null));
        transport.getWriter().add(factory.createEndElement(name2, null));

        // closing renames the file
        transport.closeFile();
        File file2 = new File(journalDirectory, "fileTwo");
        assertFileExists(file2);
        assertFileDoesNotExist(tempfile2);

        // shut it down
        transport.shutdown();

        // did we write what was expected?
        assertFileContents(EXPECTED_JOURNAL_1_CONTENTS, file1);
        assertFileContents(EXPECTED_JOURNAL_2_CONTENTS, file2);
    }

    @Test
    public void testUnableToCreateFile() throws JournalException {
        LocalDirectoryTransport transport =
                new LocalDirectoryTransport(parameters, CRUCIAL, null);

        try {
            transport.openFile("firstSillyHash", ":", new Date());
            fail("expecting JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

    @Test
    public void testFileAlreadyExists() throws JournalException {
        Transport transport =
                new LocalDirectoryTransport(parameters, CRUCIAL, parent);

        transport.openFile("firstSillyHash", "fileOne", new Date());
        transport.closeFile();

        try {
            transport.openFile("secondSillyHash", "fileOne", new Date());
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

    @Test
    public void testOpenAfterShutdown() throws JournalException {
        Transport transport =
                new LocalDirectoryTransport(parameters, CRUCIAL, parent);
        transport.shutdown();

        try {
            transport.openFile("firstSillyHash", "fileOne", new Date());
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

    @Test
    public void testGetWriterAfterShutdown() throws JournalException {
        Transport transport =
                new LocalDirectoryTransport(parameters, CRUCIAL, parent);
        transport.openFile("firstSillyHash", "fileOne", new Date());
        transport.closeFile();
        transport.shutdown();

        try {
            transport.getWriter();
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

    @Test
    public void testCloseAfterShutdown() throws JournalException {
        Transport transport =
                new LocalDirectoryTransport(parameters, CRUCIAL, parent);
        transport.shutdown();

        try {
            transport.closeFile();
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

    @Test
    public void testShutdownAfterShutdown() throws JournalException {
        Transport transport =
                new LocalDirectoryTransport(parameters, CRUCIAL, parent);
        transport.shutdown();
        // repeated shutdowns are no problem.
        transport.shutdown();
    }

    @Test
    public void testOpenAfterOpen() throws JournalException {
        Transport transport =
                new LocalDirectoryTransport(parameters, CRUCIAL, parent);
        transport.openFile("firstSillyHash", "Open", new Date());

        try {
            transport.openFile("firstSillyHash", "OpenOpen", new Date());
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

    @Test
    public void testShutdownAfterOpen() throws JournalException {
        Transport transport =
                new LocalDirectoryTransport(parameters, CRUCIAL, parent);
        transport.openFile("firstSillyHash", "OpenBeforeShutdown", new Date());

        try {
            transport.shutdown();
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception - now close the file so we can clean up.
            transport.closeFile();
        }
    }

    @Test
    public void testCloseAfterClose() throws JournalException {
        Transport transport =
                new LocalDirectoryTransport(parameters, CRUCIAL, parent);
        transport.openFile("firstSillyHash", "CloseClose", new Date());
        transport.closeFile();

        try {
            transport.closeFile();
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

    @Test
    public void testGetWriterAfterClose() throws JournalException {
        Transport transport =
                new LocalDirectoryTransport(parameters, CRUCIAL, parent);
        transport.openFile("whoCaresHash", "CloseGetWriter", new Date());
        transport.closeFile();

        try {
            transport.getWriter();
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

}
