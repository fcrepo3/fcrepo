/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast.rmi;

import java.io.File;

import java.rmi.RemoteException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fedora.server.journal.AbstractJournalTester;
import fedora.server.journal.JournalException;

import static org.junit.Assert.fail;

public class TestRmiJournalReceiver
        extends AbstractJournalTester {

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestRmiTransport.class);
    }

    private static File journalDirectory;

    private static int fileIndex;

    @BeforeClass
    public static void initializeJournalDirectory() {
        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        journalDirectory = new File(tempDirectory, "TestRmiJournalReceiver");
        journalDirectory.mkdirs();
    }

    @BeforeClass
    public static void initializeFileIndex() {
        fileIndex = 0;
    }

    @Before
    public void cleanJournalDirectory() {
        deleteDirectoryContents(journalDirectory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoDirectoryPath() throws RemoteException {
        createReceiver(new String[] {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDirectoryDoesNotExist() throws RemoteException {
        createReceiver(new String[] {"BogusDirectory"});
    }

    @Test
    public void testValidOneArg() throws RemoteException {
        createReceiver(new String[] {journalDirectory.getAbsolutePath()});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRegistryPortNumber() throws RemoteException {
        createReceiver(new String[] {journalDirectory.getAbsolutePath(),
                "BogusPort"});
    }

    @Test
    public void testValidTwoArgs() throws RemoteException {
        createReceiver(new String[] {journalDirectory.getAbsolutePath(), "1234"});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidServerPortNumber() throws RemoteException {
        createReceiver(new String[] {journalDirectory.getAbsolutePath(),
                "1234", "BogusServerPort"});
    }

    @Test
    public void testValidThreeArgs() throws RemoteException {
        createReceiver(new String[] {journalDirectory.getAbsolutePath(),
                "1234", "1235"});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidLogLevel() throws RemoteException {
        createReceiver(new String[] {journalDirectory.getAbsolutePath(),
                "1234", "1235", "BogusLogLevel"});
    }

    @Test
    public void testValidFourArgs() throws RemoteException {
        createReceiver(new String[] {journalDirectory.getAbsolutePath(),
                "1234", "1235", "INFO"});
    }

    @Test
    public void testNormalSequence() throws RemoteException, JournalException {
        String text1 = "Write this text!";
        String text2 = "  And some more also.";

        RmiJournalReceiver receiver =
                createReceiver(new String[] {journalDirectory.getAbsolutePath()});
        String filename = getFilename();
        receiver.openFile("SomeSillyHash", filename);
        receiver.writeText(RmiJournalReceiverHelper
                .figureIndexedHash("SomeSillyHash", 0), text1);
        receiver.writeText(RmiJournalReceiverHelper
                .figureIndexedHash("SomeSillyHash", 1), text2);
        receiver.closeFile();
        assertFileContents(text1 + text2, new File(journalDirectory, filename));
    }

    @Test
    public void testIncorrectItemHash() throws RemoteException,
            JournalException {
        String text1 = "This won't work.";

        RmiJournalReceiver receiver =
                createReceiver(new String[] {
                        journalDirectory.getAbsolutePath(), "1234", "1235",
                        "FATAL"});
        String filename = getFilename();
        receiver.openFile("RepoHash", filename);
        try {
            receiver.writeText("BogusItemHash", text1);
            fail("Expected an exception.");
        } catch (JournalException e) {
            // expected the exception - close the file so we can clean up.
            receiver.closeFile();
        }
    }

    @Test
    public void testOpenAfterOpen() throws RemoteException, JournalException {
        RmiJournalReceiver receiver =
                createReceiver(new String[] {
                        journalDirectory.getAbsolutePath(), "1234", "1235",
                        "FATAL"});
        receiver.openFile("RepoHash", getFilename());

        try {
            receiver.openFile("RepoHash", getFilename());
            fail("Expected an exception.");
        } catch (JournalException e) {
            // expected the exception - close the file so we can clean up.
            receiver.closeFile();
        }
    }

    @Test
    public void testCloseWithoutOpen() throws RemoteException, JournalException {
        RmiJournalReceiver receiver =
                createReceiver(new String[] {
                        journalDirectory.getAbsolutePath(), "1234", "1235",
                        "FATAL"});

        try {
            receiver.closeFile();
            fail("Expected an exception.");
        } catch (JournalException e) {
            // expected the exception.
        }
    }

    @Test
    public void testCloseAfterClose() throws RemoteException, JournalException {
        RmiJournalReceiver receiver =
                createReceiver(new String[] {
                        journalDirectory.getAbsolutePath(), "1234", "1235",
                        "FATAL"});
        receiver.openFile("RepoHash", getFilename());
        receiver.closeFile();

        try {
            receiver.closeFile();
            fail("Expected an exception.");
        } catch (JournalException e) {
            // expected the exception.
        }
    }

    @Test
    public void testWriteAfterClose() throws RemoteException, JournalException {
        RmiJournalReceiver receiver =
                createReceiver(new String[] {
                        journalDirectory.getAbsolutePath(), "1234", "1235",
                        "FATAL"});
        receiver.openFile("RepoHash", getFilename());
        receiver.closeFile();

        try {
            receiver.writeText(getIndexHash("RepoHash", 0), "Some bogus text");
            fail("Expected an exception.");
        } catch (JournalException e) {
            // expected the exception.
        }
    }

    @Test
    public void testFileExists() throws RemoteException, JournalException {
        RmiJournalReceiver receiver =
                createReceiver(new String[] {
                        journalDirectory.getAbsolutePath(), "1234", "1235",
                        "FATAL"});
        String filename = getFilename();
        receiver.openFile("RepoHash", filename);
        receiver.closeFile();

        try {
            receiver.openFile("RepoHash", filename);
            fail("Expected an exception.");
        } catch (JournalException e) {
            // expected the exception.
        }
    }

    @Test
    public void testCantCreateFile() throws RemoteException, JournalException {
        RmiJournalReceiver receiver =
                createReceiver(new String[] {
                        journalDirectory.getAbsolutePath(), "1234", "1235",
                        "FATAL"});

        try {
            receiver.openFile("RepoHash", ":");
            fail("Expected an exception.");
        } catch (JournalException e) {
            // expected the exception.
        }
    }

    private RmiJournalReceiver createReceiver(String[] args)
            throws RemoteException {
        return new RmiJournalReceiver(new RmiJournalReceiverArguments(args));
    }

    private String getFilename() {
        return "journal" + fileIndex++;
    }

    private String getIndexHash(String repoHash, int itemIndex) {
        return RmiJournalReceiverHelper.figureIndexedHash(repoHash, itemIndex);
    }

}
