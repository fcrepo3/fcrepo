/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal;

import java.io.InputStream;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import fedora.server.Context;
import fedora.server.errors.GeneralException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ModuleShutdownException;
import fedora.server.errors.ServerException;
import fedora.server.journal.entry.CreatorJournalEntry;
import fedora.server.management.ManagementDelegate;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.RelationshipTuple;

/**
 * This is the worker class to use in Journaling mode (normal mode).
 * <p>
 * Each time a "writing" Management method is called, create a
 * CreatorJournalEntry and ask it to invoke the method on the
 * ManagementDelegate. If a "read-only" Management method is called, just pass
 * it along to the ManagementDelegate.
 *
 * @author Jim Blake
 */
public class JournalCreator
        implements JournalWorker, JournalConstants {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(JournalCreator.class.getName());

    private final JournalWriter writer;

    private final String role;

    private ManagementDelegate delegate;

    /**
     * Get a JournalWriter to use, based on the server parameters.
     */
    public JournalCreator(Map<String, String> parameters,
                          String role,
                          ServerInterface server)
            throws ModuleInitializationException {
        this.role = role;

        try {
            writer = JournalWriter.getInstance(parameters, role, server);
        } catch (JournalException e) {
            String msg = "Problem creating the JournalWriter";
            LOG.error(msg, e);
            throw new ModuleInitializationException(msg, role, e);
        }
    }

    /**
     * Receive a ManagementDelegate module to perform the Management operations.
     */
    public void setManagementDelegate(ManagementDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Server is shutting down, so tell the JournalWriter to shut down.
     */
    public void shutdown() throws ModuleShutdownException {
        try {
            writer.shutdown();
        } catch (JournalException e) {
            throw new ModuleShutdownException("JournalWriter generated an error on shutdown()",
                                              role,
                                              e);
        }
    }

    //
    // -------------------------------------------------------------------------
    //
    // Create a Journal entry for each call to one of the Management API
    // "writing" methods.
    //
    // -------------------------------------------------------------------------
    //

    /**
     * Let the delegate do it, and then write a journal entry.
     */
    public String ingest(Context context,
                         InputStream serialization,
                         String logMessage,
                         String format,
                         String encoding,
                         boolean newPid) throws ServerException {
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_INGEST, context);
            cje.addArgument(ARGUMENT_NAME_SERIALIZATION, serialization);
            cje.addArgument(ARGUMENT_NAME_LOG_MESSAGE, logMessage);
            cje.addArgument(ARGUMENT_NAME_FORMAT, format);
            cje.addArgument(ARGUMENT_NAME_ENCODING, encoding);
            cje.addArgument(ARGUMENT_NAME_NEW_PID, newPid);
            return (String) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    /**
     * Create a journal entry, add the arguments, and invoke the method.
     */
    public Date modifyObject(Context context,
                             String pid,
                             String state,
                             String label,
                             String ownerId,
                             String logMessage) throws ServerException {
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_MODIFY_OBJECT, context);
            cje.addArgument(ARGUMENT_NAME_PID, pid);
            cje.addArgument(ARGUMENT_NAME_STATE, state);
            cje.addArgument(ARGUMENT_NAME_LABEL, label);
            cje.addArgument(ARGUMENT_NAME_OWNERID, ownerId);
            cje.addArgument(ARGUMENT_NAME_LOG_MESSAGE, logMessage);
            return (Date) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    /**
     * Create a journal entry, add the arguments, and invoke the method.
     */
    public Date purgeObject(Context context,
                            String pid,
                            String logMessage,
                            boolean force) throws ServerException {
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_PURGE_OBJECT, context);
            cje.addArgument(ARGUMENT_NAME_PID, pid);
            cje.addArgument(ARGUMENT_NAME_LOG_MESSAGE, logMessage);
            cje.addArgument(ARGUMENT_NAME_FORCE, force);
            return (Date) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    /**
     * Create a journal entry, add the arguments, and invoke the method.
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
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_ADD_DATASTREAM, context);
            cje.addArgument(ARGUMENT_NAME_PID, pid);
            cje.addArgument(ARGUMENT_NAME_DS_ID, dsID);
            cje.addArgument(ARGUMENT_NAME_ALT_IDS, altIDs);
            cje.addArgument(ARGUMENT_NAME_DS_LABEL, dsLabel);
            cje.addArgument(ARGUMENT_NAME_VERSIONABLE, versionable);
            cje.addArgument(ARGUMENT_NAME_MIME_TYPE, MIMEType);
            cje.addArgument(ARGUMENT_NAME_FORMAT_URI, formatURI);
            cje.addArgument(ARGUMENT_NAME_LOCATION, location);
            cje.addArgument(ARGUMENT_NAME_CONTROL_GROUP, controlGroup);
            cje.addArgument(ARGUMENT_NAME_DS_STATE, dsState);
            cje.addArgument(ARGUMENT_NAME_CHECKSUM_TYPE, checksumType);
            cje.addArgument(ARGUMENT_NAME_CHECKSUM, checksum);
            cje.addArgument(ARGUMENT_NAME_LOG_MESSAGE, logMessage);
            return (String) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    /**
     * Create a journal entry, add the arguments, and invoke the method.
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
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_MODIFY_DATASTREAM_BY_VALUE,
                                            context);
            cje.addArgument(ARGUMENT_NAME_PID, pid);
            cje.addArgument(ARGUMENT_NAME_DS_ID, datastreamID);
            cje.addArgument(ARGUMENT_NAME_ALT_IDS, altIDs);
            cje.addArgument(ARGUMENT_NAME_DS_LABEL, dsLabel);
            cje.addArgument(ARGUMENT_NAME_MIME_TYPE, mimeType);
            cje.addArgument(ARGUMENT_NAME_FORMAT_URI, formatURI);
            cje.addArgument(ARGUMENT_NAME_DS_CONTENT, dsContent);
            cje.addArgument(ARGUMENT_NAME_CHECKSUM_TYPE, checksumType);
            cje.addArgument(ARGUMENT_NAME_CHECKSUM, checksum);
            cje.addArgument(ARGUMENT_NAME_LOG_MESSAGE, logMessage);
            cje.addArgument(ARGUMENT_NAME_FORCE, force);
            return (Date) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    /**
     * Create a journal entry, add the arguments, and invoke the method.
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
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_MODIFY_DATASTREAM_BY_REFERENCE,
                                            context);
            cje.addArgument(ARGUMENT_NAME_PID, pid);
            cje.addArgument(ARGUMENT_NAME_DS_ID, datastreamID);
            cje.addArgument(ARGUMENT_NAME_ALT_IDS, altIDs);
            cje.addArgument(ARGUMENT_NAME_DS_LABEL, dsLabel);
            cje.addArgument(ARGUMENT_NAME_MIME_TYPE, mimeType);
            cje.addArgument(ARGUMENT_NAME_FORMAT_URI, formatURI);
            cje.addArgument(ARGUMENT_NAME_DS_LOCATION, dsLocation);
            cje.addArgument(ARGUMENT_NAME_CHECKSUM_TYPE, checksumType);
            cje.addArgument(ARGUMENT_NAME_CHECKSUM, checksum);
            cje.addArgument(ARGUMENT_NAME_LOG_MESSAGE, logMessage);
            cje.addArgument(ARGUMENT_NAME_FORCE, force);
            return (Date) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    /**
     * Create a journal entry, add the arguments, and invoke the method.
     */
    public Date setDatastreamState(Context context,
                                   String pid,
                                   String dsID,
                                   String dsState,
                                   String logMessage) throws ServerException {
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_SET_DATASTREAM_STATE,
                                            context);
            cje.addArgument(ARGUMENT_NAME_PID, pid);
            cje.addArgument(ARGUMENT_NAME_DS_ID, dsID);
            cje.addArgument(ARGUMENT_NAME_DS_STATE, dsState);
            cje.addArgument(ARGUMENT_NAME_LOG_MESSAGE, logMessage);
            return (Date) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    /**
     * Create a journal entry, add the arguments, and invoke the method.
     */
    public Date setDatastreamVersionable(Context context,
                                         String pid,
                                         String dsID,
                                         boolean versionable,
                                         String logMessage)
            throws ServerException {
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_SET_DATASTREAM_VERSIONABLE,
                                            context);
            cje.addArgument(ARGUMENT_NAME_PID, pid);
            cje.addArgument(ARGUMENT_NAME_DS_ID, dsID);
            cje.addArgument(ARGUMENT_NAME_VERSIONABLE, versionable);
            cje.addArgument(ARGUMENT_NAME_LOG_MESSAGE, logMessage);
            return (Date) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    /**
     * Create a journal entry, add the arguments, and invoke the method.
     */
    public Date[] purgeDatastream(Context context,
                                  String pid,
                                  String datastreamID,
                                  Date startDT,
                                  Date endDT,
                                  String logMessage,
                                  boolean force) throws ServerException {
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_PURGE_DATASTREAM, context);
            cje.addArgument(ARGUMENT_NAME_PID, pid);
            cje.addArgument(ARGUMENT_NAME_DS_ID, datastreamID);
            cje.addArgument(ARGUMENT_NAME_START_DATE, startDT);
            cje.addArgument(ARGUMENT_NAME_END_DATE, endDT);
            cje.addArgument(ARGUMENT_NAME_LOG_MESSAGE, logMessage);
            cje.addArgument(ARGUMENT_NAME_FORCE, force);
            return (Date[]) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    /**
     * Create a journal entry, add the arguments, and invoke the method.
     */
    public String putTempStream(Context context, InputStream in)
            throws ServerException {
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_PUT_TEMP_STREAM, context);
            cje.addArgument(ARGUMENT_NAME_IN, in);
            return (String) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    /**
     * Create a journal entry, add the arguments, and invoke the method.
     */
    public String[] getNextPID(Context context, int numPIDs, String namespace)
            throws ServerException {
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_GET_NEXT_PID, context);
            cje.addArgument(ARGUMENT_NAME_NUM_PIDS, numPIDs);
            cje.addArgument(ARGUMENT_NAME_NAMESPACE, namespace);
            return (String[]) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    public boolean addRelationship(Context context,
                                   String pid,
                                   String relationship,
                                   String objURI,
                                   boolean isLiteral,
                                   String datatype) throws ServerException {
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_ADD_RELATIONSHIP, context);
            cje.addArgument(ARGUMENT_NAME_PID, pid);
            cje.addArgument(ARGUMENT_NAME_RELATIONSHIP, relationship);
            cje.addArgument(ARGUMENT_NAME_OBJECT, objURI);
            cje.addArgument(ARGUMENT_NAME_IS_LITERAL, isLiteral);
            cje.addArgument(ARGUMENT_NAME_DATATYPE, datatype);
            return (Boolean) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    public boolean purgeRelationship(Context context,
                                     String pid,
                                     String relationship,
                                     String object,
                                     boolean isLiteral,
                                     String datatype) throws ServerException {
        try {
            CreatorJournalEntry cje =
                    new CreatorJournalEntry(METHOD_PURGE_RELATIONSHIP, context);
            cje.addArgument(ARGUMENT_NAME_PID, pid);
            cje.addArgument(ARGUMENT_NAME_RELATIONSHIP, relationship);
            cje.addArgument(ARGUMENT_NAME_OBJECT, object);
            cje.addArgument(ARGUMENT_NAME_IS_LITERAL, isLiteral);
            cje.addArgument(ARGUMENT_NAME_DATATYPE, datatype);
            return (Boolean) cje.invokeAndClose(delegate, writer);
        } catch (JournalException e) {
            throw new GeneralException("Problem creating the Journal", e);
        }
    }

    //
    // -------------------------------------------------------------------------
    //
    // For read-only methods, don't bother with a Journal entry.
    //
    // -------------------------------------------------------------------------
    //

    /**
     * Let the delegate do it.
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
     * Let the delegate do it.
     */
    public RelationshipTuple[] getRelationships(Context context,
                                                String pid,
                                                String relationship)
            throws ServerException {
        return delegate.getRelationships(context, pid, relationship);
    }

    /**
     * Let the delegate do it.
     */
    public InputStream getObjectXML(Context context, String pid, String encoding)
            throws ServerException {
        return delegate.getObjectXML(context, pid, encoding);
    }

    /**
     * Let the delegate do it.
     */
    public InputStream export(Context context,
                              String pid,
                              String format,
                              String exportContext,
                              String encoding) throws ServerException {
        return delegate.export(context, pid, format, exportContext, encoding);
    }

    /**
     * Let the delegate do it.
     */
    public Datastream getDatastream(Context context,
                                    String pid,
                                    String datastreamID,
                                    Date asOfDateTime) throws ServerException {
        return delegate.getDatastream(context, pid, datastreamID, asOfDateTime);
    }

    /**
     * Let the delegate do it.
     */
    public Datastream[] getDatastreams(Context context,
                                       String pid,
                                       Date asOfDateTime,
                                       String dsState) throws ServerException {
        return delegate.getDatastreams(context, pid, asOfDateTime, dsState);
    }

    /**
     * Let the delegate do it.
     */
    public Datastream[] getDatastreamHistory(Context context,
                                             String pid,
                                             String datastreamID)
            throws ServerException {
        return delegate.getDatastreamHistory(context, pid, datastreamID);
    }

    /**
     * Let the delegate do it.
     */
    public InputStream getTempStream(String id) throws ServerException {
        return delegate.getTempStream(id);
    }

}
