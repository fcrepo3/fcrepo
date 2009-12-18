/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.readerwriter.multifile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.errors.ServerException;
import fedora.server.journal.JournalConstants;
import fedora.server.journal.JournalConsumer;
import fedora.server.journal.MockJournalRecoveryLog;
import fedora.server.journal.MockServerForJournalTesting;
import fedora.server.journal.ServerInterface;
import fedora.server.management.MockManagementDelegate;

public class TestLockingFollowingJournalReader
        extends TestCase
        implements Constants, JournalConstants, MultiFileJournalConstants {

    private static final int WAIT_INTERVAL = 5;

    private static final String JOURNAL_FILENAME_PREFIX = "unit";

    private static final String DUMMY_HASH_VALUE = "Dummy Hash";

    private File journalDirectory;

    private File archiveDirectory;

    private File lockRequestFile;

    private File lockAcceptedFile;

    private Map<String, String> parameters;

    private ServerInterface server;

    private final String role = "DumbGrunt";

    private MyMockManagementDelegate delegate;

    private int initialNumberOfThreads;

    public TestLockingFollowingJournalReader(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        journalDirectory = createTempDirectory("fedoraTestingJournalFiles");
        archiveDirectory = createTempDirectory("fedoraTestingArchiveFiles");

        lockRequestFile =
                new File(journalDirectory.getPath() + File.separator
                        + "lockRequested");
        lockRequestFile.delete();

        lockAcceptedFile =
                new File(journalDirectory.getPath() + File.separator
                        + "lockAccepted");
        lockAcceptedFile.delete();

        delegate = new MyMockManagementDelegate();

        server = new MockServerForJournalTesting(delegate, DUMMY_HASH_VALUE);

        parameters = new HashMap<String, String>();
        parameters.put(PARAMETER_JOURNAL_RECOVERY_LOG_CLASSNAME,
                       MockJournalRecoveryLog.class.getName());
        parameters.put(PARAMETER_JOURNAL_READER_CLASSNAME,
                       "fedora.server.journal.readerwriter.multifile."
                               + "LockingFollowingJournalReader");
        parameters.put(PARAMETER_JOURNAL_DIRECTORY, journalDirectory.getPath());
        parameters.put(PARAMETER_ARCHIVE_DIRECTORY, archiveDirectory.getPath());
        parameters.put(PARAMETER_FOLLOW_POLLING_INTERVAL, "1");
        parameters.put(PARAMETER_JOURNAL_FILENAME_PREFIX,
                       JOURNAL_FILENAME_PREFIX);
        parameters.put(PARAMETER_LOCK_REQUESTED_FILENAME, lockRequestFile
                .getPath());
        parameters.put(PARAMETER_LOCK_ACCEPTED_FILENAME, lockAcceptedFile
                .getPath());

        initialNumberOfThreads = getNumberOfCurrentThreads();
    }

    /**
     * Create 3 files and watch it process all of them
     */
    public void testSimpleNoLocking() {
        try {
            // create 3 files, each with an ingest
            createJournalFileFromString(getSimpleIngestString());
            createJournalFileFromString(getSimpleIngestString());
            createJournalFileFromString(getSimpleIngestString());

            // create the JournalConsumer and run it.
            JournalConsumer consumer =
                    new JournalConsumer(parameters, role, server);
            startConsumerThread(consumer);
            waitWhileThreadRuns(WAIT_INTERVAL);
            consumer.shutdown();

            assertEquals("Expected to see 3 ingests", 3, delegate
                    .getCallCount());
            assertEquals("Journal files not all gone",
                         0,
                         howManyFilesInDirectory(journalDirectory));
            assertEquals("Wrong number of archive files",
                         3,
                         howManyFilesInDirectory(archiveDirectory));
        } catch (Throwable e) {
            processException(e);
        }
    }

    /**
     * A lock request created before startup will prevent processing. When the
     * request is removed, processing will occur.
     */
    public void disabledtestLockBeforeStartingAndResume() {
        try {
            // create 3 files, each with an ingest, and create a lock request.
            createJournalFileFromString(getSimpleIngestString());
            createJournalFileFromString(getSimpleIngestString());
            createJournalFileFromString(getSimpleIngestString());
            createLockRequest();

            // create the JournalConsumer and run it.
            JournalConsumer consumer =
                    new JournalConsumer(parameters, role, server);
            startConsumerThread(consumer);

            // we should see the lock accepted and no processing going on.
            waitForLockAccepted();
            waitWhileThreadRuns(WAIT_INTERVAL);
            assertEquals("Journal files should not be processed", 0, delegate
                    .getCallCount());
            assertEquals("Journal files should not be processed",
                         3,
                         howManyFilesInDirectory(journalDirectory));
            assertEquals("Journal files should not be processed",
                         0,
                         howManyFilesInDirectory(archiveDirectory));
            int lockMessageIndex = assertLockMessageInLog();

            // remove the request. We should see the lock released and
            // processing should run to completion.
            removeLockRequest();
            waitForLockReleased();
            waitWhileThreadRuns(WAIT_INTERVAL);
            consumer.shutdown();

            assertEquals("Expected to see 3 ingests", 3, delegate
                    .getCallCount());
            assertEquals("Journal files not all gone",
                         0,
                         howManyFilesInDirectory(journalDirectory));
            assertEquals("Wrong number of archive files",
                         3,
                         howManyFilesInDirectory(archiveDirectory));
            assertUnlockMessageInLog(lockMessageIndex);
        } catch (Throwable e) {
            processException(e);
        }
    }

    /**
     * A lock request created while a file is in progress, which should prevent
     * further processing until it is removed.
     */
    public void testLockWhileProcessingAndResume() {
        try {
            // create 3 files, each with an ingest
            createJournalFileFromString(getSimpleIngestString());
            createJournalFileFromString(getSimpleIngestString());
            createJournalFileFromString(getSimpleIngestString());

            // a lock request will be created while the second file is being
            // processed.
            delegate.setIngestOperation(new LockAfterSecondIngest());

            // create the JournalConsumer and run it.
            JournalConsumer consumer =
                    new JournalConsumer(parameters, role, server);
            startConsumerThread(consumer);

            // we should see the lock accepted and processing stop after the
            // second file.
            waitForLockAccepted();
            waitWhileThreadRuns(WAIT_INTERVAL);
            assertEquals("We should stop after the second ingest", 2, delegate
                    .getCallCount());
            assertEquals("One Journal file should not be processed",
                         1,
                         howManyFilesInDirectory(journalDirectory));
            assertEquals("Only two Journal files should be processed",
                         2,
                         howManyFilesInDirectory(archiveDirectory));
            int lockMessageIndex = assertLockMessageInLog();

            // remove the request. We should see the lock released and
            // processing should run to completion.
            removeLockRequest();
            waitForLockReleased();
            waitWhileThreadRuns(WAIT_INTERVAL);
            consumer.shutdown();

            assertEquals("Expected to see 3 ingests", 3, delegate
                    .getCallCount());
            assertEquals("Journal files not all gone",
                         0,
                         howManyFilesInDirectory(journalDirectory));
            assertEquals("Wrong number of archive files",
                         3,
                         howManyFilesInDirectory(archiveDirectory));
            assertUnlockMessageInLog(lockMessageIndex);
        } catch (Throwable e) {
            processException(e);
        }
    }

    /**
     * A lock request created while the system if polling, which should prevent
     * further processing until it is removed. Create 1 files and watch it
     * process all of them. Create a lock and wait for the ack. Create a 2nd
     * file, and it will not be processed. Remove the lock; ack is removed and
     * last file is processed.
     */
    public void disabledtestLockWhilePollingAndResume() {
        try {
            // create 1 file, with an ingest
            createJournalFileFromString(getSimpleIngestString());

            // create the JournalConsumer and run it.
            JournalConsumer consumer =
                    new JournalConsumer(parameters, role, server);
            startConsumerThread(consumer);

            // the file should be processed and we being polling.
            waitWhileThreadRuns(WAIT_INTERVAL);
            assertEquals("The first file should have been processed.",
                         1,
                         delegate.getCallCount());
            assertEquals("The first file should have been processed.",
                         0,
                         howManyFilesInDirectory(journalDirectory));
            assertEquals("The first file should have been processed.",
                         1,
                         howManyFilesInDirectory(archiveDirectory));

            // create a lock request and wait for the acceptance.
            createLockRequest();
            waitForLockAccepted();

            // create another Journal file, but it won't be processed.
            createJournalFileFromString(getSimpleIngestString());
            waitWhileThreadRuns(WAIT_INTERVAL);
            assertEquals("The second file should not have been processed.",
                         1,
                         delegate.getCallCount());
            assertEquals("The second file should not have been processed.",
                         1,
                         howManyFilesInDirectory(journalDirectory));
            assertEquals("The second file should not have been processed.",
                         1,
                         howManyFilesInDirectory(archiveDirectory));
            int lockMessageIndex = assertLockMessageInLog();

            // remove the lock and the file is processed.
            removeLockRequest();
            waitForLockReleased();
            waitWhileThreadRuns(WAIT_INTERVAL);
            consumer.shutdown();

            assertEquals("Expected to see 2 ingests", 2, delegate
                    .getCallCount());
            assertEquals("Journal files not all gone",
                         0,
                         howManyFilesInDirectory(journalDirectory));
            assertEquals("Wrong number of archive files",
                         2,
                         howManyFilesInDirectory(archiveDirectory));
            assertUnlockMessageInLog(lockMessageIndex);
        } catch (Throwable e) {
            processException(e);
        }
    }

    private void createLockRequest() throws IOException {
        lockRequestFile.createNewFile();
    }

    private void removeLockRequest() {
        lockRequestFile.delete();
    }

    private int howManyFilesInDirectory(File directory) {
        return MultiFileJournalHelper
                .getSortedArrayOfJournalFiles(directory,
                                              JOURNAL_FILENAME_PREFIX).length;
    }

    /**
     * Wait until the JournalConsumerThread stops, or until the time limit
     * expires, whichever comes first.
     */
    private void waitWhileThreadRuns(int maxSecondsToWait) {
        for (int i = 0; i < maxSecondsToWait; i++) {
            if (getNumberOfCurrentThreads() == initialNumberOfThreads) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Wait until the lock is accepted, or until the time runs out. If the
     * latter, complain.
     */
    private void waitForLockAccepted() {
        int maxWait = 3;
        for (int i = 0; i < maxWait; i++) {
            if (lockAcceptedFile.exists()) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        fail("Lock was not accepted after " + maxWait + " seconds.");
    }

    /**
     * Wait until the lock is released, or until the time runs out. If the
     * latter, complain.
     */
    private void waitForLockReleased() {
        int maxWait = 3;
        for (int i = 0; i < maxWait; i++) {
            if (!lockAcceptedFile.exists()) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        fail("Lock was not released after " + maxWait + " seconds.");
    }

    /**
     * Set the ManagementDelegate into the JournalConsumer, which will create
     * the JournalConsumerThread.
     */
    private void startConsumerThread(JournalConsumer consumer) {
        consumer.setManagementDelegate(delegate);
    }

    private int getNumberOfCurrentThreads() {
        int i =
                Thread.currentThread().getThreadGroup()
                        .enumerate(new Thread[500]);
        return i;
    }

    private void createJournalFileFromString(String text) throws IOException {
        File journal =
                File.createTempFile(JOURNAL_FILENAME_PREFIX,
                                    null,
                                    journalDirectory);
        journal.deleteOnExit();
        FileWriter writer = new FileWriter(journal);
        writer.write(text);
        writer.close();
    }

    /**
     * Confirm that the last message in the log is a lock message, and return
     * its position in the log.
     */
    private int assertLockMessageInLog() {
        List<String> messages = MockJournalRecoveryLog.getMessages();
        int lastMessageIndex = messages.size() - 1;
        String lastMessage = messages.get(lastMessageIndex);
        assertStringStartsWith(lastMessage, "Lock request detected:");
        return lastMessageIndex;
    }

    /**
     * Confirm that the log message following the lock message is in fact an
     * unlock message.
     */
    private void assertUnlockMessageInLog(int lockMessageIndex) {
        List<String> messages = MockJournalRecoveryLog.getMessages();
        int unlockMessageIndex = lockMessageIndex + 1;
        assertTrue(messages.size() > unlockMessageIndex);
        String unlockMessage = messages.get(unlockMessageIndex);
        assertStringStartsWith(unlockMessage, "Lock request removed");
    }

    private void assertStringStartsWith(String string, String prefix) {
        if (!string.startsWith(prefix)) {
            fail("String does not start as expected: string='" + string
                    + "', prefix='" + prefix + "'");
        }
    }

    private void processException(Throwable e) {
        if (e instanceof ServerException) {
            System.err.println("ServerException: code='"
                    + ((ServerException) e).getCode() + "', class='"
                    + e.getClass().getName() + "'");
            StackTraceElement[] traces = e.getStackTrace();
            for (StackTraceElement element : traces) {
                System.err.println(element);
            }
            Throwable cause = e.getCause();
            if (cause != null) {
                cause.printStackTrace();
            }
            fail("Threw a ServerException");
        } else {
            e.printStackTrace();
            fail("Threw an exception");
        }
    }

    private File createTempDirectory(String name) {
        File directory = new File(System.getProperty("java.io.tmpdir"), name);
        directory.mkdir();
        cleanOutDirectory(directory);
        directory.deleteOnExit();
        return directory;
    }

    private void cleanOutDirectory(File directory) {
        File[] files = directory.listFiles();
        for (File element : files) {
            element.delete();
        }
    }

    private String getSimpleIngestString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FedoraJournal repositoryHash=\""
                + DUMMY_HASH_VALUE
                + "\" timestamp=\"2006-08-11T11:14:43.011-0400\">\n"
                + "  <JournalEntry method=\"ingest\" timestamp=\"2006-08-11T11:14:42.690-0400\" clientIpAddress=\"128.84.103.30\" loginId=\"fedoraAdmin\">\n"
                + "    <context>\n"
                + "      <password>junk</password>\n"
                + "      <noOp>false</noOp>\n"
                + "      <now>2006-08-11T11:14:42.690-0400</now>\n"
                + "      <multimap name=\"environment\">\n"
                + "        <multimapkey name=\"urn:fedora:names:fedora:2.1:environment:httpRequest:authType\">\n"
                + "          <multimapvalue>BASIC</multimapvalue>\n"
                + "        </multimapkey>\n"
                + "      </multimap>\n"
                + "      <multimap name=\"subject\"></multimap>\n"
                + "      <multimap name=\"action\"> </multimap>\n"
                + "      <multimap name=\"resource\"></multimap>\n"
                + "      <multimap name=\"recovery\"></multimap>\n"
                + "    </context>\n"
                + "    <argument name=\"serialization\" type=\"stream\">PD94</argument>\n"
                + "    <argument name=\"message\" type=\"string\">Minimal Ingest sample</argument>\n"
                + "    <argument name=\"format\" type=\"string\">"
                + FOXML1_1.uri
                + "</argument>\n"
                + "    <argument name=\"encoding\" type=\"string\">UTF-8</argument>\n"
                + "    <argument name=\"newPid\" type=\"boolean\">true</argument>\n"
                + "  </JournalEntry>\n" + "</FedoraJournal>\n";
    }

    /**
     * Set one of these as the ingest object on the ManagementDelegate. When the
     * second ingest operation begins, a Lock Request will be created.
     */
    private final class LockAfterSecondIngest
            implements Runnable {

        public void run() {
            if (delegate.getCallCount() == 2) {
                try {
                    createLockRequest();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This sub-class of {@link MockManagementDelegate} allows us to insert a
     * {@link Runnable} that will be executed in the middle of an ingest call.
     *
     * @author Firstname Lastname
     */
    private static class MyMockManagementDelegate
            extends MockManagementDelegate {

        private Runnable ingestOperation;

        public void setIngestOperation(Runnable ingestOperation) {
            this.ingestOperation = ingestOperation;
        }

        @Override
        public String ingest(Context context,
                             InputStream serialization,
                             String logMessage,
                             String format,
                             String encoding,
                             boolean newPid) throws ServerException {
            String result =
                    super.ingest(context,
                                 serialization,
                                 logMessage,
                                 format,
                                 encoding,
                                 newPid);

            if (ingestOperation != null) {
                ingestOperation.run();
            }

            return result;
        }

    }

}
