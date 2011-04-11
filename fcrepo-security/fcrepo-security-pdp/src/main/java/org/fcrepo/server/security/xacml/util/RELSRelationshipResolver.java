/**
 *
 */

package org.fcrepo.server.security.xacml.util;

import java.io.File;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;
import org.fcrepo.common.MalformedPIDException;
import org.fcrepo.common.PID;

import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.management.Management;
import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.storage.types.RelationshipTuple;


/**
 * A RelationshipResolver that resolves relationships via
 * {@link Management#getRelationships(org.fcrepo.server.Context, String, String)}.
 *
 * @author Edwin Shin
 */
public class RELSRelationshipResolver extends RelationshipResolverBase
        implements RelationshipResolver {

    private static final Logger logger =
            LoggerFactory.getLogger(RELSRelationshipResolver.class);

    private Management apim;

    private Context fedoraCtx;


    public RELSRelationshipResolver() {
        this(new HashMap<String, String>()) ;
    }

    public RELSRelationshipResolver(Map<String, String> options) {
        super(options);
    }

    @Override
    public Map<String, Set<String>> getRelationships(String subject,
                                                      String relationship)
            throws MelcoeXacmlException {

        if (subject == null){
            logger.warn("Invalid subject argument for getRelationships.  Subject cannot be null");
        return new HashMap<String, Set<String>>();
        }

        String subjectURI;
        if ((subjectURI = getFedoraResourceURI(subject)) == null) {
            logger.warn("Invalid subject argument for getRelationships: " + subject + ". Should be pid or datastream (URI form optional");
            return new HashMap<String, Set<String>>();
        }

        RelationshipTuple[] tuples;
        try {
            tuples =
                    getApiM().getRelationships(getContext(),
                                               subjectURI,
                                               relationship);
        } catch (ServerException e) {
            if (e instanceof ObjectNotInLowlevelStorageException) {
                // querying a subject that doesn't exist
                return new HashMap<String, Set<String>>();
            } else {
                throw new MelcoeXacmlException(e.getMessage(), e);
            }
        }

        Map<String, Set<String>> relationships =
                new HashMap<String, Set<String>>();
        for (RelationshipTuple t : tuples) {
            String p = t.predicate;
            String o = t.object;

            Set<String> values = relationships.get(p);
            if (values == null) {
                values = new HashSet<String>();
            }
            values.add(o);
            relationships.put(p, values);
        }
        return relationships;
    }

    private Management getApiM() {
        if (apim != null) {
            return apim;
        }
        Server server;
        try {
            server = Server.getInstance(new File(Constants.FEDORA_HOME), false);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException("Failed getting instance of Fedora", e);
        }
        apim =
                (Management) server
                        .getModule("org.fcrepo.server.management.Management");
        return apim;
    }

    private Context getContext() throws MelcoeXacmlException {
        if (fedoraCtx != null) {
            return fedoraCtx;
        }
        try {
            fedoraCtx =
                    ReadOnlyContext.getContext(null,
                                               null,
                                               null,
                                               ReadOnlyContext.DO_OP);
        } catch (Exception e) {
            throw new MelcoeXacmlException(e.getMessage(), e);
        }
        return fedoraCtx;
    }

    @Override
    public Map<String, Set<String>> getRelationships(String subject)
            throws MelcoeXacmlException {
        return getRelationships(subject, null);
    }

    @Override
    public String buildRESTParentHierarchy(String pid)
    throws MelcoeXacmlException {
        Set<String> parents = getParents(pid);
        if (parents == null || parents.size() == 0) {
            return "/" + pid;
        }

        String[] parentArray = parents.toArray(new String[parents.size()]);

        // FIXME: always uses the first parent.  If/when we allow multiple hierarchies this needs changing to return all hierarchies
        return buildRESTParentHierarchy(parentArray[0]) + "/" + pid;
    }

    protected Set<String> getParents(String pid) throws MelcoeXacmlException {
        if (logger.isDebugEnabled()) {
            logger.debug("Obtaining parents for: " + pid);
        }

        Set<String> parentPIDs = new HashSet<String>();
        // repository "object" has no parents
        if (pid.equalsIgnoreCase(REPOSITORY)) {
            return parentPIDs;
        }

        if (childRelationships != null) {
            logger.warn("Parent-child relationships have been specified, but the specified relationship resolver is not able to resolve these");
        }

        // if more than one relationship specified, get all relationships for object in one go
        // otherwise constrain on the single relationship
        // means object/rels datastreams only get parsed once - a separate
        // query for each relationship would mean a separate object parse (and RELS-* parse) for each relationship
        Map<String, Set<String>> allRelationships;
        if (parentRelationships.size() == 1) {
            allRelationships = getRelationships(pid, parentRelationships.get(0));
        } else {
            allRelationships = getRelationships(pid);
        }
        for (String rel : allRelationships.keySet()) {
            // is it one of the child-parent relationships?
            if (parentRelationships.contains(rel)) {
                Set<String> parents = allRelationships.get(rel);
                if (parents != null) {
                    for (String parent : parents) {
                        try {
                        PID parentPID = new PID(parent);
                        // we want the parents in demo:123 form, not info:fedora/demo:123
                        parentPIDs.add(parentPID.toString());
                        if (logger.isDebugEnabled()) {
                            logger.debug("added parent " + parentPID.toString());
                        }
                        } catch (MalformedPIDException e) {
                            // target of relationship isn't a PID
                            logger.warn("Triple " + pid + " " + rel + " " + parent + " does not have a digital object as its target");

                        }
                    }
                }
            }
        }
        return parentPIDs;
    }

    @Override
    public Set<String> getAttributesFromQuery(String query,
                                              String queryLang,
                                              String variable)
            throws MelcoeXacmlException {
        // can't run queries for a RELS relationship resolver
        logger.warn("RELS relationship resolver does not support retrieving attributes with an RI query");
        return new HashSet<String>();
    }
}
