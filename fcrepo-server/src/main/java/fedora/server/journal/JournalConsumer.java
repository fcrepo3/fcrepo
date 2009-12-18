/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal;

import java.io.InputStream;

import java.util.Date;
import java.util.Map;

import fedora.server.Context;
import fedora.server.errors.InvalidStateException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ModuleShutdownException;
import fedora.server.errors.ServerException;
import fedora.server.journal.recoverylog.JournalRecoveryLog;
import fedora.server.management.ManagementDelegate;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.RelationshipTuple;

/**
 * The JournalWorker class to use in recovery mode or in* following mode.
 * <p>
 * Create a <code>JournalConsumerThread</code> to process the journal. If any
 * calls to Management methods come in from outside, reject them.
 * </p>
 *
 * @author Jim Blake
 */
public class JournalConsumer
        implements JournalWorker {

    private final String role;

    private final JournalConsumerThread consumerThread;

    private final JournalReader reader;

    private final JournalRecoveryLog recoveryLog;

    private ManagementDelegate delegate;

    /**
     * Get the appropriate JournalReader and JournalRecoveryLog, based on the
     * server parameters, and create a JournalConsumerThread that will process
     * the journal entries, using that reader and that log.
     */
    public JournalConsumer(Map<String, String> parameters,
                           String role,
                           ServerInterface server)
            throws ModuleInitializationException {
        this.role = role;
        recoveryLog = JournalRecoveryLog.getInstance(parameters, role, server);
        reader =
                JournalReader
                        .getInstance(parameters, role, recoveryLog, server);
        consumerThread =
                new JournalConsumerThread(parameters,
                                          role,
                                          server,
                                          reader,
                                          recoveryLog);
    }

    /**
     * Get the ManagementDelegate module and pass it to the
     * JournalConsumerThread, so it can start working.
     */
    public void setManagementDelegate(ManagementDelegate delegate) {
        this.delegate = delegate;
        consumerThread.setManagementDelegate(delegate);
    }

    /**
     * Tell the thread, the reader and the log to shut down.
     */
    public void shutdown() throws ModuleShutdownException {
        try {
            consumerThread.shutdown();
            reader.shutdown();
            recoveryLog.shutdown("Server is shutting down.");
        } catch (JournalException e) {
            throw new ModuleShutdownException("Error closing journal reader.",
                                              role,
                                              e);
        }
    }

    //
    // -------------------------------------------------------------------------
    //
    // Reject outside calls to Management API methods that modify the
    // repository.
    //
    // -------------------------------------------------------------------------
    //

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public String ingest(Context context,
                         InputStream serialization,
                         String logMessage,
                         String format,
                         String encoding,
                         boolean newPid) throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public Date modifyObject(Context context,
                             String pid,
                             String state,
                             String label,
                             String ownerId,
                             String logMessage) throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public Date purgeObject(Context context,
                            String pid,
                            String logMessage,
                            boolean force) throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public String addDatastream(Context context,
                                String pid,
                                String dsID,
                                String[] altIDs,
                                String dsLabel,
                                boolean versionable,
                                String MIMEType,
                                String formatURI,
                                String location,
                                String controlGroup,
                                String dsState,
                                String checksumType,
                                String checksum,
                                String logMessage) throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public Date modifyDatastreamByReference(Context context,
                                            String pid,
                                            String datastreamID,
                                            String[] altIDs,
                                            String dsLabel,
                                            String mimeType,
                                            String formatURI,
                                            String dsLocation,
                                            String checksumType,
                                            String checksum,
                                            String logMessage,
                                            boolean force)
            throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public Date modifyDatastreamByValue(Context context,
                                        String pid,
                                        String datastreamID,
                                        String[] altIDs,
                                        String dsLabel,
                                        String mimeType,
                                        String formatURI,
                                        InputStream dsContent,
                                        String checksumType,
                                        String checksum,
                                        String logMessage,
                                        boolean force) throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public Date[] purgeDatastream(Context context,
                                  String pid,
                                  String datastreamID,
                                  Date startDT,
                                  Date endDT,
                                  String logMessage,
                                  boolean force) throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public String putTempStream(Context context, InputStream in)
            throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public Date setDatastreamState(Context context,
                                   String pid,
                                   String dsID,
                                   String dsState,
                                   String logMessage) throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public Date setDatastreamVersionable(Context context,
                                         String pid,
                                         String dsID,
                                         boolean versionable,
                                         String logMessage)
            throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public Date setDisseminatorState(Context context,
                                     String pid,
                                     String dsID,
                                     String dsState,
                                     String logMessage) throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public String[] getNextPID(Context context, int numPIDs, String namespace)
            throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public boolean addRelationship(Context context,
                                   String pid,
                                   String relationship,
                                   String objURI,
                                   boolean isLiteral,
                                   String datatype) throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    /**
     * Reject API calls from outside while we are in recovery mode.
     */
    public boolean purgeRelationship(Context context,
                                     String pid,
                                     String relationship,
                                     String objURI,
                                     boolean isLiteral,
                                     String datatype) throws ServerException {
        throw rejectCallsFromOutsideWhileInRecoveryMode();
    }

    //
    // -------------------------------------------------------------------------
    //
    // Permit outside calls to Management API methods that do not modify the
    // repository.
    //
    // -------------------------------------------------------------------------
    //

    /**
     * Read-only method: pass the call to the {@link ManagementDelegate}.
     */
    public String compareDatastreamChecksum(Context context,
                                            String pid,
                                            String dsID,
                                            Date versionDate)
            throws ServerException {
        return delegate.compareDatastreamChecksum(context,
                                                  pid,
                                                  dsID,
                                                  versionDate);
    }

    /**
     * Read-only method: pass the call to the {@link ManagementDelegate}.
     */
    public InputStream export(Context context,
                              String pid,
                              String format,
                              String exportContext,
                              String encoding) throws ServerException {
        return delegate.export(context, pid, format, exportContext, encoding);
    }

    /**
     * Read-only method: pass the call to the {@link ManagementDelegate}.
     */
    public InputStream getObjectXML(Context context, String pid, String encoding)
            throws ServerException {
        return delegate.getObjectXML(context, pid, encoding);
    }

    /**
     * Read-only method: pass the call to the {@link ManagementDelegate}.
     */
    public Datastream getDatastream(Context context,
                                    String pid,
                                    String datastreamID,
                                    Date asOfDateTime) throws ServerException {
        return delegate.getDatastream(context, pid, datastreamID, asOfDateTime);
    }

    /**
     * Read-only method: pass the call to the {@link ManagementDelegate}.
     */
    public Datastream[] getDatastreams(Context context,
                                       String pid,
                                       Date asOfDateTime,
                                       String dsState) throws ServerException {
        return delegate.getDatastreams(context, pid, asOfDateTime, dsState);
    }

    /**
     * Read-only method: pass the call to the {@link ManagementDelegate}.
     */
    public Datastream[] getDatastreamHistory(Context context,
                                             String pid,
                                             String datastreamID)
            throws ServerException {
        return delegate.getDatastreamHistory(context, pid, datastreamID);
    }

    /**
     * Read-only method: pass the call to the {@link ManagementDelegate}.
     */
    public RelationshipTuple[] getRelationships(Context context,
                                                String pid,
                                                String relationship)
            throws ServerException {
        return delegate.getRelationships(context, pid, relationship);
    }

    /**
     * Delegate to the ManagementDelegate. Note: Unlike other methods of the
     * Management interface, this method is not exposed at the service level.
     * Therefore, it is safe to forward the call to the delegate. It is also
     * necessary because, in the course of fulfilling API-M requests that
     * involve uploaded content, this method is invoked by internal server code.
     */
    public InputStream getTempStream(String id) throws ServerException {
        return delegate.getTempStream(id);
    }

    /**
     * While the server is reading a Journal to recover its state, block any
     * attempt to use the Management API.
     *
     * @throws ServerException
     */
    private ServerException rejectCallsFromOutsideWhileInRecoveryMode() {
        return new InvalidStateException("Server is in Journal Recovery mode.");
    }

}
