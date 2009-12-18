/*
 * File: DBPolicyFinderModule.java
 *
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package melcoe.xacml.pdp.finder.policy;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import melcoe.xacml.pdp.data.PolicyDataManagerException;

import org.apache.log4j.Logger;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.ctx.Status;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinderResult;

/**
 * This is the PolicyFinderModule for the PDP. Its purpose is to basically find
 * policies. It interacts with a PolicyManager in order to obtain a policy or
 * policy set.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class GenericPolicyFinderModule
        extends PolicyFinderModule {

    private static final Logger log =
            Logger.getLogger(GenericPolicyFinderModule.class.getName());

    private PolicyManager policyManager = null;

    public GenericPolicyFinderModule() {
        super();
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

    /**
     * Initialize this module. Typically this is called by
     * <code>PolicyFinder</code> when a PDP is created.
     * 
     * @param finder
     *        the <code>PolicyFinder</code> using this module
     */
    @Override
    public void init(PolicyFinder finder) {
        try {
            policyManager = new PolicyManager(finder);
        } catch (URISyntaxException use) {
            log
                    .fatal("Error initialising DBPolicyFinderModule due to improper URI:",
                           use);
        } catch (PolicyDataManagerException pdme) {
            log.fatal("Error initialising DBPolicyFinderModule:", pdme);
        }
    }

    /**
     * Finds a policy based on a request's context. If more than one policy
     * matches, then this either returns an error or a new policy wrapping the
     * multiple policies (depending on which constructor was used to construct
     * this instance).
     * 
     * @param context
     *        the representation of the request data
     * @return the result of trying to find an applicable policy
     */
    @Override
    public PolicyFinderResult findPolicy(EvaluationCtx context) {
        try {
            AbstractPolicy policy = policyManager.getPolicy(context);

            if (policy == null) {
                return new PolicyFinderResult();
            }

            return new PolicyFinderResult(policy);
        } catch (TopLevelPolicyException tlpe) {
            return new PolicyFinderResult(tlpe.getStatus());
        } catch (PolicyDataManagerException pdme) {
            if (log.isDebugEnabled()) {
                log.debug("problem processing policy", pdme);
            }

            List<String> codes = new ArrayList<String>();
            codes.add(Status.STATUS_PROCESSING_ERROR);
            return new PolicyFinderResult(new Status(codes, pdme.getMessage()));
        }
    }
}
