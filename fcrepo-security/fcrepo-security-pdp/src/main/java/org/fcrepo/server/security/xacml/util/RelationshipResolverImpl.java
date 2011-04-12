/**
 *
 */

package org.fcrepo.server.security.xacml.util;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;
import org.fcrepo.server.storage.types.RelationshipTuple;


/**
 * A RelationshipResolver that resolves relationships via
 * {@link Management#getRelationships(org.fcrepo.server.Context, String, String)}.
 *
 * @author Edwin Shin
 */
public class RelationshipResolverImpl
        implements RelationshipResolver {

    private static final Logger logger =
            LoggerFactory.getLogger(RelationshipResolverImpl.class);

    /**
     * Designates the repository itself. Policies can apply to the repository,
     * but it is a special case, as it is not represented by a PID, and by
     * definition, has no parents.
     */
    private final static String REPOSITORY = "FedoraRepository";

    private static String DEFAULT_RELATIONSHIP =
            "info:fedora/fedora-system:def/relations-external#isMemberOf";

    private final List<String> relationships;

    private Management apim;

    private Context fedoraCtx;

    public RelationshipResolverImpl() {
        this(new HashMap<String, String>());
    }

    /**
     * Constructor that takes a map of parent-child predicates (relationships).
     * {@link ContextHandlerImpl} builds the map from the relationship-resolver
     * section of config-melcoe-pep.xml (in WEB-INF/classes).
     *
     * @param options
     * @throws MelcoePDPException
     */
    public RelationshipResolverImpl(Map<String, String> options) {
        relationships = new ArrayList<String>();
        // FIXME:  should add default relationship if no parent-child-relationship option is present, instead of options being empty
        if (options.isEmpty()) {
            relationships.add(DEFAULT_RELATIONSHIP);
        } else {
            List<String> keys = new ArrayList<String>(options.keySet());
            Collections.sort(keys);
            for (String s : keys) {
                if (s.startsWith("parent-child-relationship")) {
                    relationships.add(options.get(s));
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.finder.support.RelationshipResolver#buildRESTParentHierarchy
     * (java.lang.String)
     */
    public String buildRESTParentHierarchy(String pid)
            throws MelcoeXacmlException {
        Set<String> parents = getParents(pid);
        if (parents == null || parents.size() == 0) {
            return "/" + pid;
        }

        String[] parentArray = parents.toArray(new String[parents.size()]);

        return buildRESTParentHierarchy(parentArray[0]) + "/" + pid;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.finder.support.RelationshipResolver#getParents(java.
     * lang.String)
     */
    public Set<String> getParents(String pid) throws MelcoeXacmlException {
        if (logger.isDebugEnabled()) {
            logger.debug("Obtaining parents for: " + pid);
        }

        Set<String> parentPIDs = new HashSet<String>();
        if (pid.equalsIgnoreCase(REPOSITORY)) {
            return parentPIDs;
        }

        query: for (String relationship : relationships) {
            if (logger.isDebugEnabled()) {
                logger.debug("relationship query: " + pid + ", " + relationship);
            }

            Map<String, Set<String>> mapping;
            try {
                mapping = getRelationships(pid, relationship);
            } catch (MelcoeXacmlException e) {
                Throwable t = e.getCause();
                // An object X, may legitimately declare a parent relation to
                // another object, Y which does not exist. Therefore, we don't
                // want to continue querying for Y's parents.
                if (t != null && t instanceof ObjectNotInLowlevelStorageException) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Parent, " + pid + ", not found.");
                        }
                        break query;
                } else {
                // Unexpected error, so we throw back the original
                throw e;
            }
            }

            Set<String> parents = mapping.get(relationship);
            if (parents != null) {
                for (String parent : parents) {
                    PID parentPID = PID.getInstance(parent);
                    // we want the parents in demo:123 form, not info:fedora/demo:123
                    parentPIDs.add(parentPID.toString());
                    if (logger.isDebugEnabled()) {
                        logger.debug("added parent " + parentPID.toString());
                    }
                }
            }
        }
        return parentPIDs;
    }
    
    protected String getSubjectURI(String subject) throws MalformedPIDException {
        String strippedSubject;
        if (subject.startsWith(Constants.FEDORA.uri)) {
            strippedSubject = subject.substring(Constants.FEDORA.uri.length());
        } else {
            strippedSubject = subject;
        }
        String parts[] = strippedSubject.split("/");
        PID pid = new PID(parts[0]);
        String subjectURI;
        if (parts.length == 1) {
            subjectURI = pid.toURI();
        } else if (parts.length == 2) {
            subjectURI = pid.toURI() + "/" + parts[1]; // add datastream
        } else {
            logger.warn("Invalid subject argument for getRelationships: " + subject + ". Should be pid or datastream (URI form optional");
            subjectURI = null;
        }
        
        return subjectURI;
    }

    public Map<String, Set<String>> getRelationships(String subject)
            throws MelcoeXacmlException {
        return getRelationships(subject, null);
    }

    public Map<String, Set<String>> getRelationships(String subject,
                                                      String relationship)
            throws MelcoeXacmlException {

        String subjectURI;
        try {
            subjectURI = getSubjectURI(subject);
            if (subjectURI == null) {
                return new HashMap<String, Set<String>>();
            }
        } catch (MalformedPIDException e1) {
            logger.warn("Invalid subject argument for getRelationships: " + subject + ". PID part of URI is malformed");
            return new HashMap<String, Set<String>>();
        }


        RelationshipTuple[] tuples;
        try {
            tuples =
                    getApiM().getRelationships(getContext(),
                                               subjectURI,
                                               relationship);
            // Anticipating searches which fail because the object identified by
            // pid may not exist in the Fedora repository, since objects can
            // declare parent relationships to objects that do not exist
        } catch (ServerException e) {
            throw new MelcoeXacmlException(e.getMessage(), e);
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

    /**
     * Returns a PID object for the requested String. This method will return a
     * PID for a variety of pid permutations, e.g. demo:1, info:fedora/demo:1,
     * demo:1/DS1, info:fedora/demo:1/sdef:foo/sdep:bar/methodBaz.
     *
     * @param pid
     * @return a PID object
     */
    // FIXME: does not appear to be used anywhere
    protected PID getNormalizedPID(String pid) {
        // strip the leading "info:fedora/" if any
        if (pid.startsWith(Constants.FEDORA.uri)) {
            pid = pid.substring(Constants.FEDORA.uri.length());
        }
        // should be left with "demo:foo" or "demo:foo/demo:bar"

        return PID.getInstance(pid.split("\\/")[0]);
    }

    @Override
    public Set<String> getAttributesFromQuery(String query,
                                              String queryLang,
                                              String variable)
            throws MelcoeXacmlException {
        // can't run queries for a RELS relationship resolver
        logger.warn("RELS relationship resolver does not support retrieving attributes with an RI query");
        return Collections.emptySet();
    }
}
