package org.fcrepo.server.security.xacml.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.fcrepo.common.Constants;
import org.fcrepo.common.MalformedPIDException;
import org.fcrepo.common.PID;
import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class RelationshipResolverBase
        implements RelationshipResolver {


    private static final Logger logger =
        LoggerFactory.getLogger(RelationshipResolverBase.class);


    /**
     * Designates the repository itself. Policies can apply to the repository,
     * but it is a special case, as it is not represented by a PID, and by
     * definition, has no parents.
     */
    protected static final String REPOSITORY = Constants.FEDORA_REPOSITORY_PID.uri;
    protected static String DEFAULT_RELATIONSHIP = "info:fedora/fedora-system:def/relations-external#isMemberOf";
    // relationships to parent object
    protected final List<String> parentRelationships;
    // relationships to child object
    protected final List<String> childRelationships;


    /**
     * Constructor that takes a map of parent-child predicates (relationships).
     * {@link ContextHandlerImpl} builds the map from the relationship-resolver
     * section of config-melcoe-pep.xml (in WEB-INF/classes).
     *
     * @param options
     * @throws MelcoePDPException
     */
    public RelationshipResolverBase(Map<String, String> options) {
        parentRelationships = new ArrayList<String>();
        ArrayList<String> childRels = new ArrayList<String>();



        List<String> keys = new ArrayList<String>(options.keySet());
        Collections.sort(keys);
        for (String s : keys) {
            if (s.startsWith("xacml-parent-relationship")) {
                parentRelationships.add(options.get(s));
            } else if (s.startsWith("xacml-child-relationship")) {
                childRels.add(options.get(s));
            }
        }
        // always set a default parent relationship
        if (parentRelationships.isEmpty()) {
            parentRelationships.add(DEFAULT_RELATIONSHIP);
        }
        if (childRels.isEmpty()) {
           childRelationships = null;
        } else {
            childRelationships = childRels;
        }


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
            logger.warn("Invalid Fedora resource identifier: {}. PID part of URI is malformed", res);
            return null;
        }

        String resURI;
        if (parts.length == 1) {
            resURI = pid.toURI();
        } else if (parts.length == 2) {
            resURI = pid.toURI() + "/" + parts[1]; // add datastream ID back
        } else {
            logger.warn("Invalid Fedora resource identifier: {}. Should be pid or datastream (URI form optional", res);
            return null;
        }
        return resURI;
    }

}
