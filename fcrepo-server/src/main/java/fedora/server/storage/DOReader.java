/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import java.io.InputStream;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;

import fedora.server.errors.ServerException;
import fedora.server.storage.types.AuditRecord;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.ObjectMethodsDef;
import fedora.server.storage.types.RelationshipTuple;

/**
 * Interface for reading Fedora digital objects from within the storage sub
 * system.
 *
 * @author Sandy Payette
 */
public interface DOReader {

    /** Gets the underlying digital object this reader is working with. */
    public DigitalObject getObject();

    /**
     * Gets the date of creation of this object.
     *
     * @return the date of creation of this object.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public Date getCreateDate() throws ServerException;

    /**
     * Gets the date of the last modification of this object.
     *
     * @return the date of the last modification of this object.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public Date getLastModDate() throws ServerException;

    /**
     * Gets the userid of the user who owns the objects.
     *
     * @return the userid
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public String getOwnerId() throws ServerException;

    /**
     * Gets the entire list of audit records for the object. Changes to the list
     * affect the underlying object if this is DOWriter.
     *
     * @return the entire list of audit records for the object.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public List<AuditRecord> getAuditRecords() throws ServerException;

    /**
     * Gets the content of the entire digital object as XML. The object will be
     * returned exactly as it is stored in the repository.
     *
     * @return the content of the entire digital object as XML.
     * @throws ServerException
     *         If there object could not be found or there was was a failure in
     *         accessing the object for any reason.
     */
    public InputStream GetObjectXML() throws ServerException;

    /**
     * Gets the content of the entire digital object as XML, with public URIs as
     * references to managed content datastreams under the custodianship of the
     * repository.
     * <p>
     * The intent of this method is to return the digital object along with
     * valid URI pointers for ALL its datastreams.
     *
     * @param format
     *        The format to export the object in. If null or "default", will use
     *        the repository's configured default export format.
     * @param exportContext
     *        The use case for export (public, migrate, archive) which results
     *        in different ways of representing datastream URLs or datastream
     *        content in the output.
     * @return the content of the entire digital object as XML, with public URIs
     *         for managed content datastreams.
     * @throws ServerException
     *         If there object could not be found or there was was a failure in
     *         accessing the object for any reason.
     * @see fedora.server.storage.translation.DOTranslationUtility#SERIALIZE_EXPORT_PUBLIC
     * @see fedora.server.storage.translation.DOTranslationUtility#SERIALIZE_EXPORT_MIGRATE
     * @see fedora.server.storage.translation.DOTranslationUtility#SERIALIZE_EXPORT_ARCHIVE
     */
    public InputStream Export(String format, String exportContext)
            throws ServerException;

    /**
     * @deprecated in Fedora 3.0, use Export() instead
     */
    @Deprecated
    public InputStream ExportObject(String format, String exportContext)
            throws ServerException;

    /**
     * Gets the PID of the digital object.
     *
     * @return the PID of the digital object.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public String GetObjectPID() throws ServerException;

    /**
     * Gets the label of the digital object.
     *
     * @return the label of the digital object.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public String GetObjectLabel() throws ServerException;

    /**
     * Gets the state of the digital object. The state indicates the status of
     * the digital object at any point in time. Valid states are: A=Active,
     * I=Inactive, D=Deleted
     *
     * @return the state of the digital object.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public String GetObjectState() throws ServerException;

    /**
     * Gets a list of the content models of the object. The strings will be
     * of the format "info:fedora/PID"
     * @return the content models of the object
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public List<String> getContentModels() throws ServerException;

    /**
     * Determins whether or not the object have the given uri as a content model.
     *
     * @param contentModel The object node of the content model
     * @return true if the object have the content model.
     */
    public boolean hasContentModel(ObjectNode contentModel) throws ServerException;

    /**
     * Gets a list of Datastream identifiers for all Datastreams in the digital
     * object. Will take a state parameter to specify that only Datastreams that
     * are in a particular state should be listed (e.g., only active Datastreams
     * with a state value of "A"). If state is given as null, all datastream ids
     * will be returned, regardless of state.
     *
     * @param state
     *        The state of the Datastreams to be listed.
     * @return a list of Datastream identifiers for all Datastreams in the
     *         digital object.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public String[] ListDatastreamIDs(String state) throws ServerException;

    /**
     * Gets the creation dates of all versions of a particular datastream, in no
     * particular order.
     *
     * @param datastreamID
     *        The datastream identifier
     * @return the creation dates.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public Date[] getDatastreamVersions(String datastreamID)
            throws ServerException;

    /**
     * Gets all datastreams as of a certain date and in a certain state. This
     * iterates through all datastreams in the object and returns only those
     * that existed at the given date/time, and currently have a certain state.
     * If the date/time given is null, the most recent version of each
     * datastream is obtained. If the state is null, all datastreams as of the
     * given time will be returned, regardless of state.
     *
     * @param versDateTime
     *        The date-time stamp to get appropriate Datastream versions
     * @param state
     *        The state, null for any.
     * @return all datastreams as of a certain date and in a certain state.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public Datastream[] GetDatastreams(Date versDateTime, String state)
            throws ServerException;

    /**
     * Gets a particular Datastream in the digital object. If the date given is
     * null, the most recent version of the datastream is given. If the date is
     * non-null, the closest version of the Datastream to the specified
     * date/time (without going over) is given. If no datastreams match the
     * given criteria, null is returned.
     *
     * @param datastreamID
     *        The Datastream identifier
     * @param versDateTime
     *        The date-time stamp to get appropriate Datastream version
     * @return a particular Datastream in the digital object.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public Datastream GetDatastream(String datastreamID, Date versDateTime)
            throws ServerException;

    /**
     * Gets a particular datastream in the digital object. This is an
     * alternative to retrieving a datastream if all that is known is the
     * version id (and not the date). The datastream id and version id must
     * match actual ids of an existing datastream in the object. Otherwise, null
     * will be returned.
     *
     * @param datastreamID
     *        The datastream identifier
     * @param versionID
     *        The identifier of the particular version
     * @return a particular Datastream in the digital object
     * @throws ServerException
     *         If any time of error occurred fulfilling the request.
     */
    public Datastream getDatastream(String datastreamID, String versionID)
            throws ServerException;

    /**
     * Gets list of ALL method definitions that are available on a particular
     * digital object. This is done by reflecting on EACH Disseminator and
     * getting the PID of the service deployment object for that disseminator.
     * The methods are reflected via the service deployment object, which is
     * implementing the methods defined in a particular by a behavior
     * definition.
     *
     * @param versDateTime
     *        The date-time stamp to get appropriate version. If this is given
     *        as null, the most recent version is used.
     * @return a list of ALL method definitions that are available on a
     *         particular digital object.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public ObjectMethodsDef[] listMethods(Date versDateTime)
            throws ServerException;

    /**
     * Gets the change history of an object by returning a list of timestamps
     * that correspond to modification dates of components. This currently
     * includes changes to datastreams and disseminators.
     *
     * @param PID
     *        The persistent identifier of the digitla object.
     * @return An Array containing the list of timestamps indicating when
     *         changes were made to the object.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public String[] getObjectHistory(String PID) throws ServerException;

    /**
     * Determine if the object contains the given relationship.
     *
     * @param subject
     *        Subject of the relationship, or null if unspecified (will match
     *        any).
     * @param predicate
     *        Predicate of the relationship, or null if unspecified (will match
     *        any).
     * @param object
     *        Object (target) of the relationship, or null if unspecified (will
     *        match any).
     * @return true if the object
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public boolean hasRelationship(SubjectNode subject, PredicateNode predicate, ObjectNode object)
            throws ServerException;
    /**
     * Determine if the object contains the given relationship, assumes pid as the subject.
     *
     * @param predicate
     *        Predicate of the relationship, or null if unspecified (will match
     *        any).
     * @param object
     *        Object (target) of the relationship, or null if unspecified (will
     *        match any).
     * @return true if the object
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public boolean hasRelationship(PredicateNode predicate, ObjectNode object)
            throws ServerException;

    /**
     * Get all RELS-EXT and RELS-INT relationships in the object.
     *
     * @return All RELS-EXT and RELS-INT relationships in the object
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    public Set<RelationshipTuple> getRelationships()
            throws ServerException;

/**
 * Get all matching RELS-EXT and RELS-INT relationships in the object.
 *
 * @param subject
 *        Subject of the relationship, or null if unspecified (will match
 *        any).
 * @param predicate
 *        Predicate of the relationship, or null if unspecified (will match
 *        any).
 * @param object
 *        Object (target) of the relationship, or null if unspecified (will
 *        match any).
 * @return All matching relationships in the object
 * @throws ServerException
 *         If any type of error occurred fulfilling the request.
 */
public Set<RelationshipTuple> getRelationships(SubjectNode subject,
                                               PredicateNode predicate,
                                               ObjectNode object)
        throws ServerException;
/**
 * Get all matching RELS-EXT relationships in the object, assumes pid is the subject
 *
 * @param predicate
 *        Predicate of the relationship, or null if unspecified (will match
 *        any).
 * @param object
 *        Object (target) of the relationship, or null if unspecified (will
 *        match any).
 * @return All matching relationships in the object
 * @throws ServerException
 *         If any type of error occurred fulfilling the request.
 */
public Set<RelationshipTuple> getRelationships(PredicateNode predicate,
                                               ObjectNode object)
        throws ServerException;
}
