package org.fcrepo.server.security.xacml.pdp.finder.policy;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.security.xacml.pdp.data.PolicyStoreException;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DOReader;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinderResult;


public class RightsMetadataPolicyFinderModule
extends PolicyFinderModule {
    private final DOManager manager;
    public RightsMetadataPolicyFinderModule(DOManager manager){
        this.manager = manager;
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new PolicyFinderResult(policy);
    }
    
    private AbstractPolicy findPolicy(EvaluationCtx context, String pid) throws PolicyStoreException {
        try {
            DOReader reader = this.manager.getReader(false, ReadOnlyContext.EMPTY, pid);
            
            InputStream is =
                    reader.getDatastream("rightsMetadata", null).getContentStream();
            byte [] bytes =  IOUtils.toByteArray(is);
        } catch (Exception e) {
            throw new PolicyStoreException("Get: error reading policy "
                                                 + pid + " - " + e.getMessage(), e);
        }

        //FIXME
        return null;
    }

}