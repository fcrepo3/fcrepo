/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.validation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.fcrepo.utilities.ReadableCharArrayWriter;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Schematron validation with FedoraRules schema as default.
 *
 * @author Sandy Payette
 */
public class DOValidatorSchematronResult {

    private static final Logger logger =
            LoggerFactory.getLogger(DOValidatorSchematronResult.class);

    private final Element rootElement;

    public DOValidatorSchematronResult(DOMResult result) {
        rootElement = (Element) result.getNode().getFirstChild();
    }

    public String getXMLResult() throws TransformerException,
            TransformerConfigurationException, ParserConfigurationException {
        Writer w = new StringWriter();
        PrintWriter out = new PrintWriter(w);

        final Transformer transformer = XmlTransformUtility.getTransformer();
        Properties transProps = new Properties();
        transProps.put("method", "xml");
        transProps.put("indent", "yes");
        transformer.setOutputProperties(transProps);
        transformer
                .transform(new DOMSource(rootElement), new StreamResult(out));
        out.close();
        return w.toString();
    }

    /**
     * Check if the object passes Schematron validation
     *
     * @return <code>true</code>, object is valid, <code>false</code>
     *         object had errors.
     */
    public boolean isValid() {
        if (rootElement.getElementsByTagName("ASSERT").getLength() == 0
                && rootElement.getElementsByTagName("REPORT").getLength() == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Serializes the specified node, recursively, to a Writer and returns it as
     * a String too.
     */
    public void serializeResult(Writer out) {
        serializeNode(rootElement, out);
    }
    
    public String serializeResult() {
        ReadableCharArrayWriter writer = new ReadableCharArrayWriter();
        serializeResult(writer);
        writer.close();
        return writer.getString();
    }

    private void serializeNode(Node node, Writer out) {
        try {
            if (node == null) {
                return;
            }

            int type = node.getNodeType();
            switch (type) {
                case Node.DOCUMENT_NODE:
                    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    serializeNode(((Document) node).getDocumentElement(), out);
                    break;

                case Node.ELEMENT_NODE:
                    out.write('<');
                    out.write(node.getNodeName());

                    // do attributes
                    NamedNodeMap attrs = node.getAttributes();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        out.write(' ');
                        out.write(attrs.item(i).getNodeName());
                        out.write("=\"");
                        out.write(attrs.item(i).getNodeValue());
                        out.write('"');
                    }

                    // close up the current element
                    out.write('>');

                    // recursive call to process this node's children
                    NodeList children = node.getChildNodes();
                    if (children != null) {
                        int len = children.getLength();
                        for (int i = 0; i < len; i++) {
                            serializeNode(children.item(i), out);
                        }
                    }
                    break;

                case Node.TEXT_NODE:
                    out.write(node.getNodeValue());
                    break;
            }

            if (type == Node.ELEMENT_NODE) {
                out.write("</");
                out.write(node.getNodeName());
                out.write(">");
            }
            out.flush();
        } catch (Exception e) {
            logger.error("Error serializing node", e);
        }
    }

}
