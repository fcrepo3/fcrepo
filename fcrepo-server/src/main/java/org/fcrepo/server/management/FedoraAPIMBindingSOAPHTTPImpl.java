/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.management;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Date;

import org.apache.axis.types.NonNegativeInteger;
import org.fcrepo.common.Constants;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.InitializationException;
import org.fcrepo.server.errors.ServerInitializationException;
import org.fcrepo.server.errors.StorageDeviceException;
import org.fcrepo.server.utilities.AxisUtility;
import org.fcrepo.server.utilities.DateUtility;
import org.fcrepo.server.utilities.TypeUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the Fedora management SOAP service.
 *
 * @author Chris Wilper
 */
public class FedoraAPIMBindingSOAPHTTPImpl
        implements Constants, FedoraAPIM {

    private static final Logger logger =
            LoggerFactory.getLogger(FedoraAPIMBindingSOAPHTTPImpl.class);

    /**
     * The Fedora Server instance
     */
    private static Server s_server;

    /**
     * Whether the service has initialized... true if we got a good Server
     * instance.
     */
    private static boolean s_initialized;

    /**
     * The exception indicating that initialization failed.
     */
    private static InitializationException s_initException;

    private static Management s_management;

    /** Before fulfilling any requests, make sure we have a server instance. */
    static {
        try {
            String fedoraHome = Constants.FEDORA_HOME;
            if (fedoraHome == null) {
                s_initialized = false;
                s_initException =
                        new ServerInitializationException("Server failed to initialize: FEDORA_HOME is undefined");
            } else {
                s_server = Server.getInstance(new File(fedoraHome), false);
                s_initialized = true;
                s_management =
                        (Management) s_server
                                .getModule("org.fcrepo.server.management.Management");
            }
        } catch (InitializationException ie) {
            logger.error("Error getting server", ie);
            s_initialized = false;
            s_initException = ie;
        }
    }

    public String ingest(byte[] XML, String format, String logMessage)
            throws java.rmi.RemoteException {
        logger.debug("start: ingest");
        assertInitialized();
        try {
            // always gens pid, unless pid in stream starts with "test:" "demo:"
            // or other prefix that is configured in the retainPIDs parameter of
            // fedora.fcfg
            return s_management.ingest(ReadOnlyContext.getSoapContext(),
                                       new ByteArrayInputStream(XML),
                                       logMessage,
                                       format,
                                       "UTF-8",
                                       "new");
        } catch (Throwable th) {
            logger.error("Error ingesting", th);
            throw AxisUtility.getFault(th);
        } finally {
            logger.debug("end: ingest");
        }
    }

    public String modifyObject(String PID,
                               String state,
                               String label,
                               String ownerId,
                               String logMessage) throws RemoteException {
        logger.debug("start: modifyObject, " + PID);
        assertInitialized();
        try {
            return DateUtility.convertDateToString(s_management
                    .modifyObject(ReadOnlyContext.getSoapContext(),
                                  PID,
                                  state,
                                  label,
                                  ownerId,
                                  logMessage,
                                  null));
        } catch (Throwable th) {
            logger.error("Error modifying object", th);
            throw AxisUtility.getFault(th);
        } finally {
            logger.debug("end: modifyObject, " + PID);
        }
    }

    public byte[] getObjectXML(String PID) throws RemoteException {
        assertInitialized();
        try {
            InputStream in =
                    s_management.getObjectXML(ReadOnlyContext.getSoapContext(),
                                              PID,
                                              "UTF-8");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pipeStream(in, out);
            return out.toByteArray();
        } catch (Throwable th) {
            logger.error("Error getting object XML", th);
            throw AxisUtility.getFault(th);
        }
    }

    public byte[] export(String PID, String format, String exportContext)
            throws RemoteException {
        assertInitialized();
        try {
            InputStream in =
                    s_management.export(ReadOnlyContext.getSoapContext(),
                                        PID,
                                        format,
                                        exportContext,
                                        "UTF-8");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pipeStream(in, out);
            return out.toByteArray();
        } catch (Throwable th) {
            logger.error("Error exporting object", th);
            throw AxisUtility.getFault(th);
        }
    }

    // temporarily here

    private void pipeStream(InputStream in, OutputStream out)
            throws StorageDeviceException {
        try {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        } catch (IOException ioe) {
            throw new StorageDeviceException("Error writing to stream");
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException closeProb) {
                // ignore problems while closing
            }
        }
    }

    public String purgeObject(String PID, String logMessage)
            throws java.rmi.RemoteException {
        logger.debug("start: purgeObject, " + PID);
        assertInitialized();
        try {
            return DateUtility.convertDateToString(s_management
                    .purgeObject(ReadOnlyContext.getSoapContext(),
                                 PID,
                                 logMessage));
        } catch (Throwable th) {
            logger.error("Error purging object", th);
            throw AxisUtility.getFault(th);
        } finally {
            logger.debug("end: purgeObject, " + PID);
        }
    }

    public String addDatastream(String pid,
                                String dsID,
                                String[] altIds,
                                String label,
                                boolean versionable,
                                String MIMEType,
                                String formatURI,
                                String location,
                                String controlGroup,
                                String dsState,
                                String checksumType,
                                String checksum,
                                String logMessage) throws RemoteException {
        logger.debug("start: addDatastream, " + pid + ", " + dsID);
        assertInitialized();
        try {
            return s_management.addDatastream(ReadOnlyContext.getSoapContext(),
                                              pid,
                                              dsID,
                                              altIds,
                                              label,
                                              versionable,
                                              MIMEType,
                                              formatURI,
                                              location,
                                              controlGroup,
                                              dsState,
                                              checksumType,
                                              checksum,
                                              logMessage);
        } catch (Throwable th) {
            logger.error("Error adding datastream", th);
            throw AxisUtility.getFault(th);
        } finally {
            logger.debug("end: addDatastream, " + pid + ", " + dsID);
        }
    }

    public String modifyDatastreamByReference(String PID,
                                              String datastreamID,
                                              String[] altIDs,
                                              String dsLabel,
                                              String mimeType,
                                              String formatURI,
                                              String dsLocation,
                                              String checksumType,
                                              String checksum,
                                              String logMessage)
            throws java.rmi.RemoteException {
        logger.debug("start: modifyDatastreamByReference, " + PID + ", "
                     + datastreamID);
        assertInitialized();
        try {
            return DateUtility.convertDateToString(s_management
                    .modifyDatastreamByReference(ReadOnlyContext
                    .getSoapContext(),
                                                 PID,
                                                 datastreamID,
                                                 altIDs,
                                                 dsLabel,
                                                 mimeType,
                                                 formatURI,
                                                 dsLocation,
                                                 checksumType,
                                                 checksum,
                                                 logMessage,
                                                 null));
        } catch (Throwable th) {
            logger.error("Error modifying datastream by reference", th);
            throw AxisUtility.getFault(th);
        } finally {
            logger.debug("end: modifyDatastreamByReference, " + PID + ", "
                         + datastreamID);
        }
    }

    public String modifyDatastreamByValue(String PID,
                                          String datastreamID,
                                          String[] altIDs,
                                          String dsLabel,
                                          String mimeType,
                                          String formatURI,
                                          byte[] dsContent,
                                          String checksumType,
                                          String checksum,
                                          String logMessage)
            throws java.rmi.RemoteException {
        logger.debug("start: modifyDatastreamByValue, " + PID + ", "
                     + datastreamID);
        assertInitialized();
        try {
            ByteArrayInputStream byteStream = null;
            if (dsContent != null && dsContent.length > 0) {
                byteStream = new ByteArrayInputStream(dsContent);
            }
            return DateUtility.convertDateToString(s_management
                    .modifyDatastreamByValue(ReadOnlyContext.getSoapContext(),
                                             PID,
                                             datastreamID,
                                             altIDs,
                                             dsLabel,
                                             mimeType,
                                             formatURI,
                                             byteStream,
                                             checksumType,
                                             checksum,
                                             logMessage,
                                             null));
        } catch (Throwable th) {
            logger.error("Error modifying datastream by value", th);
            throw AxisUtility.getFault(th);
        } finally {
            logger.debug("end: modifyDatastreamByValue, " + PID + ", "
                         + datastreamID);
        }
    }

    public String setDatastreamState(String PID,
                                     String datastreamID,
                                     String dsState,
                                     String logMessage)
            throws java.rmi.RemoteException {
        assertInitialized();
        try {
            return DateUtility.convertDateToString(s_management
                    .setDatastreamState(ReadOnlyContext.getSoapContext(),
                                        PID,
                                        datastreamID,
                                        dsState,
                                        logMessage));
        } catch (Throwable th) {
            logger.error("Error setting datastream state", th);
            throw AxisUtility.getFault(th);
        }
    }

    public String setDatastreamVersionable(String PID,
                                           String datastreamID,
                                           boolean versionable,
                                           String logMessage)
            throws java.rmi.RemoteException {
        assertInitialized();
        try {
            return DateUtility.convertDateToString(s_management
                    .setDatastreamVersionable(ReadOnlyContext.getSoapContext(),
                                              PID,
                                              datastreamID,
                                              versionable,
                                              logMessage));
        } catch (Throwable th) {
            logger.error("Error setting datastream versionable", th);
            throw AxisUtility.getFault(th);
        }
    }

    public String compareDatastreamChecksum(String PID,
                                            String datastreamID,
                                            String versionDate)
            throws java.rmi.RemoteException {
        assertInitialized();
        try {
            return s_management.compareDatastreamChecksum(ReadOnlyContext
                    .getSoapContext(), PID, datastreamID, DateUtility
                    .convertStringToDate(versionDate));
        } catch (Throwable th) {
            logger.error("Error comparing datastream checksum", th);
            throw AxisUtility.getFault(th);
        }
    }

    public String[] purgeDatastream(String PID,
                                    String datastreamID,
                                    String startDT,
                                    String endDT,
                                    String logMessage)
            throws java.rmi.RemoteException {
        logger.debug("start: purgeDatastream, " + PID + ", " + datastreamID);
        assertInitialized();
        try {
            return toStringArray(s_management.purgeDatastream(ReadOnlyContext
                    .getSoapContext(), PID, datastreamID, DateUtility
                    .parseCheckedDate(startDT), DateUtility
                    .parseCheckedDate(endDT), logMessage));
        } catch (Throwable th) {
            logger.error("Error purging datastream", th);
            throw AxisUtility.getFault(th);
        } finally {
            logger.debug("end: purgeDatastream, " + PID + ", " + datastreamID);
        }
    }

    private String[] toStringArray(Date[] dates) throws Exception {
        String[] out = new String[dates.length];
        for (int i = 0; i < dates.length; i++) {
            out[i] = DateUtility.convertDateToString(dates[i]);
        }
        return out;
    }

    public org.fcrepo.server.types.gen.Datastream getDatastream(String PID,
                                                                String datastreamID,
                                                                String asOfDateTime)
            throws java.rmi.RemoteException {
        assertInitialized();
        try {
            org.fcrepo.server.storage.types.Datastream ds =
                    s_management
                            .getDatastream(ReadOnlyContext.getSoapContext(),
                                           PID,
                                           datastreamID,
                                           DateUtility
                                                   .parseCheckedDate(asOfDateTime));
            return TypeUtility.convertDatastreamToGenDatastream(ds);
        } catch (Throwable th) {
            logger.error("Error getting datastream", th);
            throw AxisUtility.getFault(th);
        }
    }

    public org.fcrepo.server.types.gen.Datastream[] getDatastreams(String PID,
                                                                   String asOfDateTime,
                                                                   String state)
            throws java.rmi.RemoteException {
        assertInitialized();
        try {
            org.fcrepo.server.storage.types.Datastream[] intDatastreams =
                    s_management.getDatastreams(ReadOnlyContext
                            .getSoapContext(), PID, DateUtility
                            .parseCheckedDate(asOfDateTime), state);
            return getGenDatastreams(intDatastreams);
        } catch (Throwable th) {
            logger.error("Error getting datastreams", th);
            throw AxisUtility.getFault(th);
        }
    }

    private org.fcrepo.server.types.gen.Datastream[] getGenDatastreams(
            org.fcrepo.server.storage.types.Datastream[] intDatastreams) {
        org.fcrepo.server.types.gen.Datastream[] genDatastreams =
                new org.fcrepo.server.types.gen.Datastream[intDatastreams.length];
        for (int i = 0; i < intDatastreams.length; i++) {
            genDatastreams[i] =
                    TypeUtility
                            .convertDatastreamToGenDatastream(intDatastreams[i]);
        }
        return genDatastreams;
    }

    private org.fcrepo.server.types.gen.RelationshipTuple[] getGenRelsTuples(
            org.fcrepo.server.storage.types.RelationshipTuple[] intRelsTuples) {
        org.fcrepo.server.types.gen.RelationshipTuple[] genRelsTuples =
                new org.fcrepo.server.types.gen.RelationshipTuple[intRelsTuples.length];
        for (int i = 0; i < intRelsTuples.length; i++) {
            genRelsTuples[i] =
                    TypeUtility
                            .convertRelsTupleToGenRelsTuple(intRelsTuples[i]);
        }
        return genRelsTuples;
    }

    public org.fcrepo.server.types.gen.Datastream[] getDatastreamHistory(String PID,
                                                                         String datastreamID)
            throws java.rmi.RemoteException {
        assertInitialized();
        try {
            org.fcrepo.server.storage.types.Datastream[] intDatastreams =
                    s_management.getDatastreamHistory(ReadOnlyContext
                            .getSoapContext(), PID, datastreamID);
            return getGenDatastreams(intDatastreams);
        } catch (Throwable th) {
            logger.error("Error getting datastream history", th);
            throw AxisUtility.getFault(th);
        }
    }

    public java.lang.String[] getNextPID(NonNegativeInteger numPIDs,
                                         String namespace)
            throws java.rmi.RemoteException {
        logger.debug("start: getNextPID");
        assertInitialized();
        try {
            if (numPIDs == null) {
                numPIDs = new NonNegativeInteger("1");
            }
            return s_management.getNextPID(ReadOnlyContext.getSoapContext(),
                                           numPIDs.intValue(),
                                           namespace);
        } catch (Throwable th) {
            logger.error("Error getting next PID", th);
            throw AxisUtility.getFault(th);
        } finally {
            logger.debug("end: getNextPID");
        }
    }

    private void assertInitialized() throws java.rmi.RemoteException {
        if (!s_initialized) {
            AxisUtility.throwFault(s_initException);
        }
    }

    public org.fcrepo.server.types.gen.RelationshipTuple[] getRelationships(String subject,
                                                                            String relationship)
            throws java.rmi.RemoteException {
        logger.debug("start: getRelationships");
        assertInitialized();
        try {
            org.fcrepo.server.storage.types.RelationshipTuple[] intRelationshipTuples = null;
            intRelationshipTuples =
                    s_management.getRelationships(ReadOnlyContext
                            .getSoapContext(), subject, relationship);
            return getGenRelsTuples(intRelationshipTuples);
        } catch (Throwable th) {
            logger.error("Error getting relationships", th);
            throw AxisUtility.getFault(th);
        } finally {
            logger.debug("end: getRelationships");
        }
    }

    public boolean addRelationship(String subject,
                                   String relationship,
                                   String object,
                                   boolean isLiteral,
                                   String datatype)
            throws java.rmi.RemoteException {
        logger.debug("start: addRelationship");
        assertInitialized();
        try {
            return s_management.addRelationship(ReadOnlyContext
                    .getSoapContext(),
                                                subject,
                                                relationship,
                                                object,
                                                isLiteral,
                                                datatype);
        } catch (Throwable th) {
            logger.error("Error adding relationships", th);
            throw AxisUtility.getFault(th);
        } finally {
            logger.debug("end: addRelationship");
        }
    }

    public boolean purgeRelationship(String subject,
                                     String relationship,
                                     String object,
                                     boolean isLiteral,
                                     String datatype)
            throws java.rmi.RemoteException {
        logger.debug("start: purgeRelationship");
        assertInitialized();
        try {
            return s_management.purgeRelationship(ReadOnlyContext
                    .getSoapContext(),
                                                  subject,
                                                  relationship,
                                                  object,
                                                  isLiteral,
                                                  datatype);
        } catch (Throwable th) {
            logger.error("Error purging relationships", th);
            throw AxisUtility.getFault(th);
        } finally {
            logger.debug("end: purgeRelationship");
        }
    }

}
