/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import fedora.server.utilities.DateUtility;


/**
 * A DOM-based utility for generating FOXML 1.1 documents.
 *
 * @author Edwin Shin
 * @since 3.0
 * @version $Id$
 */
public class Foxml11Document {

    public static final String FOXML_NS="info:fedora/fedora-system:def/foxml#";

    private DocumentBuilder builder;

    private Document doc;

    private Element rootElement;

    private Element objectProperties;

    private final XPath xpath;

    private final TransformerFactory xformFactory;

    public enum Property {
        STATE("info:fedora/fedora-system:def/model#state"),
        LABEL("info:fedora/fedora-system:def/model#label"),
        CONTENT_MODEL("info:fedora/fedora-system:def/model#contentModel"),
        CREATE_DATE("info:fedora/fedora-system:def/model#createdDate"),
        MOD_DATE("info:fedora/fedora-system:def/view#lastModifiedDate");

        private final String uri;

        Property(String uri) {
            this.uri = uri;
        }

        String uri() {
            return uri;
        }
    }

    public enum State {
        A, I, D;
    }

    public enum ControlGroup {
        X, M, E, R;
    }

    public Foxml11Document(String pid) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);

        try {
            builder = dbFactory.newDocumentBuilder();
            DOMImplementation impl = builder.getDOMImplementation();
            doc = impl.createDocument(FOXML_NS, "foxml:digitalObject", null);
            rootElement = doc.getDocumentElement();
            rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                "xmlns:xsi",
                                "http://www.w3.org/1999/XMLSchema-instance");
            rootElement.setAttributeNS("http://www.w3.org/1999/XMLSchema-instance",
                                       "xsi:schemaLocation",
                                       "info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd");
            rootElement.setAttribute("VERSION", "1.1");
            rootElement.setAttribute("PID", pid);

        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        NamespaceContextImpl nsCtx = new NamespaceContextImpl();
        nsCtx.addNamespace("foxml", FOXML_NS);
        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();
        xpath.setNamespaceContext(nsCtx);

        xformFactory = XmlTransformUtility.getTransformerFactory();
    }

    public void addObjectProperties() {
        if (objectProperties == null) {
            objectProperties =
                    doc.createElementNS(FOXML_NS, "foxml:objectProperties");
            rootElement.appendChild(objectProperties);
        }
    }

    public void addObjectProperty(Property name, String value) {
        addObjectProperties();
        Element property = doc.createElementNS(FOXML_NS, "foxml:property");
        property.setAttribute("NAME", name.uri);
        property.setAttribute("VALUE", value);
        objectProperties.appendChild(property);
    }

    public void addDatastream(String id,
                              State state,
                              ControlGroup controlGroup,
                              boolean versionable) {
        Element ds = doc.createElementNS(FOXML_NS, "foxml:datastream");
        ds.setAttribute("ID", id);
        ds.setAttribute("STATE", state.toString());
        ds.setAttribute("CONTROL_GROUP", controlGroup.toString());
        ds.setAttribute("VERSIONABLE", Boolean.toString(versionable));
        rootElement.appendChild(ds);
    }

    public void addDatastreamVersion(String dsId,
                                     String dsvId,
                                     String mimeType,
                                     String label,
                                     int size,
                                     Date created) {
        String expr = String.format("//foxml:datastream[@ID='%s']", dsId);
        try {
            NodeList nodes = (NodeList)xpath.evaluate(expr, doc, XPathConstants.NODESET);
            Node node = nodes.item(0);
            if (node == null) {
                throw new IllegalArgumentException(dsId + "does not exist.");
            }
            Element dsv = doc.createElementNS(FOXML_NS, "foxml:datastreamVersion");
            dsv.setAttribute("ID", dsvId);
            dsv.setAttribute("MIMETYPE", mimeType);
            dsv.setAttribute("LABEL", label);
            dsv.setAttribute("SIZE", Integer.toString(size));
            dsv.setAttribute("CREATED", DateUtility.convertDateToString(created));
            node.appendChild(dsv);

        } catch (XPathExpressionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void addXmlContent(String dsvId, String xmlContent) {
        try {
            Document contentDoc = builder.parse(new InputSource(new StringReader(xmlContent)));
            Node importedContent = doc.adoptNode(contentDoc.getDocumentElement());
            Node dsv = getDatastreamVersion(dsvId);
            Element content = doc.createElementNS(FOXML_NS, "foxml:xmlContent");
            dsv.appendChild(content);
            content.appendChild(importedContent);
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setContentLocation(String dsvId, String ref, String type) {
        String expr = String.format("//foxml:datastreamVersion[@ID='%s']/foxml:contentLocation", dsvId);

        try {
            NodeList nodes = (NodeList)xpath.evaluate(expr, doc, XPathConstants.NODESET);
            Element location = (Element)nodes.item(0);
            if (location == null) {
                location = addContentLocation(dsvId);
            }
            location.setAttribute("REF", ref);
            location.setAttribute("TYPE", type);

        } catch (XPathExpressionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Element addContentLocation(String dsvId) {
        Node node = getDatastreamVersion(dsvId);
        Element location = doc.createElementNS(FOXML_NS, "foxml:contentLocation");
        node.appendChild(location);
        return location;
    }

    private Node getDatastreamVersion(String dsvId) {
        String expr = String.format("//foxml:datastreamVersion[@ID='%s']", dsvId);

        try {
            NodeList nodes = (NodeList)xpath.evaluate(expr, doc, XPathConstants.NODESET);
            Node node = nodes.item(0);
            if (node == null) {
                throw new IllegalArgumentException(dsvId + "does not exist.");
            }
            return node;
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(dsvId + "does not exist.");
        }
    }

    public void serialize(OutputStream out) {
        Transformer idTransform;
        try {
            idTransform = xformFactory.newTransformer();
            Source input = new DOMSource(doc);
            Result output = new StreamResult(out);
            idTransform.transform(input, output);
        } catch (TransformerConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
