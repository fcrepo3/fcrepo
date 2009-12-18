/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fedora.common.Constants;
import fedora.common.xml.format.XMLFormat;
import fedora.common.xml.namespace.XMLNamespace;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.ValidationException;
import fedora.server.storage.types.AuditRecord;
import fedora.server.storage.types.DSBinding;
import fedora.server.storage.types.DSBindingMap;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamManagedContent;
import fedora.server.storage.types.DatastreamReferencedContent;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.Disseminator;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.StreamUtility;
import fedora.server.validation.ValidationUtility;

import fedora.utilities.Base64;

/**
 * Deserializes objects in the constructor-provided version of the METS Fedora
 * Extension format.
 *
 * @author Sandy Payette
 * @author Chris Wilper
 */
@SuppressWarnings("deprecation")
public class METSFedoraExtDODeserializer
        extends DefaultHandler
        implements Constants, DODeserializer {

    /**
     * The format this deserializer will read if unspecified at construction.
     * This defaults to the latest FOXML format.
     */
    public static final XMLFormat DEFAULT_FORMAT = METS_EXT1_1;

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(METSFedoraExtDODeserializer.class.getName());

    /** The format this deserializer reads. */
    private final XMLFormat m_format;

    /** The xlink namespace this deserializer understands; depends on format. */
    private final XMLNamespace m_xlink;

    /** The current translation context. */
    private int m_transContext;

    /** The object to deserialize to. */
    private DigitalObject m_obj;

    /** Buffer to build RDF expression of ADMID and DMDID relationships * */
    private StringBuffer m_relsBuffer;

    private boolean hasRels = false;

    /** Hashtables to record DMDID references */
    private HashMap<String, List<String>> m_dsDMDIDs; // key=dsVersionID, value=ArrayList of dsID

    /** Hashtables to record ADMID references */
    private HashMap<String, List<String>> m_dsADMIDs; // key=dsVersionID, value=ArrayList of dsID

    /** Hashtables to correlate audit record ids to datastreams */
    private HashMap<String, String> m_AuditIdToComponentId;

    private SAXParser m_parser;

    private String m_characterEncoding;

    /** Namespace prefix-to-URI mapping info from SAX2 startPrefixMapping events. */
    private HashMap<String, String> m_prefixMap;

    private HashMap<String, String> m_localPrefixMap;

    private ArrayList<String> m_prefixList;

    /** Variables to parse into */
    private boolean m_rootElementFound;

    private String m_agentRole;

    private String m_dsId;

    private String m_dsVersId;

    private Date m_dsCreateDate;

    private String m_dissemId;

    private String m_dissemState;

    private String m_dsState;

    private String m_dsInfoType;

    private String m_dsOtherInfoType;

    private String m_dsLabel;

    private int m_dsMDClass;

    private long m_dsSize;

    private String m_dsLocation;

    private String m_dsLocationType;

    private String m_dsMimeType;

    private String m_dsControlGrp;

    private boolean m_dsVersionable;

    private String m_dsFormatURI;

    private String[] m_dsAltIDs;

    private String m_dsChecksum;

    private String m_dsChecksumType;

    private StringBuffer m_dsXMLBuffer;

    // are we reading binary in an FContent element? (base64-encoded)
    private boolean m_readingContent; // indicates reading element content

    private boolean m_readingBinaryContent; // indicates reading binary element content

    private File m_binaryContentTempFile;

    private StringBuffer m_elementContent; // single element

    /** While parsing, are we inside XML metadata? */
    private boolean m_inXMLMetadata;

    /**
     * Used to differentiate between a metadata section in this object and a
     * metadata section in an inline XML datastream that happens to be a METS
     * document.
     */
    private int m_xmlDataLevel;

    /** String buffer for audit element contents */
    private StringBuffer m_auditBuffer;

    private String m_auditId;

    private String m_auditProcessType;

    private String m_auditAction;

    private String m_auditComponentID;

    private String m_auditResponsibility;

    private String m_auditDate;

    private String m_auditJustification;

    /**
     * Hashmap for holding disseminators during parsing, keyed by structMapId
     */
    private HashMap<String, Disseminator> m_dissems;

    /**
     * Currently-being-initialized disseminator, during structmap parsing.
     */
    private Disseminator m_diss;

    /**
     * Whether, while in structmap, we've already seen a div
     */
    private boolean m_indiv;

    /** The structMapId of the dissem currently being parsed. */
    private String m_structId;

    /**
     * Creates a deserializer that reads the default Fedora METS Extension
     * format.
     */
    public METSFedoraExtDODeserializer() {
        this(DEFAULT_FORMAT);
    }

    /**
     * Creates a deserializer that reads the given Fedora METS Extension format.
     *
     * @param format
     *        the version-specific Fedora METS Extension format.
     * @throws IllegalArgumentException
     *         if format is not a known Fedora METS Extension format.
     */
    public METSFedoraExtDODeserializer(XMLFormat format) {
        if (format.equals(METS_EXT1_0)) {
            m_xlink = OLD_XLINK;
        } else if (format.equals(METS_EXT1_1)) {
            m_xlink = XLINK;
        } else {
            throw new IllegalArgumentException("Not a METSFedoraExt format: "
                    + format.uri);
        }
        m_format = format;
    }

    //---
    // DODeserializer implementation
    //---

    /**
     * {@inheritDoc}
     */
    public DODeserializer getInstance() {
        return new METSFedoraExtDODeserializer(m_format);
    }

    /**
     * {@inheritDoc}
     */
    public void deserialize(InputStream in,
                            DigitalObject obj,
                            String encoding,
                            int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedEncodingException {
        LOG.debug("Deserializing " + m_format.uri + " for transContext: "
                + transContext);

        // initialize sax for this parse
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setValidating(false);
            spf.setNamespaceAware(true);
            m_parser = spf.newSAXParser();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing SAX parser", e);
        }

        m_obj = obj;
        m_obj.setOwnerId("");
        m_obj.setLabel("");
        m_characterEncoding = encoding;
        m_transContext = transContext;
        initialize();
        try {
            m_parser.parse(in, this);
        } catch (IOException ioe) {
            throw new StreamIOException("Low-level stream IO problem occurred "
                    + "while SAX parsing this object.");
        } catch (SAXException se) {
            throw new ObjectIntegrityException("METS stream was bad : "
                    + se.getMessage());
        }
        if (!m_rootElementFound) {
            throw new ObjectIntegrityException("METS root element not found");
        }

        // POST-PROCESSING...
        // convert audit records to contain component ids
        convertAudits();
        // preserve ADMID and DMDID relationships in a RELS-INT
        // datastream, if one does not already exist.
        createRelsInt();

        DOTranslationUtility.normalizeDatastreams(m_obj,
                                                  m_transContext,
                                                  m_characterEncoding);

        if (m_format.equals(METS_EXT1_0)) {
            // DISSEMINATORS... put disseminators in the instantiated digital
            // object
            Iterator<Disseminator> dissemIter = m_dissems.values().iterator();
            while (dissemIter.hasNext()) {
                Disseminator diss = dissemIter.next();
                m_obj.disseminators(diss.dissID).add(diss);
            }
        }

    }

    //---
    // DefaultHandler overrides
    //---

    /**
     * {@inheritDoc}
     */
    @Override
    public void startPrefixMapping(String prefix, String uri) {
        // Keep the prefix map up-to-date throughout the entire parse,
        // and maintain a list of newly mapped prefixes on a per-element basis.
        m_prefixMap.put(prefix, uri);
        if (m_inXMLMetadata) {
            m_localPrefixMap.put(prefix, uri);
            m_prefixList.add(prefix);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endPrefixMapping(String prefix) {
        m_prefixMap.remove(prefix);
        if (m_inXMLMetadata) {
            m_localPrefixMap.remove(prefix);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes a) throws SAXException {
        if (uri.equals(METS.uri) && !m_inXMLMetadata) {
            // a new mets element is starting
            if (localName.equals("mets")) {
                m_rootElementFound = true;
                m_obj.setPid(grab(a, METS.uri, "OBJID"));
                m_obj.setLabel(grab(a, METS.uri, "LABEL"));
                if (m_format.equals(METS_EXT1_0)) {
                    // In METS_EXT 1.0, the PROFILE attribute mapped to an
                    // object property, fedora-model:contentModel.  This will be
                    // retained as an extended property in the DigitalObject.
                    m_obj.setExtProperty(MODEL.CONTENT_MODEL.uri,
                                         grab(a, METS.uri, "PROFILE"));
                    // Similarly, the TYPE attribute mapped to rdf:type, and
                    // will also be retained as an external property.
                    m_obj.setExtProperty(RDF.TYPE.uri,
                                         grab(a, METS.uri, "TYPE"));
                }
            } else if (localName.equals("metsHdr")) {
                m_obj.setCreateDate(DateUtility
                        .convertStringToDate(grab(a, METS.uri, "CREATEDATE")));
                m_obj.setLastModDate(DateUtility
                        .convertStringToDate(grab(a, METS.uri, "LASTMODDATE")));
                try {
                    m_obj.setState(DOTranslationUtility
                        .readStateAttribute(grab(a, METS.uri, "RECORDSTATUS")));
                } catch (ParseException e) {
                    throw new SAXException("Could not read object state", e);
                }
            } else if (localName.equals("agent")) {
                m_agentRole = grab(a, METS.uri, "ROLE");
            } else if (localName.equals("name")
                    && m_agentRole.equals("IPOWNER")) {
                m_readingContent = true;
                m_elementContent = new StringBuffer();
            } else if (localName.equals("amdSec")) {
                m_dsId = grab(a, METS.uri, "ID");
                m_dsState = grab(a, METS.uri, "STATUS");
                String dsVersionable = grab(a, METS.uri, "VERSIONABLE");
                if (dsVersionable != null && !dsVersionable.equals("")) {
                    m_dsVersionable =
                            new Boolean(grab(a, METS.uri, "VERSIONABLE"))
                                    .booleanValue();
                } else {
                    m_dsVersionable = true;
                }
            } else if (localName.equals("dmdSecFedora")) {
                m_dsId = grab(a, METS.uri, "ID");
                m_dsState = grab(a, METS.uri, "STATUS");
                String dsVersionable = grab(a, METS.uri, "VERSIONABLE");
                if (dsVersionable != null && !dsVersionable.equals("")) {
                    m_dsVersionable =
                            new Boolean(grab(a, METS.uri, "VERSIONABLE"))
                                    .booleanValue();
                } else {
                    m_dsVersionable = true;
                }
            } else if (localName.equals("techMD") || localName.equals("descMD")
                    || localName.equals("sourceMD")
                    || localName.equals("rightsMD")
                    || localName.equals("digiprovMD")) {
                m_dsVersId = grab(a, METS.uri, "ID");
                if (localName.equals("techMD")) {
                    m_dsMDClass = DatastreamXMLMetadata.TECHNICAL;
                }
                if (localName.equals("sourceMD")) {
                    m_dsMDClass = DatastreamXMLMetadata.SOURCE;
                }
                if (localName.equals("rightsMD")) {
                    m_dsMDClass = DatastreamXMLMetadata.RIGHTS;
                }
                if (localName.equals("digiprovMD")) {
                    m_dsMDClass = DatastreamXMLMetadata.DIGIPROV;
                }
                if (localName.equals("descMD")) {
                    m_dsMDClass = DatastreamXMLMetadata.DESCRIPTIVE;
                }
                String dateString = grab(a, METS.uri, "CREATED");
                if (dateString != null && !dateString.equals("")) {
                    m_dsCreateDate =
                            DateUtility.convertStringToDate(dateString);
                }
            } else if (localName.equals("mdWrap")) {
                m_dsInfoType = grab(a, METS.uri, "MDTYPE");
                m_dsOtherInfoType = grab(a, METS.uri, "OTHERMDTYPE");
                m_dsLabel = grab(a, METS.uri, "LABEL");
                m_dsMimeType = grab(a, METS.uri, "MIMETYPE");
                m_dsFormatURI = grab(a, METS.uri, "FORMAT_URI");
                String altIDs = grab(a, METS.uri, "ALT_IDS");
                if (altIDs.length() == 0) {
                    m_dsAltIDs = new String[0];
                } else {
                    m_dsAltIDs = altIDs.split(" ");
                }
                m_dsChecksum = grab(a, METS.uri, "CHECKSUM");
                m_dsChecksumType = grab(a, METS.uri, "CHECKSUMTYPE");
            } else if (localName.equals("xmlData")) {
                m_dsXMLBuffer = new StringBuffer();
                m_xmlDataLevel = 0;
                m_inXMLMetadata = true;
            } else if (localName.equals("fileGrp")) {
                m_dsId = grab(a, METS.uri, "ID");
                String dsVersionable = grab(a, METS.uri, "VERSIONABLE");
                if (dsVersionable != null && !dsVersionable.equals("")) {
                    m_dsVersionable =
                            new Boolean(grab(a, METS.uri, "VERSIONABLE"))
                                    .booleanValue();
                } else {
                    m_dsVersionable = true;
                }
                // reset the values for the next file
                m_dsVersId = "";
                m_dsCreateDate = null;
                m_dsMimeType = "";
                m_dsControlGrp = "";
                m_dsFormatURI = "";
                m_dsAltIDs = new String[0];
                m_dsState = grab(a, METS.uri, "STATUS");
                m_dsSize = -1;
                m_dsChecksum = "";
                m_dsChecksumType = "";
            } else if (localName.equals("file")) {
                m_dsVersId = grab(a, METS.uri, "ID");
                String dateString = grab(a, METS.uri, "CREATED");
                if (dateString != null && !dateString.equals("")) {
                    m_dsCreateDate =
                            DateUtility.convertStringToDate(dateString);
                }
                m_dsMimeType = grab(a, METS.uri, "MIMETYPE");
                m_dsControlGrp = grab(a, METS.uri, "OWNERID");
                String ADMID = grab(a, METS.uri, "ADMID");
                if (ADMID != null && !"".equals(ADMID)) {
                    ArrayList<String> al = new ArrayList<String>();
                    if (ADMID.indexOf(" ") != -1) {
                        String[] admIds = ADMID.split(" ");
                        for (String element : admIds) {
                            al.add(element);
                        }
                    } else {
                        al.add(ADMID);
                    }
                    m_dsADMIDs.put(m_dsVersId, al);
                }
                String DMDID = grab(a, METS.uri, "DMDID");
                if (DMDID != null && !"".equals(DMDID)) {
                    ArrayList<String> al = new ArrayList<String>();
                    if (DMDID.indexOf(" ") != -1) {
                        String[] dmdIds = DMDID.split(" ");
                        for (String element : dmdIds) {
                            al.add(element);
                        }
                    } else {
                        al.add(DMDID);
                    }
                    m_dsDMDIDs.put(m_dsVersId, al);
                }
                String sizeString = grab(a, METS.uri, "SIZE");
                if (sizeString != null && !sizeString.equals("")) {
                    try {
                        m_dsSize = Long.parseLong(sizeString);
                    } catch (NumberFormatException nfe) {
                        throw new SAXException("If specified, a datastream's "
                                + "SIZE attribute must be an xsd:long.");
                    }
                }
                String formatURI = grab(a, METS.uri, "FORMAT_URI");
                if (formatURI != null && !formatURI.equals("")) {
                    m_dsFormatURI = formatURI;
                }
                String altIDs = grab(a, METS.uri, "ALT_IDS");
                if (altIDs.length() == 0) {
                    m_dsAltIDs = new String[0];
                } else {
                    m_dsAltIDs = altIDs.split(" ");
                }
                m_dsChecksum = grab(a, METS.uri, "CHECKSUM");
                m_dsChecksumType = grab(a, METS.uri, "CHECKSUMTYPE");
                // inside a "file" element, it's either going to be
                // FLocat (a reference) or FContent (inline)
            } else if (localName.equals("FLocat")) {
                m_dsLabel = grab(a, m_xlink.uri, "title");
                String dsLocation = grab(a, m_xlink.uri, "href");
                if (dsLocation == null || dsLocation.equals("")) {
                    throw new SAXException("xlink:href must be specified in FLocat element");
                }

                if (m_dsControlGrp.equalsIgnoreCase("E")
                        || m_dsControlGrp.equalsIgnoreCase("R")) {

                    // URL FORMAT VALIDATION for dsLocation:
                    // make sure we have a properly formed URL (must have protocol)
                    try {
                        ValidationUtility.validateURL(dsLocation, m_dsControlGrp);
                    } catch (ValidationException ve) {
                        throw new SAXException(ve.getMessage());
                    }
                    // system will set dsLocationType for E and R datastreams...
                    m_dsLocationType = "URL";
                    m_dsInfoType = "DATA";
                    m_dsLocation = dsLocation;
                    instantiateDatastream(new DatastreamReferencedContent());
                } else if (m_dsControlGrp.equalsIgnoreCase("M")) {
                    // URL FORMAT VALIDATION for dsLocation:
                    // For Managed Content the URL is only checked when we are parsing a
                    // a NEW ingest file because the URL is replaced with an internal identifier
                    // once the repository has sucked in the content for storage.
                    if (m_obj.isNew()) {
                        try {
                            ValidationUtility.validateURL(dsLocation, m_dsControlGrp);
                        } catch (ValidationException ve) {
                            throw new SAXException(ve.getMessage());
                        }
                    }
                    m_dsLocationType = "INTERNAL_ID";
                    m_dsInfoType = "DATA";
                    m_dsLocation = dsLocation;
                    instantiateDatastream(new DatastreamManagedContent());
                }
            } else if (localName.equals("FContent")) {
                // In METS_EXT, the FContent element contains base64-encoded
                // data.
                m_readingContent = true;
                m_elementContent = new StringBuffer();
                if (m_dsControlGrp.equalsIgnoreCase("M")) {
                    m_readingBinaryContent = true;
                    m_binaryContentTempFile = null;
                    try {
                        m_binaryContentTempFile =
                                File.createTempFile("binary-datastream", null);
                    } catch (IOException ioe) {
                        throw new SAXException(new StreamIOException("Unable to create temporary file for binary content"));
                    }
                }

            } else if (m_format.equals(METS_EXT1_0)) {
                startDisseminators(localName, a);
            }
        } else {
            if (m_inXMLMetadata) {
                // must be in xmlData... just output it, remembering the number
                // of METS:xmlData elements we see
                appendElementStart(uri, localName, qName, a, m_dsXMLBuffer);

                // METS INSIDE METS! we have an inline XML datastream
                // that is itself METS.  We do not want to parse this!
                if (uri.equals(METS.uri) && localName.equals("xmlData")) {
                    m_xmlDataLevel++;
                }
                // remember this stuff... (we don't have to look at level
                // because the audit schema doesn't allow for xml elements inside
                // these, so they're never set incorrectly)
                // signaling that we're interested in sending char data to
                // the m_auditBuffer by making it non-null, and getting
                // ready to accept data by allocating a new StringBuffer
                if (m_dsId.equals("FEDORA-AUDITTRAIL")
                        || m_dsId.equals("AUDIT")) {
                    if (localName.equals("record")) {
                        m_auditId = grab(a, uri, "ID");
                    } else if (localName.equals("process")) {
                        m_auditProcessType = grab(a, uri, "type");
                    } else if (localName.equals("action")
                            || localName.equals("componentID")
                            || localName.equals("responsibility")
                            || localName.equals("date")
                            || localName.equals("justification")) {
                        m_auditBuffer = new StringBuffer();
                    }
                }
            } else {
                // ignore all else
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(char[] ch, int start, int length) {
        if (m_inXMLMetadata) {
            if (m_auditBuffer != null) {
                m_auditBuffer.append(ch, start, length);
            } else {
                // since this data is encoded straight back to xml,
                // we need to make sure special characters &, <, >, ", and '
                // are re-converted to the xml-acceptable equivalents.
                StreamUtility.enc(ch, start, length, m_dsXMLBuffer);
            }
        } else if (m_readingContent) {
            // read normal element content into a string buffer
            if (m_elementContent != null) {
                m_elementContent.append(ch, start, length);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        // first, deal with the situation when we are processing a block of inline XML
        if (m_inXMLMetadata) {
            if (uri.equals(METS.uri) && localName.equals("xmlData")
                    && m_xmlDataLevel == 0) {
                // finished all xml metadata for this datastream
                if (m_dsId.equals("FEDORA-AUDITTRAIL")
                        || m_dsId.equals("AUDIT")) {
                    // we've been looking at an audit trail... set audit record
                    AuditRecord a = new AuditRecord();
                    // In METS each audit record is in its own <digiprovMD>
                    // element within an <amdSec>.  So, pick up the XML ID
                    // of the <digiprovMD> element for the audit record id.
                    // This amdSec is treated like a datastream, and each
                    // digiprovMD is a version, so id was parsed into dsVersId.
                    a.id = m_auditId; //m_dsVersId;
                    a.processType = m_auditProcessType;
                    a.action = m_auditAction;
                    a.componentID = m_auditComponentID;
                    a.responsibility = m_auditResponsibility;
                    a.date = DateUtility.convertStringToDate(m_auditDate);
                    a.justification = m_auditJustification;
                    m_obj.getAuditRecords().add(a);
                    m_inXMLMetadata = false; // other stuff is re-initted upon
                    // startElement for next xml metadata
                    // element
                } else {
                    // Create the right kind of datastream and add to the object
                    DatastreamXMLMetadata ds = new DatastreamXMLMetadata();
                    instantiateXMLDatastream(ds);
                    m_inXMLMetadata = false;
                    m_localPrefixMap.clear();
                }
            } else {
                // finished an element within inline xml metadata
                m_dsXMLBuffer.append("</" + qName + ">");
                // make sure we know when to pay attention to METS again
                if (uri.equals(METS.uri) && localName.equals("xmlData")) {
                    m_xmlDataLevel--;
                }
                if (m_dsId.equals("FEDORA-AUDITTRAIL")
                        || m_dsId.equals("AUDIT")) {
                    if (localName.equals("action")) {
                        m_auditAction = m_auditBuffer.toString();
                        m_auditBuffer = null;
                    } else if (localName.equals("componentID")) {
                        m_auditComponentID = m_auditBuffer.toString();
                        m_auditBuffer = null;
                    } else if (localName.equals("responsibility")) {
                        m_auditResponsibility = m_auditBuffer.toString();
                        m_auditBuffer = null;
                    } else if (localName.equals("date")) {
                        m_auditDate = m_auditBuffer.toString();
                        m_auditBuffer = null;
                    } else if (localName.equals("justification")) {
                        m_auditJustification = m_auditBuffer.toString();
                        m_auditBuffer = null;
                    }
                }
            }
            // ALL OTHER ELEMENT CASES: we are NOT processing a block of inline XML metadata
        } else {
            if (m_readingBinaryContent) {
                // In the version of METS Fedora uses, FContent assumes base64-encoded content
                if (uri.equals(METS.uri) && localName.equals("FContent")) {
                    if (m_binaryContentTempFile != null) {
                        try {
                            FileOutputStream os =
                                    new FileOutputStream(m_binaryContentTempFile);
                            // remove all spaces and newlines, this might not be necessary.
                            String elementStr =
                                    m_elementContent.toString()
                                            .replaceAll("\\s", "");
                            byte elementBytes[] = Base64.decode(elementStr);
                            os.write(elementBytes);
                            os.close();
                            m_dsLocationType = "INTERNAL_ID";
                            m_dsLocation =
                                DatastreamManagedContent.TEMP_SCHEME
                                            + m_binaryContentTempFile
                                                    .getAbsolutePath();
                            instantiateDatastream(new DatastreamManagedContent());
                        } catch (FileNotFoundException fnfe) {
                            throw new SAXException(new StreamIOException("Unable to open temporary file created for binary content"));
                        } catch (IOException fnfe) {
                            throw new SAXException(new StreamIOException("Error writing to temporary file created for binary content"));
                        }
                    }
                }
                m_binaryContentTempFile = null;
                m_readingBinaryContent = false;
                m_elementContent = null;
                // all other cases...
            } else {
                if (m_readingContent) {
                    // elements for which we were reading regular content
                    if (uri.equals(METS.uri) && localName.equals("name")
                            && m_agentRole.equals("IPOWNER")) {
                        m_obj.setOwnerId(m_elementContent.toString());
                    } else if (uri.equals(METS.uri)
                            && localName.equals("agent")) {
                        m_agentRole = null;
                    }
                    m_readingContent = false;
                    m_elementContent = null;
                } else {
                    // no other processing requirements at this time
                }
            }
        }
    }

    //---
    // Instance helpers
    //---

    private void startDisseminators(String localName, Attributes a)
            throws SAXException {
        if (localName.equals("structMap")) {
            // this is a component of a disseminator.  here we assume the rest
            // of the disseminator's information will be seen later, so we
            // construct a new Disseminator object to hold the structMap...
            // and later, the other info
            //
            // Building up a global map of Disseminators, m_dissems,
            // keyed by bindingmap ID.
            //
            if (grab(a, METS.uri, "TYPE").equals("fedora:dsBindingMap")) {
                String bmId = grab(a, METS.uri, "ID");
                if (bmId == null || bmId.equals("")) {
                    throw new SAXException("structMap with TYPE "
                            + "fedora:dsBindingMap must specify a non-empty "
                            + "ID attribute.");
                } else {
                    Disseminator diss = new Disseminator();
                    diss.dsBindMapID = bmId;
                    m_dissems.put(bmId, diss);
                    m_diss = diss;
                    m_diss.dsBindMap = new DSBindingMap();
                    m_diss.dsBindMap.dsBindMapID = bmId;
                    m_indiv = false; // flag we're not looking at inner part yet
                }
            } else {
                throw new SAXException("StructMap must have TYPE fedora:dsBindingMap");
            }
        } else if (localName.equals("div")) {
            if (m_indiv) {
                // inner part of structmap
                DSBinding binding = new DSBinding();
                if (m_diss.dsBindMap.dsBindings == null) {
                    // none yet.. create array of size one
                    DSBinding[] bindings = new DSBinding[1];
                    m_diss.dsBindMap.dsBindings = bindings;
                    m_diss.dsBindMap.dsBindings[0] = binding;
                } else {
                    // need to expand the array size by one,
                    // and do an array copy.
                    int curSize = m_diss.dsBindMap.dsBindings.length;
                    DSBinding[] oldArray = m_diss.dsBindMap.dsBindings;
                    DSBinding[] newArray = new DSBinding[curSize + 1];
                    for (int i = 0; i < curSize; i++) {
                        newArray[i] = oldArray[i];
                    }
                    newArray[curSize] = binding;
                    m_diss.dsBindMap.dsBindings = newArray;
                }
                // now populate 'binding' values...we'll have
                // everything at this point except datastreamID...
                // that comes as a child: <fptr FILEID="DS2"/>
                binding.bindKeyName = grab(a, METS.uri, "TYPE");
                binding.bindLabel = grab(a, METS.uri, "LABEL");
                binding.seqNo = grab(a, METS.uri, "ORDER");
            } else {
                m_indiv = true;
                // first (outer div) part of structmap
                m_diss.dsBindMap.dsBindMechanismPID = grab(a, METS.uri, "TYPE");
                m_diss.dsBindMap.dsBindMapLabel = grab(a, METS.uri, "LABEL");
            }
        } else if (localName.equals("fptr")) {
            // assume we're inside the inner div... that's the
            // only place the fptr element is valid.
            DSBinding binding =
                    m_diss.dsBindMap.dsBindings[m_diss.dsBindMap.dsBindings.length - 1];
            binding.datastreamID = grab(a, METS.uri, "FILEID");
        } else if (localName.equals("behaviorSec")) {
            // looks like we're in a disseminator... it should be in the
            // hash by now because we've already gone through structmaps
            // ...keyed by structmap id... remember the id (group id)
            // so we can put it in when parsing serviceBinding
            m_dissemId = grab(a, METS.uri, "ID");
            m_dissemState = grab(a, METS.uri, "STATUS");
        } else if (localName.equals("serviceBinding")) {
            // remember the structId so we can grab the right dissem
            // when parsing children
            m_structId = grab(a, METS.uri, "STRUCTID");
            // grab the disseminator associated with the provided structId
            Disseminator dissem = m_dissems.get(m_structId);
            // plug known items in..
            dissem.dissID = m_dissemId;
            dissem.dissState = m_dissemState;
            // then grab the new stuff for the dissem for this element, and
            // put it in.
            dissem.dissVersionID = grab(a, METS.uri, "ID");
            dissem.bDefID = grab(a, METS.uri, "BTYPE");
            dissem.dissCreateDT =
                    DateUtility
                            .convertStringToDate(grab(a, METS.uri, "CREATED"));
            dissem.dissLabel = grab(a, METS.uri, "LABEL");
        } else if (localName.equals("interfaceMD")) {
            Disseminator dissem = m_dissems.get(m_structId);
        } else if (localName.equals("serviceBindMD")) {
            Disseminator dissem = m_dissems.get(m_structId);
            dissem.sDepID = grab(a, m_xlink.uri, "href");
        }
    }

    private void appendElementStart(String uri,
                                    String localName,
                                    String qName,
                                    Attributes a,
                                    StringBuffer out) {
        out.append("<" + qName);
        // add the current qName's namespace to m_localPrefixMap
        // and m_prefixList if it's not already in m_localPrefixMap
        // This ensures that all namespaces used in inline XML are declared within,
        // since it's supposed to be a standalone chunk.
        String[] parts = qName.split(":");
        if (parts.length == 2) {
            String nsuri = m_localPrefixMap.get(parts[0]);
            if (nsuri == null) {
                m_localPrefixMap.put(parts[0], parts[1]);
                m_prefixList.add(parts[0]);
            }
        }
        // do we have any newly-mapped namespaces?
        while (m_prefixList.size() > 0) {
            String prefix = m_prefixList.remove(0);
            out.append(" xmlns");
            if (prefix.length() > 0) {
                out.append(":");
            }
            out.append(prefix + "=\""
                    + StreamUtility.enc(m_prefixMap.get(prefix))
                    + "\"");
        }
        for (int i = 0; i < a.getLength(); i++) {
            out.append(" " + a.getQName(i) + "=\""
                    + StreamUtility.enc(a.getValue(i)) + "\"");
        }
        out.append(">");
    }

    private void instantiateDatastream(Datastream ds) throws SAXException {

        // set datastream variables with values grabbed from the SAX parse
        ds.DatastreamID = m_dsId;
        ds.DSVersionable = m_dsVersionable;
        ds.DSFormatURI = m_dsFormatURI;
        ds.DatastreamAltIDs = m_dsAltIDs;
        ds.DSVersionID = m_dsVersId;
        ds.DSLabel = m_dsLabel;
        ds.DSCreateDT = m_dsCreateDate;
        ds.DSMIME = m_dsMimeType;
        ds.DSControlGrp = m_dsControlGrp;
        ds.DSState = m_dsState;
        ds.DSLocation = m_dsLocation;
        ds.DSLocationType = m_dsLocationType;
        ds.DSInfoType = m_dsInfoType;

        ds.DSChecksumType = m_dsChecksumType;
        LOG.debug("instantiate datastream: dsid = " + m_dsId
                + "checksumType = " + m_dsChecksumType + "checksum = "
                + m_dsChecksum);
        if (m_obj.isNew()) {
            if (m_dsChecksum != null && !m_dsChecksum.equals("")
                    && !m_dsChecksum.equals(Datastream.CHECKSUM_NONE)) {
                String tmpChecksum = ds.getChecksum();
                LOG.debug("checksum = " + tmpChecksum);
                if (!m_dsChecksum.equals(tmpChecksum)) {
                    throw new SAXException(new ValidationException("Checksum Mismatch: "
                            + tmpChecksum));
                }
            }
            ds.DSChecksumType = ds.getChecksumType();
        } else {
            ds.DSChecksum = m_dsChecksum;
        }

        // Normalize the dsLocation for the deserialization context
        ds.DSLocation =
                (DOTranslationUtility.normalizeDSLocationURLs(m_obj.getPid(),
                                                              ds,
                                                              m_transContext)).DSLocation;

        // FINALLY! add the datastream to the digital object instantiation
        m_obj.addDatastreamVersion(ds, true);
    }

    private void instantiateXMLDatastream(DatastreamXMLMetadata ds)
            throws SAXException {

        // set the attrs common to all datastream versions
        ds.DatastreamID = m_dsId;
        ds.DSVersionable = m_dsVersionable;
        ds.DSFormatURI = m_dsFormatURI;
        ds.DatastreamAltIDs = m_dsAltIDs;
        ds.DSVersionID = m_dsVersId;
        ds.DSLabel = m_dsLabel;
        ds.DSCreateDT = m_dsCreateDate;
        if (m_dsMimeType == null || m_dsMimeType.equals("")) {
            ds.DSMIME = "text/xml";
        } else {
            ds.DSMIME = m_dsMimeType;
        }
        // set the attrs specific to datastream version
        ds.DSControlGrp = "X";
        ds.DSState = m_dsState;
        ds.DSLocation = m_obj.getPid() + "+" + m_dsId + "+" + m_dsVersId;
        ds.DSLocationType = m_dsLocationType;
        ds.DSInfoType = m_dsInfoType; // METS only
        ds.DSMDClass = m_dsMDClass; // METS only
        ds.DSChecksumType = m_dsChecksumType;

        // now set the xml content stream itself...
        try {
            String xmlString = m_dsXMLBuffer.toString();
            ds.xmlContent = xmlString.getBytes(m_characterEncoding);
            //LOOK! this sets bytes, not characters.  Do we want to set this?
            ds.DSSize = ds.xmlContent.length;
        } catch (Exception uee) {
            LOG.debug("Error processing inline xml content in SAX parse: "
                    + uee.getMessage());
        }

        LOG.debug("instantiate datastream: dsid = " + m_dsId
                + "checksumType = " + m_dsChecksumType + "checksum = "
                + m_dsChecksum);
        if (m_obj.isNew()) {
            if (m_dsChecksum != null && !m_dsChecksum.equals("")
                    && !m_dsChecksum.equals(Datastream.CHECKSUM_NONE)) {
                String tmpChecksum = ds.getChecksum();
                LOG.debug("checksum = " + tmpChecksum);
                if (!m_dsChecksum.equals(tmpChecksum)) {
                    throw new SAXException(new ValidationException("Checksum Mismatch: "
                            + tmpChecksum));
                }
            }
            ds.DSChecksumType = ds.getChecksumType();
        } else {
            ds.DSChecksum = m_dsChecksum;
        }
        // FINALLY! add the xml datastream to the digitalObject
        m_obj.addDatastreamVersion(ds, true);
    }

    /**
     * convertAudits: In Fedora 2.0 and beyond, we want self-standing audit
     * records. Make sure audit records are converted to new format that
     * contains a componentID to show what component in the object the audit
     * record is about.
     */
    private void convertAudits() {
        // Only do this if ADMID values were found in the object.
        if (m_dsADMIDs.size() > 0) {
            // Look at datastreams to see if there are audit records for them.
            // NOTE:  we do not look at disseminators because in pre-2.0
            // the disseminators did not point to their audit records as
            // did the datastreams.
            Iterator<String> dsIdIter = m_obj.datastreamIdIterator();
            while (dsIdIter.hasNext()) {
                for (Datastream ds : m_obj.datastreams(dsIdIter.next())) {
                    // ADMID processing...
                    // get list of ADMIDs that go with a datastream version
                    List<String> admIdList = m_dsADMIDs.get(ds.DSVersionID);
                    List<String> cleanAdmIdList = new ArrayList<String>();
                    if (admIdList != null) {
                        Iterator<String> admIdIter = admIdList.iterator();
                        while (admIdIter.hasNext()) {
                            String admId = admIdIter.next();
                            // Detect ADMIDs that reference audit records
                            // vs. regular admin metadata. Drop audits from
                            // the list. We know we have an audit if the ADMID
                            // is not a regular datatream in the object.
                            Iterator<Datastream> matchedDatastreams =
                                    m_obj.datastreams(admId).iterator();
                            if (matchedDatastreams.hasNext()) {

                                // Keep track of audit metadata correlated with the
                                // datastream version it's about (for later use).
                                m_AuditIdToComponentId.put(admId,
                                                           ds.DSVersionID);
                            } else {
                                // Keep track of non-audit metadata in a new list.
                                cleanAdmIdList.add(admId);
                            }
                        }
                    }
                    if (cleanAdmIdList.size() <= 0) {
                        // we keep track of admin metadata references
                        // for each datastream, but we exclude the audit
                        // records from this list.  If there are no
                        // non-audit metadata references, remove the
                        // datastream entry from the master hashmap.
                        m_dsADMIDs.remove(ds.DSVersionID);
                    } else {
                        // otherwise, update the master hashmap with the
                        // clean list of non-audit metadata
                        m_dsADMIDs.put(ds.DSVersionID, cleanAdmIdList);
                    }
                }
            }
            // Now, put component ids on audit records.  Pre-Fedora 2.0
            // datastream versions pointed to their audit records.
            Iterator<AuditRecord> iter = m_obj.getAuditRecords().iterator();
            while (iter.hasNext()) {
                AuditRecord au = iter.next();
                if (au.componentID == null || au.componentID.equals("")) {
                    // Before Fedora 2.0 audit records were associated with
                    // datastream version ids.  From now on, the datastream id
                    // will be posted as the component id in the audit record,
                    // and associations to particular datastream versions can
                    // be derived via the datastream version dates and the audit
                    // record dates.
                    String dsVersId = m_AuditIdToComponentId.get(au.id);
                    if (dsVersId != null && !dsVersId.equals("")) {
                        au.componentID =
                                dsVersId.substring(0, dsVersId.indexOf("."));
                    }

                }
            }
        }
    }

    /**
     * addRelsInt: Build an RDF relationship datastream to preserve DMDID and
     * ADMID references in the digital object when METS is converted to FOXML
     * (or other formats in the future). If there is no pre-existing RELS-INT,
     * look for DMDID and ADMID attributes to create new RELS-INT datastream.
     */
    private void createRelsInt() {

        // create a new RELS-INT datastream only if one does not already exist.
        Iterator<Datastream> metsrels =
                m_obj.datastreams("RELS-INT").iterator();
        if (metsrels.hasNext()) {
            m_relsBuffer = new StringBuffer();
            appendRDFStart(m_relsBuffer);
            Iterator<String> dsIds = m_obj.datastreamIdIterator();
            while (dsIds.hasNext()) {
                // initialize hash sets to keep a list of
                // unique DMDIDs or ADMIDs at the datatream id level.
                HashSet<String> uniqueDMDIDs = new HashSet<String>();
                HashSet<String> uniqueADMIDs = new HashSet<String>();
                // get list of datastream *versions*
                for (Datastream dsVersion : m_obj.datastreams(dsIds
                        .next())) {
                    // DMDID processing...
                    List<String> dmdIdList =
                            m_dsDMDIDs.get(dsVersion.DSVersionID);
                    if (dmdIdList != null) {
                        hasRels = true;
                        Iterator<String> dmdIdIter = dmdIdList.iterator();
                        while (dmdIdIter.hasNext()) {
                            String dmdId = dmdIdIter.next();
                            // APPEND TO RDF: record the DMDID relationship.
                            // Relationships will now be recorded at the
                            // datastream level, not the datastream version level.
                            // So, is the relationship existed on more than one
                            // datastream version, only write it once to the RDF.
                            if (!uniqueDMDIDs.contains(dmdId)) {
                                appendRDFRel(m_relsBuffer,
                                             m_obj.getPid(),
                                             dsVersion.DatastreamID,
                                             "hasDescMetadata",
                                             dmdId);
                            }
                            uniqueDMDIDs.add(dmdId);
                        }
                    }
                    // ADMID processing (already cleansed of audit refs)...
                    List<String> cleanAdmIdList =
                            m_dsADMIDs.get(dsVersion.DSVersionID);
                    if (cleanAdmIdList != null) {
                        hasRels = true;
                        Iterator<String> admIdIter = cleanAdmIdList.iterator();
                        while (admIdIter.hasNext()) {
                            String admId = admIdIter.next();
                            // APPEND TO RDF: record the ADMID relationship.
                            // Relationships will now be recorded at the
                            // datastream level, not the datastream version level.
                            // So, is the relationship existed on more than one
                            // datastream version, only write it once to the RDF.
                            if (!uniqueADMIDs.contains(admId)) {
                                appendRDFRel(m_relsBuffer,
                                             m_obj.getPid(),
                                             dsVersion.DatastreamID,
                                             "hasAdminMetadata",
                                             admId);
                            }
                            uniqueADMIDs.add(admId);
                        }
                    }
                }
            }
            // APPEND RDF: finish up and add RDF as a system-generated datastream
            if (hasRels) {
                appendRDFEnd(m_relsBuffer);
                setRDFAsDatastream(m_relsBuffer);
            } else {
                m_relsBuffer = null;
            }
        }
    }

    // Create a system-generated datastream from the RDF expression of the
    // DMDID and ADMID relationships found in the METS file.
    private void setRDFAsDatastream(StringBuffer buf) {

        DatastreamXMLMetadata ds = new DatastreamXMLMetadata();
        // set the attrs common to all datastream versions
        ds.DatastreamID = "RELS-INT";
        ds.DSVersionable = false;
        ds.DSFormatURI = m_dsFormatURI;
        ds.DatastreamAltIDs = m_dsAltIDs;
        ds.DSVersionID = "RELS-INT.0";
        ds.DSLabel =
                "DO NOT EDIT: System-generated datastream to preserve METS DMDID/ADMID relationships.";
        ds.DSCreateDT = new Date();
        ds.DSMIME = "application/rdf+xml";
        // set the attrs specific to datastream version
        ds.DSControlGrp = "X";
        ds.DSState = "A";
        ds.DSLocation =
                m_obj.getPid() + "+" + ds.DatastreamID + "+" + ds.DSVersionID;
        ds.DSLocationType = "INTERNAL_ID";
        ds.DSInfoType = "DATA";
        ds.DSMDClass = DatastreamXMLMetadata.TECHNICAL;

        // now set the xml content stream itself...
        try {
            ds.xmlContent = buf.toString().getBytes(m_characterEncoding);
            ds.DSSize = ds.xmlContent.length;
        } catch (UnsupportedEncodingException uee) {
            LOG.error("Encoding error when creating RELS-INT datastream", uee);
        }
        // FINALLY! add the RDF and an inline xml datastream in the digital object
        m_obj.addDatastreamVersion(ds, true);
    }

    private StringBuffer appendRDFStart(StringBuffer buf) {

        buf.append("<" + RDF.prefix + ":RDF" + " xmlns:" + RDF.prefix + "=\""
                + RDF.uri + "\"" + " xmlns:" + RELS_EXT.prefix + "=\""
                + RELS_EXT.uri + "\">\n");
        return buf;
    }

    private StringBuffer appendRDFRel(StringBuffer buf,
                                      String pid,
                                      String subjectNodeId,
                                      String relType,
                                      String objectNodeId) {

        // RDF subject node
        buf.append("    <" + RDF.prefix + ":Description " + RDF.prefix
                + ":about=\"" + "info:fedora/" + pid + "/" + subjectNodeId
                + "\">\n");
        // RDF relationship property and object node
        buf.append("        <" + RELS_EXT.prefix + ":" + relType + " "
                + RDF.prefix + ":resource=\"" + "info:fedora/" + pid + "/"
                + objectNodeId + "\"/>\n");
        buf.append("    </" + RDF.prefix + ":Description" + ">\n");
        return buf;
    }

    private void initialize() {
        // temporary variables and state variables
        m_rootElementFound = false;
        m_inXMLMetadata = false;
        m_prefixMap = new HashMap<String, String>();
        m_localPrefixMap = new HashMap<String, String>();
        m_prefixList = new ArrayList<String>();

        // temporary variables for processing datastreams
        m_dsId = "";
        m_dsVersionable = true;
        m_dsVersId = "";
        m_dsCreateDate = null;
        m_dsState = "";
        m_dsFormatURI = "";
        m_dsAltIDs = new String[0];
        m_dsSize = -1;
        m_dsLocationType = "";
        m_dsLocation = "";
        m_dsMimeType = "";
        m_dsControlGrp = "";
        m_dsInfoType = "";
        m_dsOtherInfoType = "";
        m_dsMDClass = 0;
        m_dsLabel = "";
        m_dsXMLBuffer = null;
        m_dsADMIDs = new HashMap<String, List<String>>();
        m_dsDMDIDs = new HashMap<String, List<String>>();
        m_dsChecksum = "";
        m_dsChecksumType = "";

        // temporary variables for processing disseminators
        m_dissems = new HashMap<String, Disseminator>();

        // temporary variables for processing audit records
        m_auditBuffer = null;
        m_auditId = "";
        m_auditComponentID = "";
        m_auditProcessType = "";
        m_auditAction = "";
        m_auditResponsibility = "";
        m_auditDate = "";
        m_auditJustification = "";

        m_AuditIdToComponentId = new HashMap<String, String>();
        m_relsBuffer = null;
    }

    //---
    // Static helpers
    //---

    private static StringBuffer appendRDFEnd(StringBuffer buf) {
        buf.append("</" + RDF.prefix + ":RDF>\n");
        return buf;
    }

    private static String grab(Attributes a,
                               String namespace,
                               String elementName) {
        String ret = a.getValue(namespace, elementName);
        if (ret == null) {
            ret = a.getValue(elementName);
        }
        // set null attribute value to empty string since it's
        // generally helpful in the code to avoid null pointer exception
        // when operations are performed on attributes values.
        if (ret == null) {
            ret = "";
        }
        return ret;
    }

}
