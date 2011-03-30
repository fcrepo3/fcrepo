
package org.fcrepo.server.security.xacml.util;

import java.util.Map;
import java.util.Set;

import org.fcrepo.server.security.xacml.MelcoeXacmlException;


public interface RelationshipResolver {


    /**
     * Retrieves the relationships for this PID. Values for each relationship
     * are placed in a set.
     *
     * @param pid
     *        the subject to return relationships for - either ns:pid,
     *        ns:pid/datastream or the info:fedora/ forms
     * @return The map of relationships and values.
     * @throws MelcoeXacmlException
     */
    public Map<String, Set<String>> getRelationships(String subject)
            throws MelcoeXacmlException;


    public Map<String, Set<String>> getRelationships(String subject,
                                                      String relationship) throws MelcoeXacmlException;

    /**
     * Obtains a list of parents for the given pid.
     *
     * @param pid
     *        object id whose parents we wish to find
     * @return a Set containing the parents of the pid
     * @throws PEPException
     */
    // FIXME: not used?
    //public Set<String> getParents(String pid) throws MelcoeXacmlException;

    /**
     * Generates a REST based representation of an object and its parents. For
     * example, given the parameter b, and if b belongs to collection a, then we
     * will end up with /a/b
     *
     * @param pid
     *        the pid whose parents we need to find
     * @return the REST representation of the pid and its parents
     * @throws PEPException
     */
    public String buildRESTParentHierarchy(String pid)
            throws MelcoeXacmlException;
}
