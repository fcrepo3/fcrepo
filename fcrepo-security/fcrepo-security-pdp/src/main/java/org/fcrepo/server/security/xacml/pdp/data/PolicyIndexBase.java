package org.fcrepo.server.security.xacml.pdp.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fcrepo.server.security.xacml.pdp.finder.policy.PolicyReader;
import org.fcrepo.server.security.xacml.util.AttributeBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.ParsingException;
import com.sun.xacml.Policy;
import com.sun.xacml.PolicySet;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.finder.PolicyFinder;


/**
 * Base abstract class for all PolicyIndex implementations.
 *
 * Gets the index configuration common to all implementations.
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public abstract class PolicyIndexBase
implements PolicyIndex {
    protected static final String SUBJECT_KEY = "subjectAttributes";
    protected static final String RESOURCE_KEY = "resourceAttributes";
    protected static final String ACTION_KEY = "actionAttributes";
    protected static final String ENVIRONMENT_KEY = "environmentAttributes";
    protected static final URI SUBJECT_CATEGORY_DEFAULT =
            URI.create(AttributeDesignator.SUBJECT_CATEGORY_DEFAULT);
    // used in testing - indicates if the implementation returns indexed results
    // or if false indicates that all policies are returned irrespective of the request
    public  boolean indexed = true;

    protected Map<String, Map<String, String>> indexMap = null;
    protected PolicyReader m_policyReader;

    private static final Logger log =
        LoggerFactory.getLogger(PolicyIndexBase.class.getName());

    protected static final String METADATA_POLICY_NS = "metadata";

    // xacml namespaces and prefixes
    public static final Map<String, String> namespaces = new HashMap<String, String>();
    static {
        namespaces.put("p", XACML20_POLICY_NS);
        namespaces.put("m", METADATA_POLICY_NS); // appears to dbxml specific and probably not used
    }





    protected PolicyIndexBase(PolicyReader policyReader) throws PolicyIndexException {
        m_policyReader = policyReader;
        String[] indexMapElements =
        {SUBJECT_KEY, RESOURCE_KEY,
                ACTION_KEY, ENVIRONMENT_KEY};

        indexMap = new HashMap<String, Map<String, String>>();
        for (String s : indexMapElements) {
            indexMap.put(s, new HashMap<String, String>());
        }
    }

    public void setSubjectAttributes(Map<String, String> attributeMap) {
        setAttributeMap(SUBJECT_KEY, attributeMap);
    }

    public void setResourceAttributes(Map<String, String> attributeMap) {
        setAttributeMap(RESOURCE_KEY, attributeMap);
    }

    public void setActionAttributes(Map<String, String> attributeMap) {
        setAttributeMap(ACTION_KEY, attributeMap);
    }

    public void setEnvironmentAttributes(Map<String, String> attributeMap) {
        setAttributeMap(ENVIRONMENT_KEY, attributeMap);
    }

    protected void setAttributeMap(String mapKey, Map<String, String> attributeMap) {
        indexMap.get(mapKey).putAll(attributeMap);
    }

    /**
     * This method extracts the attributes listed in the indexMap from the given
     * evaluation context.
     *
     * @param eval
     *        the Evaluation Context from which to extract Attributes
     * @return a Map of Attributes for each category (Subject, Resource, Action,
     *         Environment)
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Collection<AttributeBean>> getAttributeMap(EvaluationCtx eval) throws URISyntaxException {
        final URI defaultCategoryURI = SUBJECT_CATEGORY_DEFAULT;

        Map<String, String> im = null;
        Map<String, Collection<AttributeBean>> attributeMap =
                new HashMap<String, Collection<AttributeBean>>();
        Map<String, AttributeBean> attributeBeans = null;

        im = indexMap.get(SUBJECT_KEY);
        attributeBeans = new HashMap<String, AttributeBean>();
        for (String attributeId : im.keySet()) {
            EvaluationResult result =
                    eval.getSubjectAttribute(new URI(im.get(attributeId)),
                                             new URI(attributeId),
                                             defaultCategoryURI);
            if (result.getStatus() == null && !result.indeterminate()) {
                AttributeValue attr = result.getAttributeValue();
                if (attr.returnsBag()) {
                    Iterator<AttributeValue> i =
                            ((BagAttribute) attr).iterator();
                    if (i.hasNext()) {
                        while (i.hasNext()) {
                            AttributeValue value = i.next();
                            String attributeType = im.get(attributeId);

                            AttributeBean ab = attributeBeans.get(attributeId);
                            if (ab == null) {
                                ab = new AttributeBean();
                                ab.setId(attributeId);
                                ab.setType(attributeType);
                                attributeBeans.put(attributeId, ab);
                            }

                            ab.addValue(value.encode());
                        }
                    }
                }
            }
        }
        attributeMap.put(SUBJECT_KEY, attributeBeans.values());

        im = indexMap.get(RESOURCE_KEY);
        attributeBeans = new HashMap<String, AttributeBean>();
        for (String attributeId : im.keySet()) {
            EvaluationResult result =
                    eval.getResourceAttribute(new URI(im.get(attributeId)),
                                              new URI(attributeId),
                                              null);
            if (result.getStatus() == null && !result.indeterminate()) {
                AttributeValue attr = result.getAttributeValue();
                if (attr.returnsBag()) {
                    Iterator<AttributeValue> i =
                            ((BagAttribute) attr).iterator();
                    if (i.hasNext()) {
                        while (i.hasNext()) {
                            AttributeValue value = i.next();
                            String attributeType = im.get(attributeId);

                            AttributeBean ab = attributeBeans.get(attributeId);
                            if (ab == null) {
                                ab = new AttributeBean();
                                ab.setId(attributeId);
                                ab.setType(attributeType);
                                attributeBeans.put(attributeId, ab);
                            }

                            if (attributeId.equals(XACML_RESOURCE_ID)
                                    && value.encode().startsWith("/")) {
                                String[] components =
                                        makeComponents(value.encode());
                                if (components != null && components.length > 0) {
                                    for (String c : components) {
                                        ab.addValue(c);
                                    }
                                } else {
                                    ab.addValue(value.encode());
                                }
                            } else {
                                ab.addValue(value.encode());
                            }
                        }
                    }
                }
            }
        }
        attributeMap.put(RESOURCE_KEY, attributeBeans.values());

        im = indexMap.get(ACTION_KEY);
        attributeBeans = new HashMap<String, AttributeBean>();
        for (String attributeId : im.keySet()) {
            EvaluationResult result =
                    eval.getActionAttribute(new URI(im.get(attributeId)),
                                            new URI(attributeId),
                                            null);
            if (result.getStatus() == null && !result.indeterminate()) {
                AttributeValue attr = result.getAttributeValue();
                if (attr.returnsBag()) {
                    Iterator<AttributeValue> i =
                            ((BagAttribute) attr).iterator();
                    if (i.hasNext()) {
                        while (i.hasNext()) {
                            AttributeValue value = i.next();
                            String attributeType = im.get(attributeId);

                            AttributeBean ab = attributeBeans.get(attributeId);
                            if (ab == null) {
                                ab = new AttributeBean();
                                ab.setId(attributeId);
                                ab.setType(attributeType);
                                attributeBeans.put(attributeId, ab);
                            }

                            ab.addValue(value.encode());
                        }
                    }
                }
            }
        }
        attributeMap.put(ACTION_KEY, attributeBeans.values());

        im = indexMap.get(ENVIRONMENT_KEY);
        attributeBeans = new HashMap<String, AttributeBean>();
        for (String attributeId : im.keySet()) {
            URI imAttrId = new URI(im.get(attributeId));
            URI attrId = new URI(attributeId);
            EvaluationResult result =
                    eval.getEnvironmentAttribute(imAttrId, attrId, null);
            if (result.getStatus() == null && !result.indeterminate()) {
                AttributeValue attr = result.getAttributeValue();
                if (attr.returnsBag()) {
                    Iterator<AttributeValue> i =
                            ((BagAttribute) attr).iterator();
                    if (i.hasNext()) {
                        while (i.hasNext()) {
                            AttributeValue value = i.next();
                            String attributeType = im.get(attributeId);

                            AttributeBean ab = attributeBeans.get(attributeId);
                            if (ab == null) {
                                ab = new AttributeBean();
                                ab.setId(attributeId);
                                ab.setType(attributeType);
                                attributeBeans.put(attributeId, ab);
                            }

                            ab.addValue(value.encode());
                        }
                    }
                }
            }
        }
        attributeMap.put(ENVIRONMENT_KEY, attributeBeans.values());

        return attributeMap;
    }

    /**
     * A private method that handles reading the policy and creates the correct
     * kind of AbstractPolicy.
     * Because this makes use of the policyFinder, it cannot be reused between finders.
     * Consider moving to policyManager, which is not intended to be reused outside
     * of a policyFinderModule, which is not intended to be reused amongst PolicyFinder instances.
     */
    protected AbstractPolicy handleDocument(Document doc, PolicyFinder policyFinder) throws ParsingException {
        // handle the policy, if it's a known type
        Element root = doc.getDocumentElement();
        String name = root.getTagName();

        // see what type of policy this is
        if (name.equals("Policy")) {
            return Policy.getInstance(root);
        } else if (name.equals("PolicySet")) {
            return PolicySet.getInstance(root, policyFinder);
        } else {
            // this isn't a root type that we know how to handle
            throw new ParsingException("Unknown root document type: " + name);
        }
    }

    /**
     * Splits a XACML hierarchical resource-id value into a set of resource-id values
     * that can be matched against a policy.
     *
     *  Eg an incoming request for /res1/res2/res3/.* should match
     *  /res1/.*
     *  /res1/res2/.*
     *  /res1/res2/res3/.*
     *
     *  in policies.
     *
     * @param resourceId XACML hierarchical resource-id value
     * @return array of individual resource-id values that can be used to match against policies
     */
    protected static String[] makeComponents(String resourceId) {
        if (resourceId == null || resourceId.isEmpty()
                || !resourceId.startsWith("/")) {
            return null;
        }

        List<String> components = new ArrayList<String>();

        String[] parts = resourceId.split("\\/");

        int bufPrimer = 0;
        for (int x = 1; x < parts.length; x++) {
            bufPrimer = Math.max(bufPrimer, (parts[x].length() + 1));
            StringBuilder sb = new StringBuilder(bufPrimer);
            for (int y = 0; y < x; y++) {
                sb.append("/");
                sb.append(parts[y + 1]);
            }

            String componentBase = sb.toString();
            bufPrimer = componentBase.length() + 16;
            components.add(componentBase);

            if (x != parts.length - 1) {
                components.add(componentBase.concat("/.*"));
            } else {
                components.add(componentBase.concat("$"));
            }
        }

        return components.toArray(parts);
    }



}