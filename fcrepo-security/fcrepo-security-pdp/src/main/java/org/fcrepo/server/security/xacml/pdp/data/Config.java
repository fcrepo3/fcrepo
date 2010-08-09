/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */


package org.fcrepo.server.security.xacml.pdp.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import org.fcrepo.server.security.xacml.pdp.MelcoePDP;


/**
 * Reads configuration values for policy storage, indexing and combining
 *
 */
public class Config {

    private static String CONFIG_FILE = "config-policy-storage.xml";
    private static String POLICY_STORE_CLASSNAME = null;
    private static String POLICY_INDEX_CLASSNAME = null;
    private static String POLICY_COMBINING_ALGORITHM_CLASSNAME = null;

    private static boolean init = false;

    private static synchronized void getConfig() throws PolicyConfigException {

        if (init)
            return;

        String home = MelcoePDP.PDP_HOME.getAbsolutePath();

        String filename = home + "/conf/" + CONFIG_FILE;

        File f = new File(filename);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        Document doc = null;
        try {
            docBuilder = factory.newDocumentBuilder();
            doc = docBuilder.parse(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            throw new PolicyConfigException("Configuration file " + f.getAbsolutePath() + " not found", e);
        } catch (SAXException e) {
            throw new PolicyConfigException("Error reading/parsing config file " + f.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new PolicyConfigException("Error reading/parsing config file " + f.getAbsolutePath(), e);
        } catch (ParserConfigurationException e) {
            throw new PolicyConfigException("Error reading/parsing config file " + f.getAbsolutePath(), e);
        }

        NodeList nodes = doc.getElementsByTagName("PolicyStorage").item(0).getChildNodes();

        for (int x = 0; x < nodes.getLength(); x++)
        {
            Node node = nodes.item(x);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                if (node.getNodeName().equals("PolicyStore"))
                    POLICY_STORE_CLASSNAME = node.getFirstChild().getNodeValue();
                else if (node.getNodeName().equals("PolicyIndex"))
                    POLICY_INDEX_CLASSNAME = node.getFirstChild().getNodeValue();
                else if (node.getNodeName().equals("PolicyCombiningAlgorithm"))
                    POLICY_COMBINING_ALGORITHM_CLASSNAME = node.getFirstChild().getNodeValue();
            }
        }

        String configErrors = "";
        if (POLICY_STORE_CLASSNAME == null)
            configErrors += "PolicyStore ";
        if (POLICY_INDEX_CLASSNAME == null)
            configErrors += "PolicyIndex ";
        if (POLICY_COMBINING_ALGORITHM_CLASSNAME == null)
            configErrors += "PolicyCombiningAlgorithm ";

        if (configErrors.equals("")) {
            init = true;
        } else {
            throw new PolicyConfigException("Missing configuration parameters " + configErrors + "in config file " + f.getAbsolutePath());
        }
    }

    public static String policyStoreClassName() throws PolicyConfigException {
        getConfig();
        return POLICY_STORE_CLASSNAME;
    }
    public static String policyIndexClassName() throws PolicyConfigException {
        getConfig();
        return POLICY_INDEX_CLASSNAME;
    }
    public static String policyCombiningAlgorithmClassName() throws PolicyConfigException {
        getConfig();
        return POLICY_COMBINING_ALGORITHM_CLASSNAME;
    }




}
