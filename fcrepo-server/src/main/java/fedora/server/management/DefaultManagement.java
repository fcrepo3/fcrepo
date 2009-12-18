/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.management;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import org.apache.commons.betwixt.XMLUtils;

import org.apache.log4j.Logger;

import org.jrdf.graph.URIReference;

import org.w3c.dom.Document;

import fedora.common.Constants;
import fedora.common.PID;
import fedora.common.rdf.SimpleURIReference;

import fedora.server.Context;
import fedora.server.RecoveryContext;
import fedora.server.Server;
import fedora.server.errors.GeneralException;
import fedora.server.errors.InvalidStateException;
import fedora.server.errors.InvalidXMLNameException;
import fedora.server.errors.ServerException;
import fedora.server.errors.StreamReadException;
import fedora.server.errors.StreamWriteException;
import fedora.server.errors.ValidationException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.security.Authorization;
import fedora.server.storage.ContentManagerParams;
import fedora.server.storage.DOManager;
import fedora.server.storage.DOReader;
import fedora.server.storage.DOWriter;
import fedora.server.storage.ExternalContentManager;
import fedora.server.storage.types.AuditRecord;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamManagedContent;
import fedora.server.storage.types.DatastreamReferencedContent;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.RelationshipTuple;
import fedora.server.utilities.StreamUtility;
import fedora.server.validation.ValidationConstants;
import fedora.server.validation.ValidationUtility;

/**
 * Implements API-M without regard to the transport/messaging protocol.
 *
 * @author Chris Wilper
 * @version $Id$
 */
public class DefaultManagement
        implements Constants, Management, ManagementDelegate {

    /** Logger for this class. */
    private static Logger LOG =
            Logger.getLogger(DefaultManagement.class.getName());

    private final Authorization m_authz;

    private final DOManager m_manager;

    private final ExternalContentManager m_contentManager;

    private final int m_uploadStorageMinutes;

    private int m_lastId;

    private final File m_tempDir;

    private final Hashtable<String, Long> m_uploadStartTime;

    private long m_lastPurgeInMillis = System.currentTimeMillis();

    private final long m_purgeDelayInMillis;

    /**
     * @param purgeDelayInMillis milliseconds to delay before removing
     *                           old uploaded files
     * @author Frederic Buffet & Tommy Bourdin (Atos Worldline)
     * @date   August 1, 2008
     */
    public DefaultManagement(Authorization authz,
                             DOManager doMgr,
                             ExternalContentManager ecMgr,
                             int uploadMinutes,
                             int lastId,
                             File tempDir,
                             Hashtable<String, Long> uploadStartTime,
                             long purgeDelayInMillis) {
        m_authz = authz;
        m_manager = doMgr;
        m_contentManager = ecMgr;
        m_uploadStorageMinutes = uploadMinutes;
        m_lastId = lastId;
        m_tempDir = tempDir;
        m_uploadStartTime = uploadStartTime;
        m_purgeDelayInMillis = purgeDelayInMillis;
    }

    public String ingest(Context context,
                         InputStream serialization,
                         String logMessage,
                         String format,
                         String encoding,
                         boolean newPid) throws ServerException {
        DOWriter w = null;
        try {
            LOG.debug("Entered ingest");
            w = m_manager.getIngestWriter(Server.USE_DEFINITIVE_STORE,
                                          context,
                                          serialization,
                                          format,
                                          encoding,
                                          newPid);
            String pid = w.GetObjectPID();

            m_authz.enforceIngest(context, pid, format, encoding);

            // Only create an audit record if there is a log message to capture
            if (logMessage != null && !logMessage.equals("")) {
                Date nowUTC = Server.getCurrentDate(context);
                addAuditRecord(context, w, "ingest", "", logMessage, nowUTC);
            }

            w.commit(logMessage);
            return pid;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg = new StringBuilder("Completed ingest(");
                logMsg.append("objectXML");
                logMsg.append(", format: ").append(format);
                logMsg.append(", encoding: ").append(encoding);
                logMsg.append(", newPid: ").append(newPid);
                logMsg.append(", logMessage: ").append(logMessage);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            finishModification(w, "ingest");
        }
    }

    private void finishModification(DOWriter w, String method)
            throws ServerException {
        if (w != null) {
            m_manager.releaseWriter(w);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Exiting " + method);
            Runtime r = Runtime.getRuntime();
            LOG.debug("Memory: " + r.freeMemory() + " bytes free of "
                    + r.totalMemory() + " available.");
        }
    }

    public Date modifyObject(Context context,
                             String pid,
                             String state,
                             String label,
                             String ownerId,
                             String logMessage) throws ServerException {
        DOWriter w = null;
        try {
            LOG.debug("Entered modifyObject");

            m_authz.enforceModifyObject(context,
                                                    pid,
                                                    state,
                                                    ownerId);

            checkObjectLabel(label);

            w = m_manager.getWriter(Server.USE_DEFINITIVE_STORE, context, pid);
            if (state != null && !state.equals("")) {
                if (!state.equals("A") && !state.equals("D")
                        && !state.equals("I")) {
                    throw new InvalidStateException("The object state of \""
                            + state
                            + "\" is invalid. The allowed values for state are: "
                            + " A (active), D (deleted), and I (inactive).");
                }
                w.setState(state);
            }

            if (label != null) {
                w.setLabel(label);
            }
            if (ownerId != null) {
                w.setOwnerId(ownerId);
            }

            // Update audit trail
            Date nowUTC = Server.getCurrentDate(context);
            addAuditRecord(context, w, "modifyObject", "", logMessage, nowUTC);

            w.commit(logMessage);
            return w.getLastModDate();
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed modifyObject(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", state: ").append(state);
                logMsg.append(", label: ").append(label);
                logMsg.append(", ownderId: ").append(ownerId);
                logMsg.append(", logMessage: ").append(logMessage);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            finishModification(w, "modifyObject");
        }
    }

    public InputStream getObjectXML(Context context, String pid, String encoding)
            throws ServerException {
        try {
            LOG.debug("Entered getObjectXML");

            m_authz.enforceGetObjectXML(context, pid, encoding);

            DOReader reader =
                    m_manager.getReader(Server.USE_DEFINITIVE_STORE,
                                        context,
                                        pid);
            InputStream instream = reader.GetObjectXML();
            return instream;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed getObjectXML(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", encoding: ").append(encoding);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            LOG.debug("Exiting getObjectXML");
        }
    }

    public InputStream export(Context context,
                              String pid,
                              String format,
                              String exportContext,
                              String encoding) throws ServerException {
        try {
            LOG.debug("Entered export");

            m_authz.enforceExport(context,
                                              pid,
                                              format,
                                              exportContext,
                                              encoding);

            DOReader reader =
                    m_manager.getReader(Server.USE_DEFINITIVE_STORE,
                                        context,
                                        pid);
            InputStream instream = reader.Export(format, exportContext);

            return instream;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg = new StringBuilder("Completed export(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", format: ").append(format);
                logMsg.append(", exportContext: ").append(exportContext);
                logMsg.append(", encoding: ").append(encoding);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            LOG.debug("Exiting export");
        }
    }

    public Date purgeObject(Context context,
                            String pid,
                            String logMessage,
                            boolean force) throws ServerException {
        if (force) {
            throw new GeneralException("Forced object removal is not "
                    + "yet supported.");
        }
        DOWriter w = null;
        try {
            LOG.debug("Entered purgeObject");

            m_authz.enforcePurgeObject(context, pid);

            w = m_manager.getWriter(Server.USE_DEFINITIVE_STORE, context, pid);
            w.remove();
            w.commit(logMessage);
            Date serverDate = Server.getCurrentDate(context);
            return serverDate;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed purgeObject(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", logMessage: ").append(logMessage);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            finishModification(w, "purgeObject");
        }
    }

    public String addDatastream(Context context,
                                String pid,
                                String dsID,
                                String[] altIDs,
                                String dsLabel,
                                boolean versionable,
                                String MIMEType,
                                String formatURI,
                                String dsLocation,
                                String controlGroup,
                                String dsState,
                                String checksumType,
                                String checksum,
                                String logMessage) throws ServerException {

        LOG.debug("Entered addDatastream");

        // empty MIME types are allowed. assume they meant "" if they provide it
        // as null.
        if (MIMEType == null) {
            MIMEType = "";
        }

        // empty altIDs are allowed. assume they meant String[0] if they provide
        // it as null.
        if (altIDs == null) {
            altIDs = new String[0];
        }

        // If the datastream ID is not specified directly, see
        // if we can get it from the RecoveryContext
        if (dsID == null && context instanceof RecoveryContext) {
            RecoveryContext rContext = (RecoveryContext) context;
            dsID =
                    rContext
                            .getRecoveryValue(Constants.RECOVERY.DATASTREAM_ID.uri);
            if (dsID != null) {
                LOG.debug("Using new dsID from recovery context");
            }
        }

        // check for valid xml name for datastream ID
        if (dsID != null) {
            if (!XMLUtils.isWellFormedXMLName(dsID)) {
                throw new InvalidXMLNameException("Invalid syntax for datastream ID. "
                        + "The datastream ID of \""
                        + dsID
                        + "\" is"
                        + "not a valid XML Name");
            }
        }

        if (dsID != null
                && (dsID.equals("AUDIT") || dsID.equals("FEDORA-AUDITTRAIL"))) {
            throw new GeneralException("Creation of a datastream with an"
                    + " identifier of 'AUDIT' or 'FEDORA-AUDITTRAIL' is not permitted.");
        }
        DOWriter w = null;
        try {
            m_authz.enforceAddDatastream(context,
                                         pid,
                                         dsID,
                                         altIDs,
                                         MIMEType,
                                         formatURI,
                                         dsLocation,
                                         controlGroup,
                                         dsState,
                                         checksumType,
                                         checksum);

            checkDatastreamID(dsID);
            checkDatastreamLabel(dsLabel);

            w = m_manager.getWriter(Server.USE_DEFINITIVE_STORE, context, pid);
            Datastream ds;
            if (controlGroup.equals("X")) {
                ds = new DatastreamXMLMetadata();
                ds.DSInfoType = ""; // field is now deprecated
                try {
                    InputStream in;
                    MIMETypedStream mimeTypedStream = null;
                    if (dsLocation.startsWith(DatastreamManagedContent.UPLOADED_SCHEME)) {
                        in = getTempStream(dsLocation);
                    } else {
                        ContentManagerParams params = new ContentManagerParams(dsLocation);
                        params.setContext(context);
                        mimeTypedStream = m_contentManager.getExternalContent(params);
                        in = mimeTypedStream.getStream();
                    }
                    // set and validate the content
                    DatastreamXMLMetadata dsm = (DatastreamXMLMetadata) ds;
                    dsm.xmlContent = getEmbeddableXML(in);
                    ValidationUtility.validateReservedDatastream(PID.getInstance(pid),
                                                                 dsID,
                                                                 dsm.getContentStream());
                    if(mimeTypedStream != null) {
                        mimeTypedStream.close();
                    }
                } catch (Exception e) {
                    String extraInfo;
                    if (e.getMessage() == null) {
                        extraInfo = "";
                    } else {
                        extraInfo = " : " + e.getMessage();
                    }
                    throw new GeneralException("Error with " + dsLocation
                            + extraInfo);
                }
            } else if (controlGroup.equals("M")) {
                ds = new DatastreamManagedContent();
                ds.DSInfoType = "DATA";
            } else if (controlGroup.equals("R") || controlGroup.equals("E")) {
                ds = new DatastreamReferencedContent();
                ds.DSInfoType = "DATA";
            } else {
                throw new GeneralException("Invalid control group: "
                        + controlGroup);
            }
            ds.isNew = true;
            ds.DSControlGrp = controlGroup;
            ds.DSVersionable = versionable;
            if (!dsState.equals("A") && !dsState.equals("D")
                    && !dsState.equals("I")) {
                throw new InvalidStateException("The datastream state of \""
                        + dsState
                        + "\" is invalid. The allowed values for state are: "
                        + " A (active), D (deleted), and I (inactive).");
            }
            ds.DSState = dsState;
            // set new datastream id if not provided...
            if (dsID == null || dsID.length() == 0) {
                ds.DatastreamID = w.newDatastreamID();
            } else {
                if (dsID.indexOf(" ") != -1) {
                    throw new GeneralException("Datastream ids cannot contain spaces.");
                }
                if (dsID.indexOf("+") != -1) {
                    throw new GeneralException("Datastream ids cannot contain plusses.");
                }
                if (dsID.indexOf(":") != -1) {
                    throw new GeneralException("Datastream ids cannot contain colons.");
                }
                if (w.GetDatastream(dsID, null) != null) {
                    throw new GeneralException("A datastream already exists with ID: "
                            + dsID);
                } else {
                    ds.DatastreamID = dsID;
                }
            }
            // add version level attributes and
            // create new ds version id ...
            ds.DSVersionID = ds.DatastreamID + ".0";
            ds.DSLabel = dsLabel;
            ds.DSLocation = dsLocation;
            if (dsLocation != null) {
                ValidationUtility.validateURL(dsLocation,ds.DSControlGrp);
            }
            ds.DSFormatURI = formatURI;
            ds.DatastreamAltIDs = altIDs;
            ds.DSMIME = MIMEType;
            ds.DSChecksumType = Datastream.validateChecksumType(checksumType);

            if (checksum != null && checksumType != null) {
                String check = ds.getChecksum();
                if (!checksum.equals(check)) {
                    throw new ValidationException("Checksum Mismatch: " + check);
                }
            }

            // Update audit trail
            Date nowUTC = Server.getCurrentDate(context);
            addAuditRecord(context,
                           w,
                           "addDatastream",
                           ds.DatastreamID,
                           logMessage,
                           nowUTC);

            // Commit the updates
            ds.DSCreateDT = nowUTC;
            w.addDatastream(ds, true);
            w.commit("Added a new datastream");

            return ds.DatastreamID;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed addDatastream(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", dsID: ").append(dsID);
                appendAltIDs(logMsg, altIDs);
                logMsg.append(", dsLabel: ").append(dsLabel);
                logMsg.append(", versionable: ").append(versionable);
                logMsg.append(", MIMEType: ").append(MIMEType);
                logMsg.append(", formatURI: ").append(formatURI);
                logMsg.append(", dsLocation: ").append(dsLocation);
                logMsg.append(", controlGroup: ").append(controlGroup);
                logMsg.append(", dsState: ").append(dsState);
                logMsg.append(", checksumType: ").append(checksumType);
                logMsg.append(", checksum: ").append(checksum);
                logMsg.append(", logMessage: ").append(logMessage);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            finishModification(w, "addDatastream");
        }
    }

    public Date modifyDatastreamByReference(Context context,
                                            String pid,
                                            String datastreamId,
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

        // check for valid xml name for datastream ID
        if (datastreamId != null) {
            if (!XMLUtils.isWellFormedXMLName(datastreamId)) {
                throw new InvalidXMLNameException("Invalid syntax for "
                        + "datastream ID. The datastream ID of \""
                        + datastreamId + "\" is not a valid XML Name");
            }
        }

        if (datastreamId.equals("AUDIT")
                || datastreamId.equals("FEDORA-AUDITTRAIL")) {
            throw new GeneralException("Modification of the system-controlled AUDIT"
                    + " datastream is not permitted.");
        }

        DOWriter w = null;
        try {
            LOG.debug("Entered modifyDatastreamByReference");
            m_authz
                    .enforceModifyDatastreamByReference(context,
                                                        pid,
                                                        datastreamId,
                                                        altIDs,
                                                        mimeType,
                                                        formatURI,
                                                        dsLocation,
                                                        checksumType,
                                                        checksum);

            checkDatastreamLabel(dsLabel);
            w = m_manager.getWriter(Server.USE_DEFINITIVE_STORE, context, pid);
            fedora.server.storage.types.Datastream orig =
                    w.GetDatastream(datastreamId, null);
            Date nowUTC; // variable for ds modified date

            // some forbidden scenarios...
            if (orig.DSControlGrp.equals("X")) {
                throw new GeneralException("Inline XML datastreams must be modified by value, not by reference.");
            }
            if (orig.DSState.equals("D")) {
                throw new GeneralException("Changing attributes on deleted datastreams is forbidden.");
            }

            // A NULL INPUT PARM MEANS NO CHANGE TO DS ATTRIBUTE...
            // if input parms are null, the ds attribute should not be changed,
            // so set the parm values to the existing values in the datastream.
            if (dsLabel == null) {
                dsLabel = orig.DSLabel;
            }
            if (mimeType == null) {
                mimeType = orig.DSMIME;
            }
            if (formatURI == null) {
                formatURI = orig.DSFormatURI;
            }
            if (altIDs == null) {
                altIDs = orig.DatastreamAltIDs;
            }
            if (checksumType == null) {
                checksumType = orig.DSChecksumType;
            } else {
                checksumType = Datastream.validateChecksumType(checksumType);
            }

            // In cases where an empty attribute value is not allowed, then
            // NULL or EMPTY PARM means no change to ds attribute...
            if (dsLocation == null || dsLocation.equals("")) {
                if (orig.DSControlGrp.equals("M")) {
                    // if managed content location is unspecified,
                    // cause a copy of the prior content to be made at
                    // commit-time
                    dsLocation = DatastreamManagedContent.COPY_SCHEME + orig.DSLocation;
                } else {
                    dsLocation = orig.DSLocation;
                }
            } else {
                ValidationUtility.validateURL(dsLocation,orig.DSControlGrp);
            }

            // if "force" is false and the mime type changed, validate the
            // original datastream with respect to any disseminators it is
            // involved in, and keep a record of that information for later
            // (so we can determine whether the mime type change would cause
            // data contract invalidation)
            // Map oldValidationReports = null;
            // if ( !mimeType.equals(orig.DSMIME) && !force) {
            // oldValidationReports = getAllBindingMapValidationReports(
            // context, w, datastreamId);
            // }

            // instantiate the right class of datastream
            // (inline xml "X" datastreams have already been rejected)
            Datastream newds;
            if (orig.DSControlGrp.equals("M")) {
                newds = new DatastreamManagedContent();
            } else {
                newds = new DatastreamReferencedContent();
            }
            // update ds attributes that are common to all versions...
            // first, those that cannot be changed by client...
            newds.DatastreamID = orig.DatastreamID;
            newds.DSControlGrp = orig.DSControlGrp;
            newds.DSInfoType = orig.DSInfoType;
            // next, those that can be changed by client...
            newds.DSState = orig.DSState;
            newds.DSVersionable = orig.DSVersionable;

            // update ds version-level attributes, and
            // make sure ds gets a new version id
            newds.DSVersionID = w.newDatastreamID(datastreamId);
            newds.DSLabel = dsLabel;
            newds.DSMIME = mimeType;
            newds.DSFormatURI = formatURI;
            newds.DatastreamAltIDs = altIDs;
            nowUTC = Server.getCurrentDate(context);
            newds.DSCreateDT = nowUTC;
            // newds.DSSize will be computed later
            newds.DSLocation = dsLocation;
            newds.DSChecksumType = checksumType;

            // next, add the datastream via the object writer
            w.addDatastream(newds, orig.DSVersionable);

            // if a checksum is passed in verify that the checksum computed for
            // the datastream
            // matches the one that is passed in.
            if (checksum != null) {
                if (checksumType == null) {
                    newds.DSChecksumType = orig.DSChecksumType;
                }
                String check = newds.getChecksum();
                if (!checksum.equals(check)) {
                    throw new ValidationException("Checksum Mismatch: " + check);
                }
            }

            // Update audit trail
            addAuditRecord(context,
                           w,
                           "modifyDatastreamByReference",
                           newds.DatastreamID,
                           logMessage,
                           nowUTC);

            // if all went ok, check if we need to validate, then commit.
            // if (oldValidationReports != null) { // mime changed and
            // force=false
            // rejectMimeChangeIfCausedInvalidation(
            // oldValidationReports,
            // getAllBindingMapValidationReports(context,
            // w,
            // datastreamId));
            // }
            w.commit(logMessage);

            return nowUTC;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed modifyDatastreamByReference(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", datastreamId: ").append(datastreamId);
                appendAltIDs(logMsg, altIDs);
                logMsg.append(", dsLabel: ").append(dsLabel);
                logMsg.append(", mimeType: ").append(mimeType);
                logMsg.append(", formatURI: ").append(formatURI);
                logMsg.append(", dsLocation: ").append(dsLocation);
                logMsg.append(", checksumType: ").append(checksumType);
                logMsg.append(", checksum: ").append(checksum);
                logMsg.append(", logMessage: ").append(logMessage);
                logMsg.append(", force: ").append(force);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            finishModification(w, "modifyDatastreamByReference");
        }
    }

    public Date modifyDatastreamByValue(Context context,
                                        String pid,
                                        String datastreamId,
                                        String[] altIDs,
                                        String dsLabel,
                                        String mimeType,
                                        String formatURI,
                                        InputStream dsContent,
                                        String checksumType,
                                        String checksum,
                                        String logMessage,
                                        boolean force) throws ServerException {

        // check for valid xml name for datastream ID
        if (datastreamId != null) {
            if (!XMLUtils.isWellFormedXMLName(datastreamId)) {
                throw new InvalidXMLNameException("Invalid syntax for "
                        + "datastream ID. The datastream ID of \""
                        + datastreamId + "\" is not a valid XML Name");
            }
        }

        if (datastreamId.equals("AUDIT")
                || datastreamId.equals("FEDORA-AUDITTRAIL")) {
            throw new GeneralException("Modification of the system-controlled AUDIT"
                    + " datastream is not permitted.");
        }
        DOWriter w = null;
        try {
            LOG.debug("Entered modifyDatastreamByValue");
            m_authz.enforceModifyDatastreamByValue(context,
                                                               pid,
                                                               datastreamId,
                                                               altIDs,
                                                               mimeType,
                                                               formatURI,
                                                               checksumType,
                                                               checksum);

            checkDatastreamLabel(dsLabel);
            w = m_manager.getWriter(Server.USE_DEFINITIVE_STORE, context, pid);
            fedora.server.storage.types.Datastream orig =
                    w.GetDatastream(datastreamId, null);

            // some forbidden scenarios...
            if (orig.DSState.equals("D")) {
                throw new GeneralException("Changing attributes on deleted datastreams is forbidden.");
            }
            if (!orig.DSControlGrp.equals("X")) {
                throw new GeneralException("Only content of inline XML datastreams may"
                        + " be modified by value.\n"
                        + "Use modifyDatastreamByReference instead.");
            }

            // A NULL INPUT PARM MEANS NO CHANGE TO DS ATTRIBUTE...
            // if input parms are null, the ds attribute should not be changed,
            // so set the parm values to the existing values in the datastream.
            if (dsLabel == null) {
                dsLabel = orig.DSLabel;
            }
            if (mimeType == null) {
                mimeType = orig.DSMIME;
            }
            if (formatURI == null) {
                formatURI = orig.DSFormatURI;
            }
            if (altIDs == null) {
                altIDs = orig.DatastreamAltIDs;
            }
            if (checksumType == null) {
                checksumType = orig.DSChecksumType;
            } else {
                checksumType = Datastream.validateChecksumType(checksumType);
            }

            DatastreamXMLMetadata newds = new DatastreamXMLMetadata();
            newds.DSMDClass = ((DatastreamXMLMetadata) orig).DSMDClass;
            if (dsContent == null) {
                // If the dsContent input stream parm is null,
                // that means "do not change the content".
                // Accordingly, here we just make a copy of the old content.
                newds.xmlContent = ((DatastreamXMLMetadata) orig).xmlContent;
            } else {
                // set and validate the content
                newds.xmlContent = getEmbeddableXML(dsContent);
                ValidationUtility.validateReservedDatastream(PID.getInstance(pid),
                                                             orig.DatastreamID,
                                                             newds.getContentStream());
            }

            // update ds attributes that are common to all versions...
            // first, those that cannot be changed by client...
            newds.DatastreamID = orig.DatastreamID;
            newds.DSControlGrp = orig.DSControlGrp;
            newds.DSInfoType = orig.DSInfoType;
            // next, those that can be changed by client...
            newds.DSState = orig.DSState;
            newds.DSVersionable = orig.DSVersionable;

            // update ds version level attributes, and
            // make sure ds gets a new version id
            newds.DSVersionID = w.newDatastreamID(datastreamId);
            newds.DSLabel = dsLabel;
            newds.DatastreamAltIDs = altIDs;
            newds.DSMIME = mimeType;
            newds.DSFormatURI = formatURI;
            Date nowUTC = Server.getCurrentDate(context);
            newds.DSCreateDT = nowUTC;

            newds.DSChecksumType = checksumType;

            // next, add the datastream via the object writer
            w.addDatastream(newds, orig.DSVersionable);

            // if a checksum is passed in verify that the checksum computed for
            // the datastream
            // matches the one that is passed in.
            if (checksum != null) {
                String check = newds.getChecksum();
                if (!checksum.equals(check)) {
                    throw new ValidationException("Checksum Mismatch: " + check);
                }
            }

            // Update audit trail
            addAuditRecord(context,
                           w,
                           "modifyDatastreamByValue",
                           newds.DatastreamID,
                           logMessage,
                           nowUTC);

            w.commit(logMessage);

            return nowUTC;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed modifyDatastreamByValue(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", datastreamId: ").append(datastreamId);
                appendAltIDs(logMsg, altIDs);
                logMsg.append(", dsLabel: ").append(dsLabel);
                logMsg.append(", mimeType: ").append(mimeType);
                logMsg.append(", formatURI: ").append(formatURI);
                logMsg.append(", dsContent ");
                logMsg.append(", checksumType: ").append(checksumType);
                logMsg.append(", checksum: ").append(checksum);
                logMsg.append(", logMessage: ").append(logMessage);
                logMsg.append(", force: ").append(force);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            finishModification(w, "modifyDatastreamByValue");
        }
    }

    public Date[] purgeDatastream(Context context,
                                  String pid,
                                  String datastreamID,
                                  Date startDT,
                                  Date endDT,
                                  String logMessage,
                                  boolean force) throws ServerException {
        if (force) {
            throw new GeneralException("Forced datastream removal is not "
                    + "yet supported.");
        }
        DOWriter w = null;
        try {
            LOG.debug("Entered purgeDatastream");

            m_authz.enforcePurgeDatastream(context,
                                                       pid,
                                                       datastreamID,
                                                       endDT);

            w = m_manager.getWriter(Server.USE_DEFINITIVE_STORE, context, pid);
            Date[] deletedDates =
                    w.removeDatastream(datastreamID, startDT, endDT);
            // check if there's at least one version with this id...
            if (w.GetDatastream(datastreamID, null) == null) {
                // if deleting would result in no versions remaining,
                // only continue if there are no disseminators that use
                // this datastream.
                // to do this, we must look through all versions of every
                // disseminator, regardless of state
                ArrayList<String> usedList = new ArrayList<String>();
                if (datastreamID.equals("DC")) {
                    usedList.add("The default disseminator");
                }
                if (usedList.size() > 0) {
                    StringBuffer msg = new StringBuffer();
                    msg.append("Cannot purge entire datastream because it\n");
                    msg.append("is used by the following disseminators:");
                    for (int i = 0; i < usedList.size(); i++) {
                        msg.append("\n - " + usedList.get(i));
                    }
                    throw new GeneralException(msg.toString());
                }
            }
            // add an explanation of what happened to the user-supplied message.
            if (logMessage == null) {
                logMessage = "";
            } else {
                logMessage += " . . . ";
            }
            logMessage +=
                    getPurgeLogMessage("datastream",
                                       datastreamID,
                                       startDT,
                                       endDT,
                                       deletedDates);

            // Update audit trail
            Date nowUTC = Server.getCurrentDate(context);
            addAuditRecord(context,
                           w,
                           "purgeDatastream",
                           datastreamID,
                           logMessage,
                           nowUTC);

            // It looks like all went ok, so commit
            w.commit(logMessage);
            // ... then give the response
            return deletedDates;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed purgeDatastream(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", datastreamID: ").append(datastreamID);
                logMsg.append(", startDT: ").append(startDT);
                logMsg.append(", endDT: ").append(endDT);
                logMsg.append(", logMessage: ").append(logMessage);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            finishModification(w, "purgeDatastream");
        }
    }

    private String getPurgeLogMessage(String kindaThing,
                                      String id,
                                      Date start,
                                      Date end,
                                      Date[] deletedDates) {
        SimpleDateFormat formatter =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        StringBuffer buf = new StringBuffer();
        buf.append("Purged ");
        buf.append(kindaThing);
        buf.append(" (ID=");
        buf.append(id);
        buf.append("), versions ranging from ");
        if (start == null) {
            buf.append("the beginning of time");
        } else {
            buf.append(formatter.format(start));
        }
        buf.append(" to ");
        if (end == null) {
            buf.append("the end of time");
        } else {
            buf.append(formatter.format(end));
        }
        buf.append(".  This resulted in the permanent removal of ");
        buf.append(deletedDates.length + " ");
        buf.append(kindaThing);
        buf.append(" version(s) (");
        for (int i = 0; i < deletedDates.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(formatter.format(deletedDates[i]));
        }
        buf.append(") and all associated audit records.");
        return buf.toString();
    }

    public Datastream getDatastream(Context context,
                                    String pid,
                                    String datastreamID,
                                    Date asOfDateTime) throws ServerException {
        try {
            LOG.debug("Entered getDatastream");

            m_authz.enforceGetDatastream(context,
                                                     pid,
                                                     datastreamID,
                                                     asOfDateTime);

            DOReader r =
                    m_manager.getReader(Server.GLOBAL_CHOICE, context, pid);

            return r.GetDatastream(datastreamID, asOfDateTime);
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed getDatastream(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", datastreamID: ").append(datastreamID);
                logMsg.append(", asOfDateTime: ").append(asOfDateTime);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            LOG.debug("Exiting getDatastream");
        }
    }

    public Datastream[] getDatastreams(Context context,
                                       String pid,
                                       Date asOfDateTime,
                                       String state) throws ServerException {
        try {
            LOG.debug("Entered getDatastreams");

            m_authz.enforceGetDatastreams(context,
                                                      pid,
                                                      asOfDateTime,
                                                      state);

            DOReader r =
                    m_manager.getReader(Server.GLOBAL_CHOICE, context, pid);

            return r.GetDatastreams(asOfDateTime, state);
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed getDatastreams(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", asOfDateTime: ").append(asOfDateTime);
                logMsg.append(", state: ").append(state);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            LOG.debug("Exiting getDatastreams");
        }
    }

    public Datastream[] getDatastreamHistory(Context context,
                                             String pid,
                                             String datastreamID)
            throws ServerException {
        try {
            LOG.debug("Entered getDatastreamHistory");

            m_authz.enforceGetDatastreamHistory(context,
                                                            pid,
                                                            datastreamID);

            DOReader r =
                    m_manager.getReader(Server.GLOBAL_CHOICE, context, pid);
            Date[] versionDates = r.getDatastreamVersions(datastreamID);
            Datastream[] versions = new Datastream[versionDates.length];
            for (int i = 0; i < versionDates.length; i++) {
                versions[i] = r.GetDatastream(datastreamID, versionDates[i]);
            }
            // sort, ascending
            Arrays.sort(versions, new DatastreamDateComparator());
            // reverse it (make it descend, so most recent date is element 0)
            Datastream[] out = new Datastream[versions.length];
            for (int i = 0; i < versions.length; i++) {
                out[i] = versions[versions.length - 1 - i];
            }

            return out;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed getDatastreamHistory(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", datastreamID: ").append(datastreamID);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            LOG.debug("Exiting getDatastreamHistory");
        }
    }

    public class DatastreamDateComparator
            implements Comparator<Object> {

        public int compare(Object o1, Object o2) {
            long ms1 = ((Datastream) o1).DSCreateDT.getTime();
            long ms2 = ((Datastream) o1).DSCreateDT.getTime();
            if (ms1 < ms2) {
                return -1;
            }
            if (ms1 > ms2) {
                return 1;
            }
            return 0;
        }
    }

    public String[] getNextPID(Context context, int numPIDs, String namespace)
            throws ServerException {
        try {
            LOG.debug("Entered getNextPID");
            m_authz.enforceGetNextPid(context, namespace, numPIDs);

            String[] pidList = null;

            // If the pidList is in the RecoveryContext, just reserve them
            // rather than generating new ones.
            if (context instanceof RecoveryContext) {
                RecoveryContext rContext = (RecoveryContext) context;
                pidList =
                        rContext
                                .getRecoveryValues(Constants.RECOVERY.PID_LIST.uri);
                if (pidList != null && pidList.length > 0) {
                    LOG.debug("Reserving and returning PID_LIST "
                            + "from recovery context");
                    m_manager.reservePIDs(pidList);
                }
            }

            if (pidList == null || pidList.length == 0) {
                pidList = m_manager.getNextPID(numPIDs, namespace);
            }

            return pidList;

        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed getNextPID(");
                logMsg.append("numPIDs: ").append(numPIDs);
                logMsg.append(", namespace: ").append(namespace);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            LOG.debug("Exiting getNextPID");
        }
    }

    public String putTempStream(Context context, InputStream in)
            throws StreamWriteException, AuthzException {
        m_authz.enforceUpload(context);
        // first clean up after old stuff
        purgeUploadedFiles();
        // then generate an id
        int id = getNextTempId(context);
        // and attempt to save the stream
        File outFile = new File(m_tempDir, "" + id);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
            StreamUtility.pipeStream(in, out, 32768);
        } catch (Exception e) {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ex) {
                }
                outFile.delete();
            }
            throw new StreamWriteException("Error writing temp stream", e);
        }
        // if we got this far w/o an exception, add to hash with current time
        // and return the identifier-that-looks-like-a-url
        long now = System.currentTimeMillis();
        m_uploadStartTime.put("" + id, new Long(now));
        return "uploaded://" + id;
    }

    private synchronized int getNextTempId(Context context) {

        int recoveryId = -1;

        // If the RecoveryContext has an uploaded://n url, use n.
        if (context instanceof RecoveryContext) {
            RecoveryContext rContext = (RecoveryContext) context;
            String uploadURL =
                    rContext.getRecoveryValue(Constants.RECOVERY.UPLOAD_ID.uri);
            if (uploadURL != null) {
                try {
                    String n = uploadURL.substring(11);
                    recoveryId = Integer.parseInt(n);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unable to parse UPLOAD_ID "
                            + "from recovery context: '" + uploadURL + "'");
                }
            }
        }
        if (recoveryId == -1) {
            m_lastId++;
        } else {
            m_lastId = recoveryId;
        }
        return m_lastId;
    }

    public InputStream getTempStream(String id) throws StreamReadException {
        // it should come in starting with "uploaded://"
        if (id.startsWith(DatastreamManagedContent.UPLOADED_SCHEME) || id.length() < 12) {
            String internalId = id.substring(11);
            if (m_uploadStartTime.get(internalId) != null) {
                // found... return inputstream
                try {
                    return new FileInputStream(new File(m_tempDir, internalId));
                } catch (Exception e) {
                    throw new StreamReadException(e.getMessage());
                }
            } else {
                throw new StreamReadException("Id specified, '" + id
                        + "', does not match an existing file.");
            }
        } else {
            throw new StreamReadException("Invalid id syntax '" + id + "'.");
        }
    }

    public Date setDatastreamState(Context context,
                                   String pid,
                                   String datastreamID,
                                   String dsState,
                                   String logMessage) throws ServerException {
        DOWriter w = null;
        try {
            LOG.debug("Entered setDatastreamState");

            m_authz.enforceSetDatastreamState(context,
                                                          pid,
                                                          datastreamID,
                                                          dsState);

            w = m_manager.getWriter(Server.USE_DEFINITIVE_STORE, context, pid);
            if (!dsState.equals("A") && !dsState.equals("D")
                    && !dsState.equals("I")) {
                throw new InvalidStateException("The datastream state of \""
                        + dsState
                        + "\" is invalid. The allowed values for state are: "
                        + " A (active), D (deleted), and I (inactive).");
            }
            w.setDatastreamState(datastreamID, dsState);

            // Update audit trail
            Date nowUTC = Server.getCurrentDate(context);
            addAuditRecord(context,
                           w,
                           "setDatastreamState",
                           datastreamID,
                           logMessage,
                           nowUTC);

            // if all went ok, commit
            w.commit(logMessage);
            return nowUTC;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed setDatastreamState(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", datastreamID: ").append(datastreamID);
                logMsg.append(", dsState: ").append(dsState);
                logMsg.append(", logMessage: ").append(logMessage);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            finishModification(w, "setDatastreamState");
        }
    }

    public Date setDatastreamVersionable(Context context,
                                         String pid,
                                         String datastreamID,
                                         boolean versionable,
                                         String logMessage)
            throws ServerException {
        DOWriter w = null;
        try {
            LOG.debug("Entered setDatastreamVersionable");

            m_authz.enforceSetDatastreamVersionable(context,
                                                                pid,
                                                                datastreamID,
                                                                versionable);

            w = m_manager.getWriter(Server.USE_DEFINITIVE_STORE, context, pid);
            w.setDatastreamVersionable(datastreamID, versionable);

            // Update audit trail
            Date nowUTC = Server.getCurrentDate(context);
            addAuditRecord(context,
                           w,
                           "setDatastreamVersionable",
                           datastreamID,
                           logMessage,
                           nowUTC);

            // if all went ok, commit
            w.commit(logMessage);
            return nowUTC;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed setDatastreamVersionable(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", datastreamID: ").append(datastreamID);
                logMsg.append(", versionable: ").append(versionable);
                logMsg.append(", logMessage: ").append(logMessage);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            finishModification(w, "setDatastreamVersionable");
        }
    }

    public String compareDatastreamChecksum(Context context,
                                            String pid,
                                            String datastreamID,
                                            Date versionDate)
            throws ServerException {
        DOReader r = null;
        try {
            LOG.debug("Entered compareDatastreamChecksum");

            m_authz.enforceCompareDatastreamChecksum(context,
                                                                 pid,
                                                                 datastreamID,
                                                                 versionDate);

            LOG.debug("Getting Reader");
            r = m_manager.getReader(Server.USE_DEFINITIVE_STORE, context, pid);
            LOG.debug("Getting datastream:" + datastreamID + "date: "
                    + versionDate);
            Datastream ds = r.GetDatastream(datastreamID, versionDate);
            LOG.debug("Got Datastream, comparing checksum");
            boolean check = ds.compareChecksum();
            LOG.debug("compared checksum = " + check);

            return check ? ds.getChecksum() : "Checksum validation error";
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed compareDatastreamChecksum(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", datastreamID: ").append(datastreamID);
                logMsg.append(", versionDate: ").append(versionDate);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            LOG.debug("Exiting compareDatastreamChecksum");
        }
    }

    /**
     * Get a byte array containing an xml chunk that is safe to embed in another
     * UTF-8 xml document.
     * <p>
     * This will ensure that the xml is:
     * <ul>
     * <li> well-formed. If not, an exception will be raised.</li>
     * <li> encoded in UTF-8. It will be converted otherwise.</li>
     * <li> devoid of processing instructions. These will be stripped if
     * present.</li>
     * <li> devoid of DOCTYPE declarations. These will be stripped if present.</li>
     * <li> devoid of internal entity references. These will be expanded if
     * present.</li>
     * </ul>
     * </p>
     */
    private byte[] getEmbeddableXML(InputStream in) throws GeneralException {
        // parse with xerces and re-serialize the fixed xml to a byte array
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputFormat fmt = new OutputFormat("XML", "UTF-8", true);
            fmt.setIndent(2);
            fmt.setLineWidth(120);
            fmt.setPreserveSpace(false);
            fmt.setOmitXMLDeclaration(true);
            fmt.setOmitDocumentType(true);
            XMLSerializer ser = new XMLSerializer(out, fmt);
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(in);
            ser.serialize(doc);
            return out.toByteArray();
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = "";
            }
            throw new GeneralException("XML was not well-formed. " + message, e);
        }
    }

    private void checkDatastreamID(String id) throws ValidationException {
        checkString(id,
                    "Datastream id",
                    ValidationConstants.DATASTREAM_ID_MAXLEN,
                    ValidationConstants.DATASTREAM_ID_BADCHARS);
    }

    private void checkDatastreamLabel(String label) throws ValidationException {
        checkString(label,
                    "Datastream label",
                    ValidationConstants.DATASTREAM_LABEL_MAXLEN,
                    null);
    }

    private void checkObjectLabel(String label) throws ValidationException {
        checkString(label,
                    "Object label",
                    ValidationConstants.OBJECT_LABEL_MAXLEN,
                    null);
    }

    private void checkString(String string,
                             String kind,
                             int maxLen,
                             char[] badChars) throws ValidationException {
        if (string != null) {
            if (string.length() > maxLen) {
                throw new ValidationException(kind + " is too long. Maximum "
                        + "length is " + maxLen + " characters.");
            } else if (badChars != null) {
                for (char c : badChars) {
                    if (string.indexOf(c) != -1) {
                        throw new ValidationException(kind + " contains a "
                                + "'" + c + "', but that character is not "
                                + "allowed.");
                    }
                }
            }
        }
    }


    // helper class to get pid from subject and to get URI form of subject
    // subject can either be a pid or an info:fedora/ uri
    private static class SubjectProcessor {

        private static Pattern pidRegex = Pattern.compile("^([A-Za-z0-9]|-|\\.)+:(([A-Za-z0-9])|-|\\.|~|_|(%[0-9A-F]{2}))+$");

        static String getSubjectAsUri(String subject) {
            // if we weren't given a pid, assume it's a URI
            if (!isPid(subject))
                return subject;
            // otherwise return URI from the pid
            LOG.warn("Relationships API methods:  the 'pid' (" + subject + ") form of a relationship's subject is deprecated.  Please specify the subject using the " + Constants.FEDORA.uri + " uri scheme.");
            return PID.toURI(subject);
        }
        static String getSubjectPID(String subject) throws ServerException {
            if (isPid(subject))
                return subject;
            // check for info:uri scheme
            if (subject.startsWith(Constants.FEDORA.uri)) {
                // pid is everything after the first / to the 2nd / or to the end of the string
                return subject.split("/",3)[1];

            } else {
                throw new GeneralException("Subject URI must be in the " + Constants.FEDORA.uri + " scheme.");
            }

        }
        private static boolean isPid(String subject) {
            return pidRegex.matcher(subject).matches();
        }
    }


    public RelationshipTuple[] getRelationships(Context context,
                                                String subject,
                                                String relationship)
            throws ServerException {
        DOReader r = null;
        String pid = null;
        try {
            LOG.debug("Entered getRelationships");

            pid = SubjectProcessor.getSubjectPID(subject);

            m_authz.enforceGetRelationships(context,
                                                        pid,
                                                        relationship);

            r = m_manager.getReader(Server.USE_DEFINITIVE_STORE, context, pid);
            LOG.debug("Getting Relationships:  pid = " + pid + " predicate = "
                    + relationship);
            try {
                URIReference pred = null;
                if (relationship != null) {
                    pred = new SimpleURIReference(new URI(relationship));
                }
                URIReference subj = null;
                if (subject != null) {
                    subj = new SimpleURIReference(new URI(SubjectProcessor.getSubjectAsUri(subject)));
                }
                Set<RelationshipTuple> tuples =
                        r.getRelationships(subj, pred, null);
                return tuples.toArray(new RelationshipTuple[tuples.size()]);
            } catch (URISyntaxException e) {
                throw new GeneralException("Relationship must be a URI", e);
            }
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed getRelationships(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", relationship: ").append(relationship);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            LOG.debug("Exiting getRelationships");
        }
    }

    public boolean addRelationship(Context context,
                                   String subject,
                                   String relationship,
                                   String object,
                                   boolean isLiteral,
                                   String datatype) throws ServerException {
        DOWriter w = null;
        String pid = null;
        try {
            LOG.debug("Entered addRelationship");
            pid = SubjectProcessor.getSubjectPID(subject);
            m_authz.enforceAddRelationship(context,
                                                       pid,
                                                       relationship,
                                                       object,
                                                       isLiteral,
                                                       datatype);

            w = m_manager.getWriter(Server.USE_DEFINITIVE_STORE, context, pid);
            boolean added =
                    w
                            .addRelationship(SubjectProcessor.getSubjectAsUri(subject),
                                             relationship,
                                             object,
                                             isLiteral,
                                             datatype);

            // if all went ok, commit
            if (added) {
                w.commit(null);
            }

            return added;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed addRelationship(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", relationship: ").append(relationship);
                logMsg.append(", object: ").append(object);
                logMsg.append(", isLiteral: ").append(isLiteral);
                logMsg.append(", datatype: ").append(datatype);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            finishModification(w, "addRelationship");
        }
    }

    public boolean purgeRelationship(Context context,
                                     String subject,
                                     String relationship,
                                     String object,
                                     boolean isLiteral,
                                     String datatype) throws ServerException {
        DOWriter w = null;
        String pid = null;
        try {
            LOG.debug("Entered purgeRelationship");
            pid = SubjectProcessor.getSubjectPID(subject);
            m_authz.enforcePurgeRelationship(context,
                                                         pid,
                                                         relationship,
                                                         object,
                                                         isLiteral,
                                                         datatype);

            w = m_manager.getWriter(Server.USE_DEFINITIVE_STORE, context, pid);
            boolean purged =
                    w.purgeRelationship(SubjectProcessor.getSubjectAsUri(subject),
                                        relationship,
                                        object,
                                        isLiteral,
                                        datatype);

            // if all went ok, commit
            if (purged) {
                w.commit(null);
            }
            return purged;
        } finally {
            // Log completion
            if (LOG.isInfoEnabled()) {
                StringBuilder logMsg =
                        new StringBuilder("Completed purgeRelationship(");
                logMsg.append("pid: ").append(pid);
                logMsg.append(", relationship: ").append(relationship);
                logMsg.append(", object: ").append(object);
                logMsg.append(", isLiteral: ").append(isLiteral);
                logMsg.append(", datatype: ").append(datatype);
                logMsg.append(")");
                LOG.info(logMsg.toString());
            }

            finishModification(w, "purgeRelationship");
        }
    }

    /**
     * Creates a new audit record and adds it to the digital object audit trail.
     */
    private void addAuditRecord(Context context,
                                DOWriter w,
                                String action,
                                String componentID,
                                String justification,
                                Date nowUTC) throws ServerException {
        AuditRecord audit = new AuditRecord();
        audit.id = w.newAuditRecordID();
        audit.processType = "Fedora API-M";
        audit.action = action;
        audit.componentID = componentID;
        audit.responsibility =
                context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri);
        audit.date = nowUTC;
        audit.justification = justification;
        w.getAuditRecords().add(audit);
    }

    /**
     * Appends alt IDs to the log message.
     */
    private static void appendAltIDs(StringBuilder logMsg, String[] altIDs) {
        logMsg.append(", altIDs: ");
        if (altIDs == null) {
            logMsg.append("null");
        } else {
            for (String altID : altIDs) {
                logMsg.append("'").append(altID).append("'");
            }
        }
    }

    /**
     * Deletes expired uploaded files.
     * <p>
     * This method is called for each upload. But we respect a minimim delay
     * between two purges. This delay is given by m_purgeDelayInMillis.
     */
    private void purgeUploadedFiles() {
        long currentTimeMillis = System.currentTimeMillis();

        // Do purge if purge delay is past before last purge
        // -------------------------------------------------
        long nextPurgeInMillis =
          this.m_lastPurgeInMillis + this.m_purgeDelayInMillis;
        if (nextPurgeInMillis < currentTimeMillis) {
            this.m_lastPurgeInMillis = currentTimeMillis;

            // Compute limit file time to purged
            // ---------------------------------
            long minStartTime =
                currentTimeMillis - (this.m_uploadStorageMinutes * 60000);

            // List files to purge and remove filename to map
            // This operation is synchronized to be thread-safe
            // ------------------------------------------------
            List<String> removeList = new ArrayList<String>();
            synchronized (this.m_uploadStartTime) {
                for (Entry<String, Long> entry :
                  m_uploadStartTime.entrySet()) {
                    String filename = entry.getKey();
                    long startTime = entry.getValue().longValue();
                    if (startTime < minStartTime) {
                        removeList.add(filename);
                    }
                }
                for (String filename : removeList) {
                    this.m_uploadStartTime.remove(filename);
                }
            }

            // Delete file to purged
            // This operation is out of synchronised block for performances
            // ------------------------------------------------------------
            for (int i = 0; i < removeList.size(); i++) {
                String id = removeList.get(i);

                File file = new File(this.m_tempDir, id);
                if (file.exists()) {
                    if (file.delete()) {
                        LOG.info("Removed uploaded file '" + id
                            + "' because it expired.");
                    } else {
                        LOG.warn("Could not remove expired uploaded file '"
                            + id + "'. Check permissions in management/upload/ directory.");
                    }
                }
            }
        }
    }

}
