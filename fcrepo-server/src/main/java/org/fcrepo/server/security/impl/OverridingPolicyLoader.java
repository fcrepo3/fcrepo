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


public class OverridingPolicyLoader extends PolicyLoader {

    private static final Logger logger =
            LoggerFactory.getLogger(OverridingPolicyLoader.class);

    public static enum Strategy { FILENAME, ID }

    private Strategy m_override = Strategy.ID;

    public OverridingPolicyLoader(RepositoryReader repoReader) {
        super(repoReader);
    }

    public void setOverride(Strategy override) {
        m_override = override;
    }

    @Override
    public Map<String, AbstractPolicy> loadPolicies(PolicyParser policyParser, boolean validate, File dir)
            throws IOException, ValidationException {
        Map<String,AbstractPolicy> policies = new HashMap<String,AbstractPolicy>();
        File defaultDir = new File(dir,"default");
        if (defaultDir.exists()){
            policies.putAll(loadPolicies(policyParser, validate,defaultDir));
        }
        for (File file: dir.listFiles()) {
            if (!file.getName().equals("default")){
                if (file.isDirectory()) {
                    policies.putAll(loadPolicies(policyParser, validate, file));
                } else {
                    if (file.getName().endsWith(".xml")) {
                        logger.info("Loading policy: {}", file.getPath());
                        InputStream policyStream = new FileInputStream(file);
                        AbstractPolicy policy = policyParser.parse(policyStream, validate);
                        logger.info("Loaded policy ID: {}", policy.getId());
                        String key = null;
                        if (m_override == Strategy.FILENAME) {
                            key = file.getName();
                        }
                        else key = policy.getId().toString();
                        policies.put(key, policy);
                    }
                }
            }
        }
        return policies;
    }

}