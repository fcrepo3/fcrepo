/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast;

import java.util.Date;
import java.util.Map;

import javax.xml.stream.XMLEventWriter;

import fedora.server.journal.JournalException;

/**
 * <p>
 * Transport.java
 * </p>
 * <p>
 * The abstract superclass of Journal Transport objects. It enforces the
 * constructors of subclasses, by requiring parent, server, etc.
 * </p>
 * <p>
 * Sub-classes must implement the four life-cycle methods:
 * {@link #openFile(String, String, Date) openFile},
 * {@link #getWriter() getWriter}, {@link #closeFile() closeFile}, and
 * {@link #shutdown() shutdown}. Each of these should call
 * {@link #testStateChange(State) testStateChange} before operating, in case the
 * Transport is in an invalid state for that operation. The exception is
 * {@link #getWriter() getWriter}, which does not change the Transport state,
 * and should call {@link #testWriterState() testWriterState} instead.
 * </p>
 *
 * @author jblake
 * @version $Id: Transport.java,v 1.3 2007/06/01 17:21:31 jblake Exp $
 */
public abstract class Transport {

    /** The states of in the life-cycle of a Transport. */
    public static enum State {
        FILE_CLOSED, FILE_OPEN, SHUTDOWN
    }

    /** The parameters that were passed to this Transport for its use. */
    protected final Map<String, String> parameters;

    /** If this transport throws an Exception, is it a crucial problem? */
    protected final boolean crucial;

    /** The parent can format a file header or footer. */
    protected final TransportParent parent;

    /** Initial state is not shutdown, but no file is open, either. */
    private State currentState = State.FILE_CLOSED;

    public Transport(Map<String, String> parameters,
                     boolean crucial,
                     TransportParent parent)
            throws JournalException {
        this.parameters = parameters;
        this.crucial = crucial;
        this.parent = parent;
    }

    Map<String, String> getParameters() {
        return parameters;
    }

    public boolean isCrucial() {
        return crucial;
    }

    /**
     * <p>
     * Subclasses should call this before attempting an operation that will
     * change the current state, so if the change is not permitted, it will not
     * be performed.
     * </p>
     * <p>
     * Note that a redundant call to Shutdown is not considered an error.
     * </p>
     */
    public void testStateChange(State desiredState) throws JournalException {
        if (currentState == State.FILE_CLOSED
                && desiredState == State.FILE_CLOSED) {
            throw new JournalException("Request to close Transport when not open.");
        }
        if (currentState == State.FILE_OPEN && desiredState == State.FILE_OPEN) {
            throw new JournalException("Request to open Transport when already open.");
        }
        if (currentState == State.FILE_OPEN && desiredState == State.SHUTDOWN) {
            throw new JournalException("Request to shutdown Transport with file open.");
        }
        if (currentState == State.SHUTDOWN && desiredState == State.FILE_OPEN) {
            throw new JournalException("Request to open Transport after shutdown.");
        }
        if (currentState == State.SHUTDOWN && desiredState == State.FILE_CLOSED) {
            throw new JournalException("Request to close Transport after shutdown.");
        }
    }

    /**
     * Subclasses should call this before executing a
     * {@link #getWriter() getWriter} request. This insures that the Transport
     * is in the proper state.
     */
    protected void testWriterState() throws JournalException {
        if (currentState == State.FILE_CLOSED) {
            throw new JournalException("Trying to write to a Transport that is not open.");
        }
        if (currentState == State.SHUTDOWN) {
            throw new JournalException("Trying to write to a Transport after shutdown.");
        }
    }

    /**
     * Subclasses should call this to change the current state, after the
     * operation has been performed.
     */
    public void setState(State newState) throws JournalException {
        testStateChange(newState);
        currentState = newState;
    }

    public State getState() {
        return currentState;
    }

    /**
     * Subclasses should implement this to create a new logical journal file.
     * Check {@link #testStateChange(State) testStateChange} before performing
     * the operation, and call
     * {@link TransportParent#writeDocumentHeader(XMLEventWriter, String, Date)}
     * to do the formatting.
     */
    public abstract void openFile(String repositoryHash,
                                  String filename,
                                  Date currentDate) throws JournalException;

    /**
     * Subclasses should implement this to provite an {@link XMLEventWriter} for
     * the JournalWriter to write to. Check {@link #testWriterState()} before
     * performing the operation.
     */
    public abstract XMLEventWriter getWriter() throws JournalException;

    /**
     * Subclasses should implement this to close a logical journal file. Check
     * {@link #testStateChange(State) testStateChange} before performing the
     * operation, and call
     * {@link TransportParent#writeDocumentTrailer(XMLEventWriter)} to do the
     * formatting.
     */
    public abstract void closeFile() throws JournalException;

    /**
     * Subclasses should implement this to close any resources associated with
     * the Transport (unless it is already shut down). Check
     * {@link #testStateChange(State) testStateChange} before performing the
     * operation.
     */
    public abstract void shutdown() throws JournalException;

}
