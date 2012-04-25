/*
 * File: PolicyManager.java
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.fcrepo.server.security.xacml.pdp.data.PolicyIndex;
import org.fcrepo.server.security.xacml.pdp.data.PolicyIndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.MatchResult;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.PolicySet;
import com.sun.xacml.Target;
import com.sun.xacml.TargetMatch;
import com.sun.xacml.TargetSection;
import com.sun.xacml.combine.PolicyCombiningAlgorithm;
import com.sun.xacml.ctx.Status;
import com.sun.xacml.finder.PolicyFinder;

/**
 * This class interacts with the policy cache on behalf of the PolicyFinder
 * modules. It also does the matching of the policies and creation of policy
 * sets.
 *
 * @author nishen@melcoe.mq.edu.au
 */
public class PolicyManager {

    private static final Logger logger =
            LoggerFactory.getLogger(PolicyManager.class);

    private final PolicyIndex m_policyIndex;

    private PolicyCombiningAlgorithm m_combiningAlg = null;
    
    private final PolicyFinder m_policyFinder;

    private Target m_target = null;

    //private PolicyReader m_policyReader = null;

    // the policy identifier for any policy sets we dynamically create
    private static final String PARENT_POLICY_ID =
            "urn:com:sun:xacml:support:finder:dynamic-policy-set";

    private static URI parentPolicyId = null;
    static {
        try{
            parentPolicyId = new URI(PARENT_POLICY_ID);
        } catch (Exception e) {}
    }

    /**
     * This constructor creates a PolicyManager instance. It takes a
     * PolicyFinder its argument. Its purpose is to obtain a set of policies
     * from the Policy Index , match them against the evaluation context and
     * return a policy or policy set that conforms to the evaluation context.
     *
     * @param polFinder
     *        the policy finder
     * @throws URISyntaxException
     * @throws {@link PolicyStoreException}
     */
    public PolicyManager(PolicyIndex policyIndex, PolicyCombiningAlgorithm combiningAlg,
                         PolicyFinder policyFinder) {

        m_policyIndex = policyIndex;
        m_combiningAlg = combiningAlg;
        m_policyFinder = policyFinder;
        m_target =
                new Target(new TargetSection(null,
                                             TargetMatch.SUBJECT,
                                             PolicyMetaData.XACML_VERSION_2_0),
                           new TargetSection(null,
                                             TargetMatch.RESOURCE,
                                             PolicyMetaData.XACML_VERSION_2_0),
                           new TargetSection(null,
                                             TargetMatch.ACTION,
                                             PolicyMetaData.XACML_VERSION_2_0),
                           new TargetSection(null,
                                             TargetMatch.ENVIRONMENT,
                                             PolicyMetaData.XACML_VERSION_2_0));
    }
    
    /**
     * Obtains a policy or policy set of matching policies from the policy
     * store. If more than one policy is returned it creates a dynamic policy
     * set that contains all the applicable policies.
     *
     * @param eval
     *        the Evaluation Context
     * @return the Policy/PolicySet that applies to this EvaluationCtx
     * @throws TopLevelPolicyException
     * @throws {@link PolicyStoreException}
     */
    public AbstractPolicy getPolicy(EvaluationCtx eval)
            throws TopLevelPolicyException, PolicyIndexException {
        Map<String, AbstractPolicy> potentialPolicies =
                m_policyIndex.getPolicies(eval, m_policyFinder);
        logger.debug("Obtained policies: " + potentialPolicies.size());

        AbstractPolicy policy = matchPolicies(eval, potentialPolicies);
        logger.debug("Matched policies and created abstract policy.");

        return policy;
    }

    /**
     * Given and Evaluation Context and a list of potential policies, this
     * method matches each policy against the Evaluation Context and extracts
     * only the ones that match. If there is more than one policy, a new dynamic
     * policy set is created and returned. Otherwise the policy that is found is
     * returned.
     *
     * @param eval
     *        the Evaluation Context
     * @param policyList
     *        the list of policies as a map with PolicyId as key and policy as a
     *        byte array as the value
     * @return the Policy/PolicySet that applies to this EvaluationCtx
     * @throws {@link TopLevelPolicyException}
     */
    private AbstractPolicy matchPolicies(EvaluationCtx eval,
                                         Map<String, AbstractPolicy> policyList)
            throws TopLevelPolicyException {
        // setup a list of matching policies
        Map<String, AbstractPolicy> list =
                new HashMap<String, AbstractPolicy>();

        // get an iterator over all the identifiers
        for (String policyId : policyList.keySet()) {
            AbstractPolicy policy = policyList.get(policyId);

            MatchResult match = policy.match(eval);

            int result = match.getResult();
            if (result == MatchResult.INDETERMINATE) {
                throw new TopLevelPolicyException(match.getStatus());
            }

            // if we matched, we keep track of the matching policy...
            if (result == MatchResult.MATCH) {
                // ...first checking if this is the first match and if
                // we automatically nest policies
                if (m_combiningAlg == null && list.size() > 0) {
                    ArrayList<String> code = new ArrayList<String>();
                    code.add(Status.STATUS_PROCESSING_ERROR);
                    Status status =
                            new Status(code, "too many applicable"
                                    + " top-level policies");
                    throw new TopLevelPolicyException(status);
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Matched policy: " + policyId);
                }

                list.put(policyId, policy);
            }
        }

        // no errors happened during the search, so now take the right
        // action based on how many policies we found
        switch (list.size()) {
            case 0:
                return null;
            case 1:
                Iterator<AbstractPolicy> i = list.values().iterator();
                AbstractPolicy p = i.next();
                return p;
            default:
                return new PolicySet(parentPolicyId,
                                     m_combiningAlg,
                                     m_target,
                                     new ArrayList<AbstractPolicy>(list
                                             .values()));
        }
    }
}