package org.fcrepo.server.security.xacml.pdp.finder.policy.rightsMetadata;

import java.io.InputStream;

import java.util.Map;

import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.security.xacml.pdp.data.PolicyStoreException;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.types.Datastream;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinderResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RightsMetadataPolicyFinderModule
extends PolicyFinderModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(RightsMetadataPolicyFinderModule.class);
    private final DOManager manager;
    private final Map<String,String> actionMap;
    public RightsMetadataPolicyFinderModule(Server server, Map<String,String> actionMap){
        this.manager = server.getBean("org.fcrepo.server.storage.DOManager",DOManager.class);
        this.actionMap = actionMap;
    }

    @Override
    public void init(PolicyFinder arg0) {
            
    }
    
    @Override
    public PolicyFinderResult findPolicy(EvaluationCtx context) {
        String pid = org.fcrepo.server.security.PolicyFinderModule.getPid(context);
        AbstractPolicy policy = null;
        try {
            policy = findPolicy(context, pid);
        } catch (PolicyStoreException e) {
            LOGGER.error("There's an error here!", e);
        }
        if (policy == null) {
            return new PolicyFinderResult();
        }
        return new PolicyFinderResult(policy);
    }
    
    private AbstractPolicy findPolicy(EvaluationCtx context, String pid) throws PolicyStoreException {
        if (pid.equals( "FedoraRepository")) return null; // It's a trap! This attribute is reused for repo-wide policies
        try {
            LOGGER.debug("finding policy for object with pid={}", pid);
            DOReader reader = this.manager.getReader(false, ReadOnlyContext.EMPTY, pid);
            Datastream rightsMD = reader.GetDatastream("rightsMetadata", null);
            if (rightsMD != null) {
                InputStream is =
                    rightsMD.getContentStream();
                LOGGER.debug("located rightsMetadata DS for object with pid={}", pid);
                return new RightsMetadataPolicy(pid,this.actionMap,is);
            } else {
                LOGGER.debug("could not locate rightsMetadata DS for object with pid={}", pid);
                return null;
            }
        } catch (Exception e) {
            throw new PolicyStoreException("Get: error reading policy "
                                                 + pid + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * Always returns <code>true</code> since this module does support finding
     * policies based on context.
     *
     * @return true
     */
    @Override
    public boolean isRequestSupported() {
        return true;
    }


}