package org.fcrepo.server.security.xacml.pdp.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.cond.EvaluationResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.pdp.MelcoePDP;
import org.fcrepo.server.security.xacml.util.AttributeBean;


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

    // used in testing - indicates if the implementation returns indexed results
    // or if false indicates that all policies are returned irrespective of the request
    public  boolean indexed = true;

    protected Map<String, Map<String, String>> indexMap = null;
    private static final Logger log =
        LoggerFactory.getLogger(PolicyIndexBase.class.getName());

    protected static final String METADATA_POLICY_NS = "metadata";


    // FIXME: migrate to Spring-based configuration
    // this path is relative to the pdp directory
    private static final String CONFIG_FILE = "/conf/config-policy-index.xml";

    // xacml namespaces and prefixes
    public static final Map<String, String> namespaces = new HashMap<String, String>();
    static {
        namespaces.put("p", XACML20_POLICY_NS);
        namespaces.put("m", METADATA_POLICY_NS); // appears to dbxml specific and probably not used
    }





    protected PolicyIndexBase() throws PolicyIndexException {
        initConfig();

    }

    /**
     * read index configuration from config file
     * configuration is a list of policy target attributes to index
     * @throws PolicyIndexException
     */
    private void initConfig() throws PolicyIndexException {
            String home = MelcoePDP.PDP_HOME.getAbsolutePath();

            String filename = home + CONFIG_FILE;
            File f = new File(filename);

            log.info("Loading config file: " + f.getAbsolutePath());



            Document doc = null;
            try {

            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();

                doc = docBuilder.parse(new FileInputStream(f));
            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                throw new PolicyIndexException("Configuration file " + filename + " not found.", e);
            } catch (SAXException e) {
                throw new PolicyIndexException("Error parsing config file + " + filename, e);
            } catch (IOException e) {
                throw new PolicyIndexException("Error reading config file " + filename, e);

            }

            NodeList nodes = null;

            // get index map information
            String[] indexMapElements =
            {"subjectAttributes", "resourceAttributes",
                    "actionAttributes", "environmentAttributes"};

            indexMap = new HashMap<String, Map<String, String>>();
            for (String s : indexMapElements) {
                indexMap.put(s, new HashMap<String, String>());
            }

            nodes =
                doc.getElementsByTagName("indexMap").item(0)
                .getChildNodes();
            for (int x = 0; x < nodes.getLength(); x++) {
                Node node = nodes.item(x);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    if (log.isDebugEnabled()) {
                        log.debug("Node name: " + node.getNodeName());
                    }

                    NodeList attrs = node.getChildNodes();
                    for (int y = 0; y < attrs.getLength(); y++) {
                        Node attr = attrs.item(y);
                        if (attr.getNodeType() == Node.ELEMENT_NODE) {
                            String name =
                                attr.getAttributes().getNamedItem("name")
                                .getNodeValue();
                            String type =
                                attr.getAttributes().getNamedItem("type")
                                .getNodeValue();
                            indexMap.get(node.getNodeName()).put(name, type);
                        }
                    }
                }
            }

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
    protected Map<String, Set<AttributeBean>> getAttributeMap(EvaluationCtx eval) throws URISyntaxException {
        URI defaultCategoryURI =
                new URI(AttributeDesignator.SUBJECT_CATEGORY_DEFAULT);

        Map<String, String> im = null;
        Map<String, Set<AttributeBean>> attributeMap =
                new HashMap<String, Set<AttributeBean>>();
        Map<String, AttributeBean> attributeBeans = null;

        im = indexMap.get("subjectAttributes");
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
        attributeMap.put("subjectAttributes", new HashSet(attributeBeans
                .values()));

        im = indexMap.get("resourceAttributes");
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
        attributeMap.put("resourceAttributes", new HashSet(attributeBeans
                .values()));

        im = indexMap.get("actionAttributes");
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
        attributeMap.put("actionAttributes", new HashSet(attributeBeans
                .values()));

        im = indexMap.get("environmentAttributes");
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
        attributeMap.put("environmentAttributes", new HashSet(attributeBeans
                .values()));

        return attributeMap;
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
        if (resourceId == null || resourceId.equals("")
                || !resourceId.startsWith("/")) {
            return null;
        }

        List<String> components = new ArrayList<String>();

        String[] parts = resourceId.split("\\/");

        for (int x = 1; x < parts.length; x++) {
            StringBuilder sb = new StringBuilder();
            for (int y = 0; y < x; y++) {
                sb.append("/");
                sb.append(parts[y + 1]);
            }

            components.add(sb.toString());

            if (x != parts.length - 1) {
                components.add(sb.toString() + "/.*");
            } else {
                components.add(sb.toString() + "$");
            }
        }

        return components.toArray(new String[components.size()]);
    }



}
