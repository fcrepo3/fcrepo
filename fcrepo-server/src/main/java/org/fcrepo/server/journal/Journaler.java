/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.journal;

import org.fcrepo.server.Context;
import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ModuleShutdownException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.management.Management;
import org.fcrepo.server.management.ManagementDelegate;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.RelationshipTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * A Management module that decorates a ManagementDelegate module with code that
 * either creates a Journal or consumes a Journal, depending on the startup
 * parameters.
 *
 * @author Jim Blake
 */
public class Journaler
        extends Module
        implements Management, JournalConstants {

    private static final Logger logger =
            LoggerFactory.getLogger(Journaler.class);

    private JournalWorker worker;

    private boolean inRecoveryMode;

    private ServerInterface serverInterface;

    public Journaler(Map<String, String> moduleParameters,
                     Server server,
                     String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    /**
     * Augment the parameters with values obtained from System Properties, and
     * create the proper worker (JournalCreator or JournalConsumer) for the
     * current mode.
     */
    @Override
    public void initModule() throws ModuleInitializationException {
        Map<String, String> parameters = getParameters();
        copyPropertiesOverParameters(parameters);
        serverInterface = new ServerWrapper(getServer());
        logger.info("Journaling parameters: " + parameters);
        parseParameters(parameters);

        if (inRecoveryMode) {
            worker =
                    new JournalConsumer(parameters, getRole(), serverInterface);
        } else {
            worker = new JournalCreator(parameters, getRole(), serverInterface);
        }
        logger.info("Journal worker module is: " + worker.toString());
    }

    /**
     * Get the ManagementDelegate module and pass it to the worker.
     */
    @Override
    public void postInitModule() throws ModuleInitializationException {
        ManagementDelegate delegate = serverInterface.getManagementDelegate();
        if (delegate == null) {
            throw new ModuleInitializationException("Can't get a ManagementDelegate from Server.getModule()",
                                                    getRole());
        }
        worker.setManagementDelegate(delegate);
    }

    /**
     * Tell the worker to shut down.
     */
    @Override
    public void shutdownModule() throws ModuleShutdownException {
        worker.shutdown();
    }

    /**
     * Augment, and perhaps override, the server parameters, using any System
     * Property whose name begins with "fedora.journal.". So, for example, a
     * System Property of "fedora.journal.mode" will override a server parameter
     * of "mode".
     */
    private void copyPropertiesOverParameters(Map<String, String> parameters) {
        Properties properties = System.getProperties();
        for (Object object : properties.keySet()) {
            String key = (String) object;
            if (key.startsWith(SYSTEM_PROPERTY_PREFIX)) {
                parameters.put(key.substring(SYSTEM_PROPERTY_PREFIX.length()),
                               properties.getProperty(key));
            }
        }
    }

    /**
     * Check the parameters for required values and for acceptable values. At
     * this point, the only parameter we care about is "mode".
     */
    private void parseParameters(Map<String, String> parameters)
            throws ModuleInitializationException {
        logger.info("Parameters: " + parameters);

        String mode = parameters.get(PARAMETER_JOURNAL_MODE);
        if (mode == null) {
            inRecoveryMode = false;
        } else if (mode.equals(VALUE_JOURNAL_MODE_NORMAL)) {
            inRecoveryMode = false;
        } else if (mode.equals(VALUE_JOURNAL_MODE_RECOVER)) {
            inRecoveryMode = true;
        } else {
            throw new ModuleInitializationException("'"
                                                    + PARAMETER_JOURNAL_MODE + "' parameter must be '"
                                                    + VALUE_JOURNAL_MODE_NORMAL + "'(default) or '"
                                                    + VALUE_JOURNAL_MODE_RECOVER + "'", getRole());
        }
    }

    //
    // -------------------------------------------------------------------------
    //
    // Delegate all of the "Management" methods to the worker.
    //
    // -------------------------------------------------------------------------
    //

    /**
     * Delegate to the JournalWorker.
     */
    public String ingest(Context context,
                         InputStream serialization,
                         String logMessage,
                         String format,
                         String encoding,
                         String pid) throws ServerException {
        return worker.ingest(context,
                             serialization,
                             logMessage,
                             format,
                             encoding,
                             pid);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public Date modifyObject(Context context,
                             String pid,
                             String state,
                             String label,
                             String ownerId,
                             String logMessage) throws ServerException {
        return worker.modifyObject(context,
                                   pid,
                                   state,
                                   label,
                                   ownerId,
                                   logMessage);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public InputStream getObjectXML(Context context, String pid, String encoding)
            throws ServerException {
        return worker.getObjectXML(context, pid, encoding);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public InputStream export(Context context,
                              String pid,
                              String format,
                              String exportContext,
                              String encoding) throws ServerException {
        return worker.export(context, pid, format, exportContext, encoding);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public Date purgeObject(Context context,
                            String pid,
                            String logMessage) throws ServerException {
        return worker.purgeObject(context, pid, logMessage);
    }

    /**
     * Delegate to the JournalWorker.
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
        return worker.addDatastream(context,
                                    pid,
                                    dsID,
                                    altIDs,
                                    dsLabel,
                                    versionable,
                                    MIMEType,
                                    formatURI,
                                    location,
                                    controlGroup,
                                    dsState,
                                    checksumType,
                                    checksum,
                                    logMessage);
    }

    /**
     * Delegate to the JournalWorker.
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
                                            String logMessage)
            throws ServerException {
        return worker.modifyDatastreamByReference(context,
                                                  pid,
                                                  datastreamID,
                                                  altIDs,
                                                  dsLabel,
                                                  mimeType,
                                                  formatURI,
                                                  dsLocation,
                                                  checksumType,
                                                  checksum,
                                                  logMessage);
    }

    /**
     * Delegate to the JournalWorker.
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
                                        String logMessage
    ) throws ServerException {
        return worker.modifyDatastreamByValue(context,
                                              pid,
                                              datastreamID,
                                              altIDs,
                                              dsLabel,
                                              mimeType,
                                              formatURI,
                                              dsContent,
                                              checksumType,
                                              checksum,
                                              logMessage
        );
    }

    /**
     * Delegate to the JournalWorker.
     */
    public Date[] purgeDatastream(Context context,
                                  String pid,
                                  String datastreamID,
                                  Date startDT,
                                  Date endDT,
                                  String logMessage) throws ServerException {
        return worker.purgeDatastream(context,
                                      pid,
                                      datastreamID,
                                      startDT,
                                      endDT,
                                      logMessage);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public Datastream getDatastream(Context context,
                                    String pid,
                                    String datastreamID,
                                    Date asOfDateTime) throws ServerException {
        return worker.getDatastream(context, pid, datastreamID, asOfDateTime);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public Datastream[] getDatastreams(Context context,
                                       String pid,
                                       Date asOfDateTime,
                                       String dsState) throws ServerException {
        return worker.getDatastreams(context, pid, asOfDateTime, dsState);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public Datastream[] getDatastreamHistory(Context context,
                                             String pid,
                                             String datastreamID)
            throws ServerException {
        return worker.getDatastreamHistory(context, pid, datastreamID);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public String putTempStream(Context context, InputStream in)
            throws ServerException {
        return worker.putTempStream(context, in);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public InputStream getTempStream(String id) throws ServerException {
        return worker.getTempStream(id);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public Date setDatastreamState(Context context,
                                   String pid,
                                   String dsID,
                                   String dsState,
                                   String logMessage) throws ServerException {
        return worker.setDatastreamState(context,
                                         pid,
                                         dsID,
                                         dsState,
                                         logMessage);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public Date setDatastreamVersionable(Context context,
                                         String pid,
                                         String dsID,
                                         boolean versionable,
                                         String logMessage)
            throws ServerException {
        return worker.setDatastreamVersionable(context,
                                               pid,
                                               dsID,
                                               versionable,
                                               logMessage);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public String compareDatastreamChecksum(Context context,
                                            String pid,
                                            String dsID,
                                            Date versionDate)
            throws ServerException {
        return worker
                .compareDatastreamChecksum(context, pid, dsID, versionDate);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public String[] getNextPID(Context context, int numPIDs, String namespace)
            throws ServerException {
        return worker.getNextPID(context, numPIDs, namespace);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public RelationshipTuple[] getRelationships(Context context,
                                                String pid,
                                                String relationship)
            throws ServerException {
        return worker.getRelationships(context, pid, relationship);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public boolean addRelationship(Context context,
                                   String pid,
                                   String relationship,
                                   String object,
                                   boolean isLiteral,
                                   String datatype) throws ServerException {
        return worker.addRelationship(context,
                                      pid,
                                      relationship,
                                      object,
                                      isLiteral,
                                      datatype);
    }

    /**
     * Delegate to the JournalWorker.
     */
    public boolean purgeRelationship(Context context,
                                     String pid,
                                     String relationship,
                                     String object,
                                     boolean isLiteral,
                                     String datatype) throws ServerException {
        return worker.purgeRelationship(context,
                                        pid,
                                        relationship,
                                        object,
                                        isLiteral,
                                        datatype);
    }

}
