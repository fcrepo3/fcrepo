/*
 * File: HierarchicalLowestChildPermitOverridesPolicyAlg.java
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

package melcoe.xacml.pdp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.Indenter;
import com.sun.xacml.MatchResult;
import com.sun.xacml.TargetMatchGroup;
import com.sun.xacml.combine.PolicyCombinerElement;
import com.sun.xacml.combine.PolicyCombiningAlgorithm;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;

public class HierarchicalLowestChildDenyOverridesPolicyAlg
        extends PolicyCombiningAlgorithm {

    private static final Logger log =
            Logger
                    .getLogger(HierarchicalLowestChildDenyOverridesPolicyAlg.class
                            .getName());

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    public static final String XACML_RESOURCE_ID =
            "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    public static final String algId =
            "urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:hierarchical-lowest-child-deny-overrides";

    private static URI identifierURI;

    private static RuntimeException earlyException;

    static {
        try {
            identifierURI = new URI(algId);
        } catch (URISyntaxException se) {
            earlyException = new IllegalArgumentException();
            earlyException.initCause(se);
        }
    }

    /**
     * Standard constructor.
     */
    public HierarchicalLowestChildDenyOverridesPolicyAlg() {
        super(identifierURI);

        if (earlyException != null) {
            throw earlyException;
        }

        factory = DocumentBuilderFactory.newInstance();
    }

    /**
     * Protected constructor used by the ordered version of this algorithm.
     * 
     * @param identifier
     *        the algorithm's identifier
     */
    protected HierarchicalLowestChildDenyOverridesPolicyAlg(URI identifier) {
        super(identifier);
    }

    /**
     * Applies the combining rule to the set of policies based on the evaluation
     * context.
     * 
     * @param context
     *        the context from the request
     * @param parameters
     *        a (possibly empty) non-null <code>List</code> of
     *        <code>CombinerParameter<code>s
     * @param policyElements
     *        the policies to combine
     * @return the result of running the combining algorithm
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result combine(EvaluationCtx context,
                          List parameters,
                          List policyElements) {
        log.info("Combining using: " + getIdentifier());

        boolean atLeastOneError = false;
        boolean atLeastOnePermit = false;
        Set denyObligations = new HashSet();
        Status firstIndeterminateStatus = null;

        Set<AbstractPolicy> matchedPolicies = new HashSet<AbstractPolicy>();

        Iterator it = policyElements.iterator();
        while (it.hasNext()) {
            AbstractPolicy policy =
                    ((PolicyCombinerElement) it.next()).getPolicy();

            // make sure that the policy matches the context
            MatchResult match = policy.match(context);

            if (match.getResult() == MatchResult.INDETERMINATE) {
                atLeastOneError = true;

                // keep track of the first error, regardless of cause
                if (firstIndeterminateStatus == null) {
                    firstIndeterminateStatus = match.getStatus();
                }
            } else if (match.getResult() == MatchResult.MATCH) {
                matchedPolicies.add(policy);
            }
        }

        Set<AbstractPolicy> applicablePolicies =
                getApplicablePolicies(context, matchedPolicies);

        for (AbstractPolicy policy : applicablePolicies) {
            Result result = policy.evaluate(context);
            int effect = result.getDecision();

            if (effect == Result.DECISION_DENY) {
                denyObligations.addAll(result.getObligations());
                return new Result(Result.DECISION_DENY, context.getResourceId()
                        .encode(), denyObligations);
            }

            if (effect == Result.DECISION_PERMIT) {
                atLeastOnePermit = true;
            } else if (effect == Result.DECISION_INDETERMINATE) {
                atLeastOneError = true;

                // keep track of the first error, regardless of cause
                if (firstIndeterminateStatus == null) {
                    firstIndeterminateStatus = result.getStatus();
                }
            }
        }

        // if we got a PERMIT, return it
        if (atLeastOnePermit) {
            return new Result(Result.DECISION_PERMIT, context.getResourceId()
                    .encode());
        }

        // if we got an INDETERMINATE, return it
        if (atLeastOneError) {
            return new Result(Result.DECISION_INDETERMINATE,
                              firstIndeterminateStatus,
                              context.getResourceId().encode());
        }

        // if we got here, then nothing applied to us
        return new Result(Result.DECISION_NOT_APPLICABLE, context
                .getResourceId().encode());
    }

    private Set<AbstractPolicy> getApplicablePolicies(EvaluationCtx context,
                                                      Set<AbstractPolicy> policies) {
        int largest = 0;
        Set<AbstractPolicy> applicablePolicies = new HashSet<AbstractPolicy>();

        for (AbstractPolicy policy : policies) {
            String resourceId = null;

            @SuppressWarnings("unchecked")
            List<TargetMatchGroup> tmg =
                    policy.getTarget().getResourcesSection().getMatchGroups();
            for (TargetMatchGroup t : tmg) {
                if (t.match(context).getResult() > 0) {
                    continue;
                }

                resourceId = extractResourceId(t);

                if (resourceId == null) {
                    log.warn("Policy did not contain resourceId: "
                            + policy.getId());
                    continue;
                }

                if (log.isDebugEnabled()) {
                    log.debug("ResourceID: " + resourceId);
                }
            }

            int current;

            if ("".equals(resourceId)) {
                current = 0;
            } else {
                current = getLength(resourceId);
            }

            if (current > largest) {
                largest = current;
                applicablePolicies = new HashSet<AbstractPolicy>();
            }

            if (current >= largest) {
                applicablePolicies.add(policy);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Applicable policies:");
            for (AbstractPolicy p : applicablePolicies) {
                log.debug("\t" + p.getId());
            }
        }

        return applicablePolicies;
    }

    private String extractResourceId(TargetMatchGroup tmg) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tmg.encode(output, new Indenter(4));

        DocumentBuilder docBuilder = null;

        try {
            docBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException pe) {
            log.error("Error obtaining an XML parser: " + pe.getMessage(), pe);
            return null;
        }

        Document doc = null;
        try {
            doc =
                    docBuilder.parse(new ByteArrayInputStream(output
                            .toByteArray()));
        } catch (Exception e) {
            log.error("Problem parsing TargetMatchGroup to obtain id");
            return null;
        }

        String resourceId = null;
        String designator = null;
        String value = null;

        NodeList nodes =
                doc.getElementsByTagName("ResourceMatch").item(0)
                        .getChildNodes();
        for (int x = 0; x < nodes.getLength() && resourceId == null; x++) {
            Node n = nodes.item(x);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if ("AttributeValue".equals(n.getNodeName())) {
                    value = n.getFirstChild().getNodeValue();
                } else if ("ResourceAttributeDesignator"
                        .equals(n.getNodeName())) {
                    designator =
                            n.getAttributes().getNamedItem("AttributeId")
                                    .getNodeValue();
                }

                if (XACML_RESOURCE_ID.equals(designator)) {
                    resourceId = value;
                }
            }
        }

        if (resourceId == null) {
            resourceId = "";
        }

        return resourceId;
    }

    private int getLength(String resourceId) {
        if (resourceId == null || "".equals(resourceId)) {
            if (log.isDebugEnabled()) {
                log.debug("Length: " + resourceId + " " + 0);
            }

            return 0;
        }

        String[] components = resourceId.split("\\/");

        for (int x = 0; x < components.length; x++) {
            if (components[x].matches(".*[^\\w\\-\\&\\:\\+\\~\\$]+.*")) {
                if (log.isDebugEnabled()) {
                    log.debug("Length: " + resourceId + " " + (x - 1)
                            + "\tComponent: " + components[x]);
                }

                return x - 1;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Length [return]: " + resourceId + " "
                    + (components.length - 1));
        }

        return components.length - 1;
    }
}
