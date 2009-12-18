/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast;

import java.io.StringWriter;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;

import org.junit.Before;
import org.junit.Test;

import fedora.server.errors.ServerException;
import fedora.server.journal.JournalException;
import fedora.server.journal.JournalOperatingMode;
import fedora.server.journal.JournalWriter;
import fedora.server.journal.MockServerForJournalTesting;
import fedora.server.journal.ServerInterface;
import fedora.server.management.MockManagementDelegate;

import static org.junit.Assert.assertEquals;

import static fedora.server.journal.readerwriter.multicast.MulticastJournalWriter.TRANSPORT_PARAMETER_PREFIX;

public class TestMulticastJournalWriterOperation {

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestMulticastJournalWriterInitializations.class);
    }

    private static final String DUMMY_ROLE = "dummyRole";

    private ServerInterface server;

    private Map<String, String> parameters;

    /**
     * Using a mock object only so we can predict what filenames will be
     * created.
     */
    private MockMulticastJournalWriter journalWriter;

    private MockTransport transport1;

    private MockTransport transport2;

    private StringWriter logWriter;

    @Before
    public void initalizeBasicParameters() {
        parameters = new HashMap<String, String>();
        parameters.put("journalWriterClassname",
                       MockMulticastJournalWriter.class.getName());
        addParameter("one.classname", MockTransport.class.getName());
        addParameter("one.crucial", "false");
        addParameter("two.classname", MockTransport.class.getName());
        addParameter("two.crucial", "true");
    }

    @Before
    public void initializeMockServer() {
        server =
                new MockServerForJournalTesting(new MockManagementDelegate(),
                                                "myHashValue");
    }

    @Before
    public void initializeJournalOperatingMode() {
        JournalOperatingMode.setMode(JournalOperatingMode.NORMAL);
    }

    @Before
    public void initializeLog4j() {
        logWriter = new StringWriter();
        SimpleLayout myLayout = new SimpleLayout() {

            // Just eat the Throwable object in the LogEvent
            @Override
            public boolean ignoresThrowable() {
                return false;
            }

        };
        WriterAppender myAppender = new WriterAppender(myLayout, logWriter);
        myAppender.setImmediateFlush(true);
        Logger root = Logger.getRootLogger();
        root.addAppender(myAppender);
    }

    @Test
    public void testPrepareReachesBothTransports() throws JournalException,
            ServerException {
        createJournalWriterAndTransports();
        Date currentDate = parseDateString("20050316.144555.123");
        journalWriter.setCurrentDate(currentDate);

        journalWriter.prepareToWriteJournalEntry();
        assertCorrectNumberOfRequests(1, 0, 0, 0);
        assertCorrectCurrentDate(currentDate);
        assertEquals("transport1 repository hash",
                     server.getRepositoryHash(),
                     transport1.getRepositoryHash());
        assertCorrectFilenames("fedoraJournal20050316.194555.123Z");
        assertEquals("transport2 repository hash",
                     server.getRepositoryHash(),
                     transport2.getRepositoryHash());
        assertExpectedLogMessages("");
    }

    @Test
    public void testWriteReachesBothTransports() throws JournalException,
            ServerException {
        createJournalWriterAndTransports();
        journalWriter.setCurrentDate(parseDateString("20070218.085507.951"));

        journalWriter.prepareToWriteJournalEntry();
        assertCorrectNumberOfRequests(1, 0, 0, 0);

        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_1);
        assertCorrectNumberOfRequests(1, 1, 0, 0);
        assertEquals("transport1 journal file contents",
                     SampleJournalFile1.FILE_CONTENTS,
                     transport1.getFileContents());
        assertEquals("transport2 journal file contents",
                     SampleJournalFile1.FILE_CONTENTS,
                     transport2.getFileContents());
    }

    @Test
    public void testShutdownWithFileOpen() throws JournalException,
            ServerException {
        createJournalWriterAndTransports();

        journalWriter.prepareToWriteJournalEntry();
        assertCorrectNumberOfRequests(1, 0, 0, 0);

        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_2);
        assertCorrectNumberOfRequests(1, 1, 0, 0);

        journalWriter.shutdown();
        assertCorrectNumberOfRequests(1, 1, 1, 1);

        assertExpectedLogMessages("");
    }

    @Test
    public void testShutdownWithFileClosed() throws JournalException,
            ServerException {
        parameters.put("journalFileSizeLimit", "1");
        createJournalWriterAndTransports();

        journalWriter.prepareToWriteJournalEntry();
        assertCorrectNumberOfRequests(1, 0, 0, 0);

        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_2);
        assertCorrectNumberOfRequests(1, 1, 1, 0);

        journalWriter.shutdown();
        assertCorrectNumberOfRequests(1, 1, 1, 1);

        assertExpectedLogMessages("");
    }

    @Test
    public void testHitSizeLimitThenAnotherEntry() throws JournalException,
            ServerException {
        parameters.put("journalFileSizeLimit", "15000");
        createJournalWriterAndTransports();

        Date currentDate;
        // force the date, so we can check the filenames.
        currentDate = parseDateString("20050316.144555.123");
        journalWriter.setCurrentDate(currentDate);

        // Write first entry
        journalWriter.prepareToWriteJournalEntry();
        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_1);
        assertCorrectNumberOfRequests(1, 1, 0, 0);
        assertCorrectFilenames("fedoraJournal20050316.194555.123Z");
        assertCorrectCurrentDate(currentDate);

        // Write second entry, and check that the file has been closed.
        journalWriter.prepareToWriteJournalEntry();
        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_2);
        assertCorrectNumberOfRequests(1, 2, 1, 0);

        // force a different date, so we can check the filenames.
        currentDate = parseDateString("20050316.150000.000");
        journalWriter.setCurrentDate(currentDate);

        // Write third entry to the next file.
        journalWriter.prepareToWriteJournalEntry();
        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_3);
        assertCorrectNumberOfRequests(2, 3, 1, 0);
        assertCorrectFilenames("fedoraJournal20050316.200000.000Z");
        assertCorrectCurrentDate(currentDate);

        assertExpectedLogMessages("");
    }

    @Test
    public void testAgeLimitThenAnotherEntry() throws JournalException,
            ServerException {
        parameters.put("journalFileAgeLimit", "1");
        createJournalWriterAndTransports();

        Date currentDate;
        // force the date, so we can check the filenames.
        currentDate = parseDateString("20050316.144555.999");
        journalWriter.setCurrentDate(currentDate);

        // Write first entry
        journalWriter.prepareToWriteJournalEntry();
        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_1);
        assertCorrectNumberOfRequests(1, 1, 0, 0);
        assertCorrectFilenames("fedoraJournal20050316.194555.999Z");
        assertCorrectCurrentDate(currentDate);

        // Wait, and check that the file has been closed.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertCorrectNumberOfRequests(1, 1, 1, 0);

        // force a different date, so we can check the filenames.
        currentDate = parseDateString("20050316.150001.000");
        journalWriter.setCurrentDate(currentDate);

        // Write second entry to the next file.
        journalWriter.prepareToWriteJournalEntry();
        assertCorrectNumberOfRequests(2, 1, 1, 0);
        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_3);
        assertCorrectNumberOfRequests(2, 2, 1, 0);
        assertCorrectFilenames("fedoraJournal20050316.200001.000Z");
        assertCorrectCurrentDate(currentDate);

        // Write third entry to the next file.
        journalWriter.prepareToWriteJournalEntry();
        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_3);
        assertCorrectNumberOfRequests(2, 3, 1, 0);

        // Wait, and check that the file has been closed.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertCorrectNumberOfRequests(2, 3, 2, 0);

        assertExpectedLogMessages("");
    }

    @Test
    public void testPrepareAfterShutdown() throws JournalException,
            ServerException {
        createJournalWriterAndTransports();

        // Write an entry and shutdown.
        journalWriter.prepareToWriteJournalEntry();
        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_1);
        journalWriter.shutdown();
        assertCorrectNumberOfRequests(1, 1, 1, 1);

        // Prepare to write another entry - takes no action.
        journalWriter.prepareToWriteJournalEntry();
        assertCorrectNumberOfRequests(1, 1, 1, 1);
    }

    @Test
    public void testWriteAfterShutdown() throws JournalException,
            ServerException {
        createJournalWriterAndTransports();

        // Write an entry and shutdown.
        journalWriter.prepareToWriteJournalEntry();
        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_1);
        journalWriter.shutdown();
        assertCorrectNumberOfRequests(1, 1, 1, 1);

        // Try to write another entry - takes no action.
        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_2);
        assertCorrectNumberOfRequests(1, 1, 1, 1);

        assertExpectedLogMessages("");
    }

    @Test
    public void testShutdownAfterShutdown() throws JournalException,
            ServerException {
        createJournalWriterAndTransports();

        // Write an entry and shutdown.
        journalWriter.prepareToWriteJournalEntry();
        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_1);
        journalWriter.shutdown();
        assertCorrectNumberOfRequests(1, 1, 1, 1);

        // Repeated shutdown calls are no problem.
        journalWriter.shutdown();
        assertCorrectNumberOfRequests(1, 1, 1, 1);

        assertExpectedLogMessages("");
    }

    @Test
    public void testExceptionFromNonCriticalTransport() throws JournalException {
        createJournalWriterAndTransports();
        transport1.setThrowExceptionOnGetWriter(true);

        // Writing a journal entry should cause a high-level log message, but no
        // mode change.
        journalWriter.prepareToWriteJournalEntry();
        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_1);
        assertCorrectNumberOfRequests(1, 1, 0, 0);
        assertEquals("should be no mode change.",
                     JournalOperatingMode.NORMAL,
                     JournalOperatingMode.getMode());

        assertExpectedLogMessages("ERROR - Exception thrown from "
                + "non-crucial Journal Transport: 'one'");
    }

    @Test
    public void testExceptionFromCriticalTransport() throws JournalException {
        createJournalWriterAndTransports();
        transport2.setThrowExceptionOnGetWriter(true);

        // Writing a journal entry should cause a high-level log message, and a
        // mode change.
        journalWriter.prepareToWriteJournalEntry();
        journalWriter.writeJournalEntry(SampleJournalEntries.ENTRY_1);
        assertCorrectNumberOfRequests(1, 1, 0, 0);
        assertEquals("should be a mode change.",
                     JournalOperatingMode.READ_ONLY,
                     JournalOperatingMode.getMode());

        assertExpectedLogMessages("FATAL - Exception thrown from "
                + "crucial Journal Transport: 'two'");
    }

    private void addParameter(String suffix, String value) {
        parameters.put(TRANSPORT_PARAMETER_PREFIX + suffix, value);
    }

    private void createJournalWriterAndTransports() throws JournalException {
        journalWriter =
                (MockMulticastJournalWriter) JournalWriter
                        .getInstance(parameters, DUMMY_ROLE, server);
        transport1 = (MockTransport) journalWriter.getTransports().get("one");
        transport2 = (MockTransport) journalWriter.getTransports().get("two");
    }

    private Date parseDateString(String dateString) {
        try {
            return new SimpleDateFormat("yyyyMMdd.HHmmss.SSS")
                    .parse(dateString);
        } catch (ParseException e) {
            // eat the exception!
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Confirm that both of the transports received the expected numbers of
     * requests to openFile, getWriter, closeFile and shutdown.
     */
    private void assertCorrectNumberOfRequests(int openFileRequests,
                                               int getWriterRequests,
                                               int closeFileRequests,
                                               int shutdownRequests) {
        assertCorrectNumberOfRequests("transport1",
                                      transport1,
                                      openFileRequests,
                                      getWriterRequests,
                                      closeFileRequests,
                                      shutdownRequests);
        assertCorrectNumberOfRequests("transport2",
                                      transport2,
                                      openFileRequests,
                                      getWriterRequests,
                                      closeFileRequests,
                                      shutdownRequests);
    }

    private void assertCorrectNumberOfRequests(String name,
                                               MockTransport transport,
                                               int openFileRequests,
                                               int getWriterRequests,
                                               int closeFileRequests,
                                               int shutdownRequests) {
        assertEquals(name + " should get " + openFileRequests
                + " openFile() request(s)", openFileRequests, transport1
                .getHowManyOpenFileRequests());
        assertEquals(name + " should get " + getWriterRequests
                + " getWriter() request(s)", getWriterRequests, transport1
                .getHowManyGetWriterRequests());
        assertEquals(name + " should get " + closeFileRequests
                + " closeFile() request(s)", closeFileRequests, transport1
                .getHowManyCloseFileRequests());
        assertEquals(name + " should get " + shutdownRequests
                + " shutdown() request(s)", shutdownRequests, transport1
                .getHowManyShutdownRequests());
    }

    /**
     * Confirm that both of the transports are using the expected filename.
     */
    private void assertCorrectFilenames(String filename) {
        assertEquals("transport1 filename", filename, transport1.getFilename());
        assertEquals("transport2 filename", filename, transport2.getFilename());
    }

    /**
     * Confirm that both of the transports have the expected current date.
     */
    private void assertCorrectCurrentDate(Date currentDate) {
        assertEquals("transport1 date", currentDate, transport1
                .getCurrentDate());
        assertEquals("transport2 date", currentDate, transport2
                .getCurrentDate());
    }

    private void assertExpectedLogMessages(String expectedLogMessage) {
        assertEquals("Surprising log message(s)", expectedLogMessage, logWriter
                .toString().trim());
    }

}
