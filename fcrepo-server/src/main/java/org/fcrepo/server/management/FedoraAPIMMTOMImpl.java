/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.management;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.binding.soap.SoapFault;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.StorageDeviceException;
import org.fcrepo.server.utilities.CXFUtility;
import org.fcrepo.server.utilities.TypeUtility;
import org.fcrepo.utilities.DateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jiri Kremser
 */

// @MTOM
// @StreamingAttachment(parseEagerly = true, memoryThreshold = 40000L)
public class FedoraAPIMMTOMImpl
        implements FedoraAPIMMTOM {

    private static final Logger LOG = LoggerFactory
            .getLogger(FedoraAPIMMTOMImpl.class);

    @Resource
    private WebServiceContext context;

    /**
     * The Fedora Server instance
     */
    private final Server m_server;

    private final Management m_management;

    public FedoraAPIMMTOMImpl(Server server) {
        m_server = server;
        m_management =
                (Management) m_server
                        .getModule("org.fcrepo.server.management.Management");
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#ingest(javax.activation.
     * DataHandler objectXML ,)String format ,)String logMessage )*
     */
    @Override
    public String ingest(DataHandler objectXML, String format, String logMessage) {
        LOG.debug("start: ingest");
        assertInitialized();
        try {
            // always gens pid, unless pid in stream starts with "test:" "demo:"
            // or other prefix that is configured in the retainPIDs parameter of
            // fedora.fcfg
            MessageContext ctx = context.getMessageContext();
            InputStream byteStream = null;
            if (objectXML != null) {
                byteStream = objectXML.getInputStream();
            }
            return m_management.ingest(ReadOnlyContext.getSoapContext(ctx),
                                       byteStream,
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
        LOG.debug("start: modifyObject, {}", pid);
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            return DateUtility.convertDateToString(m_management
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
            LOG.debug("end: modifyObject, {}", pid);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#getObjectXML(String pid
     * )*
     */
    @Override
    public DataHandler getObjectXML(String pid) {
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            InputStream in =
                    m_management.getObjectXML(ReadOnlyContext
                            .getSoapContext(ctx), pid, "UTF-8");
            ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
            pipeStream(in, out);
            return new DataHandler(new ByteArrayDataSource(out.toByteArray(),
                                                           "text/xml"));
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
    public DataHandler export(String pid, String format, String context) {
        assertInitialized();
        try {
            MessageContext ctx =
                    FedoraAPIMMTOMImpl.this.context.getMessageContext();
            InputStream in =
                    m_management.export(ReadOnlyContext.getSoapContext(ctx),
                                        pid,
                                        format,
                                        context,
                                        "UTF-8");
            ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
            pipeStream(in, out);
            return new DataHandler(new ByteArrayDataSource(out.toByteArray(),
                                                           "text/xml"));
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
        LOG.debug("start: purgeObject, {}", pid);
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            return DateUtility.convertDateToString(m_management
                    .purgeObject(ReadOnlyContext.getSoapContext(ctx),
                                 pid,
                                 logMessage));
        } catch (Throwable th) {
            LOG.error("Error purging object", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: purgeObject, {}", pid);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#addDatastream( String
     * pid ,)String dsID ,)org.fcrepo.server.types.mtom.gen.ArrayOfString altIDs
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
        LOG.debug("start: addDatastream, {}, {}", pid, dsID);
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            String[] altIDsArray = null;
            if (altIDs != null && altIDs.getItem() != null) {
                altIDsArray = altIDs.getItem().toArray(new String[0]);
            }
            return m_management.addDatastream(ReadOnlyContext
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
            LOG.debug("end: addDatastream, {}, {}", pid, dsID);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.management.FedoraAPIMMTOM#modifyDatastreamByReference
     * (String pid ,)String dsID
     * ,)org.fcrepo.server.types.mtom.gen.ArrayOfString altIDs ,)String dsLabel
     * ,)String mimeType ,)String formatURI ,)String dsLocation ,)String
     * checksumType ,)String checksum ,)String logMessage ,)boolean force )*
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
        LOG.debug("start: modifyDatastreamByReference, {}, {}", pid, dsID);
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            String[] altIDsArray = null;
            if (altIDs != null && altIDs.getItem() != null) {
                altIDsArray = altIDs.getItem().toArray(new String[0]);
            }
            return DateUtility.convertDateToString(m_management
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
            LOG.debug("end: modifyDatastreamByReference, {}, {}", pid, dsID);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.management.FedoraAPIMMTOM#modifyDatastreamByValue(
     * String pid ,)String dsID ,)org.fcrepo.server.types.mtom.gen.ArrayOfString
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
                                          DataHandler dsContent,
                                          String checksumType,
                                          String checksum,
                                          String logMessage,
                                          boolean force) {
        LOG.debug("start: modifyDatastreamByValue, {}, {}", pid, dsID);
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            InputStream byteStream = null;
            if (dsContent != null) {
                byteStream = dsContent.getInputStream();
            }
            String[] altIDsArray = null;
            if (altIDs != null && altIDs.getItem() != null) {
                altIDsArray = altIDs.getItem().toArray(new String[0]);
            }
            return DateUtility.convertDateToString(m_management
                    .modifyDatastreamByValue(ReadOnlyContext
                                                     .getSoapContext(ctx),
                                             pid,
                                             dsID,
                                             altIDsArray,
                                             dsLabel,
                                             mimeType,
                                             formatURI,
                                             byteStream,
                                             checksumType,
                                             checksum,
                                             logMessage,
                                             null));
        } catch (Throwable th) {
            LOG.error("Error modifying datastream by value", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: modifyDatastreamByValue, {}, {}", pid, dsID);
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
            return DateUtility.convertDateToString(m_management
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
            return DateUtility.convertDateToString(m_management
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
            return m_management.compareDatastreamChecksum(ReadOnlyContext
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
                    m_management.getDatastream(ReadOnlyContext
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
                    m_management.getDatastreams(ReadOnlyContext
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
                    m_management.getDatastreamHistory(ReadOnlyContext
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
        LOG.debug("start: purgeDatastream, {}, {}", pid, dsID);
        assertInitialized();
        try {
            MessageContext ctx = context.getMessageContext();
            return toStringList(m_management.purgeDatastream(ReadOnlyContext
                    .getSoapContext(ctx), pid, dsID, DateUtility
                    .parseDateOrNull(startDT), DateUtility
                    .parseDateOrNull(endDT), logMessage));
        } catch (Throwable th) {
            LOG.error("Error purging datastream", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: purgeDatastream, {}, {}", pid, dsID);
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
                    m_management
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
                    m_management.getRelationships(ReadOnlyContext
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
            return m_management.addRelationship(ReadOnlyContext
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
            return m_management.purgeRelationship(ReadOnlyContext
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
            return TypeUtility.convertValidationToGenValidation(m_management
                    .validate(ReadOnlyContext.getSoapContext(ctx),
                              pid,
                              DateUtility.parseDateOrNull(asOfDateTime)));
        } catch (Throwable th) {
            LOG.error("Error validating", th);
            throw CXFUtility.getFault(th);
        } finally {
            LOG.debug("end: validate");
        }
    }

    private void assertInitialized() throws SoapFault {
        if (m_server == null) {
            CXFUtility.throwFault(new ModuleInitializationException("Null was injected for Server to WS implementor",
                    "org.fcrepo.server.management.FedoraAPIMMTOM"));
        }
        if (m_management == null) {
            CXFUtility.throwFault(new ModuleInitializationException("No Management module found for WS implementor",
                    "org.fcrepo.server.management.FedoraAPIMMTOM"));
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
            genRelsTuples
                    .add(TypeUtility.convertRelsTupleToGenRelsTuple(tuple));
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
