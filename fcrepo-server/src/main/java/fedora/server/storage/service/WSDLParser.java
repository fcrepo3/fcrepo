/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.service;

import java.io.InputStream;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import fedora.common.Constants;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.RepositoryConfigurationException;

/**
 * Parses WSDL document into a Service object. The Service object encapsulates
 * all information about the service: the service name, the PortType (with
 * abstract operation definitions), and one or more Ports (with operation
 * binding information). This parser will not yet handle every pattern we may
 * encounter in WSDL, therefore, it throws exceptions for WSDL elements that it
 * does not yet support. The goal is to evolve the WSDLParser to be able to
 * handle any kind of WSDL, and to have Fedora be able to play nicely with any
 * kind of WSDL.
 * 
 * @author Sandy Payette
 */
class WSDLParser
        extends DefaultHandler
        implements Constants {

    /**
     * URI-to-namespace prefix mapping info from SAX2 startPrefixMapping events.
     */
    private HashMap<String, String> nsPrefixMap;

    // Variables for keeping state during SAX parse.
    private boolean inWSDLTypes = false;

    private boolean inSimpleType = false;

    private boolean inRestriction = false;

    private boolean inMessage = false;

    private boolean inPortType = false;

    private boolean inAbstractOperation = false;

    private boolean inService = false;

    private boolean inBinding = false;

    private boolean isHTTPOperation = false;

    private boolean isSOAPOperation = false;

    private boolean inInput = false;

    private boolean inOutput = false;

    // WSDL Entities

    private SimpleType wsdlSimpleType;

    private Message wsdlMessage;

    private Part wsdlMessagePart;

    private PortType wsdlPortType;

    private Port wsdlPort;

    private AbstractOperation wsdlOperation;

    private HTTPOperationInOut wsdlHTTPOpInOut;

    private Binding wsdlBinding;

    private Service wsdlService;

    // Working variables and tables...

    private Hashtable<String, SimpleType> wsdlTypeTbl; // typeName, Type object

    private Hashtable<String, Message> wsdlMessageTbl; // messageName, Message object

    private Hashtable<String, String> wsdlPortBindingTbl; // portName, name of binding

    private Hashtable<String, Binding> wsdlBindingTbl; // bindingName, Binding object

    private Hashtable<String, AbstractOperation> wsdlAbstrOperTbl; // operationName, AbstractOperation object

    private Vector<String> tmp_enum;

    private Vector<Part> tmp_parts;

    private Vector<AbstractOperation> tmp_operations;

    private String tmp_portBindingName;

    private String tmp_portBindingLocalName;

    private Vector<Port> tmp_ports;

    private String tmp_operationName;

    private String tmp_bindingName;

    private String tmp_bindingPortTypeName;

    private String tmp_bindingPortTypeLocalName;

    private Vector<AbstractOperation> tmp_bindOperations;

    private Vector<MIMEContent> tmp_MIMEContent;

    /**
     * Constructor to enable another class to initiate the parsing
     */
    public WSDLParser() {
    }

    /**
     * Constructor allows this class to initiate the parsing
     */
    public WSDLParser(InputStream in)
            throws RepositoryConfigurationException, ObjectIntegrityException {
        XMLReader xmlReader = null;
        try {
            SAXParserFactory saxfactory = SAXParserFactory.newInstance();
            saxfactory.setValidating(false);
            SAXParser parser = saxfactory.newSAXParser();
            xmlReader = parser.getXMLReader();
            xmlReader.setContentHandler(this);
            xmlReader.setFeature("http://xml.org/sax/features/namespaces",
                                 false);
            xmlReader
                    .setFeature("http://xml.org/sax/features/namespace-prefixes",
                                false);
        } catch (Exception e) {
            throw new RepositoryConfigurationException("Internal SAX error while "
                    + "preparing for WSDL datastream parsing: "
                    + e.getMessage());
        }
        try {
            xmlReader.parse(new InputSource(in));
        } catch (Exception e) {
            throw new ObjectIntegrityException("Error parsing WSDL datastream"
                    + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public Service getService() {
        return wsdlService;
    }

    public void startDocument() throws SAXException {
        nsPrefixMap = new HashMap<String, String>();
        wsdlTypeTbl = new Hashtable<String, SimpleType>();
        wsdlMessageTbl = new Hashtable<String, Message>();
        wsdlPortBindingTbl = new Hashtable<String, String>();
        wsdlBindingTbl = new Hashtable<String, Binding>();
        wsdlAbstrOperTbl = new Hashtable<String, AbstractOperation>();
    }

    public void endDocument() throws SAXException {
        doServiceJoins();
        wsdlTypeTbl = null;
        wsdlMessageTbl = null;
        wsdlPortBindingTbl = null;
        wsdlBindingTbl = null;
        wsdlAbstrOperTbl = null;
    }

    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        nsPrefixMap.put(uri, prefix);
    }

    public void skippedEntity(String name) throws SAXException {
        StringBuffer sb = new StringBuffer();
        sb.append('&');
        sb.append(name);
        sb.append(';');
        char[] text = new char[sb.length()];
        sb.getChars(0, sb.length(), text, 0);
        characters(text, 0, text.length);
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
    }

    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes attrs) throws SAXException {
        // Look for things in the WSDL that this parser is not ready to support yet!
        // Exception will be thrown and the parsing will stop if an unsupported
        // pattern is found.
        checkForUnsupportedPattern(namespaceURI, localName, qName, attrs);

        // Gather up all XML schema type definitions for the service.
        if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                && localName.equalsIgnoreCase("types")) {
            inWSDLTypes = true;
        } else if (inWSDLTypes) {
            if (namespaceURI.equalsIgnoreCase(XML_XSD.uri)
                    && localName.equalsIgnoreCase("simpleType")) {
                inSimpleType = true;
                wsdlSimpleType = new SimpleType();
                wsdlSimpleType.typeName = attrs.getValue("name");
            } else if (inSimpleType) {
                if (namespaceURI.equalsIgnoreCase(XML_XSD.uri)
                        && localName.equalsIgnoreCase("restriction")) {
                    inRestriction = true;
                    tmp_enum = new Vector<String>();
                    wsdlSimpleType.baseTypeName = attrs.getValue("base");
                    String nsprefix = null;
                    StringTokenizer st =
                            new StringTokenizer(wsdlSimpleType.baseTypeName,
                                                ":");
                    if (st.hasMoreTokens()) {
                        nsprefix = st.nextToken();
                        wsdlSimpleType.baseTypeLocalName = st.nextToken();
                    } else {
                        wsdlSimpleType.baseTypeLocalName =
                                wsdlSimpleType.baseTypeName;
                    }
                    // FIXIT!! Test whether null nsprefix makes this bag.
                    if (nsprefix.equalsIgnoreCase((String) nsPrefixMap
                            .get(XML_XSD.uri))) {
                        wsdlSimpleType.baseTypeNamespaceURI = XML_XSD.uri;
                    } else {
                        throw new SAXException("WSDLParser: base for simpleType cannot be other than XSD type"
                                + wsdlSimpleType.baseTypeName);
                    }
                } else if (inRestriction
                        && namespaceURI.equalsIgnoreCase(XML_XSD.uri)
                        && localName.equalsIgnoreCase("enumeration")) {
                    tmp_enum.add(attrs.getValue("value"));
                }
            }
        }

        // Second, parse WSDL message definitions that will be used in defining abstract operations.

        else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                && localName.equalsIgnoreCase("message")) {
            inMessage = true;
            wsdlMessage = new Message();
            wsdlMessage.messageName = attrs.getValue("name");
            tmp_parts = new Vector<Part>();
        } else if (inMessage && namespaceURI.equalsIgnoreCase(WSDL.uri)
                && localName.equalsIgnoreCase("part")) {
            wsdlMessagePart = new Part();
            wsdlMessagePart.partName = attrs.getValue("name");
            wsdlMessagePart.partTypeName = attrs.getValue("type");
        }

        // Third, process the WSDL portType for the abstract operations

        else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                && localName.equalsIgnoreCase("portType")) {
            inPortType = true;
            wsdlPortType = new PortType();
            wsdlPortType.portTypeName = attrs.getValue("name");
            tmp_operations = new Vector<AbstractOperation>();
        } else if (inPortType) {
            if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                    && localName.equalsIgnoreCase("operation")) {
                inAbstractOperation = true;
                wsdlOperation = new AbstractOperation();
                wsdlOperation.operationName = attrs.getValue("name");
            } else if (inAbstractOperation) {
                // FIXIT!! According to the WSDL XML schema, the input and output
                // elements can have an optional "name" attribute in addition to the
                // "message" attribute. I am ignoring the "name" attribute and just picking
                // up the message.  Consider if there are reasons to parse the "name" attribute.
                if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                        && localName.equalsIgnoreCase("input")) {
                    wsdlOperation.inputMessage = new Message();
                    wsdlOperation.inputMessage.messageName =
                            attrs.getValue("message");
                    StringTokenizer st =
                            new StringTokenizer(wsdlOperation.inputMessage.messageName,
                                                ":");
                    if (st.hasMoreTokens()) {
                        st.nextToken();
                        wsdlOperation.inputMessage.messageName = st.nextToken();
                    }
                } else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                        && localName.equalsIgnoreCase("output")) {
                    wsdlOperation.outputMessage = new Message();
                    wsdlOperation.outputMessage.messageName =
                            attrs.getValue("message");
                    StringTokenizer st =
                            new StringTokenizer(wsdlOperation.outputMessage.messageName,
                                                ":");
                    if (st.hasMoreTokens()) {
                        st.nextToken();
                        wsdlOperation.outputMessage.messageName =
                                st.nextToken();
                    }
                }
                // FIXIT!! Parse the fault element.  Also note that according to the
                // WSDL XML schema, the "name" attribute on the fault element is NOT optional
                // like it is with input and output elements.
            }
        }

        // Get the Service and Port information
        else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                && localName.equalsIgnoreCase("service")) {
            inService = true;
            wsdlService = new Service();
            wsdlService.serviceName = attrs.getValue("name");
            tmp_ports = new Vector<Port>();
        } else if (inService) {
            if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                    && localName.equalsIgnoreCase("port")) {
                wsdlPort = new Port();
                wsdlPort.portName = attrs.getValue("name");
                tmp_portBindingName = attrs.getValue("binding");
                String nsprefix = null;
                tmp_portBindingLocalName = null;
                StringTokenizer st =
                        new StringTokenizer(tmp_portBindingName, ":");
                if (st.hasMoreTokens()) {
                    nsprefix = st.nextToken();
                    tmp_portBindingLocalName = st.nextToken();
                } else {
                    tmp_portBindingLocalName = tmp_portBindingName;
                }
            } else if (namespaceURI.equalsIgnoreCase(WSDL_HTTP.uri)
                    && localName.equalsIgnoreCase("address")) {
                wsdlPort.portBaseURL = attrs.getValue("location");
            } else if (namespaceURI.equalsIgnoreCase(SOAP.uri)
                    && localName.equalsIgnoreCase("address")) {
                wsdlPort.portBaseURL = attrs.getValue("location");
            }
        }

        // Get the Operation Bindings

        else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                && localName.equalsIgnoreCase("binding")) {
            inBinding = true;
            tmp_bindingName = attrs.getValue("name");
            tmp_bindingPortTypeName = attrs.getValue("type");
            tmp_bindingPortTypeLocalName = null;
            StringTokenizer st =
                    new StringTokenizer(tmp_bindingPortTypeName, ":");
            if (st.hasMoreTokens()) {
                String nsprefix = st.nextToken();
                tmp_bindingPortTypeLocalName = st.nextToken();
            } else {
                tmp_bindingPortTypeLocalName = tmp_bindingPortTypeName;
            }
        } else if (inBinding) {
            if (namespaceURI.equalsIgnoreCase(WSDL_HTTP.uri)
                    && localName.equalsIgnoreCase("binding")) {
                wsdlBinding = new HTTPBinding();
                wsdlBinding.bindingName = tmp_bindingName;
                wsdlBinding.portTypeLocalName = tmp_bindingPortTypeLocalName;
                ((HTTPBinding) wsdlBinding).bindingVerb =
                        attrs.getValue("verb");
                tmp_bindOperations = new Vector<AbstractOperation>();
            } else if (namespaceURI.equalsIgnoreCase(SOAP.uri)
                    && localName.equalsIgnoreCase("binding")) {
                wsdlBinding = new SOAPBinding();
                wsdlBinding.bindingName = tmp_bindingName;
                wsdlBinding.portTypeLocalName = tmp_bindingPortTypeLocalName;
                ((SOAPBinding) wsdlBinding).bindingStyle =
                        attrs.getValue("style");
                ((SOAPBinding) wsdlBinding).bindingTransport =
                        attrs.getValue("transport");
                tmp_bindOperations = new Vector<AbstractOperation>();
            } else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                    && localName.equalsIgnoreCase("operation")) {
                tmp_operationName = attrs.getValue("name");
            } else if (namespaceURI.equalsIgnoreCase(WSDL_HTTP.uri)
                    && localName.equalsIgnoreCase("operation")) {
                wsdlOperation = new HTTPOperation();
                wsdlOperation.operationName = tmp_operationName;
                ((HTTPOperation) wsdlOperation).operationLocation =
                        attrs.getValue("location");
                isHTTPOperation = true;
            } else if (namespaceURI.equalsIgnoreCase(SOAP.uri)
                    && localName.equalsIgnoreCase("operation")) {
                wsdlOperation = new SOAPOperation();
                wsdlOperation.operationName = tmp_operationName;
                ((SOAPOperation) wsdlOperation).soapAction =
                        attrs.getValue("soapAction");
                ((SOAPOperation) wsdlOperation).soapActionStyle =
                        attrs.getValue("style");
                isSOAPOperation = true;
            } else if (isHTTPOperation) {
                if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                        && localName.equalsIgnoreCase("input")) {
                    inInput = true;
                    wsdlHTTPOpInOut = new HTTPOperationInOut();
                    tmp_MIMEContent = new Vector<MIMEContent>();
                } else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                        && localName.equalsIgnoreCase("output")) {
                    inOutput = true;
                    wsdlHTTPOpInOut = new HTTPOperationInOut();
                    tmp_MIMEContent = new Vector<MIMEContent>();
                } else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                        && localName.equalsIgnoreCase("fault")) {
                    // FIXIT!! do something!!
                } else if (inInput) {
                    if (namespaceURI.equalsIgnoreCase(WSDL_HTTP.uri)
                            && localName.equalsIgnoreCase("urlReplacement")) {
                        wsdlHTTPOpInOut.ioBindingType =
                                HTTPOperationInOut.URL_REPLACE_BINDING_TYPE;
                        wsdlHTTPOpInOut.ioMIMEContent = null;
                    } else if (namespaceURI.equalsIgnoreCase(WSDL_MIME.uri)) {
                        wsdlHTTPOpInOut.ioBindingType =
                                HTTPOperationInOut.MIME_BINDING_TYPE;
                        tmp_MIMEContent.add(parseMIMEBinding(namespaceURI,
                                                             localName,
                                                             qName,
                                                             attrs));
                    } else {
                        // FIXIT!!  Probably unnecessary, but...
                        throw new SAXException("WSDLParser: Found an input extension element on HTTP operation "
                                + "that Fedora does not yet support: " + qName);
                    }
                } else if (inOutput) {
                    if (namespaceURI.equalsIgnoreCase(WSDL_MIME.uri)) {
                        wsdlHTTPOpInOut.ioBindingType =
                                HTTPOperationInOut.MIME_BINDING_TYPE;
                        tmp_MIMEContent.add(parseMIMEBinding(namespaceURI,
                                                             localName,
                                                             qName,
                                                             attrs));
                    } else {
                        // FIXIT!!  Probably unnecessary, but...
                        throw new SAXException("WSDLParser: Found an output extension element on HTTP operation "
                                + "that Fedora does not yet support: " + qName);
                    }
                }
            } else if (isSOAPOperation) {
                // FIXIT!! implement something
            }
        }
    }

    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        // Type post-processing
        if (inWSDLTypes) {
            if (inSimpleType) {
                if (inRestriction) {
                    if (namespaceURI.equalsIgnoreCase(XML_XSD.uri)
                            && localName.equalsIgnoreCase("restriction")) {
                        wsdlSimpleType.enumerationOfValues = tmp_enum;
                        tmp_enum = null;
                        inRestriction = false;
                    }
                } else if (namespaceURI.equalsIgnoreCase(XML_XSD.uri)
                        && localName.equalsIgnoreCase("simpleType")) {
                    wsdlTypeTbl.put(wsdlSimpleType.typeName, wsdlSimpleType);
                    wsdlSimpleType = null;
                    inSimpleType = false;
                }
            } else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                    && localName.equalsIgnoreCase("types")) {
                inWSDLTypes = false;
            }
        }
        // Message post-processing
        else if (inMessage) {
            if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                    && localName.equalsIgnoreCase("part")) {
                tmp_parts.add(wsdlMessagePart);
                wsdlMessagePart = null;
            }

            else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                    && localName.equalsIgnoreCase("message")) {
                wsdlMessage.messageParts =
                        (Part[]) tmp_parts.toArray(new Part[0]);
                wsdlMessageTbl.put(wsdlMessage.messageName, wsdlMessage);
                wsdlMessage = null;
                tmp_parts = null;
                inMessage = false;
            }
        }
        // PortType post-processing
        else if (inPortType) {
            if (inAbstractOperation) {
                if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                        && localName.equalsIgnoreCase("operation")) {
                    tmp_operations.add(wsdlOperation);
                    wsdlOperation = null;
                    inAbstractOperation = false;
                }
            } else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                    && localName.equalsIgnoreCase("portType")) {
                wsdlPortType.operations =
                        (AbstractOperation[]) tmp_operations
                                .toArray(new AbstractOperation[0]);
                tmp_operations = null;
                inPortType = false;
            }
        }
        // Service post-processing
        else if (inService) {
            if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                    && localName.equalsIgnoreCase("port")) {
                tmp_ports.add(wsdlPort);
                wsdlPortBindingTbl.put(wsdlPort.portName,
                                       tmp_portBindingLocalName);
                wsdlPort = null;
                tmp_portBindingLocalName = null;
                tmp_portBindingName = null;
            } else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                    && localName.equalsIgnoreCase("service")) {
                wsdlService.ports = (Port[]) tmp_ports.toArray(new Port[0]);
                tmp_ports = null;
                inService = false;
            }
        }
        // Binding post-processing
        else if (inBinding) {
            if (isHTTPOperation) {
                if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                        && localName.equalsIgnoreCase("input")) {
                    wsdlHTTPOpInOut.ioMIMEContent =
                            (MIMEContent[]) tmp_MIMEContent
                                    .toArray(new MIMEContent[0]);
                    ((HTTPOperation) wsdlOperation).inputBinding =
                            wsdlHTTPOpInOut;
                    tmp_MIMEContent = null;
                    wsdlHTTPOpInOut = null;
                    inInput = false;
                } else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                        && localName.equalsIgnoreCase("output")) {
                    wsdlHTTPOpInOut.ioMIMEContent =
                            (MIMEContent[]) tmp_MIMEContent
                                    .toArray(new MIMEContent[0]);
                    ((HTTPOperation) wsdlOperation).outputBinding =
                            wsdlHTTPOpInOut;
                    tmp_MIMEContent = null;
                    wsdlHTTPOpInOut = null;
                    inOutput = false;
                } else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                        && localName.equalsIgnoreCase("operation")) {
                    tmp_bindOperations.add(wsdlOperation);
                    tmp_operationName = null;
                    isHTTPOperation = false;
                    wsdlOperation = null;
                }
            } else if (isSOAPOperation) {
                // FIXIT!! implement something!!!
            } else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                    && localName.equalsIgnoreCase("binding")) {
                if (wsdlBinding
                        .getClass()
                        .getName()
                        .equalsIgnoreCase("fedora.server.storage.service.HTTPBinding")) {
                    ((HTTPBinding) wsdlBinding).operations =
                            (HTTPOperation[]) tmp_bindOperations
                                    .toArray(new HTTPOperation[0]);
                    tmp_bindOperations = null;
                } else if (wsdlBinding
                        .getClass()
                        .getName()
                        .equalsIgnoreCase("fedora.server.storage.service.SOAPBinding")) {
                    // FIXIT!! Finish up class definition for SOAPOperations.
                    ((SOAPBinding) wsdlBinding).operations =
                            (SOAPOperation[]) tmp_bindOperations
                                    .toArray(new SOAPOperation[0]);
                    tmp_bindOperations = null;
                }
                wsdlBindingTbl.put(wsdlBinding.bindingName, wsdlBinding);
                wsdlBinding = null;
                tmp_bindingName = null;
                tmp_bindingPortTypeName = null;
                tmp_bindingPortTypeLocalName = null;
                inBinding = false;
            }
        }
    }

    private MIMEContent parseMIMEBinding(String namespaceURI,
                                         String localName,
                                         String qName,
                                         Attributes attrs) throws SAXException {
        MIMEContent content = new MIMEContent();
        content.elementType = localName;
        content.messagePartName = attrs.getValue("part");

        if (namespaceURI.equalsIgnoreCase(WSDL_MIME.uri)
                && localName.equalsIgnoreCase("content")) {
            content.mimeType = attrs.getValue("type");
        } else if (namespaceURI.equalsIgnoreCase(WSDL_MIME.uri)
                && localName.equalsIgnoreCase("mimeXml")) {
            content.mimeType = "text/xml";
        } else {
            throw new SAXException("WSDLParser: Detected a MIME Binding that Fedora does not yet support:"
                    + qName);
        }
        return content;
    }

    private void doServiceJoins() throws SAXException {
        doPortTypeToMessageJoin();
        doPortToMessageJoin();
    }

    private void doPortTypeToMessageJoin() throws SAXException {
        // For each Abstract Operation, get the input and output messages.
        // Each Abstract Operation has input and output Message objects, but the
        // Message objects only have the message name filled in. The full message
        // information will be added via a lookup to the wsdlMessageTbl which was
        // created when the WSDL message definitions were parsed.
        // But, the messages also need some augmentation.  We must lookup the
        // schema type information for the message parts.  This is done via the
        // doMessageToTypeJoin operation.
        AbstractOperation[] absOps = wsdlPortType.operations;
        for (int i = 0; i < absOps.length; i++) {
            // FIXIT!! What if either an input message or an output message does not exist on an abstract operation??
            absOps[i].inputMessage =
                    doMessageToTypeJoin((Message) wsdlMessageTbl
                            .get(absOps[i].inputMessage.messageName));
            wsdlAbstrOperTbl.put(absOps[i].operationName, absOps[i]);
            absOps[i].outputMessage =
                    doMessageToTypeJoin((Message) wsdlMessageTbl
                            .get(absOps[i].outputMessage.messageName));
            wsdlAbstrOperTbl.put(absOps[i].operationName, absOps[i]);
        }
        wsdlPortType.operations = absOps;
        wsdlService.portType = wsdlPortType;
        wsdlPortType = null;

    }

    private void doPortToMessageJoin() throws SAXException {

        for (int i = 0; i < wsdlService.ports.length; i++) {
            wsdlService.ports[i].binding =
                    (Binding) wsdlBindingTbl.get(wsdlPortBindingTbl
                            .get(wsdlService.ports[i].portName));

            if (wsdlService.ports[i].binding
                    .getClass()
                    .getName()
                    .equalsIgnoreCase("fedora.server.storage.service.HTTPBinding")) {
                for (int j = 0; j < ((HTTPBinding) wsdlService.ports[i].binding).operations.length; j++) {
                    String opName =
                            ((HTTPBinding) wsdlService.ports[i].binding).operations[j].operationName;
                    AbstractOperation absOp =
                            (AbstractOperation) wsdlAbstrOperTbl.get(opName);

                    ((HTTPBinding) wsdlService.ports[i].binding).operations[j].inputMessage =
                            absOp.inputMessage;
                    ((HTTPBinding) wsdlService.ports[i].binding).operations[j].outputMessage =
                            absOp.outputMessage;
                }
            } else if (wsdlService.ports[i].binding
                    .getClass()
                    .getName()
                    .equalsIgnoreCase("fedora.server.storage.service.SOAPBinding")) {
                // FIXIT!!  implement something
            }
        }
    }

    /**
     * For each Message, iterate through Message Parts. For each MessagePart,
     * get partTypeName and break into ns/localName. Lookup to wsdlTypeTbl to
     * get type info for parts.
     */
    private Message doMessageToTypeJoin(Message msg) throws SAXException {
        for (int i = 0; i < msg.messageParts.length; i++) {
            Type schemaType = null;
            String nsprefix = null;
            String partTypeLocalName = null;
            StringTokenizer st =
                    new StringTokenizer(msg.messageParts[i].partTypeName, ":");
            if (st.hasMoreTokens()) {
                nsprefix = st.nextToken();
                partTypeLocalName = st.nextToken();
            } else {
                partTypeLocalName = msg.messageParts[i].partTypeName;
            }
            // FIXIT!! Test whether null nsprefix makes this bag.
            if (nsprefix
                    .equalsIgnoreCase((String) nsPrefixMap.get(XML_XSD.uri))) {
                msg.messageParts[i].partBaseTypeNamespaceURI = XML_XSD.uri;
                msg.messageParts[i].partBaseTypeLocalName = partTypeLocalName;
            }
            // If the part is not an XSD base type, then we assume it is a type defined
            // in the schema definition section of THIS WSDL document (i.e., either
            // a simpleType or a complexType).  NOTE that complexType not currently supported
            // by Fedora.  Also, note that if the part type is not found it the type table
            // (wsdlTypeTbl was created from parsing the schema types section of the WSDL),
            // then an exception will be thrown.  This means that Fedora only supports WSDL
            // whose message parts are either XSD base types or simpleTypes defined in the WSDL document.
            else if ((schemaType = (Type) wsdlTypeTbl.get(partTypeLocalName)) != null) {
                if (schemaType
                        .getClass()
                        .getName()
                        .equalsIgnoreCase("fedora.server.storage.service.SimpleType")) {
                    msg.messageParts[i].partBaseTypeNamespaceURI =
                            schemaType.baseTypeNamespaceURI;
                    msg.messageParts[i].partBaseTypeLocalName =
                            schemaType.baseTypeLocalName;
                    msg.messageParts[i].enumerationOfValues =
                            ((SimpleType) schemaType).enumerationOfValues;
                    // FIXIT!! Do we want to put more information in the Part object??
                }
            } else {
                throw new SAXException("WSDLParser: message part type must be XSD type or defined in WSDL as simpleType"
                        + msg.messageParts[i].partTypeName);
            }
        }
        return msg;
    }

    // FIXIT!! We probably want to look for stuff we can't support when sdep objects
    // are ingested into the repository!  We can create Schematron rules to look for
    // unsupported patterns.  I am throwing these exceptions for now, and may continue
    // to do so just to be conservative.  If someone picks up this code and expects it
    // to support the parsing of all possible WSDL patterns, I want these exceptions
    // to be thrown as a signifier that this does not yet support every kind of WSDL
    // we might encounter!
    private void checkForUnsupportedPattern(String namespaceURI,
                                            String localName,
                                            String qName,
                                            Attributes attrs)
            throws SAXException {
        // Elements in XML schema definitions (within WSDL types)
        if (namespaceURI.equalsIgnoreCase(XML_XSD.uri)
                && localName.equalsIgnoreCase("complexType")) {
            throw new SAXException("WSDLParser: Detected a WSDL pattern that Fedora does not yet support: "
                    + qName);
        } else if (namespaceURI.equalsIgnoreCase(XML_XSD.uri)
                && localName.equalsIgnoreCase("element")) {
            throw new SAXException("WSDLParser: Detected a WSDL pattern that Fedora does not yet support: "
                    + qName);
        } else if (namespaceURI.equalsIgnoreCase(XML_XSD.uri)
                && localName.equalsIgnoreCase("list")) {
            throw new SAXException("WSDLParser: Detected a WSDL pattern that Fedora does not yet support: "
                    + qName);
        } else if (namespaceURI.equalsIgnoreCase(XML_XSD.uri)
                && localName.equalsIgnoreCase("union")) {
            throw new SAXException("WSDLParser: Detected a WSDL pattern that Fedora does not yet support: "
                    + qName);
        }
        // WSDL Elements
        else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                && localName.equalsIgnoreCase("import")) {
            throw new SAXException("WSDLParser: Detected a WSDL pattern that Fedora does not yet support: "
                    + qName);
        } else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                && localName.equalsIgnoreCase("fault")) {
            throw new SAXException("WSDLParser: Detected a WSDL pattern that Fedora does not yet support: "
                    + qName);
        } else if (namespaceURI.equalsIgnoreCase(WSDL.uri)
                && localName.equalsIgnoreCase("part")
                && attrs.getValue("element") != null) {
            throw new SAXException("WSDLParser: Detected a WSDL pattern that Fedora does not yet support: "
                    + qName + " element attr");
        }
        // Extension Elements
        else if (namespaceURI.equalsIgnoreCase(SOAP.uri)
                && localName.equalsIgnoreCase("binding")) {
            throw new SAXException("WSDLParser: Detected a WSDL pattern that Fedora does not yet support: "
                    + qName);
        } else if (namespaceURI.equalsIgnoreCase(WSDL_MIME.uri)
                && localName.equalsIgnoreCase("multipartRelated")) {
            throw new SAXException("WSDLParser: Detected a WSDL pattern that Fedora does not yet support: "
                    + qName);
        } else if (namespaceURI.equalsIgnoreCase(WSDL_HTTP.uri)
                && localName.equalsIgnoreCase("urlEncoded")) {
            throw new SAXException("WSDLParser: Detected a WSDL pattern that Fedora does not yet support: "
                    + qName);
        }
    }
}