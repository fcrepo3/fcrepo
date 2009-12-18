/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast.rmi;

import java.io.BufferedWriter;
import java.io.IOException;

import java.rmi.RemoteException;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import fedora.server.journal.JournalException;

import static junit.framework.Assert.assertEquals;

/**
 * <p>
 * <b>Title:</b> TestRmiTransportWriter.java
 * </p>
 * <p>
 * <b>Description:</b> Put the RmiTransportWriter through its paces.
 * </p>
 *
 * @author jblake
 * @version $Id: TestRmiTransportWriter.java,v 1.3 2007/06/01 17:21:31 jblake
 *          Exp $
 */
public class TestRmiTransportWriter {

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestRmiTransport.class);
    }

    MockRmiJournalReceiver receiver;

    String repositoryHash;

    @Before
    public void initializeReceiver() throws RemoteException {
        receiver = new MockRmiJournalReceiver();
    }

    @Before
    public void initializeRepositoryHash() {
        repositoryHash = "Hash" + new Date().getTime();
    }

    @Test
    public void testConstructorOpensConnection() throws JournalException {
        new RmiTransportWriter(receiver, repositoryHash, "theFilename");
        assertCorrectNumberOfCalls(receiver, 1, 0, 0);
        assertEquals(repositoryHash, receiver.getRepositoryHash());
        assertEquals("theFilename", receiver.getFilename());
    }

    @Test(expected = JournalException.class)
    public void testConstructorGetsException() throws JournalException {
        receiver.setOpenFileThrowsException(true);
        new RmiTransportWriter(receiver, repositoryHash, "theFilename");
    }

    @Test
    public void testSeriesOfWrites() throws JournalException, IOException {
        RmiTransportWriter writer =
                new RmiTransportWriter(receiver, repositoryHash, "theFilename");
        assertCorrectNumberOfCalls(receiver, 1, 0, 0);

        String text1 = "Some silly text";
        char[] chars = text1.toCharArray();
        writer.write(chars, 0, text1.length());
        assertCorrectNumberOfCalls(receiver, 1, 1, 0);
        assertEquals("unexpected text 1", text1, receiver.getText());
        assertCorrectItemHash(receiver, 0);

        String text2 = "This is something else";
        writer.write(text2);
        assertCorrectNumberOfCalls(receiver, 1, 2, 0);
        assertEquals("unexpected text 2", text2, receiver.getText());
        assertCorrectItemHash(receiver, 1);

        String text3 = "What's going on?";
        writer.write(text3, 3, 8);
        assertCorrectNumberOfCalls(receiver, 1, 3, 0);
        assertEquals("unexpected text 3", text3.substring(3, 11), receiver
                .getText());
        assertCorrectItemHash(receiver, 2);
    }

    @Test(expected = IOException.class)
    public void testWriteGetsException() throws JournalException, IOException {
        receiver.setWriteTextThrowsException(true);

        RmiTransportWriter writer =
                new RmiTransportWriter(receiver, repositoryHash, "theFilename");
        writer.write("Throw an exception");
    }

    @Test
    public void testCloseClosesFile() throws JournalException, IOException {
        RmiTransportWriter writer =
                new RmiTransportWriter(receiver, repositoryHash, "theFilename");
        writer.close();
        assertCorrectNumberOfCalls(receiver, 1, 0, 1);
    }

    @Test(expected = IOException.class)
    public void testCloseThrowsException() throws JournalException, IOException {
        // Note that the mock receiver throws a JournalException on close, but
        // the writer wraps it in an IOException.
        receiver.setCloseFileThrowsException(true);

        RmiTransportWriter writer =
                new RmiTransportWriter(receiver, repositoryHash, "theFilename");
        writer.close();
    }

    @Test
    public void testBigBufferYieldsOnlyOneWrite() throws JournalException,
            IOException {
        String text1 = "Write a bunch of stuff to the buffer";
        String text2 = "But it doesn't go out to the RmiTransportWriter";
        String text3 = "Until we do a flush or a close.";

        BufferedWriter buffered =
                new BufferedWriter(new RmiTransportWriter(receiver,
                                                          repositoryHash,
                                                          "theFilename"), 10000);
        assertCorrectNumberOfCalls(receiver, 1, 0, 0);

        buffered.write(text1);
        assertCorrectNumberOfCalls(receiver, 1, 0, 0);
        buffered.write(text2);
        assertCorrectNumberOfCalls(receiver, 1, 0, 0);
        buffered.write(text3);
        assertCorrectNumberOfCalls(receiver, 1, 0, 0);

        buffered.flush();
        assertCorrectNumberOfCalls(receiver, 1, 1, 0);
        assertEquals("unexpected text", text1 + text2 + text3, receiver
                .getText());
    }

    private void assertCorrectItemHash(MockRmiJournalReceiver receiver,
                                       int index) {
        assertEquals("unexpected item hash", RmiJournalReceiverHelper
                .figureIndexedHash(repositoryHash, index), receiver
                .getIndexedHash());
    }

    private void assertCorrectNumberOfCalls(MockRmiJournalReceiver receiver,
                                            int i,
                                            int j,
                                            int k) {
        assertEquals("wrong number of calls to openFile()", i, receiver
                .howManyCallsToOpenFile());
        assertEquals("wrong number of calls to writeText()", j, receiver
                .howManyCallsToWriteText());
        assertEquals("wrong number of calls to closeFile()", k, receiver
                .howManyCallsToClosefile());
    }

}
