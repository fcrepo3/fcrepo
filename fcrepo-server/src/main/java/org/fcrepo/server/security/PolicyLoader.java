package org.fcrepo.server.security;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.ValidationException;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Datastream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jboss.security.xacml.sunxacml.AbstractPolicy;


public abstract class PolicyLoader {
    private static final Logger logger =
            LoggerFactory.getLogger(PolicyLoader.class);


    protected final RepositoryReader m_repoReader;

    public PolicyLoader(RepositoryReader repoReader) {
        m_repoReader = repoReader;
    }

    // load and parse all policies (*.xml) from a given directory, recursively
    public abstract Map<String,AbstractPolicy> loadPolicies(PolicyParser policyParser, boolean validate, File dir)
            throws IOException, ValidationException;

    // if the object exists and has a POLICY datastream, parse and return it
    // the passed parser must be safe to use in this thread
    protected AbstractPolicy loadObjectPolicy(PolicyParser policyParser, String pid, boolean validate) throws ServerException {
        try {
            DOReader reader = m_repoReader.getReader(Server.USE_DEFINITIVE_STORE,
                                                     ReadOnlyContext.EMPTY,
                                                     pid);
            Datastream ds = reader.GetDatastream("POLICY", null);
            if (ds != null) {
                logger.debug("Using POLICY for {}", pid);
                return policyParser.parse(ds.getContentStream(), validate);
            } else {
                return null;
            }
        } catch (ObjectNotInLowlevelStorageException e) {
            return null;
        }
    }
}