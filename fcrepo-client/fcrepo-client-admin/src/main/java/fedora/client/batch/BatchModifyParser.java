/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.batch;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.axis.types.NonNegativeInteger;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import fedora.client.FedoraClient;
import fedora.client.Uploader;
import fedora.client.batch.types.Datastream;
import fedora.client.batch.types.DigitalObject;
import fedora.client.utility.ingest.AutoIngestor;

import fedora.common.Constants;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;
import fedora.server.types.gen.ComparisonOperator;
import fedora.server.types.gen.Condition;
import fedora.server.types.gen.FieldSearchQuery;
import fedora.server.types.gen.FieldSearchResult;
import fedora.server.types.gen.ObjectFields;
import fedora.server.utilities.StreamUtility;

/**
 * Parses a stream of batch modify directives and makes appropriate calls to the
 * a Fedora server. The parsing is configured to parse directives in the file
 * sequentially. Logs are written for each successful and failed directive that
 * is processed. Recoverable(non-fatal) errors are written to the log file and
 * processing continues. Catastrophic errors will cause parsing to halt and set
 * the count of failed directives to -1 indicating that parsing was halted prior
 * to the end of the file. In this case the logs will contain all directives
 * processed up to the point of failure.
 *
 * @author Ross Wayland
 */
public class BatchModifyParser
        extends DefaultHandler
        implements Constants {

    /** Instance of Uploader */
    private static Uploader UPLOADER;

    /** Instance of FedoraAPIM */
    private static FedoraAPIM APIM;

    private static FedoraAPIA APIA;

    /** Log file print stream. */
    private static PrintStream out;

    /** Count of directives that succeeded. */
    private int succeededCount = 0;

    /** Count of directives that failed. */
    private int failedCount = 0;

    /** Variables for keeping state during SAX parse. */
    private StringBuffer m_dsXMLBuffer;

    private boolean m_inXMLMetadata;

    private boolean addObject = false;

    private boolean modifyObject = false;

    private boolean purgeObject = false;

    private boolean addDatastream = false;

    private boolean modifyDatastream = false;

    private boolean purgeDatastream = false;

    private boolean setDatastreamState = false;

    private boolean setDatastreamVersionable = false;

    private boolean compareDatastreamChecksum = false;

    private Datastream m_ds;

    private DigitalObject m_obj;

    /**
     * <p>
     * Constructor allows this class to initiate the parsing.
     * </p>
     *
     * @param UPLOADER
     *        - An instance of Uploader.
     * @param APIM
     *        - An instance of FedoraAPIM.
     * @param APIA
     *        - An instance of Fedora APIA.
     * @param in
     *        - An input stream containing the xml to be parsed.
     * @param out
     *        - A print stream used for writing log info.
     */
    public BatchModifyParser(Uploader UPLOADER,
                             FedoraAPIM APIM,
                             FedoraAPIA APIA,
                             InputStream in,
                             PrintStream out) {
        BatchModifyParser.out = out;
        BatchModifyParser.APIM = APIM;
        BatchModifyParser.APIA = APIA;
        BatchModifyParser.UPLOADER = UPLOADER;
        XMLReader xmlReader = null;

        // Configure the SAX parser.
        try {
            SAXParserFactory saxfactory = SAXParserFactory.newInstance();
            saxfactory.setValidating(true);
            SAXParser parser = saxfactory.newSAXParser();
            xmlReader = parser.getXMLReader();
            xmlReader.setContentHandler(this);
            xmlReader
                    .setFeature("http://xml.org/sax/features/namespaces", true);
            xmlReader
                    .setFeature("http://xml.org/sax/features/namespace-prefixes",
                                true);
            xmlReader
                    .setFeature("http://apache.org/xml/features/validation/schema",
                                true);
            xmlReader.setErrorHandler(new BatchModifyXMLErrorHandler());
        } catch (Exception e) {
            // An Exception indicates a fatal error and parsing was halted. Set
            // failedCount to -1 to indicate to the calling class that parsing failed.
            // Throwing an Exception would make class variables for succeededCount
            // and failedCount unavailable.
            logParserError(e, null);
            failedCount = -1;
        }

        // Parse the file.
        try {
            xmlReader.parse(new InputSource(in));
        } catch (Exception e) {
            // An Exception indicates a fatal error and parsing was halted. Set
            // failedCount to -1 to indicate to the calling class that parsing failed.
            // Throwing an Exception would make class variables for succeededCount
            // and failedCount unavailable.
            logParserError(e, null);
            failedCount = -1;
        }
    }

    /**
     * <p>
     * Get the count of failed directives. Note that a failed count value of -1
     * indicates that a fatal parsing error occurred before all directives could
     * be parsed and the number of unprocessed directives is indeterminate. The
     * log file will contain details on how many directives were successfully
     * processed before the fatal error was encountered.
     * </p>
     *
     * @return The count of failed directives.
     */
    public int getFailedCount() {
        return failedCount;
    }

    /**
     * <p>
     * Get the count of successful directives.
     * </p>
     *
     * @return The count of successful directives.
     */
    public int getSucceededCount() {
        return succeededCount;
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        StringBuffer sb = new StringBuffer();
        sb.append('&');
        sb.append(name);
        sb.append(';');
        char[] text = new char[sb.length()];
        sb.getChars(0, sb.length(), text, 0);
        characters(text, 0, text.length);
    }

    private void appendElementStart(String uri,
                                    String localName,
                                    String qName,
                                    Attributes a,
                                    StringBuffer out) {
        out.append("<" + qName);
        for (int i = 0; i < a.getLength(); i++) {
            out.append(" " + a.getQName(i) + "=\""
                    + StreamUtility.enc(a.getValue(i)) + "\"");
        }
        out.append(">");
    }

    @Override
    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (m_inXMLMetadata) {
            // since this data is encoded straight back to xml,
            // we need to make sure special characters &, <, >, ", and '
            // are re-converted to the xml-acceptable equivalents.
            StreamUtility.enc(ch, start, length, m_dsXMLBuffer);
        }
    }

    @Override
    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes attrs) throws SAXException {

        if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("addObject")) {
            addObject = false;
            m_obj = new DigitalObject();

            // Get required attributes
            m_obj.pid = attrs.getValue("pid");
            m_obj.label = attrs.getValue("label");
            m_obj.logMessage = attrs.getValue("logMessage");

            try {
                if (m_obj.label.equals("")) {
                    failedCount++;
                    logFailedDirective(m_obj.pid,
                                       localName,
                                       null,
                                       "Object Label must be non-empty.");
                    return;
                }
                if (!m_obj.pid.equals("")) {
                    if (m_obj.pid.indexOf(":") < 1) {
                        failedCount++;
                        logFailedDirective(m_obj.pid,
                                           localName,
                                           null,
                                           "Custom PID should be of the form \"namespace:1234\"");
                        return;
                    }
                }
                addObject = true;

            } catch (Exception e) {
                failedCount++;
                logFailedDirective(m_obj.pid, localName, e, "");
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("modifyObject")) {
            modifyObject = false;
            m_obj = new DigitalObject();

            // Get required attributes
            m_obj.pid = attrs.getValue("pid");
            m_obj.logMessage = attrs.getValue("logMessage");

            try {
                if (!m_obj.pid.equals("")) {
                    if (m_obj.pid.indexOf(":") < 1) {
                        failedCount++;
                        logFailedDirective(m_obj.pid,
                                           localName,
                                           null,
                                           "Custom PID should be of the form \"namespace:1234\"");
                        return;
                    }
                }

                // Get optional attributes
                if (attrs.getValue("label") != null) {
                    m_obj.label = attrs.getValue("label");
                } else {
                    m_obj.label = null;
                }
                if (attrs.getValue("state") != null) {
                    m_obj.state = attrs.getValue("state");
                } else {
                    m_obj.state = null;
                }
                if (attrs.getValue("ownerId") != null) {
                    m_obj.ownerId = attrs.getValue("ownerId");
                } else {
                    m_obj.ownerId = null;
                }
                modifyObject = true;

            } catch (Exception e) {
                failedCount++;
                logFailedDirective(m_obj.pid, localName, e, "");
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("purgeObject")) {
            purgeObject = false;
            m_obj = new DigitalObject();

            // Get required attributes
            m_obj.pid = attrs.getValue("pid");
            m_obj.logMessage = attrs.getValue("logMessage");

            try {
                if (!m_obj.pid.equals("")) {
                    if (m_obj.pid.indexOf(":") < 1) {
                        failedCount++;
                        logFailedDirective(m_obj.pid,
                                           localName,
                                           null,
                                           "Custom PID should be of the form \"namespace:1234\"");
                        return;
                    }
                }

                // Get optional attributes
                if (attrs.getValue("force") != null) {
                    m_obj.force =
                            new Boolean(attrs.getValue("force")).booleanValue();
                } else {
                    m_obj.force = false;
                }
                purgeObject = true;

            } catch (Exception e) {
                failedCount++;
                logFailedDirective(m_obj.pid, localName, e, "");
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("addDatastream")) {
            try {
                addDatastream = false;
                m_ds = new Datastream();

                // Get required attributes
                m_ds.objectPID = attrs.getValue("pid");
                m_ds.dsControlGrp = attrs.getValue("dsControlGroupType");
                m_ds.dsLabel = attrs.getValue("dsLabel");
                m_ds.dsState = attrs.getValue("dsState");
                m_ds.dsMIME = attrs.getValue("dsMIME");
                m_ds.logMessage = attrs.getValue("logMessage");

                // Check for optional attributes
                if (attrs.getValue("dsID") != null
                        && !attrs.getValue("dsID").equals("")) {
                    m_ds.dsID = attrs.getValue("dsID");
                } else {
                    m_ds.dsID = null;
                }
                if (attrs.getValue("dsLocation") != null
                        && !attrs.getValue("dsLocation").equals("")) {
                    m_ds.dsLocation = attrs.getValue("dsLocation");
                }
                if (attrs.getValue("formatURI") != null
                        && !attrs.getValue("formatURI").equals("")) {
                    m_ds.formatURI = attrs.getValue("formatURI");
                }
                if (attrs.getValue("versionable") != null
                        && !attrs.getValue("versionable").equals("")) {
                    m_ds.versionable =
                            new Boolean(attrs.getValue("versionable"))
                                    .booleanValue();
                }
                if (attrs.getValue("altIDs") != null
                        && !attrs.getValue("altIDs").equals("")) {
                    m_ds.altIDs = attrs.getValue("altIDs").split(" ");
                }
                if (attrs.getValue("checksumType") != null
                        && !attrs.getValue("checksumType").equals("")) {
                    m_ds.checksumType = attrs.getValue("checksumType");
                }
                if (attrs.getValue("checksum") != null
                        && !attrs.getValue("checksum").equals("")) {
                    m_ds.checksum = attrs.getValue("checksum");
                }

                addDatastream = true;

            } catch (Exception e) {
                e.printStackTrace();
                failedCount++;
                logFailedDirective(m_ds.objectPID, localName, e, "");
                return;
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("purgeDatastream")) {

            try {
                purgeDatastream = false;
                m_ds = new Datastream();

                // Get required attributes
                m_ds.objectPID = attrs.getValue("pid");
                m_ds.dsID = attrs.getValue("dsID");
                m_ds.logMessage = attrs.getValue("logMessage");

                // Get optional attributes. If asOfDate attribute is missing
                // or empty its value is null and indicates that all versions
                // of the datastream are to be purged.
                if (attrs.getValue("asOfDate") != null
                        && !attrs.getValue("asOfDate").equals("")) {
                    m_ds.asOfDate = attrs.getValue("asOfDate");
                }
                if (attrs.getValue("endDate") != null
                        && !attrs.getValue("endDate").equals("")) {
                    m_ds.endDate = attrs.getValue("endDate");
                }
                if (attrs.getValue("force") != null
                        && !attrs.getValue("force").equals("")) {
                    m_ds.force =
                            new Boolean(attrs.getValue("force")).booleanValue();
                }

                purgeDatastream = true;

            } catch (Exception e) {
                failedCount++;
                logFailedDirective(m_ds.objectPID, localName, e, "");
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("modifyDatastream")) {

            try {
                fedora.server.types.gen.Datastream dsOrig = null;
                modifyDatastream = false;

                // Get required attributes
                m_ds = new Datastream();
                m_ds.objectPID = attrs.getValue("pid");
                m_ds.dsID = attrs.getValue("dsID");
                m_ds.dsControlGrp = attrs.getValue("dsControlGroupType");
                m_ds.logMessage = attrs.getValue("logMessage");

                try {
                    dsOrig =
                            APIM.getDatastream(m_ds.objectPID, m_ds.dsID, null);
                } catch (Exception e) {
                    failedCount++;
                    logFailedDirective(m_ds.objectPID,
                                       localName,
                                       null,
                                       "Datastream ID: " + m_ds.dsID
                                               + " does not exist"
                                               + " in the object: "
                                               + m_ds.objectPID
                                               + " .\n    Unable to modify"
                                               + "datastream.");
                    return;
                }

                // Check that datastream control group type matches that of the
                // original datastream being modified. This would get caught
                // later by the server, but may as well detect this now and
                // flag as an error in directives file.
                if (dsOrig.getControlGroup().getValue()
                        .equalsIgnoreCase(m_ds.dsControlGrp)) {

                    // Check for optional atributes. Missing attributes (null) indicate
                    // that no change is to occur to that attribute and that the original
                    // value of that datastream attribute is to be retained. Attributes that
                    // contain the empty string indicate that the datastream attribute value
                    // is to be set to the empty string.
                    if (attrs.getValue("dsLabel") != null) {
                        m_ds.dsLabel = attrs.getValue("dsLabel");
                    } else {
                        m_ds.dsLabel = dsOrig.getLabel();
                    }
                    if (attrs.getValue("dsLocation") != null) {
                        m_ds.dsLocation = attrs.getValue("dsLocation");
                    } else {
                        m_ds.dsLocation = dsOrig.getLocation();
                    }
                    if (attrs.getValue("dsMIME") != null) {
                        m_ds.dsMIME = attrs.getValue("dsMIME");
                    } else {
                        m_ds.dsMIME = dsOrig.getMIMEType();
                    }
                    if (attrs.getValue("force") != null) {
                        m_ds.force =
                                new Boolean(attrs.getValue("force"))
                                        .booleanValue();
                    } else {
                        m_ds.force = false;
                    }
                    if (attrs.getValue("altIDs") != null) {
                        m_ds.altIDs = attrs.getValue("altIDs").split(" ");
                    } else {
                        m_ds.altIDs = dsOrig.getAltIDs();
                    }
                    if (attrs.getValue("formatURI") != null) {
                        m_ds.formatURI = attrs.getValue("formatURI");
                    } else {
                        m_ds.formatURI = dsOrig.getFormatURI();
                    }
                    if (attrs.getValue("checksumType") != null) {
                        m_ds.checksumType = attrs.getValue("checksumType");
                    }
                    if (attrs.getValue("checksum") != null) {
                        m_ds.checksum = attrs.getValue("checksum");
                    }

                    modifyDatastream = true;

                } else {
                    failedCount++;
                    logFailedDirective(m_ds.objectPID,
                                       localName,
                                       null,
                                       " Datastream Control Group Type of: "
                                               + m_ds.dsControlGrp
                                               + " in directives file does not match control group"
                                               + " type in original datastream: "
                                               + dsOrig.getControlGroup()
                                                       .getValue());
                }
            } catch (Exception e) {
                failedCount++;
                logFailedDirective(m_ds.objectPID, localName, e, "");
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("setDatastreamState")) {

            try {
                m_ds = new Datastream();
                setDatastreamState = false;

                // Get require attributes
                m_ds.objectPID = attrs.getValue("pid");
                m_ds.dsID = attrs.getValue("dsID");
                m_ds.dsState = attrs.getValue("dsState");
                m_ds.logMessage = attrs.getValue("logMessage");
                setDatastreamState = true;

            } catch (Exception e) {
                failedCount++;
                logFailedDirective(m_ds.objectPID, localName, e, "");
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("setDatastreamVersionable")) {

            try {
                m_ds = new Datastream();
                setDatastreamVersionable = false;

                // Get require attributes
                m_ds.objectPID = attrs.getValue("pid");
                m_ds.dsID = attrs.getValue("dsID");
                m_ds.versionable =
                        new Boolean(attrs.getValue("versionable"))
                                .booleanValue();
                m_ds.logMessage = attrs.getValue("logMessage");
                setDatastreamVersionable = true;

            } catch (Exception e) {
                failedCount++;
                logFailedDirective(m_ds.objectPID, localName, e, "");
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("compareDatastreamChecksum")) {

            try {
                m_ds = new Datastream();
                compareDatastreamChecksum = false;

                // Get require attributes
                m_ds.objectPID = attrs.getValue("pid");
                m_ds.dsID = attrs.getValue("dsID");

                // Get optional attributes
                if (attrs.getValue("asOfDate") != null
                        && !attrs.getValue("asOfDate").equals("")) {
                    m_ds.asOfDate = attrs.getValue("asOfDate");
                }

                compareDatastreamChecksum = true;

            } catch (Exception e) {
                failedCount++;
                logFailedDirective(m_ds.objectPID, localName, e, "");
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("xmlData")) {
            m_inXMLMetadata = true;
            m_dsXMLBuffer = new StringBuffer();
        } else {
            if (m_inXMLMetadata) {
                appendElementStart(namespaceURI,
                                   localName,
                                   qName,
                                   attrs,
                                   m_dsXMLBuffer);
            }
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {

        if (m_inXMLMetadata) {
            if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                    && localName.equals("xmlData")) {
                try {
                    m_ds.xmlContent =
                            m_dsXMLBuffer.toString().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // won't happen
                }
                m_inXMLMetadata = false;
            } else {
                // finished an element in xmlData...append end tag.
                m_dsXMLBuffer.append("</");
                m_dsXMLBuffer.append(qName);
                m_dsXMLBuffer.append(">");
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("addObject")) {
            try {
                if (addObject) {
                    StringBuffer xml = new StringBuffer();
                    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    xml.append("<" + FOXML.DIGITAL_OBJECT.qName + " ");
                    xml.append(FOXML.VERSION.localName + "=\"1.1\"");
                    if (m_obj.pid != null && m_obj.pid.length() > 0) {
                        xml.append(" " + FOXML.PID.localName + "=\"");
                        xml.append(StreamUtility.enc(m_obj.pid) + "\"");
                    }
                    xml.append("\n");
                    xml.append("    xmlns:" + FOXML.prefix + "=\"");
                    xml.append(FOXML.uri + "\"\n");
                    xml.append("    xmlns:" + XSI.prefix + "=\"");
                    xml.append(XSI.uri + "\"\n");
                    xml.append("    " + XSI.SCHEMA_LOCATION.qName + "=\"");
                    xml.append(FOXML.uri + " " + FOXML1_1.xsdLocation);
                    xml.append("\">\n");
                    xml.append("  <" + FOXML.OBJECT_PROPERTIES.qName + ">\n");
                    appendProperty(xml, MODEL.LABEL.uri, m_obj.label);
                    xml.append("  </" + FOXML.OBJECT_PROPERTIES.qName + ">\n");
                    xml.append("</" + FOXML.DIGITAL_OBJECT.qName + ">");
                    String objXML = xml.toString();
                    ByteArrayInputStream in =
                            new ByteArrayInputStream(objXML.getBytes("UTF-8"));
                    String newPID =
                            AutoIngestor
                                    .ingestAndCommit(APIA,
                                                     APIM,
                                                     in,
                                                     FOXML1_1.uri,
                                                     "Created with BatchModify Utility \"addObject\" directive");
                    succeededCount++;
                    logSucceededDirective(newPID,
                                          localName,
                                          " Added new object with PID: "
                                                  + newPID);
                }
            } catch (Exception e) {
                if (addObject) {
                    failedCount++;
                    logFailedDirective(m_obj.pid, localName, e, "");
                }
            } finally {
                addObject = false;
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("modifyObject")) {

            try {

                // Process modifyObject only if no previous errors encountered
                if (modifyObject) {
                    APIM.modifyObject(m_obj.pid,
                                      m_obj.state,
                                      m_obj.label,
                                      m_obj.ownerId,
                                      "ModifyObject");
                }
                succeededCount++;
                logSucceededDirective(m_obj.pid, localName, "Object PID: "
                        + m_obj.pid + " modified");

            } catch (Exception e) {
                if (modifyObject) {
                    failedCount++;
                    logFailedDirective(m_obj.pid, localName, e, null);
                }
            } finally {
                modifyObject = false;
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("purgeObject")) {

            try {

                // Process purgeDatastream only if no previous errors encountered
                if (purgeObject) {
                    String purgedPid = null;
                    purgedPid =
                            APIM.purgeObject(m_obj.pid,
                                             "PurgeObject",
                                             m_obj.force);
                    if (purgedPid != null) {
                        succeededCount++;
                        logSucceededDirective(m_obj.pid,
                                              localName,
                                              "Purged PID: " + m_obj.pid);
                    } else {
                        failedCount++;
                        logFailedDirective(m_ds.objectPID,
                                           localName,
                                           null,
                                           "Unable to purge object with PID: "
                                                   + m_obj.pid);
                    }
                }
            } catch (Exception e) {
                if (purgeObject) {
                    failedCount++;
                    logFailedDirective(m_obj.pid, localName, e, "");

                }
            } finally {
                purgeObject = false;
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("addDatastream")) {
            try {

                // Process addDatastream only if no previous errors encountered
                if (addDatastream) {
                    String datastreamID = null;
                    if (m_ds.dsControlGrp.equalsIgnoreCase("X")) {
                        InputStream xmlMetadata =
                                new ByteArrayInputStream(m_ds.xmlContent);
                        m_ds.dsLocation = UPLOADER.upload(xmlMetadata);
                        datastreamID =
                                APIM.addDatastream(m_ds.objectPID,
                                                   m_ds.dsID,
                                                   m_ds.altIDs,
                                                   m_ds.dsLabel,
                                                   m_ds.versionable,
                                                   m_ds.dsMIME,
                                                   m_ds.formatURI,
                                                   m_ds.dsLocation,
                                                   m_ds.dsControlGrp,
                                                   m_ds.dsState,
                                                   m_ds.checksumType,
                                                   m_ds.checksum,
                                                   m_ds.logMessage);
                    } else if (m_ds.dsControlGrp.equalsIgnoreCase("E")
                            || m_ds.dsControlGrp.equalsIgnoreCase("M")
                            || m_ds.dsControlGrp.equalsIgnoreCase("R")) {
                        datastreamID =
                                APIM.addDatastream(m_ds.objectPID,
                                                   m_ds.dsID,
                                                   m_ds.altIDs,
                                                   m_ds.dsLabel,
                                                   m_ds.versionable,
                                                   m_ds.dsMIME,
                                                   m_ds.formatURI,
                                                   m_ds.dsLocation,
                                                   m_ds.dsControlGrp,
                                                   m_ds.dsState,
                                                   m_ds.checksumType,
                                                   m_ds.checksum,
                                                   m_ds.logMessage);
                    }
                    if (datastreamID != null) {
                        succeededCount++;
                        logSucceededDirective(m_ds.objectPID,
                                              localName,
                                              "datastreamID: " + datastreamID
                                                      + " added");
                    } else {
                        failedCount++;
                        logFailedDirective(m_ds.objectPID,
                                           localName,
                                           null,
                                           "Unable to add datastream");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (addDatastream) {
                    failedCount++;
                    logFailedDirective(m_ds.objectPID, localName, e, "");
                }
            } finally {
                addDatastream = false;
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("purgeDatastream")) {

            try {

                // Process purgeDatastream only if no previous errors encountered
                if (purgeDatastream) {
                    String[] versionsPurged = null;
                    versionsPurged =
                            APIM.purgeDatastream(m_ds.objectPID,
                                                 m_ds.dsID,
                                                 m_ds.asOfDate,
                                                 m_ds.endDate,
                                                 m_ds.logMessage,
                                                 m_ds.force);
                    if (versionsPurged.length > 0) {
                        succeededCount++;
                        if (m_ds.asOfDate != null && m_ds.endDate != null) {
                            logSucceededDirective(m_ds.objectPID,
                                                  localName,
                                                  "datastreamID: "
                                                          + m_ds.dsID
                                                          + "\n    Purged all versions from: "
                                                          + m_ds.asOfDate
                                                          + " to "
                                                          + m_ds.endDate
                                                          + "\n    Versions purged: "
                                                          + versionsPurged.length);
                        } else if (m_ds.asOfDate == null
                                && m_ds.endDate == null) {
                            logSucceededDirective(m_ds.objectPID,
                                                  localName,
                                                  "datastreamID: "
                                                          + m_ds.dsID
                                                          + "\n    Purged all versions. "
                                                          + "\n    Versions purged: "
                                                          + versionsPurged.length);
                        } else if (m_ds.asOfDate != null
                                && m_ds.endDate == null) {
                            logSucceededDirective(m_ds.objectPID,
                                                  localName,
                                                  "datastreamID: "
                                                          + m_ds.dsID
                                                          + "\n    Purged all versions after : "
                                                          + m_ds.asOfDate
                                                          + "\n    Versions purged: "
                                                          + versionsPurged.length);
                        } else if (m_ds.asOfDate == null
                                && m_ds.endDate != null) {
                            logSucceededDirective(m_ds.objectPID,
                                                  localName,
                                                  "datastreamID: "
                                                          + m_ds.dsID
                                                          + "\n    Purged all versions prior to : "
                                                          + m_ds.endDate
                                                          + "\n    Versions purged: "
                                                          + versionsPurged.length);
                        }
                    } else {
                        failedCount++;
                        logFailedDirective(m_ds.objectPID,
                                           localName,
                                           null,
                                           "Unable to purge datastream; verify datastream ID and/or asOfDate");
                    }
                }
            } catch (Exception e) {
                if (purgeDatastream) {
                    failedCount++;
                    logFailedDirective(m_ds.objectPID, localName, e, "");
                }
            } finally {
                purgeDatastream = false;
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("modifyDatastream")) {

            try {

                // Process modifyDatastream only if no previous errors encountered
                if (modifyDatastream) {
                    if (m_ds.dsControlGrp.equalsIgnoreCase("X")) {
                        APIM.modifyDatastreamByValue(m_ds.objectPID,
                                                     m_ds.dsID,
                                                     m_ds.altIDs,
                                                     m_ds.dsLabel,
                                                     m_ds.dsMIME,
                                                     m_ds.formatURI,
                                                     m_ds.xmlContent,
                                                     m_ds.checksumType,
                                                     m_ds.checksum,
                                                     m_ds.logMessage,
                                                     m_ds.force);
                    } else if (m_ds.dsControlGrp.equalsIgnoreCase("E")
                            || m_ds.dsControlGrp.equalsIgnoreCase("M")
                            || m_ds.dsControlGrp.equalsIgnoreCase("R")) {
                        APIM.modifyDatastreamByReference(m_ds.objectPID,
                                                         m_ds.dsID,
                                                         m_ds.altIDs,
                                                         m_ds.dsLabel,
                                                         m_ds.dsMIME,
                                                         m_ds.formatURI,
                                                         m_ds.dsLocation,
                                                         m_ds.checksumType,
                                                         m_ds.checksum,
                                                         m_ds.logMessage,
                                                         m_ds.force);
                    }
                    succeededCount++;
                    logSucceededDirective(m_ds.objectPID,
                                          localName,
                                          "DatastreamID: " + m_ds.dsID
                                                  + " modified");
                }

            } catch (Exception e) {
                if (modifyDatastream) {
                    failedCount++;
                    logFailedDirective(m_ds.objectPID, localName, e, null);
                }
            } finally {
                modifyDatastream = false;
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("setDatastreamState")) {

            try {

                // Process setDatastreamState only if no previous errors encountered
                if (setDatastreamState) {
                    APIM.setDatastreamState(m_ds.objectPID,
                                            m_ds.dsID,
                                            m_ds.dsState,
                                            "SetDatastreamState");
                    succeededCount++;
                    logSucceededDirective(m_ds.objectPID,
                                          localName,
                                          "datastream: " + m_ds.dsID
                                                  + "\n    Set dsState: "
                                                  + m_ds.dsState);
                }

            } catch (Exception e) {
                if (setDatastreamState) {
                    failedCount++;
                    logFailedDirective(m_ds.objectPID, localName, e, null);
                }
            } finally {
                setDatastreamState = false;
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("setDatastreamVersionable")) {

            try {

                // Process setDatastreamVersionable only if no previous errors encountered
                if (setDatastreamVersionable) {
                    APIM.setDatastreamVersionable(m_ds.objectPID,
                                                  m_ds.dsID,
                                                  m_ds.versionable,
                                                  "SetDatastreamVersionable");
                    succeededCount++;
                    logSucceededDirective(m_ds.objectPID,
                                          localName,
                                          "datastream: " + m_ds.dsID
                                                  + "\n    Set dsVersionable: "
                                                  + m_ds.versionable);
                }

            } catch (Exception e) {
                if (setDatastreamVersionable) {
                    failedCount++;
                    logFailedDirective(m_ds.objectPID, localName, e, null);
                }
            } finally {
                setDatastreamVersionable = false;
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("compareDatastreamChecksum")) {

            try {

                // Process compareDatastreamChecksum only if no previous errors encountered
                if (compareDatastreamChecksum) {
                    String msg =
                            APIM.compareDatastreamChecksum(m_ds.objectPID,
                                                           m_ds.dsID,
                                                           m_ds.asOfDate);

                    if (!msg.equals("Checksum validation error")) {
                        succeededCount++;
                        logSucceededDirective(m_ds.objectPID,
                                              localName,
                                              "datastream: "
                                                      + m_ds.dsID
                                                      + "\n    compareDatastreamChecksum: "
                                                      + msg);
                    } else {
                        throw new Exception("Checksum validation error");
                    }
                }

            } catch (Exception e) {
                if (compareDatastreamChecksum) {
                    failedCount++;
                    logFailedDirective(m_ds.objectPID, localName, e, null);
                }
            } finally {
                compareDatastreamChecksum = false;
            }
        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("addDatastreamBinding")) {

        } else if (namespaceURI.equalsIgnoreCase(BATCH_MODIFY.uri)
                && localName.equalsIgnoreCase("removeDatastreamBinding")) {

        }
    }

    private static void appendProperty(StringBuffer xml,
                                       String uri,
                                       String value) {
        xml.append("    <" + FOXML.PROPERTY.qName + " ");
        xml.append(FOXML.NAME.localName + "=\"");
        xml.append(uri + "\" " + FOXML.VALUE.localName + "=\"");
        xml.append(StreamUtility.enc(value) + "\"/>\n");
    }

    /**
     * <p>
     * Write a log of what happened when a directive fails.
     * <p>
     *
     * @param sourcePID
     *        - The PID of the object being processed.
     * @param directive
     *        - The name of the directive being processed.
     * @param e
     *        - The Exception that was thrown.
     * @param msg
     *        - A message providing additional info if no Exception was thrown.
     */
    private static void logFailedDirective(String sourcePID,
                                           String directive,
                                           Exception e,
                                           String msg) {
        out.println("  <failed directive=\"" + directive + "\" sourcePID=\""
                + sourcePID + "\">");
        if (e != null) {
            String message = e.getMessage();
            if (message == null) {
                message = e.getClass().getName();
            }
            out.println("    " + StreamUtility.enc(message));
        } else {
            out.println("    " + StreamUtility.enc(msg));
        }
        out.println("  </failed>");
    }

    /**
     * <p>
     * Write a log of what happened when there is a parsing error.
     * </p>
     *
     * @param e
     *        - The Exception that was thrown.
     * @param msg
     *        - A message indicating additional info if no Exception was thrown.
     */
    private static void logParserError(Exception e, String msg) {
        out.println("  <parserError>");
        if (e != null) {
            String message = e.getMessage();
            if (message == null) {
                message = e.getClass().getName();
            }
            out.println("    " + StreamUtility.enc(message));
        } else {
            out.println("    " + StreamUtility.enc(msg));
        }
        out.println("  </parserError>");
    }

    /**
     * <p>
     * Write a log when a directive is successfully processed.
     * </p>
     *
     * @param sourcePID
     *        - The PID of the object processed.
     * @param directive
     *        - The name of the directive processed.
     * @param msg
     *        - A message.
     */
    private static void logSucceededDirective(String sourcePID,
                                              String directive,
                                              String msg) {
        out.println("  <succeeded directive=\"" + directive + "\" sourcePID=\""
                + sourcePID + "\">");
        out.println("    " + StreamUtility.enc(msg));
        out.println("  </succeeded>");
    }

    public static Map getServiceLabelMap() throws IOException {
        try {
            HashMap<String, String> labelMap = new HashMap<String, String>();
            FieldSearchQuery query = new FieldSearchQuery();
            Condition[] conditions = new Condition[1];
            conditions[0] = new Condition();
            conditions[0].setProperty("fType");
            conditions[0].setOperator(ComparisonOperator.fromValue("eq"));
            conditions[0].setValue("D");
            query.setConditions(conditions);
            String[] fields = new String[] {"pid", "label"};

            if (true) {
                /* FIXME: find some other way to do this */
                throw new UnsupportedOperationException("This operation uses obsolete field search semantics");
            }

            FieldSearchResult result =
                    APIA.findObjects(fields,
                                     new NonNegativeInteger("50"),
                                     query);
            while (result != null) {
                ObjectFields[] resultList = result.getResultList();
                for (ObjectFields element : resultList) {
                    labelMap.put(element.getPid(), element.getLabel());
                }
                if (result.getListSession() != null) {
                    result =
                            APIA.resumeFindObjects(result.getListSession()
                                    .getToken());
                } else {
                    result = null;
                }
            }
            return labelMap;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Get a map of pid-to-label of service deployments that implement the
     * service defined by the indicated definition.
     *
     * @param sDefPID
     *        PID of the associated service defintion object.
     * @return A list of the service deployment labels.
     * @throws IOException
     *         If an error occurs in retrieving the list of labels.
     */
    public static Map getDeploymentLabelMap(String sDefPID) throws IOException {
        try {
            HashMap<String, String> labelMap = new HashMap<String, String>();
            FieldSearchQuery query = new FieldSearchQuery();
            Condition[] conditions = new Condition[2];
            conditions[0] = new Condition();
            conditions[0].setProperty("fType");
            conditions[0].setOperator(ComparisonOperator.fromValue("eq"));
            conditions[0].setValue("M");
            conditions[1] = new Condition();
            conditions[1].setProperty("bDef");
            conditions[1].setOperator(ComparisonOperator.fromValue("has"));
            conditions[1].setValue(sDefPID);
            query.setConditions(conditions);
            String[] fields = new String[] {"pid", "label"};

            if (true) {
                /*
                 * FIXME: find some other way to do this, if we care. It uses
                 * fType and bDef in field search, which are no longer
                 * available.
                 */
                throw new UnsupportedOperationException("This operation uses obsolete field search semantics");
            }
            FieldSearchResult result =
                    APIA.findObjects(fields,
                                     new NonNegativeInteger("50"),
                                     query);
            while (result != null) {
                ObjectFields[] resultList = result.getResultList();
                for (ObjectFields element : resultList) {
                    labelMap.put(element.getPid(), element.getLabel());
                }
                if (result.getListSession() != null) {
                    result =
                            APIA.resumeFindObjects(result.getListSession()
                                    .getToken());
                } else {
                    result = null;
                }
            }
            return labelMap;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * <p>
     * Main method for testing only.
     * </p>
     *
     * @param args
     *        Array of input parms consisting of hostname, port, username,
     *        password, protocol, directives, log.
     */
    public static void main(String[] args) {

        if (args.length == 5 || args.length == 6) {
            String host = args[0];
            int port = new Integer(args[1]).intValue();
            String user = args[2];
            String pass = args[3];
            String protocol = args[4];

            String context = Constants.FEDORA_DEFAULT_APP_CONTEXT;
            if (args.length == 6 && !args[5].equals("")) {
                context = args[5];
            }

            PrintStream logFile;
            FedoraAPIM APIM;
            FedoraAPIA APIA;

            try {
                UPLOADER = new Uploader(host, port, context, user, pass);
                logFile =
                        new PrintStream(new FileOutputStream("C:\\zlogfile.txt"));
                //APIM = fedora.client.APIMStubFactory.getStub(protocol, host, port, user, pass);
                //APIA = fedora.client.APIAStubFactory.getStub(protocol, host, port, user, pass);

                // ******************************************
                // NEW: use new client utility class
                String baseURL =
                        protocol + "://" + host + ":" + port + "/" + context;
                FedoraClient fc = new FedoraClient(baseURL, user, pass);
                APIA = fc.getAPIA();
                APIM = fc.getAPIM();
                //*******************************************

                InputStream file =
                        new FileInputStream("c:\\fedora\\mellon\\dist\\client\\demo\\batch-demo\\modify-batch-directives-valid.xml");
                BatchModifyParser bmp =
                        new BatchModifyParser(UPLOADER,
                                              APIM,
                                              APIA,
                                              file,
                                              logFile);
                file.close();
                logFile.close();
            } catch (Exception e) {
                System.out.println("ERROR: "
                        + e.getClass().getName()
                        + " - "
                        + (e.getMessage() == null ? "(no detail provided)" : e
                                .getMessage()));
            }
        } else {
            System.out.println("Enter args for: host port user pass protocol");
        }
    }

}
