/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.xacml.pdp.finder.policy;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;



/**
 * 
 *
 * @author Edwin Shin
 */
public class RightsMetadataDocument {

    public static final String RMD_NS =
            "http://hydra-collab.stanford.edu/schemas/rightsMetadata/v1";

    private XPathExpression edit_persons;

    private XPathExpression read_persons;

    private DocumentBuilder builder;

    private Document doc;


    private final XPath xpath;

    public enum Action {
        discover, read, edit;
    }

    public RightsMetadataDocument(InputStream in) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(false);

        try {
            builder = dbFactory.newDocumentBuilder();
            doc = builder.parse(in);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();

        try {
            edit_persons =
                    xpath.compile("//rightsMetadata/access[@type='edit']/machine/person/text()");
            read_persons =
                    xpath.compile("//rightsMetadata/access[@type='read']/machine/person/text()");
        } catch (XPathExpressionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public Map<String, Set<String>> getActionSubjectMap() throws XPathExpressionException {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();

        Set<String> editPersons = new HashSet<String>();
        NodeList nl =
                (NodeList) edit_persons.evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nl.getLength(); i++) {
            editPersons.add(nl.item(i).getNodeValue());
        }
        map.put("edit", editPersons);

        Set<String> readPersons = new HashSet<String>();
        nl = (NodeList) read_persons.evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nl.getLength(); i++) {
            readPersons.add(nl.item(i).getNodeValue());
        }
        // edit implies read permissions
        readPersons.addAll(editPersons);
        map.put("read", readPersons);


        return map;
    }
}
