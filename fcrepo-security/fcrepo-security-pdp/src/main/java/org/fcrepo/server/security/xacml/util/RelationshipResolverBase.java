package org.fcrepo.server.security.xacml.util;

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

import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;


public abstract class RelationshipResolverBase
        implements RelationshipResolver {


    private static final Logger logger =
        LoggerFactory.getLogger(RelationshipResolverBase.class);


    /**
     * Designates the repository itself. Policies can apply to the repository,
     * but it is a special case, as it is not represented by a PID, and by
     * definition, has no parents.
     */
    private static final String REPOSITORY = "FedoraRepository";
    protected static String DEFAULT_RELATIONSHIP = "info:fedora/fedora-system:def/relations-external#isMemberOf";
    protected final List<String> relationships;

    public RelationshipResolverBase() {
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
    public RelationshipResolverBase(Map<String, String> options) {
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

    /**
     * given either a ns:pid/ds identifier or an info:fedora/ URI form of the same
     * return the URI form, validating the PID part.  Returns null if the argument
     * was invalid/could not be validated/converted
     *
     * @param res identifier for fedora resource
     * @return URI form
     */
    protected String getFedoraResourceURI(String res) {
        // strip off info URI part if present
        String strippedRes;
        if (res.startsWith(Constants.FEDORA.uri)) {
            strippedRes = res.substring(Constants.FEDORA.uri.length());
        } else {
            strippedRes = res;
        }
        // split into pid + datastream (if present), validate PID and then recombine datastream back in using URI form of PID
        String parts[] = strippedRes.split("/");
        PID pid;
        try {
            pid = new PID(parts[0]);
        } catch (MalformedPIDException e1) {
            logger.warn("Invalid Fedora resource identifier: " + res + ". PID part of URI is malformed");
            return null;
        }

        String resURI;
        if (parts.length == 1) {
            resURI = pid.toURI();
        } else if (parts.length == 2) {
            resURI = pid.toURI() + "/" + parts[1]; // add datastream ID back
        } else {
            logger.warn("Invalid Fedora resource identifier: " + res + ". Should be pid or datastream (URI form optional");
            return null;
        }
        return resURI;
    }

}
