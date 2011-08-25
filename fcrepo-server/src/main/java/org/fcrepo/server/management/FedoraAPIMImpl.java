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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import javax.annotation.Resource;

import org.apache.cxf.binding.soap.SoapFault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.InitializationException;
import org.fcrepo.server.errors.ServerInitializationException;
import org.fcrepo.server.errors.StorageDeviceException;
import org.fcrepo.server.utilities.CXFUtility;
import org.fcrepo.server.utilities.TypeUtility;

import org.fcrepo.utilities.DateUtility;

// @javax.jws.WebService(
// serviceName = "Fedora-API-M-Service",
// portName = "Fedora-API-M-Port-SOAPHTTPS",
// targetNamespace = "http://www.fedora.info/definitions/1/0/api/",
// /*wsdlLocation =
// "file:/home/freon/workspace/fcrepo/fcrepo-common/../resources/wsdl/Fedora-API-M.wsdl",*/
// endpointInterface = "org.fcrepo.server.management.FedoraAPIM")
//
/**
 * @author Jiri Kremser
 */

public class FedoraAPIMImpl
        implements FedoraAPIM {

    private static final Logger LOG = LoggerFactory
            .getLogger(FedoraAPIMImpl.class);

    @Resource
    private WebServiceContext context;

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
                s_server = Server.getInstance(new File(fedoraHome), true);
                s_initialized = true;
                s_management =
                        (Management) s_server
                                .getModule("org.fcrepo.server.management.Management");
            }
        } catch (InitializationException ie) {
            LOG.error("Error getting server", ie);
            s_initialized = false;
            s_initException = ie;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#ingest(javax.activation.
     * DataHandler objectXML ,)String format ,)String logMessage )*
     */
    @Override
    public String ingest(byte[] objectXML, String format, String logMessage) {
        LOG.debug("start: ingest");
        assertInitialized();
        try {
            // always gens pid, unless pid in stream starts with "test:" "demo:"
            // or other prefix that is configured in the retainPIDs parameter of
            // fedora.fcfg
            MessageContext ctx = context.getMessageContext();
            return s_management.ingest(ReadOnlyContext.getSoapContext(ctx),
                                       new ByteArrayInputStream(objectXML),
                                       logMessage,
                                       format,
                                       "UTF-8",
                                       "new");
        } catch (Throwable th) {
            LOG.error("Error ingesting", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: ingest");
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#modifyObject(String pid
     * ,)String state ,)String label ,)String ownerId ,)String logMessage )*
     */
    @Override
    public String modifyObject(String pid,
                               String state,
                               String label,
                               String ownerId,
                               String logMessage) {
        LOG.debug("start: modifyObject, " + pid);
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            return DateUtility.convertDateToString(s_management
                    .modifyObject(ReadOnlyContext.getSoapContext(ctx),
                                  pid,
                                  state,
                                  label,
                                  ownerId,
                                  logMessage,
                                  null));
        } catch (Throwable th) {
            LOG.error("Error modifying object", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: modifyObject, " + pid);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#getObjectXML(String pid
     * )*
     */
    @Override
    public byte[] getObjectXML(String pid) {
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            InputStream in =
                    s_management.getObjectXML(ReadOnlyContext
                            .getSoapContext(ctx), pid, "UTF-8");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pipeStream(in, out);
            return out.toByteArray();
        } catch (Throwable th) {
            LOG.error("Error getting object XML", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#export(String pid
     * ,)String format ,)String context )*
     */
    @Override
    public byte[] export(String pid, String format, String context) {
        assertInitialized();
        try {
            MessageContext ctx =
                    FedoraAPIMImpl.this.context.getMessageContext();
            InputStream in =
                    s_management.export(ReadOnlyContext.getSoapContext(ctx),
                                        pid,
                                        format,
                                        context,
                                        "UTF-8");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pipeStream(in, out);
            return out.toByteArray();
        } catch (Throwable th) {
            LOG.error("Error exporting object", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#purgeObject(String pid
     * ,)String logMessage ,)boolean force )*
     */
    @Override
    public String purgeObject(String pid, String logMessage, boolean force) {
        LOG.debug("start: purgeObject, " + pid);
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            return DateUtility.convertDateToString(s_management
                    .purgeObject(ReadOnlyContext.getSoapContext(ctx),
                                 pid,
                                 logMessage));
        } catch (Throwable th) {
            LOG.error("Error purging object", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: purgeObject, " + pid);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#addDatastream( String
     * pid ,)String dsID ,)org.fcrepo.server.types.gen.ArrayOfString altIDs
     * ,)String dsLabel ,)boolean versionable ,)String mimeType ,)String
     * formatURI ,)String dsLocation ,)String controlGroup ,)String dsState
     * ,)String checksumType ,)String checksum ,)String logMessage )*
     */
    @Override
    public String addDatastream(String pid,
                                String dsID,
                                org.fcrepo.server.types.gen.ArrayOfString altIDs,
                                String dsLabel,
                                boolean versionable,
                                String mimeType,
                                String formatURI,
                                String dsLocation,
                                String controlGroup,
                                String dsState,
                                String checksumType,
                                String checksum,
                                String logMessage) {
        LOG.debug("start: addDatastream, " + pid + ", " + dsID);
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            String[] altIDsArray = null;
            if (altIDs != null && altIDs.getItem() != null) {
                altIDsArray = altIDs.getItem().toArray(new String[0]);
            }
            return s_management.addDatastream(ReadOnlyContext
                                                      .getSoapContext(ctx),
                                              pid,
                                              dsID,
                                              altIDsArray,
                                              dsLabel,
                                              versionable,
                                              mimeType,
                                              formatURI,
                                              dsLocation,
                                              controlGroup,
                                              dsState,
                                              checksumType,
                                              checksum,
                                              logMessage);
        } catch (Throwable th) {
            LOG.error("Error adding datastream", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: addDatastream, " + pid + ", " + dsID);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.management.FedoraAPIMMTOM#modifyDatastreamByReference
     * (String pid ,)String dsID ,)org.fcrepo.server.types.gen.ArrayOfString
     * altIDs ,)String dsLabel ,)String mimeType ,)String formatURI ,)String
     * dsLocation ,)String checksumType ,)String checksum ,)String logMessage
     * ,)boolean force )*
     */
    @Override
    public String modifyDatastreamByReference(String pid,
                                              String dsID,
                                              org.fcrepo.server.types.gen.ArrayOfString altIDs,
                                              String dsLabel,
                                              String mimeType,
                                              String formatURI,
                                              String dsLocation,
                                              String checksumType,
                                              String checksum,
                                              String logMessage,
                                              boolean force) {
        LOG.debug("start: modifyDatastreamByReference, " + pid + ", " + dsID);
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            String[] altIDsArray = null;
            if (altIDs != null && altIDs.getItem() != null) {
                altIDsArray = altIDs.getItem().toArray(new String[0]);
            }
            return DateUtility.convertDateToString(s_management
                    .modifyDatastreamByReference(ReadOnlyContext
                                                         .getSoapContext(ctx),
                                                 pid,
                                                 dsID,
                                                 altIDsArray,
                                                 dsLabel,
                                                 mimeType,
                                                 formatURI,
                                                 dsLocation,
                                                 checksumType,
                                                 checksum,
                                                 logMessage,
                                                 null));
        } catch (Throwable th) {
            LOG.error("Error modifying datastream by reference", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: modifyDatastreamByReference, " + pid + ", " + dsID);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#modifyDatastreamByValue(
     * String pid ,)String dsID ,)org.fcrepo.server.types.gen.ArrayOfString
     * altIDs ,)String dsLabel ,)String mimeType ,)String formatURI
     * ,)javax.activation.DataHandler dsContent ,)String checksumType ,)String
     * checksum ,)String logMessage ,)boolean force )*
     */
    @Override
    public String modifyDatastreamByValue(String pid,
                                          String dsID,
                                          org.fcrepo.server.types.gen.ArrayOfString altIDs,
                                          String dsLabel,
                                          String mimeType,
                                          String formatURI,
                                          byte[] dsContent,
                                          String checksumType,
                                          String checksum,
                                          String logMessage,
                                          boolean force) {
        LOG.debug("start: modifyDatastreamByValue, " + pid + ", " + dsID);
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            String[] altIDsArray = null;
            if (altIDs != null && altIDs.getItem() != null) {
                altIDsArray = altIDs.getItem().toArray(new String[0]);
            }
            return DateUtility
                    .convertDateToString(s_management.modifyDatastreamByValue(ReadOnlyContext
                                                                                      .getSoapContext(ctx),
                                                                              pid,
                                                                              dsID,
                                                                              altIDsArray,
                                                                              dsLabel,
                                                                              mimeType,
                                                                              formatURI,
                                                                              new ByteArrayInputStream(dsContent),
                                                                              checksumType,
                                                                              checksum,
                                                                              logMessage,
                                                                              null));
        } catch (Throwable th) {
            LOG.error("Error modifying datastream by value", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: modifyDatastreamByValue, " + pid + ", " + dsID);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#setDatastreamState(java.
     * lang.String pid ,)String dsID ,)String dsState ,)String logMessage )*
     */
    @Override
    public String setDatastreamState(String pid,
                                     String dsID,
                                     String dsState,
                                     String logMessage) {
        LOG.info("Executing operation setDatastreamState");
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            return DateUtility.convertDateToString(s_management
                    .setDatastreamState(ReadOnlyContext.getSoapContext(ctx),
                                        pid,
                                        dsID,
                                        dsState,
                                        logMessage));
        } catch (Throwable th) {
            LOG.error("Error setting datastream state", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#setDatastreamVersionable
     * (String pid ,)String dsID ,)boolean versionable ,)String logMessage )*
     */
    @Override
    public String setDatastreamVersionable(String pid,
                                           String dsID,
                                           boolean versionable,
                                           String logMessage) {
        LOG.info("Executing operation setDatastreamVersionable");
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            return DateUtility.convertDateToString(s_management
                    .setDatastreamVersionable(ReadOnlyContext
                                                      .getSoapContext(ctx),
                                              pid,
                                              dsID,
                                              versionable,
                                              logMessage));
        } catch (Throwable th) {
            LOG.error("Error setting datastream state", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.management.FedoraAPIMMTOM#compareDatastreamChecksum
     * (String pid ,)String dsID ,)String versionDate )*
     */
    @Override
    public String compareDatastreamChecksum(String pid,
                                            String dsID,
                                            String versionDate) {
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            return s_management.compareDatastreamChecksum(ReadOnlyContext
                    .getSoapContext(ctx), pid, dsID, DateUtility
                    .parseDateOrNull(versionDate));
        } catch (Throwable th) {
            LOG.error("Error comparing datastream checksum", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#getDatastream( String
     * pid ,)String dsID ,)String asOfDateTime )*
     */
    @Override
    public org.fcrepo.server.types.gen.Datastream getDatastream(String pid,
                                                                String dsID,
                                                                String asOfDateTime) {
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            org.fcrepo.server.storage.types.Datastream ds =
                    s_management.getDatastream(ReadOnlyContext
                            .getSoapContext(ctx), pid, dsID, DateUtility
                            .parseDateOrNull(asOfDateTime));
            return TypeUtility.convertDatastreamToGenDatastream(ds);
        } catch (Throwable th) {
            LOG.error("Error getting datastream", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#getDatastreams(java.lang
     * .String pid ,)String asOfDateTime ,)String dsState )*
     */
    @Override
    public List<org.fcrepo.server.types.gen.Datastream> getDatastreams(String pid,
                                                                       String asOfDateTime,
                                                                       String dsState) {
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            org.fcrepo.server.storage.types.Datastream[] intDatastreams =
                    s_management.getDatastreams(ReadOnlyContext
                            .getSoapContext(ctx), pid, DateUtility
                            .parseDateOrNull(asOfDateTime), dsState);
            return getGenDatastreams(intDatastreams);
        } catch (Throwable th) {
            LOG.error("Error getting datastreams", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.management.FedoraAPIMMTOM#getDatastreamHistory(java
     * .lang.String pid ,)String dsID )*
     */
    @Override
    public List<org.fcrepo.server.types.gen.Datastream> getDatastreamHistory(String pid,
                                                                             String dsID) {
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            org.fcrepo.server.storage.types.Datastream[] intDatastreams =
                    s_management.getDatastreamHistory(ReadOnlyContext
                            .getSoapContext(ctx), pid, dsID);
            return getGenDatastreams(intDatastreams);
        } catch (Throwable th) {
            LOG.error("Error getting datastream history", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.management.FedoraAPIMMTOM#purgeDatastream(java.lang
     * .String pid ,)String dsID ,)String startDT ,)String endDT ,)String
     * logMessage ,)boolean force )*
     */
    @Override
    public List<String> purgeDatastream(String pid,
                                        String dsID,
                                        String startDT,
                                        String endDT,
                                        String logMessage,
                                        boolean force) {
        LOG.debug("start: purgeDatastream, " + pid + ", " + dsID);
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            return toStringList(s_management.purgeDatastream(ReadOnlyContext
                    .getSoapContext(ctx), pid, dsID, DateUtility
                    .parseDateOrNull(startDT), DateUtility
                    .parseDateOrNull(endDT), logMessage));
        } catch (Throwable th) {
            LOG.error("Error purging datastream", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: purgeDatastream, " + pid + ", " + dsID);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.management.FedoraAPIMMTOM#getNextPID(java.math.BigInteger
     * numPIDs ,)String pidNamespace )*
     */
    @Override
    public List<String> getNextPID(java.math.BigInteger numPIDs,
                                   String pidNamespace) {
        LOG.debug("start: getNextPID");
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            if (numPIDs == null) {
                numPIDs = new java.math.BigInteger("1");
            }
            String[] aux =
                    s_management
                            .getNextPID(ReadOnlyContext.getSoapContext(ctx),
                                        numPIDs.intValue(),
                                        pidNamespace);
            List<String> auxList = null;
            if (aux != null) {
                auxList = Arrays.asList(aux);
            }
            return auxList;
        } catch (Throwable th) {
            LOG.error("Error getting next PID", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: getNextPID");
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.management.FedoraAPIMMTOM#getRelationships(java.lang
     * .String pid ,)String relationship )*
     */
    @Override
    public List<org.fcrepo.server.types.gen.RelationshipTuple> getRelationships(String pid,
                                                                                String relationship) {
        LOG.debug("start: getRelationships");
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            org.fcrepo.server.storage.types.RelationshipTuple[] intRelationshipTuples =
                    null;
            intRelationshipTuples =
                    s_management.getRelationships(ReadOnlyContext
                            .getSoapContext(ctx), pid, relationship);
            return getGenRelsTuples(intRelationshipTuples);
        } catch (Throwable th) {
            LOG.error("Error getting relationships", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: getRelationships");
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.management.FedoraAPIMMTOM#addRelationship(java.lang
     * .String pid ,)String relationship ,)String object ,)boolean isLiteral
     * ,)String datatype )*
     */
    @Override
    public boolean addRelationship(String pid,
                                   String relationship,
                                   String object,
                                   boolean isLiteral,
                                   String datatype) {
        LOG.debug("start: addRelationship");
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            return s_management.addRelationship(ReadOnlyContext
                                                        .getSoapContext(ctx),
                                                pid,
                                                relationship,
                                                object,
                                                isLiteral,
                                                datatype);
        } catch (Throwable th) {
            LOG.error("Error adding relationships", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: addRelationship");
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#purgeRelationship(String
     * pid ,)String relationship ,)String object ,)boolean isLiteral ,)String
     * datatype )*
     */
    @Override
    public boolean purgeRelationship(String pid,
                                     String relationship,
                                     String object,
                                     boolean isLiteral,
                                     String datatype) {
        LOG.debug("start: purgeRelationship");
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            return s_management.purgeRelationship(ReadOnlyContext
                                                          .getSoapContext(ctx),
                                                  pid,
                                                  relationship,
                                                  object,
                                                  isLiteral,
                                                  datatype);
        } catch (Throwable th) {
            LOG.error("Error purging relationships", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: purgeRelationship");
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#validate(String pid
     * ,)String asOfDateTime )*
     */
    @Override
    public org.fcrepo.server.types.gen.Validation validate(String pid,
                                                           String asOfDateTime) {
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            return TypeUtility
                    .convertValidationToGenValidationMTOM(s_management.validate(ReadOnlyContext
                                                                                        .getSoapContext(ctx),
                                                                                pid,
                                                                                DateUtility
                                                                                        .parseDateOrNull(asOfDateTime)));
        } catch (Throwable th) {
            LOG.error("Error purging relationships", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: purgeRelationship");
        }
    }

    private void assertInitialized() throws SoapFault {
        if (!s_initialized) {
            CXFUtility.throwFault(s_initException);
        }
    }

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

    private List<org.fcrepo.server.types.gen.RelationshipTuple> getGenRelsTuples(org.fcrepo.server.storage.types.RelationshipTuple[] intRelsTuples) {
        List<org.fcrepo.server.types.gen.RelationshipTuple> genRelsTuples =
                new ArrayList<org.fcrepo.server.types.gen.RelationshipTuple>(intRelsTuples.length);
        for (org.fcrepo.server.storage.types.RelationshipTuple tuple : intRelsTuples) {
            genRelsTuples.add(TypeUtility
                    .convertRelsTupleToGenRelsTupleMTOM(tuple));
        }
        return genRelsTuples;
    }

    private List<String> toStringList(Date[] dates) throws Exception {
        List<String> out = new ArrayList<String>(dates.length);
        for (Date date : dates) {
            out.add(DateUtility.convertDateToString(date));
        }
        return out;
    }

    private List<org.fcrepo.server.types.gen.Datastream> getGenDatastreams(org.fcrepo.server.storage.types.Datastream[] intDatastreams) {
        List<org.fcrepo.server.types.gen.Datastream> genDatastreams =
                new ArrayList<org.fcrepo.server.types.gen.Datastream>(intDatastreams.length);
        for (org.fcrepo.server.storage.types.Datastream datastream : intDatastreams) {
            genDatastreams.add(TypeUtility
                    .convertDatastreamToGenDatastream(datastream));
        }
        return genDatastreams;
    }

}
