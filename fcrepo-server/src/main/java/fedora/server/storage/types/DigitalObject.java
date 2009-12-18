/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;

/**
 * Java representation of a Fedora digital object.
 * <p>
 * A DigitalObject instance may be used by DOReader and DOWriter instances as
 * temporary storage for an object's attributes and components.
 * </p>
 * <p>
 * Implementations of this interface are responsible for temporary storage of
 * these items, by whatever mechanism they deem fit. The most obvious
 * implementation would simply store everything in memory.
 * </p>
 * <p>
 * Implementations of this interface are <b>not</b> responsible for any sort of
 * validation on these items, or serialization/deserialization to/from specific
 * formats.
 * </p>
 *
 * @author Chris Wilper
 */
public interface DigitalObject {

    public boolean isNew();

    public void setNew(boolean isNew);

    /**
     * Gets the pid.
     *
     * @return The pid, or null if it hasn't been set.
     */
    public String getPid();

    /**
     * Sets the pid.
     *
     * @param pid
     *        The pid.
     */
    public void setPid(String pid);

    /**
     * Gets the state.
     *
     * @return The state, or null if it hasn't been set.
     */
    public String getState();

    /**
     * Sets the state.
     *
     * @param state
     *        The state.
     */
    public void setState(String state);

    /**
     * Gets the userid of the user who owns the object.
     *
     * @return The userid
     */
    public String getOwnerId();

    /**
     * Sets the owner of the object.
     *
     * @param user
     *        The userid.
     */
    public void setOwnerId(String owner);

    /**
     * Gets the label.
     *
     * @return The label, or null if it hasn't been set.
     */
    public String getLabel();

    /**
     * Sets the label.
     *
     * @param label
     *        The label.
     */
    public void setLabel(String label);

    /**
     * Gets the date the object was created.
     *
     * @return The date, or null if it hasn't been set.
     */
    public Date getCreateDate();

    /**
     * Sets the date the object was created.
     *
     * @param createDate
     *        The date.
     */
    public void setCreateDate(Date createDate);

    /**
     * Gets the date the object was last modified.
     *
     * @return The date, or null if it hasn't been set.
     */
    public Date getLastModDate();

    /**
     * Sets the date the object was last modified.
     *
     * @param lastModDate
     *        The date.
     */
    public void setLastModDate(Date lastModDate);

    /**
     * Gets this object's mutable List of AuditRecord objects.
     *
     * @return The List of AuditRecords, possibly of zero size but never null.
     */
    public List<AuditRecord> getAuditRecords();

    /**
     * Gets an Iterator over the datastream ids in this object.
     * <p>
     * </p>
     * The Iterator is not tied to the underlying Collection and cannot be used
     * to remove datastreams.
     *
     * @return A new Iterator of datastream ids, possibly of zero size but never
     *         null.
     */
    public Iterator<String> datastreamIdIterator();

    /**
     * Gets an interable view that consists of versions of the same datastream
     * that is identified by the requested datastream identifier.
     * <p>
     * Datastreams within any iterators produced here are references to the
     * actual datastreams in this DigitalObject, so modifying their contents is
     * a persistent change. However, remove() is disabled, so to remove a
     * datastream from the object, use
     * {@link #removeDatastreamVersion(Datastream)}
     * <p>
     *
     * @param id
     *        The datastream id.
     * @return The list, possibly of zero size but never null.
     */
    public Iterable<Datastream> datastreams(String id);

    /**
     * Adds a datastream to a digital object, respecting the versionable flag of
     * that datastream. Appending a new version of the datastream if the
     * datastream is marked as versionable or replacing the existing version(s)
     * of the datastream is it is marked as non-versionable identifier.
     *
     * @param ds
     *        The datastream to add.
     * @param addNewVersion
     *        Controls whether to add a new version, or replace existing
     *        version.
     */
    public void addDatastreamVersion(Datastream ds, boolean addNewVersion);

    /**
     * Removes a datastream from a digital object.
     *
     * @param ds
     *        Datastream to remove.
     */
    public void removeDatastreamVersion(Datastream ds);

    /**
     * Gets an Iterator over the disseminator ids in this object.
     * <p>
     * The Iterator is not tied to the underlying Collection and cannot be used
     * to remove datastreams.
     * </p>
     *
     * @return A new Iterator of disseminator ids, possibly of zero size but
     *         never null.
     */
    @Deprecated
    public Iterator<String> disseminatorIdIterator();

    /**
     * Gets a mutable List that consists of versions of the same disseminator
     * which is identified by the requested disseminator identifier.
     *
     * @param id
     *        The disseminator id.
     * @return The list, possibly of zero size but never null.
     */
    @Deprecated
    public List<Disseminator> disseminators(String id);

    /**
     * Generate a unique id for a datastream.
     */
    public String newDatastreamID();

    /**
     * Generate a unique id for a datastream version.
     */
    public String newDatastreamID(String dsID);

    /**
     * Generate a unique id for an audit record.
     */
    public String newAuditRecordID();

    /**
     * Sets an extended property on the object.
     *
     * @param propName
     *        The property name, either a string, or URI as string.
     */
    public void setExtProperty(String propName, String propValue);

    /**
     * Gets an extended property value, given the property name.
     *
     * @return The property value.
     */
    public String getExtProperty(String propName);

    /**
     * Gets a Map containing all of the extended properties on the object. Map
     * key is property name.
     *
     * @return The property Map.
     */
    public Map<String, String> getExtProperties();

    /**
     * Determine if the object contains the given relationship.
     * <p>
     * Returns results that are accurate for the current state of the object at
     * the time of invocation. Thus, if there is some change to the object that
     * changes the set of relationships contained within, the next call to
     * hasRelationship will reflect those changes.
     * </p>
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
     */
    public boolean hasRelationship(SubjectNode subject, PredicateNode predicate, ObjectNode object);

    /**
     * Determine if the object contains the given relationship, assumes pid is the subject
     * <p>
     * Returns results that are accurate for the current state of the object at
     * the time of invocation. Thus, if there is some change to the object that
     * changes the set of relationships contained within, the next call to
     * hasRelationship will reflect those changes.
     * </p>
     *
     * @param predicate
     *        Predicate of the relationship, or null if unspecified (will match
     *        any).
     * @param object
     *        Object (target) of the relationship, or null if unspecified (will
     *        match any).
     * @return true if the object
     */
    public boolean hasRelationship(PredicateNode predicate, ObjectNode object);


    /**
     * Get all RELS-EXT and RELS-INT relationships in the object.
     * <p>
     * Returns results that are accurate for the current state of the object at
     * the time of invocation. Thus, if there is some change to the object that
     * changes the set of relationships contained within, the next call to
     * getRelationships will reflect those changes.
     * </p>
     *
     * @return All matching relationships in the object
     */
    public Set<RelationshipTuple> getRelationships();

    /**
     * Get all matching RELS-EXT and RELS-INT relationships in the object.
     * <p>
     * Returns results that are accurate for the current state of the object at
     * the time of invocation. Thus, if there is some change to the object that
     * changes the set of relationships contained within, the next call to
     * getRelationships will reflect those changes.
     * </p>
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
     * @return All RELS-EXT and RELS-INT relationships in the object
     */
    public Set<RelationshipTuple> getRelationships(SubjectNode subject,
                                                   PredicateNode predicate,
                                                   ObjectNode object);
    /**
     * Get all matching RELS-EXT relationships in the object, assumes pid is the subject
     * <p>
     * Returns results that are accurate for the current state of the object at
     * the time of invocation. Thus, if there is some change to the object that
     * changes the set of relationships contained within, the next call to
     * getRelationships will reflect those changes.
     * </p>
     *
     * @param predicate
     *        Predicate of the relationship, or null if unspecified (will match
     *        any).
     * @param object
     *        Object (target) of the relationship, or null if unspecified (will
     *        match any).
     * @return All RELS-EXT and RELS-INT relationships in the object
     */
    public Set<RelationshipTuple> getRelationships(PredicateNode predicate,
                                                   ObjectNode object);


    /**
     * Gets a list of the content models of the object. The strings will be
     * of the format "info:fedora/PID"
     * @return the content models of the object
     */
    public List<String> getContentModels();

    /**
     * Determins whether or not the object have the given uri as a content model.
     *
     * @param contentModel The object node of the content model
     * @return true if the object have the content model.
     */
    public boolean hasContentModel(ObjectNode contentModel);

}
