package org.fcrepo.server.security.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.fcrepo.server.errors.ValidationException;
import org.fcrepo.server.security.PolicyParser;
import org.fcrepo.server.security.PolicyLoader;
import org.fcrepo.server.storage.RepositoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.AbstractPolicy;


public class SimplePolicyLoader extends PolicyLoader {
    private static final Logger logger =
            LoggerFactory.getLogger(SimplePolicyLoader.class);

    public SimplePolicyLoader(RepositoryReader repoReader) {
        super(repoReader);
    }

    @Override
    public Map<String, AbstractPolicy> loadPolicies(PolicyParser policyParser, boolean validate, File dir)
            throws IOException, ValidationException {
        Map<String,AbstractPolicy> policies = new HashMap<String,AbstractPolicy>();
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) {
                policies.putAll(loadPolicies(policyParser, validate, file));
            } else {
                if (file.getName().endsWith(".xml")) {
                    logger.info("Loading policy: {}", file.getPath());
                    InputStream policyStream = new FileInputStream(file);
                    policies.put(file.getPath(), policyParser.parse(policyStream, validate));
                }
            }
        }
        return policies;
    }

}