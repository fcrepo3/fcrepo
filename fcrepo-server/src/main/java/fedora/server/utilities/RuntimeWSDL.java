/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility for combining WSDL and XSD into a single file, with a specific
 * service endpoint.
 * 
 * @author Chris Wilper
 */
public class RuntimeWSDL {

    /** The final WSDL document. */
    private Document _wsdlDoc;

    /**
     * Instantiate from the given files and service endpoint URL.
     */
    public RuntimeWSDL(File schemaFile, File sourceWSDL, String endpoint)
            throws IOException {

        try {

            // init dom parser and parse input
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document xsdDoc = builder.parse(schemaFile);
            _wsdlDoc = builder.parse(sourceWSDL);

            // put schema into the wsdl types section
            Element typesElement = getTopElement("types");
            removeChildren(typesElement);
            Node xsdNode =
                    _wsdlDoc.importNode(xsdDoc.getDocumentElement(), true); // deep copy
            typesElement.appendChild(xsdNode);

            // set the endpoint
            // - requires an existing service element with a name attribute
            // - requires at least one port element under the service element
            // - removes extra ports if more than one exist under service
            // - sets the port name to service[@name] plus "-$scheme-Port",
            //   where $scheme the uppercase URI scheme of the endpoint
            // - sets the address location to given endpoint URL
            Element serviceElement = getTopElement("service");
            String serviceName = serviceElement.getAttributeNS(null, "name");
            if (serviceName == null) {
                throw new IOException("WSDL missing required attribute of "
                        + "service element: name");
            }

            NodeList ports = serviceElement.getElementsByTagNameNS("*", "port");

            while (ports.getLength() > 1) {
                serviceElement.removeChild(ports.item(0));
            }
            if (ports.getLength() == 0) {
                throw new IOException("WSDL missing required element: port");
            }
            Element port = (Element) ports.item(0);

            int i = endpoint.indexOf(":");
            String schemePart = "";
            if (i != -1) {
                schemePart = "-" + endpoint.substring(0, i).toUpperCase();
            }
            port.setAttribute("name", serviceName + schemePart + "-Port");

            Element address =
                    (Element) port.getElementsByTagNameNS("*", "address")
                            .item(0);
            if (address != null) {
                address.setAttribute("location", endpoint);
            } else {
                throw new IOException("WSDL missing required element: address");
            }

        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                IOException ioe = new IOException("Error getting RuntimeWSDL");
                ioe.initCause(e);
                throw ioe;
            }
        }
    }

    private Element getTopElement(String name) throws IOException {
        Element element =
                (Element) _wsdlDoc.getDocumentElement()
                        .getElementsByTagNameNS("*", name).item(0);
        if (element != null) {
            return element;
        } else {
            throw new IOException("WSDL missing required element: " + name);
        }
    }

    private static final void removeChildren(Element element) {
        Node firstChild = element.getFirstChild();
        while (firstChild != null) {
            element.removeChild(firstChild);
            firstChild = element.getFirstChild();
        }
    }

    /**
     * Serialize the final WSDL document to the given writer.
     */
    public void serialize(Writer out) throws IOException {

        OutputFormat fmt = new OutputFormat("XML", "UTF-8", true);
        fmt.setIndent(2);
        fmt.setLineWidth(80);
        fmt.setPreserveSpace(false);
        fmt.setOmitXMLDeclaration(false);
        fmt.setOmitDocumentType(true);

        XMLSerializer ser = new XMLSerializer(out, fmt);

        ser.serialize(_wsdlDoc);
    }

    /**
     * Command-line test. Usage: java RuntimeWSDL schemaFile sourceWSDL endpoint
     */
    public static void main(String[] args) throws Exception {
        java.io.PrintWriter out = new java.io.PrintWriter(System.out);
        new RuntimeWSDL(new File(args[0]), new File(args[1]), args[2])
                .serialize(out);
        out.flush();
    }

}
