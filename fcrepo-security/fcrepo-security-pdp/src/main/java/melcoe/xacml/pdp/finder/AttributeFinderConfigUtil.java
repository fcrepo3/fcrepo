
package melcoe.xacml.pdp.finder;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import melcoe.xacml.pdp.MelcoePDP;
import melcoe.xacml.pdp.data.PolicyDataManagerException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AttributeFinderConfigUtil {

    private static final Logger log =
            Logger.getLogger(AttributeFinderConfigUtil.class.getName());

    public static Map<Integer, Set<String>> getAttributeFinderConfig(String className)
            throws AttributeFinderException {
        Map<Integer, Set<String>> attributeSet =
                new HashMap<Integer, Set<String>>();

        List<String> designatorTable = new ArrayList<String>();
        designatorTable.add("subject");
        designatorTable.add("resource");
        designatorTable.add("action");
        designatorTable.add("environment");

        try {
            String home = MelcoePDP.PDP_HOME.getAbsolutePath();

            String filename = home + "/conf/config-attribute-finder.xml";
            File f = new File(filename);
            if (!f.exists()) {
                throw new PolicyDataManagerException("Could not locate config file: "
                        + f.getAbsolutePath());
            }

            log.info("Loading attribute finder config file: "
                    + f.getAbsolutePath());

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new FileInputStream(f));

            NodeList nodes = null;

            nodes = doc.getElementsByTagName("AttributeFinder");
            for (int x = 0; x < nodes.getLength(); x++) {
                String name =
                        nodes.item(x).getAttributes().getNamedItem("name")
                                .getNodeValue();
                if (className.equals(name)) {
                    NodeList attributes = nodes.item(x).getChildNodes();
                    for (int y = 0; y < attributes.getLength(); y++) {
                        Node n = attributes.item(y);
                        if (n.getNodeType() == Node.ELEMENT_NODE
                                && "attribute".equals(n.getNodeName())) {
                            String designator =
                                    n.getAttributes()
                                            .getNamedItem("designator")
                                            .getNodeValue();

                            if (designator == null) {
                                throw new AttributeFinderException("Bad configuration file. Missing Designator.");
                            }

                            if (!designatorTable.contains(designator)) {
                                throw new AttributeFinderException("Incorrect designator type. Must be 'subject', 'resource', 'action' or 'environment'");
                            }

                            Integer designatorValue =
                                    new Integer(designatorTable
                                            .indexOf(designator));
                            String attribute =
                                    n.getAttributes().getNamedItem("name")
                                            .getNodeValue();

                            Set<String> attrs =
                                    attributeSet.get(designatorValue);
                            if (attrs == null) {
                                attrs = new HashSet<String>();
                                attributeSet.put(designatorValue, attrs);
                            }
                            attrs.add(attribute);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.fatal("Could not initialise AttributeFinder: [" + className
                    + "] " + e.getMessage(), e);
            throw new AttributeFinderException("Could not initialise AttributeFinder: ["
                                                       + className
                                                       + "] "
                                                       + e.getMessage(),
                                               e);
        }

        return attributeSet;
    }

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
            log.fatal("Could not initialise DBXML: " + e.getMessage(), e);
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
                    if (log.isDebugEnabled()) {
                        log.debug(className + ": " + name + " = " + value);
                    }

                    options.put(name, value);
                }
            }
        } catch (Exception e) {
            log.fatal("Could not initialise DBXML: " + e.getMessage(), e);
            throw new AttributeFinderException("Could not initialise AttributeFinder: ["
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

        String filename = home + "/conf/config-attribute-finder.xml";
        File f = new File(filename);
        if (!f.exists()) {
            throw new PolicyDataManagerException("Could not locate config file: "
                    + f.getAbsolutePath());
        }

        log
                .info("Loading attribute finder config file: "
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
                if (log.isDebugEnabled()) {
                    log.debug("Located AttributeFinder: " + className);
                }

                return (Element) nodes.item(x);
            }
        }

        throw new AttributeFinderException("AttributeFinder not found: "
                + className);
    }
}
