
package org.fcrepo.server.security.xacml.pdp.finder;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.fcrepo.server.security.xacml.pdp.MelcoePDP;
import org.fcrepo.server.security.xacml.pdp.data.PolicyStoreException;
import org.fcrepo.server.security.xacml.util.AttributeFinderConfig;
import org.fcrepo.server.security.xacml.util.Designator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Deprecated
public class AttributeFinderConfigUtil {

    private static final Logger logger =
            LoggerFactory.getLogger(AttributeFinderConfigUtil.class);

    private static final String CONFIG_FILE = "/conf/config-attribute-finder.xml";


    public static AttributeFinderConfig getAttributeFinderConfig(String className) throws AttributeFinderException {
        AttributeFinderConfig config = new AttributeFinderConfig();

        try {

            Element attributeFinder = getAttributeFinder(className);
            if (attributeFinder == null) {
                throw new Exception("AttributeFinder not found: " + className);
            }
            NodeList attributes = attributeFinder.getChildNodes();
            // do each attribute node
            for (int y = 0; y < attributes.getLength(); y++) {
                Node attribute = attributes.item(y);
                if (attribute.getNodeType() == Node.ELEMENT_NODE && attribute.getNodeName().equals("attribute")) {
                    String designatorName = attribute.getAttributes().getNamedItem("designator").getNodeValue();
                    if (designatorName == null)
                        throw new AttributeFinderException("Invalid attribute finder config, missing designator attribute");
                    Designator des = config.get(designatorName);
                    if (des == null) {
                        des = config.put(designatorName);
                    }
                    String attributeName = attribute.getAttributes().getNamedItem("name").getNodeValue();
                    if (attributeName == null)
                        throw new AttributeFinderException("Invalid attribute finder config, missing name attribute");
                    org.fcrepo.server.security.xacml.util.Attribute attr =
                            des.get(attributeName);
                    if (attr == null) {
                        attr = des.put(attributeName);
                    } else {
                        logger.warn("Duplicate attribute definition for " + designatorName + " : " + attributeName);
                    }
                    logger.debug("Added attribute " + designatorName + " : " + attributeName);
                    // do each child config element
                    NodeList attributeConfigs = attribute.getChildNodes();
                    for (int z = 0; z < attributeConfigs.getLength(); z++) {
                        Node attributeConfig = attributeConfigs.item(z);
                        if (attributeConfig.getNodeType() == Node.ELEMENT_NODE && attributeConfig.getNodeName().equals("config")) {
                            String configName = attributeConfig.getAttributes().getNamedItem("name").getNodeValue();
                            if (configName == null)
                                throw new AttributeFinderException("Missing name attribute on config element");
                            String configValue = attributeConfig.getAttributes().getNamedItem("value").getNodeValue();
                            if (configValue == null) {
                                throw new AttributeFinderException("Missing value attribute on config element");
                            }
                            attr.put(configName, configValue);
                            logger.debug("Added attribute config " + configName + " : " + configValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error initialising attribute finder " + e.getMessage());
            throw new AttributeFinderException("Error initialising attribute finder " + e.getMessage(),e);
        }

        return config;
    }



    // the old one
    // FIXME: remove...
//    public static Map<Integer, Set<String>> getAttributeFinderConfigx(String className)
//            throws AttributeFinderException {
//        Map<Integer, Set<String>> attributeSet =
//                new HashMap<Integer, Set<String>>();
//
//        List<String> designatorTable = new ArrayList<String>();
//        designatorTable.add("subject");
//        designatorTable.add("resource");
//        designatorTable.add("action");
//        designatorTable.add("environment");
//
//        try {
//            Element attributeFinder = getAttributeFinder(className);
//            if (attributeFinder == null) {
//                throw new Exception("AttributeFinder not found: " + className);
//            }
//
//            NodeList attributes = attributeFinder.getChildNodes();
//            for (int y = 0; y < attributes.getLength(); y++) {
//                Node n = attributes.item(y);
//                if (n.getNodeType() == Node.ELEMENT_NODE
//                        && "attribute".equals(n.getNodeName())) {
//                    String designator =
//                        n.getAttributes()
//                        .getNamedItem("designator")
//                        .getNodeValue();
//
//                    if (designator == null) {
//                        throw new AttributeFinderException("Bad configuration file. Missing Designator.");
//                    }
//
//                    if (!designatorTable.contains(designator)) {
//                        throw new AttributeFinderException("Incorrect designator type. Must be 'subject', 'resource', 'action' or 'environment'");
//                    }
//
//                    Integer designatorValue =
//                        new Integer(designatorTable
//                                    .indexOf(designator));
//                    String attribute =
//                        n.getAttributes().getNamedItem("name")
//                        .getNodeValue();
//
//                    Set<String> attrs =
//                        attributeSet.get(designatorValue);
//                    if (attrs == null) {
//                        attrs = new HashSet<String>();
//                        attributeSet.put(designatorValue, attrs);
//                    }
//                    attrs.add(attribute);
//                }
//            }
//
//
//        } catch (Exception e) {
//            logger.error("Could not initialise AttributeFinder: [" + className
//                         + "] " + e.getMessage(), e);
//            throw new AttributeFinderException("Could not initialise AttributeFinder: ["
//                                                       + className
//                                                       + "] "
//                                                       + e.getMessage(),
//                                               e);
//        }
//
//        return attributeSet;
//    }
//
    public static Map<String, String> getResolverConfig(String className)
            throws AttributeFinderException {
        Map<String, String> config = new HashMap<String, String>();

        try {
            Element attributeFinder = getAttributeFinder(className);
            if (attributeFinder == null) {
                throw new Exception("AttributeFinder not found: " + className);
            }

            NodeList attributes = attributeFinder.getChildNodes();
            for (int y = 0; y < attributes.getLength(); y++) {
                Node n = attributes.item(y);
                if (n.getNodeType() == Node.ELEMENT_NODE
                        && "resolver".equals(n.getNodeName())) {
                    config.put("url", n.getAttributes().getNamedItem("url")
                            .getNodeValue());
                    config.put("username", n.getAttributes()
                            .getNamedItem("username").getNodeValue());
                    config.put("password", n.getAttributes()
                            .getNamedItem("password").getNodeValue());
                }
            }
        } catch (Exception e) {
            logger.error("Could not initialise DBXML: " + e.getMessage(), e);
            throw new AttributeFinderException("Could not initialise AttributeFinder: ["
                                                       + className
                                                       + "] "
                                                       + e.getMessage(),
                                               e);
        }

        return config;
    }

    public static Map<String, String> getOptionMap(String className)
            throws AttributeFinderException {
        Map<String, String> options = new HashMap<String, String>();

        try {
            Element attributeFinder = getAttributeFinder(className);
            if (attributeFinder == null) {
                throw new Exception("AttributeFinder not found: " + className);
            }

            NodeList attributes =
                    attributeFinder.getElementsByTagName("option");
            for (int y = 0; y < attributes.getLength(); y++) {
                Node n = attributes.item(y);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    String name =
                            n.getAttributes().getNamedItem("name")
                                    .getNodeValue();
                    String value =
                            n.getAttributes().getNamedItem("value")
                                    .getNodeValue();
                    if (logger.isDebugEnabled()) {
                        logger.debug(className + ": " + name + " = " + value);
                    }

                    options.put(name, value);
                }
            }
        } catch (Exception e) {
            logger.error("Could not get option map: " + e.getMessage(), e);
            throw new AttributeFinderException("Could not get option map for AttributeFinder: ["
                                                       + className
                                                       + "] "
                                                       + e.getMessage(),
                                               e);
        }

        return options;
    }

    private static Element getAttributeFinder(String className)
            throws Exception {
        String home = MelcoePDP.PDP_HOME.getAbsolutePath();

        String filename = home + CONFIG_FILE;
        File f = new File(filename);
        if (!f.exists()) {
            throw new PolicyStoreException("Could not locate config file: "
                    + f.getAbsolutePath());
        }

        logger.info("Loading attribute finder config file: "
                        + f.getAbsolutePath());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document doc = docBuilder.parse(new FileInputStream(f));

        NodeList nodes = null;

        nodes = doc.getElementsByTagName("AttributeFinder");
        for (int x = 0; x < nodes.getLength(); x++) {
            String name =
                    nodes.item(x).getAttributes().getNamedItem("name")
                            .getNodeValue();
            if (className.equals(name)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Located AttributeFinder: " + className);
                }

                return (Element) nodes.item(x);
            }
        }

        throw new AttributeFinderException("AttributeFinder not found: "
                + className);
    }
}