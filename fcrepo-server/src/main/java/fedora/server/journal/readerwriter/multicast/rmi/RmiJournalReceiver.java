/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast.rmi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.lf5.LogLevel;

import fedora.server.journal.JournalException;
import fedora.server.journal.readerwriter.multicast.TransportOutputFile;

/**
 * <p>
 * <b>Title:</b> RmiJournalReceiver.java
 * </p>
 * <p>
 * <b>Description:</b> A free-standing RMI server that receives journal
 * messages from an RmiTransport and writes them to files in a specified
 * directory.
 * </p>
 * <p>
 * Note: command-line arguments are specified in
 * {@link RmiJournalReceiverArguments}.
 * </p>
 *
 * @author jblake
 * @version $Id: RmiJournalReceiver.java,v 1.4 2007/06/21 15:59:53 jblake Exp $
 */
@SuppressWarnings("serial")
public class RmiJournalReceiver
        extends UnicastRemoteObject
        implements RmiJournalReceiverInterface {

    private static final Logger LOG =
            Logger.getLogger(RmiJournalReceiver.class);

    /** The parsed arguments from the command line. */
    private final RmiJournalReceiverArguments arguments;

    /** The directory for journal files. */
    private final File directory;

    /** The journal file that is currently open, or null if no file is open. */
    private TransportOutputFile journalFile;

    /**
     * The number of {@link #writeText(String, String) writeText} calls that
     * have been made against the currently open file - used in calculating the
     * itemHash.
     */
    private long itemIndex;

    /** A {@link FileWriter} on the current journal file. */
    private Writer writer;

    /** The repository hash that was supplied when the current file was opened. */
    private String currentRepositoryHash;

    /**
     * On creation, parse the arguments and initialize Log4J for the
     * application.
     */
    public RmiJournalReceiver(RmiJournalReceiverArguments arguments)
            throws RemoteException {
        super(arguments.getServerPortNumber());
        this.arguments = arguments;
        directory = arguments.getDirectoryPath();
        initializeLog4J(arguments.getLogLevel());
    }

    /**
     * Set the logger to write to the console, at whatever level the user
     * specified in the command line argumements.
     */
    private void initializeLog4J(LogLevel logLevel) {
        ConsoleAppender appender =
                new ConsoleAppender(new SimpleLayout(), "System.out");
        appender.setName("Console");
        appender.activateOptions();

        Logger root = Logger.getRootLogger();
        root.addAppender(appender);
        root.setLevel(Level.toLevel(logLevel.getLabel()));
    }

    /**
     * Create an RMI registry, and bind this object to the expected name.
     */
    public void exportAndBind() throws RemoteException, AlreadyBoundException,
            InterruptedException {
        Registry registry =
                LocateRegistry
                        .createRegistry(arguments.getRegistryPortNumber());
        registry.rebind(RMI_BINDING_NAME, this);
        Thread.sleep(2000);
        LOG.info("RmiJournalReceiver is ready - journal directory is '"
                + arguments.getDirectoryPath().getAbsolutePath() + "'");
    }

    /**
     * Request to open a file. Check that:
     * <ul>
     * <li>a file is not already open,</li>
     * <li>we can create a {@link TransportOutputFile}, and open a
     * {@link Writer} on it.</li>
     * </ul>
     */
    public void openFile(String repositoryHash, String filename)
            throws JournalException {
        if (journalFile != null) {
            throw logAndGetException("Attempting to open file '" + filename
                    + "' when file '" + journalFile.getName()
                    + "' has not been closed.");
        }

        try {
            journalFile = new TransportOutputFile(directory, filename);
            writer = journalFile.open();
        } catch (IOException e) {
            throw logAndGetException("Problem opening" + filename + "'", e);
        }

        currentRepositoryHash = repositoryHash;
        itemIndex = 0;
        LOG.debug("opened file '" + filename + "', hash is '" + repositoryHash
                + "'");
    }

    /**
     * Request to write text to the current journal file. Check that:
     * <ul>
     * <li>a file is open,</li>
     * <li>the supplied indexedHash matches the one we calculate,</li>
     * <li>the write is successful.</li>
     * </ul>
     * Increment the itemIndex after a successful write.
     */
    public void writeText(String indexedHash, String text)
            throws JournalException {
        if (journalFile == null) {
            throw logAndGetException("Attempting to write when no file "
                    + "is open.");
        }

        String calculatedHash =
                RmiJournalReceiverHelper
                        .figureIndexedHash(currentRepositoryHash, itemIndex);
        if (!calculatedHash.equals(indexedHash)) {
            LOG.debug("calculatedHash='" + calculatedHash + "', providedHash='"
                    + indexedHash + "'");
            throw logAndGetException("indexed hash is incorrect.");
        }

        try {
            writer.append(text);
            writer.flush();
        } catch (IOException e) {
            throw logAndGetException("Failed to write to '"
                    + journalFile.getName() + "'", e);
        }

        LOG.debug("Wrote item #" + itemIndex + " to file '"
                + journalFile.getName() + "'");
        itemIndex++;
    }

    /**
     * Request to close a file. Check that:
     * <ul>
     * <li>a file is open,</li>
     * <li>we are able to close the file.</li>
     * </ul>
     */
    public void closeFile() throws JournalException {
        if (journalFile == null) {
            throw logAndGetException("Attempting to close a file "
                    + "when no file is open.");
        }

        try {
            writer.close();
            journalFile.close();
        } catch (IOException e) {
            throw logAndGetException("Problem closing the file '"
                    + journalFile.getName() + "'", e);
        }

        LOG.debug("closing file: '" + journalFile.getName() + "'");
        journalFile = null;
    }

    private JournalException logAndGetException(String message) {
        LOG.error(message);
        return new JournalException(message);
    }

    private JournalException logAndGetException(String message, Throwable e) {
        LOG.error(message, e);
        return new JournalException(message + ": " + e.toString());
    }

    /**
     * Main routine: create the receiver from the arguments, and bind the
     * receiver in the registry.
     */
    public static void main(String[] args) {
        try {
            RmiJournalReceiverArguments arguments =
                    new RmiJournalReceiverArguments(args);
            RmiJournalReceiver receiver = new RmiJournalReceiver(arguments);
            receiver.exportAndBind();
            while (true) {
                Thread.sleep(60000);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("RmiJournalReciever failed: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("RmiJournalReciever failed: ");
            e.printStackTrace();
        }
    }

}
