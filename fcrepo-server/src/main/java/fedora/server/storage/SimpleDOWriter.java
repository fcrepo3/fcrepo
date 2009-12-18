/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.Triple;

import org.trippi.RDFFormat;
import org.trippi.TripleIterator;
import org.trippi.TrippiException;

import fedora.common.Constants;
import fedora.common.PID;
import fedora.common.rdf.SimpleLiteral;
import fedora.common.rdf.SimpleTriple;
import fedora.common.rdf.SimpleURIReference;

import fedora.server.Context;
import fedora.server.Server;
import fedora.server.errors.GeneralException;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.ServerException;
import fedora.server.storage.translation.DOTranslator;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;
import fedora.server.utilities.FilteredTripleIterator;
import fedora.server.validation.ValidationUtility;

/**
 * A DigitalObject-backed DOWriter.
 * <p>
 * This interface supports transaction behavior with the commit(String) and
 * rollBack() methods. When a DOWriter is instantiated, there is an implicit
 * transaction. Write methods may be called, but they won't affect the the
 * underlying data store until commit(String) is invoked. This also has the
 * effect of creating another implicit transaction. If temporary changes are no
 * longer wanted, rollBack() may be called to return the object to it's original
 * form. rollBack() is only valid for the current transaction.
 * </p>
 * <p>
 * The read methods of DOWriter reflect on the composition of the object in the
 * context of the current transaction.
 * </p>
 *
 * @author Chris Wilper
 */
public class SimpleDOWriter
        extends SimpleDOReader
        implements Constants, DOWriter {

    private static ObjectIntegrityException ERROR_PENDING_REMOVAL =
            new ObjectIntegrityException("That can't be done because you said "
                    + "I should remove the object and i assume that's what you "
                    + "want unless you call rollback()");

    private static ObjectIntegrityException ERROR_INVALIDATED =
            new ObjectIntegrityException("The handle is no longer valid "
                    + "... this object has already been committed or explicitly"
                    + " invalidated.");

    private final DigitalObject m_obj;

    private final Context m_context;

    private final DefaultDOManager m_mgr;

    private boolean m_pendingRemoval = false;

    private boolean m_invalidated = false;

    private boolean m_committed = false;

    public SimpleDOWriter(Context context,
                          DefaultDOManager mgr,
                          DOTranslator translator,
                          String exportFormat,
                          String encoding,
                          DigitalObject obj) {
        super(context, mgr, translator, exportFormat, encoding, obj);
        m_context = context;
        m_obj = obj;
        m_mgr = mgr;
    }

    public void setState(String state) throws ObjectIntegrityException {
        assertNotInvalidated();
        assertNotPendingRemoval();
        m_obj.setState(state);
    }

    public void setOwnerId(String ownerId) throws ObjectIntegrityException {
        assertNotInvalidated();
        assertNotPendingRemoval();
        m_obj.setOwnerId(ownerId);
    }

    public void setDatastreamState(String datastreamID, String dsState)
            throws ServerException {
        assertNotInvalidated();
        assertNotPendingRemoval();

        // Set all versions of this datastreamID to the specified state
        for (Datastream ds : m_obj.datastreams(datastreamID)) {
            ds.DSState = dsState;
        }
    }

    public void setDatastreamVersionable(String datastreamID,
                                         boolean versionable)
            throws ServerException {
        assertNotInvalidated();
        assertNotPendingRemoval();

        // Set all versions of this datastreamID to the specified versionable
        // status
        for (Datastream ds : m_obj.datastreams(datastreamID)) {
            ds.DSVersionable = versionable;
        }
    }

    public void setLabel(String label) throws ObjectIntegrityException {
        assertNotInvalidated();
        assertNotPendingRemoval();
        if (label != null && label.equals("")) {
            label = null;
        }
        m_obj.setLabel(label);
    }

    /**
     * Removes the entire digital object.
     *
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public void remove() throws ObjectIntegrityException {
        assertNotInvalidated();
        assertNotPendingRemoval();
        m_pendingRemoval = true;
    }

    /**
     * Adds a datastream to the object.
     *
     * @param datastream
     *        The datastream.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public void addDatastream(Datastream datastream, boolean addNewVersion)
            throws ServerException {
        assertNotInvalidated();
        assertNotPendingRemoval();
        // use this call to handle versionable
        m_obj.addDatastreamVersion(datastream, addNewVersion);
    }

    /**
     * Removes a datastream from the object.
     *
     * @param id
     *        The id of the datastream.
     * @param start
     *        The start date (inclusive) of versions to remove. If
     *        <code>null</code>, this is taken to be the smallest possible
     *        value.
     * @param end
     *        The end date (inclusive) of versions to remove. If
     *        <code>null</code>, this is taken to be the greatest possible
     *        value.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public Date[] removeDatastream(String id, Date start, Date end)
            throws ServerException {
        assertNotInvalidated();
        assertNotPendingRemoval();
        ArrayList<Datastream> removeList = new ArrayList<Datastream>();
        for (Datastream ds : m_obj.datastreams(id)) {
            boolean doRemove = false;
            if (start != null) {
                if (end != null) {
                    if (ds.DSCreateDT.compareTo(start) >= 0
                            && ds.DSCreateDT.compareTo(end) <= 0) {
                        doRemove = true;
                    }
                } else {
                    if (ds.DSCreateDT.compareTo(start) >= 0) {
                        doRemove = true;
                    }
                }
            } else {
                if (end != null) {
                    if (ds.DSCreateDT.compareTo(end) <= 0) {
                        doRemove = true;
                    }
                } else {
                    doRemove = true;
                }
            }
            if (doRemove) {
                // Note: We don't remove old audit records by design.

                // add this datastream to the datastream to-be-removed list.
                removeList.add(ds);
            }
        }

        /* Now that we've identified all ds versions to remove, remove 'em */
        for (Datastream toRemove : removeList) {
            m_obj.removeDatastreamVersion(toRemove);
        }

        // finally, return the dates of each deleted item
        Date[] deletedDates = new Date[removeList.size()];
        for (int i = 0; i < removeList.size(); i++) {
            deletedDates[i] = (removeList.get(i)).DSCreateDT;
        }
        return deletedDates;
    }

    // from the relationship subject, determine which datastream to modify etc
    private String resolveSubjectToDatastream(String subject) throws ServerException{
        String dsId = null;
        String pidURI = PID.toURI(m_obj.getPid());
        if (subject.equals(pidURI)) {
            dsId = "RELS-EXT";
        } else {
            if (subject.startsWith(pidURI + "/")) {
                dsId = "RELS-INT";
            } else {
                throw new GeneralException("Cannot determine which relationship datastream to update for subject " + subject + ".  Relationship subjects must be the URI of the object or the URI of a datastream within the object.");
            }
        }
        return dsId;

    }

    public boolean addRelationship(String subject,
                                   String relationship,
                                   String object,
                                   boolean isLiteral,
                                   String datatype) throws ServerException {

        return addRelationship(resolveSubjectToDatastream(subject),
                               subject,
                               relationship,
                               object,
                               isLiteral,
                               datatype);
    }

    public boolean addRelationship(String dsId,
                                   String subject,
                                   String relationship,
                                   String object,
                                   boolean isLiteral,
                                   String datatype) throws ServerException {

        Triple toAdd =
                createTriple(subject, relationship, object, isLiteral, datatype);
        Datastream relsDatastream = GetDatastream(dsId, null);
        if (relsDatastream == null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Map<String, String> map = new HashMap<String, String>();
            // namespaces for RELS-EXT
            if (dsId.equals("RELS-EXT")) {
                map.put(RELS_EXT.prefix, RELS_EXT.uri);
                map.put(MODEL.prefix, MODEL.uri);
            }
            map.put(RDF.prefix, RDF.uri);

            try {
                TripleIterator triples =
                        new FilteredTripleIterator(map, toAdd, true);
                triples.toStream(out, RDFFormat.RDF_XML, false);
            } catch (TrippiException e) {
                throw new GeneralException(e.getMessage(), e);
            }

            DatastreamXMLMetadata newds = new DatastreamXMLMetadata();
            newds.DatastreamID = dsId;
            newds.DatastreamAltIDs = new String[0];
            // formats for internal datastreams
            if (dsId.equals("RELS-EXT")) {
                newds.DSFormatURI = RELS_EXT1_0.uri;
            } else {
                if (dsId.equals("RELS-INT"))
                    newds.DSFormatURI = RELS_INT1_0.uri;
            }
            newds.DSMIME = "application/rdf+xml";
            newds.DSControlGrp = "X";
            newds.DSInfoType = null;
            newds.DSState = "A";
            newds.DSVersionable = false;
            newds.DSVersionID = dsId + ".0";
            newds.DSLabel = "Relationships";
            newds.DSCreateDT = Server.getCurrentDate(m_context);
            newds.DSLocation = null;
            newds.DSLocationType = null;
            newds.DSChecksumType = Datastream.getDefaultChecksumType();
            newds.xmlContent = out.toByteArray();
            newds.DSSize = newds.xmlContent.length;

            ValidationUtility.validateReservedDatastream(PID.getInstance(m_obj.getPid()),
                                                         newds.DatastreamID,
                                                         newds.getContentStream());
            addDatastream(newds, false);
        } else { // (relsDatastream != null)
            FilteredTripleIterator newIter = null;
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                TripleIterator iter =
                        TripleIterator.fromStream(relsDatastream.getContentStream(),
                                                  RDFFormat.RDF_XML);
                newIter = new FilteredTripleIterator(iter, toAdd, true);
                newIter.toStream(out, RDFFormat.RDF_XML, false);

                if (newIter.wasChangeMade()) {
                    DatastreamXMLMetadata newds = new DatastreamXMLMetadata();
                    newds.DSMDClass =
                            ((DatastreamXMLMetadata) relsDatastream).DSMDClass;
                    newds.DatastreamID = relsDatastream.DatastreamID;
                    newds.DatastreamAltIDs = relsDatastream.DatastreamAltIDs;
                    newds.DSFormatURI = relsDatastream.DSFormatURI;
                    newds.DSMIME = relsDatastream.DSMIME;
                    newds.DSControlGrp = "X";
                    newds.DSInfoType = relsDatastream.DSInfoType;
                    newds.DSState = relsDatastream.DSState;
                    newds.DSVersionable = relsDatastream.DSVersionable;
                    newds.DSVersionID = newDatastreamID(dsId);
                    newds.DSLabel = relsDatastream.DSLabel;
                    newds.DSCreateDT = Server.getCurrentDate(m_context);
                    newds.DSLocation = null;
                    newds.DSLocationType = null;
                    newds.DSChecksumType = relsDatastream.DSChecksumType;
                    newds.xmlContent = out.toByteArray();
                    newds.DSSize = newds.xmlContent.length;

                    ValidationUtility.validateReservedDatastream(PID.getInstance(m_obj.getPid()),
                                                                 newds.DatastreamID,
                                                                 newds.getContentStream());
                    addDatastream(newds, newds.DSVersionable);
                } else {
                    // relationship already exists
                    return false;
                }
            } catch (TrippiException e) {
                throw new GeneralException(e.getMessage(), e);
            } finally {
                try {
                    if (newIter != null) {
                        newIter.close();
                    }
                } catch (TrippiException e) {
                    throw new GeneralException(e.getMessage(), e);
                }
            }
        }
        return true;
    }

    public boolean purgeRelationship(String subject,
                                     String relationship,
                                     String object,
                                     boolean isLiteral,
                                     String datatype) throws ServerException {

        return purgeRelationship(resolveSubjectToDatastream(subject),
                                 subject,
                                 relationship,
                                 object,
                                 isLiteral,
                                 datatype);
    }

    public boolean purgeRelationship(String dsId,
                                     String subject,
                                     String relationship,
                                     String object,
                                     boolean isLiteral,
                                     String datatype) throws ServerException {

        Triple toPurge =
                createTriple(subject, relationship, object, isLiteral, datatype);

        Datastream relsDatastream = GetDatastream(dsId, null);
        if (relsDatastream == null) {
            // relationship does not exist
            return false;
        } else { // (relsExt != null)
            InputStream relsDatastreamIS = relsDatastream.getContentStream();

            TripleIterator iter = null;
            FilteredTripleIterator newIter = null;
            try {
                iter = TripleIterator.fromStream(relsDatastreamIS, RDFFormat.RDF_XML);

                newIter = new FilteredTripleIterator(iter, toPurge, false);
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                newIter.toStream(out, RDFFormat.RDF_XML, false);

                if (newIter.wasChangeMade()) {
                    DatastreamXMLMetadata newds = new DatastreamXMLMetadata();
                    newds.DSMDClass =
                            ((DatastreamXMLMetadata) relsDatastream).DSMDClass;
                    newds.DatastreamID = dsId;
                    newds.DatastreamAltIDs = relsDatastream.DatastreamAltIDs;
                    newds.DSFormatURI = relsDatastream.DSFormatURI;
                    newds.DSMIME = relsDatastream.DSMIME;
                    newds.DSControlGrp = "X";
                    newds.DSInfoType = relsDatastream.DSInfoType;
                    newds.DSState = relsDatastream.DSState;
                    newds.DSVersionable = relsDatastream.DSVersionable;
                    newds.DSVersionID = newDatastreamID(dsId);
                    newds.DSLabel = relsDatastream.DSLabel;
                    newds.DSCreateDT = Server.getCurrentDate(m_context);

                    newds.DSLocation = null;
                    newds.DSLocationType = null;
                    newds.DSChecksumType = relsDatastream.DSChecksumType;
                    newds.xmlContent = out.toByteArray();
                    newds.DSSize = newds.xmlContent.length;

                    ValidationUtility.validateReservedDatastream(PID.getInstance(m_obj.getPid()),
                                                                 newds.DatastreamID,
                                                                 newds.getContentStream());
                    addDatastream(newds, newds.DSVersionable);
                } else {
                    // relationship does not exist
                    return false;
                }
            } catch (TrippiException e) {
                throw new GeneralException(e.getMessage(), e);
            } finally {
                try {
                    if (newIter != null) {
                        newIter.close(); // also closes the contained iter
                    }
                } catch (TrippiException e) {
                    throw new GeneralException(e.getMessage(), e);
                }
            }
        }
        return true;
    }

    private static Triple createTriple(String subject,
                                       String predicate,
                                       String object,
                                       boolean isLiteral,
                                       String datatype)
            throws ServerException {
        ObjectNode o = null;
        try {
            if (isLiteral) {
                if (datatype == null || datatype.length() == 0) {
                    o = new SimpleLiteral(object);
                } else {
                    o = new SimpleLiteral(object, new URI(datatype));
                }
            } else {
                o = new SimpleURIReference(new URI(object));
            }
            return new SimpleTriple(new SimpleURIReference(new URI(subject)),
                                    new SimpleURIReference(new URI(predicate)),
                                    o);
        } catch (URISyntaxException e) {
            throw new GeneralException(e.getMessage(), e);
        }
    }

    /**
     * Saves the changes thus far to the permanent copy of the digital object.
     *
     * @param logMessage
     *        An explanation of the change(s).
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public void commit(String logMessage) throws ServerException {
        assertNotInvalidated();
        m_mgr.doCommit(Server.USE_DEFINITIVE_STORE,
                       m_context,
                       m_obj,
                       logMessage,
                       m_pendingRemoval);
        m_committed = true;
        invalidate();
    }

    public void invalidate() {
        m_invalidated = true;
    }

    /**
     * Generate a unique id for a datastream.
     */
    public String newDatastreamID() {
        return m_obj.newDatastreamID();
    }

    /**
     * Generate a unique id for a datastream version.
     */
    public String newDatastreamID(String dsID) {
        return m_obj.newDatastreamID(dsID);
    }

    /**
     * Generate a unique id for an audit record.
     */
    public String newAuditRecordID() {
        return m_obj.newAuditRecordID();
    }

    private void assertNotPendingRemoval() throws ObjectIntegrityException {
        if (m_pendingRemoval) {
            throw ERROR_PENDING_REMOVAL;
        }
    }

    private void assertNotInvalidated() throws ObjectIntegrityException {
        if (m_invalidated) {
            throw ERROR_INVALIDATED;
        }
    }

    public boolean isCommitted() {
        return m_committed;
    }

    public boolean isNew() {
        return m_obj.isNew();
    }
}
