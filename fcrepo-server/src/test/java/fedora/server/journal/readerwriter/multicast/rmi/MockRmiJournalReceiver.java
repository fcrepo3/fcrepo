/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast.rmi;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.Arrays;

import fedora.server.journal.JournalException;

@SuppressWarnings("serial")
public class MockRmiJournalReceiver
        extends UnicastRemoteObject
        implements RmiJournalReceiverInterface {

    private static boolean trace;

    private int howManyCallsToOpenFile;

    private int howManyCallsToWriteText;

    private int howManyCallsToClosefile;

    private boolean openFileThrowsException;

    private boolean writeTextThrowsException;

    private boolean closeFileThrowsException;

    private String repositoryHash;

    private String filename;

    private String indexedHash;

    private String text;

    // -------------------------------------------------------------------------
    // Mocking infrastructure.
    // -------------------------------------------------------------------------

    public MockRmiJournalReceiver()
            throws RemoteException {
    }

    public int howManyCallsToClosefile() {
        return howManyCallsToClosefile;
    }

    public int howManyCallsToOpenFile() {
        return howManyCallsToOpenFile;
    }

    public int howManyCallsToWriteText() {
        return howManyCallsToWriteText;
    }

    public void setCloseFileThrowsException(boolean closeFileThrowsException) {
        this.closeFileThrowsException = closeFileThrowsException;
    }

    public void setOpenFileThrowsException(boolean openFileThrowsException) {
        this.openFileThrowsException = openFileThrowsException;
    }

    public void setWriteTextThrowsException(boolean writeTextThrowsException) {
        this.writeTextThrowsException = writeTextThrowsException;
    }

    public String getRepositoryHash() {
        return repositoryHash;
    }

    public String getFilename() {
        return filename;
    }

    public String getIndexedHash() {
        return indexedHash;
    }

    public String getText() {
        return text;
    }

    // -------------------------------------------------------------------------
    // Mocked methods.
    // -------------------------------------------------------------------------

    public void openFile(String repositoryHash, String filename)
            throws RemoteException, JournalException {
        howManyCallsToOpenFile++;

        if (openFileThrowsException) {
            throw new RemoteException("openFile throws exception");
        }

        this.repositoryHash = repositoryHash;
        this.filename = filename;

        if (trace) {
            System.out.println("openFile(" + repositoryHash + ", " + filename
                    + ")");
        }
    }

    public void writeText(String indexedHash, String text)
            throws RemoteException, JournalException {
        howManyCallsToWriteText++;

        if (writeTextThrowsException) {
            throw new RemoteException("writeText throws exception");
        }

        this.indexedHash = indexedHash;
        this.text = text;

        if (trace) {
            System.out.println("writeText(" + indexedHash + ", " + text + ")");
        }
    }

    public void closeFile() throws RemoteException, JournalException {
        howManyCallsToClosefile++;

        if (closeFileThrowsException) {
            throw new JournalException("closeFile throws exception");
        }

        if (trace) {
            System.out.println("closeFile()");
        }
    }

    /**
     * Use this if you need to create an actual RMI connection to test the
     * RmiTransport.
     */
    public static void main(String[] args) throws RemoteException,
            AlreadyBoundException {
        trace = true;

        try {
            MockRmiJournalReceiver receiver = new MockRmiJournalReceiver();

            if (Arrays.asList(args).contains("throwException")) {
                receiver.setOpenFileThrowsException(true);
            }

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind("RmiJournalReceiver", receiver);
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
