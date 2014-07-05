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

package org.fcrepo.server.security.xacml.pdp.finder.policy;

import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.List;

import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;
import org.fcrepo.server.security.xacml.pdp.data.PolicyIndex;
import org.fcrepo.server.security.xacml.pdp.data.PolicyIndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jboss.security.xacml.sunxacml.AbstractPolicy;
import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.combine.PolicyCombiningAlgorithm;
import org.jboss.security.xacml.sunxacml.ctx.Status;
import org.jboss.security.xacml.sunxacml.finder.PolicyFinder;
import org.jboss.security.xacml.sunxacml.finder.PolicyFinderModule;
import org.jboss.security.xacml.sunxacml.finder.PolicyFinderResult;

/**
 * This is the PolicyFinderModule for the PDP. Its purpose is to basically find
 * policies. It interacts with a PolicyManager in order to obtain a policy or
 * policy set.
 *
 * @author nishen@melcoe.mq.edu.au
 */
public class GenericPolicyFinderModule
        extends PolicyFinderModule {

    private static final Logger logger =
            LoggerFactory.getLogger(GenericPolicyFinderModule.class);

    private PolicyIndex m_policyIndex;
    private PolicyCombiningAlgorithm m_combiningAlg;
    private PolicyManager m_policyManager = null;

    public GenericPolicyFinderModule(PolicyIndex policyIndex, PolicyCombiningAlgorithm combiningAlg)
        throws PolicyIndexException, URISyntaxException {
        super();
        m_policyIndex = policyIndex;
        m_combiningAlg = combiningAlg;
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

     */
    public void init() throws MelcoePDPException {
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
            AbstractPolicy policy = m_policyManager.getPolicy(context);

            if (policy == null) {
                return new PolicyFinderResult();
            }

            return new PolicyFinderResult(policy);
        } catch (TopLevelPolicyException tlpe) {
            return new PolicyFinderResult(tlpe.getStatus());
        } catch (PolicyIndexException pdme) {
            if (logger.isDebugEnabled()) {
                logger.debug("problem processing policy", pdme);
            }

            List<String> codes = new ArrayList<String>();
            codes.add(Status.STATUS_PROCESSING_ERROR);
            return new PolicyFinderResult(new Status(codes, pdme.getMessage()));
        }
    }

    @Override
    /**
     * This callback is invoked by the policyFinder after adding its modules
     */
    public void init(PolicyFinder policyFinder) {
        m_policyManager = new PolicyManager(m_policyIndex, m_combiningAlg, policyFinder);
    }
}