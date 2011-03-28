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

import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
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
public class RelationshipResolverImpl extends RelationshipResolverBase
        implements RelationshipResolver {

    static final Logger logger =
            LoggerFactory.getLogger(RelationshipResolverImpl.class);

    private Management apim;

    private Context fedoraCtx;


    public RelationshipResolverImpl() {
        super();
    }

    public RelationshipResolverImpl(Map<String, String> options) {
        super(options);
    }

    @Override
    public Map<String, Set<String>> getRelationships(String subject,
                                                      String relationship)
            throws MelcoeXacmlException {


        String subjectURI;
        if ((subjectURI = getFedoraResourceURI(subject)) == null) {
            logger.warn("Invalid subject argumet for getRelationships: " + subject + ". Should be pid or datastream (URI form optional");
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

    @Override
    public Map<String, Set<String>> getRelationships(String subject)
            throws MelcoeXacmlException {
        return getRelationships(subject, null);
    }
}
