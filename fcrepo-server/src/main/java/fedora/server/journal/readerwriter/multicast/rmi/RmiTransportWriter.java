/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast.rmi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import java.rmi.RemoteException;

import fedora.server.journal.JournalException;

/**
 * <p>
 * <b>Title:</b> RmiTransportWriter.java
 * </p>
 * <p>
 * <b>Description:</b> A writer that passes characters on to an
 * RmiTransportSink. You should wrap this in a {@link BufferedWriter} to reduce
 * network traffic to something manageable.
 * </p>
 *
 * @author jblake
 * @version $Id: RmiTransportWriter.java,v 1.3 2007/06/01 17:21:32 jblake Exp $
 */
public class RmiTransportWriter
        extends Writer {

    private final RmiJournalReceiverInterface receiver;

    private final String repositoryHash;

    private long itemIndex;

    /**
     * When the writer is created, open the connection to the receiver.
     */
    public RmiTransportWriter(RmiJournalReceiverInterface receiver,
                              String repositoryHash,
                              String filename)
            throws JournalException {
        this.receiver = receiver;
        this.repositoryHash = repositoryHash;

        try {
            receiver.openFile(repositoryHash, filename);
        } catch (RemoteException e) {
            throw new JournalException(e);
        }
    }

    /**
     * Each time characters are written, send them to the receiver. Keep track
     * of how many writes, so we can create a proper item hash.
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        try {
            String indexedHash =
                    RmiJournalReceiverHelper.figureIndexedHash(repositoryHash,
                                                               itemIndex);
            receiver.writeText(indexedHash, new String(cbuf, off, len));
            itemIndex++;
        } catch (JournalException e) {
            IOException wrapper = new IOException();
            wrapper.initCause(e);
            throw wrapper;
        }
    }

    /**
     * Time to close the file? Tell the receiver.
     */
    @Override
    public void close() throws IOException {
        try {
            receiver.closeFile();
        } catch (JournalException e) {
            IOException wrapper = new IOException();
            wrapper.initCause(e);
            throw wrapper;
        }
    }

    /**
     * Flush has no effect. Everything is written as it comes in.
     */
    @Override
    public void flush() throws IOException {
    }

}
