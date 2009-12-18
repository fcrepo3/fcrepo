/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Chris Wilper
 */
public interface OAIProvider {

    /**
     * Get a human readable name for the repository.
     * 
     * @return the repository name.
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract String getRepositoryName() throws RepositoryException;

    /**
     * Get the HTTP endpoint for the OAI-PMH interface.
     * 
     * @return the base URL.
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract String getBaseURL(String protocol, String port)
            throws RepositoryException;

    /**
     * Get the version of the OAI-PMH supported by the repository.
     * 
     * @return the protocol version number.
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract String getProtocolVersion() throws RepositoryException;

    /**
     * Get a Date (in UTC) that is the guaranteed lower limit of all datestamps
     * recording changes, modifications, or deletions in the repository. A
     * repository must not use datestamps lower than this.
     * 
     * @return the earliest datetime stamp.
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract Date getEarliestDatestamp() throws RepositoryException;

    /**
     * Get the manner in which the repository supports the notion of deleted
     * records.
     * 
     * @return manner in which repository supports the notion of deletion.
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract DeletedRecordSupport getDeletedRecordSupport()
            throws RepositoryException;

    /**
     * Get the finest harvesting granularity supported by the repository.
     * 
     * @return the finest harvesting granularity supported by the repository.
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract DateGranularitySupport getDateGranularitySupport()
            throws RepositoryException;

    /**
     * Get the email addresses of administrators of the repository. This set
     * must contain at least one item.
     * 
     * @return the email addresses of administrators of the repository.
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract Set getAdminEmails() throws RepositoryException;

    /**
     * Get the compression encodings supported by the repository. This set may
     * be empty. Recommended values are those in RFC 2616 Section 14.11
     * 
     * @return the compression encodings supported by the repository.
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract Set getSupportedCompressionEncodings()
            throws RepositoryException;

    /**
     * Get XML descriptions of the repository. Each Set element must be a String
     * containing a description according to some W3C schema, where the
     * xsi:schemaLocation attribute is used on the root element. See
     * http://www.openarchives.org/OAI/2.0/guidelines.htm for guidelines
     * regarding these repository-level descriptions.
     * 
     * @return XML descriptions of the repository.
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract Set getDescriptions() throws RepositoryException;

    /**
     * Get an individual metadata record from the repository.
     * 
     * @param identifier
     *        the record identifier.
     * @param metadataPrefix
     *        the metadata prefix identifier.
     * @return an individual metadata record from the repository.
     * @throws CannotDisseminateFormatException
     * @throws IDDoesNotExistException
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract Record getRecord(String identifier, String metadataPrefix)
            throws CannotDisseminateFormatException, IDDoesNotExistException,
            RepositoryException;

    /**
     * Get the Records in the repository matching the given criteria. Any of the
     * arguments (except metadataPrefix) may be null, indicating "any". If the
     * size of the returned list is over getMaxRecords(), the last element is a
     * resumptionToken (a String) which can be used to get the rest of the list.
     * 
     * @param from
     *        the beginning date of the date range.
     * @param until
     *        the ending date of the date range.
     * @param metadataPrefix
     *        the metadata prefix.
     * @param set
     *        the set of criteria
     * @return the Records in the repository matching the given criteria.
     * @throws CannotDisseminateFormatException
     * @throws NoRecordsMatchException
     * @throws NoSetHierarchyException
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract List getRecords(Date from,
                                    Date until,
                                    String metadataPrefix,
                                    String set)
            throws CannotDisseminateFormatException, NoRecordsMatchException,
            NoSetHierarchyException, RepositoryException;

    /**
     * Get the remaining portion of a set of Records. If the size of the
     * returned list is over getMaxRecords(), the last element is another
     * resumptionToken (a String) which can be used to get the rest of the list.
     * 
     * @param resumptionToken
     *        a string that can be used to get the rest of the list.
     * @return the remaining portion of a set of Records.
     * @throws CannotDisseminateFormatException
     * @throws NoRecordsMatchException
     * @throws NoSetHierarchyException
     * @throws BadResumptionTokenException
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract List getRecords(String resumptionToken)
            throws CannotDisseminateFormatException, NoRecordsMatchException,
            NoSetHierarchyException, BadResumptionTokenException,
            RepositoryException;

    /**
     * Just like getRecords, but returns Header objects. If the size of the
     * returned list is over getMaxHeaders(), the last element is a
     * resumptionToken (a String) which can be used to get the rest of the list.
     * 
     * @param from
     *        the beginning date range.
     * @param until
     *        the end of the date range.
     * @param metadataPrefix
     *        the metadata prefix.
     * @param set
     *        the set of criteria.
     * @return the remaining portion of a set of Header objects.
     * @throws CannotDisseminateFormatException
     * @throws NoRecordsMatchException
     * @throws NoSetHierarchyException
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract List getHeaders(Date from,
                                    Date until,
                                    String metadataPrefix,
                                    String set)
            throws CannotDisseminateFormatException, NoRecordsMatchException,
            NoSetHierarchyException, RepositoryException;

    /**
     * Get the remaining portion of a set of Headers. If the size of the
     * returned list is over getMaxHeaders() the last element is another
     * resumptionToken (a String) which can be used to get the rest of the list.
     * 
     * @param resumptionToken
     *        a string that can be used to get the rest of the list.
     * @return the remaining portion of a set of Headers.
     * @throws CannotDisseminateFormatException
     * @throws NoRecordsMatchException
     * @throws NoSetHierarchyException
     * @throws BadResumptionTokenException
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract List getHeaders(String resumptionToken)
            throws CannotDisseminateFormatException, NoRecordsMatchException,
            NoSetHierarchyException, BadResumptionTokenException,
            RepositoryException;

    /**
     * Get the setSpecs, setNames, and setDescriptions of sets in the
     * repository. Each set has a setSpec, a name, and zero or more
     * descriptions, held by a SetInfo object. If the size of the returned list
     * is over getMaxSets(), the last element is a resumptionToken (a String)
     * which can be used to get the rest of the list.
     * 
     * @return the setSpecs, setNames, and setDescriptions of sets in the
     *         repository.
     * @throws NoSetHierarchyException
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract List getSets() throws NoSetHierarchyException,
            RepositoryException;

    /**
     * Get the remaining portion of a set of Sets. If the size of the returned
     * list is over getMaxSets(), the last element is another resumptionToken (a
     * String) which can be used to get the rest of the list.
     * 
     * @param resumptionToken
     *        a string that can be used to get the rest of the list.
     * @return the remaining portion of a set of Sets.
     * @throws BadResumptionTokenException
     * @throws NoSetHierarchyException
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract List getSets(String resumptionToken)
            throws BadResumptionTokenException, NoSetHierarchyException,
            RepositoryException;

    /**
     * Get the MetadataFormats supported across the repository or for an
     * individual item in the repository.
     * 
     * @param id
     *        The item identifier, or null, meaning "the entire repository"
     * @return the set of metadata formats supported by the repository.
     * @throws IDDoesNotExistException
     * @throws NoMetadataFormatsException
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract Set getMetadataFormats(String id)
            throws IDDoesNotExistException, NoMetadataFormatsException,
            RepositoryException;

    /**
     * Get the maximum number of sets that are returned at a time. A negative
     * value signifies no maximum.
     * 
     * @return the maximum number of sets that are returned at a time.
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract long getMaxSets() throws RepositoryException;

    /**
     * Get the maximum number of records that are returned at a time. A negative
     * value signifies no maximum.
     * 
     * @return the maximum number of records that are returned at a time.
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract long getMaxRecords() throws RepositoryException;

    /**
     * Get the maximum number of headers that are returned at a time. A negative
     * value signifies no maximum.
     * 
     * @return the maximum number of headers that are returned at a time.
     * @throws RepositoryException
     *         if an error has occurred.
     */
    public abstract long getMaxHeaders() throws RepositoryException;
}
