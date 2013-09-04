/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage.translation.handlers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fcrepo.common.Constants;
import org.fcrepo.common.Models;
import org.fcrepo.common.xml.format.XMLFormat;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.errors.ValidationException;
import org.fcrepo.server.storage.translation.DOTranslationUtility;
import org.fcrepo.server.storage.types.AuditRecord;
import org.fcrepo.server.storage.types.DSBinding;
import org.fcrepo.server.storage.types.DSBindingMap;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamManagedContent;
import org.fcrepo.server.storage.types.DatastreamReferencedContent;
import org.fcrepo.server.storage.types.DatastreamXMLMetadata;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.storage.types.Disseminator;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.server.validation.ValidationUtility;
import org.fcrepo.utilities.Base64;
import org.fcrepo.utilities.DateUtility;
import org.fcrepo.utilities.ReadableByteArrayOutputStream;
import org.fcrepo.utilities.ReadableCharArrayWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;




/**
 * Deserializes objects in the constructor-provided version of FOXML.
 *
 * @author Sandy Payette
 * @author Chris Wilper
 */
@SuppressWarnings("deprecation")
public class FOXMLContentHandler
        extends DefaultHandler
        implements Constants {

    /**
     * The format this deserializer will read if unspecified at construction.
     * This defaults to the latest FOXML format.
     */
    public static final XMLFormat DEFAULT_FORMAT = FOXML1_1;

    private static final Logger logger =
            LoggerFactory.getLogger(FOXMLContentHandler.class);

    /** The format this deserializer reads. */
    private final XMLFormat m_format;

    /** The current translation context. */
    private int m_transContext;

    /** The object to deserialize to. */
    private DigitalObject m_obj;


    // Namespace prefix-to-URI mapping info from SAX2 startPrefixMapping events.
    private HashMap<String, String> m_prefixMap;

    private HashMap<String, String> m_localPrefixMap;

    private ArrayList<String> m_prefixList;

    private String m_characterEncoding;

    private boolean m_rootElementFound;

    private String m_objPropertyName;

    private boolean m_readingBinaryContent; // indicates reading base64-encoded content

    private File m_binaryContentTempFile;

    private boolean m_inXMLMetadata;

    // Indicator for FOXML within FOXML (inline XML datastream contains FOXML)
    private int m_xmlDataLevel;

    // temporary variables for datastream processing
    private String m_dsId;

    private boolean m_dsVersionable;

    private String m_dsVersId;

    private Date m_dsCreateDate;

    private String m_dsState;

    private String[] m_dsAltIds;

    private String m_dsFormatURI;

    private String m_dsLabel;

    private long m_dsSize;

    private String m_dsLocationType;

    private String m_dsLocation;

    private String m_dsMimeType;

    private String m_dsControlGrp;

    private String m_dsInfoType; // for METS backward compatibility

    private String m_dsOtherInfoType; // for METS backward compatibility

    private int m_dsMDClass; // for METS backward compatibility

    private final Pattern metsPattern =
            Pattern.compile("info:fedora/fedora-system:format/xml.mets.");

    private String m_dsChecksumType;

    private String m_dsChecksum;

    // temporary variables for processing disseminators
    private Disseminator m_diss;

    private String m_dissID;

    @Deprecated
    private String m_sDefID;

    private String m_dissState;

    private boolean m_dissVersionable;

    private ArrayList<DSBinding> m_dsBindings;

    // temporary variables for processing audit records
    private AuditRecord m_auditRec;

    private boolean m_gotAudit = false;

    private String m_auditComponentID;

    private String m_auditProcessType;

    private String m_auditAction;

    private String m_auditResponsibility;

    private String m_auditDate;

    private String m_auditJustification;

    // buffers for reading content
    private ReadableByteArrayOutputStream m_elementContent; // single element

    private ReadableCharArrayWriter m_dsXMLBuffer; // chunks of inline XML metadata
    
    /**
     * Creates a deserializer that reads the default FOXML format.
     */
    public FOXMLContentHandler(DigitalObject obj,
            String encoding,
            int transContext) {
        this(obj, DEFAULT_FORMAT, encoding, transContext);
    }

    /**
     * Creates a deserializer that reads the given FOXML format.
     *
     * @param format
     *        the version-specific FOXML format.
     * @throws IllegalArgumentException
     *         if format is not a known FOXML format.
     */
    public FOXMLContentHandler(DigitalObject obj,
            XMLFormat format,
            String encoding,
            int transContext) {
        if (format.equals(FOXML1_0) || format.equals(FOXML1_1)) {
            m_format = format;
        } else {
            throw new IllegalArgumentException("Not a FOXML format: "
                    + format.uri);
        }
        m_obj = obj;
        m_obj.setLabel("");
        m_obj.setOwnerId("");
        m_characterEncoding = encoding;
        m_transContext = transContext;
        initialize();
    }
    
    public boolean rootElementFound() {
        return m_rootElementFound;
    }

    //---
    // DefaultHandler overrides
    //---

    /**
     * {@inheritDoc}
     */
    @Override
    public void startPrefixMapping(String prefix, String uri) {
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

        // null out any existing content buffer.
        // This will start a fresh buffer for every element encountered.
        m_elementContent = null;

        if (uri.equals(FOXML.uri) && !m_inXMLMetadata) {
            // WE ARE NOT INSIDE A BLOCK OF INLINE XML...
            if (localName.equals("digitalObject")) {
                m_rootElementFound = true;
                //======================
                // OBJECT IDENTIFIERS...
                //======================
                m_obj.setPid(grab(a, FOXML.uri, "PID"));
                //=====================
                // OBJECT PROPERTIES...
                //=====================
            } else if (localName.equals("property")
                    || localName.equals("extproperty")) {
                m_objPropertyName = grab(a, FOXML.uri, "NAME");
                if (m_objPropertyName.equals(MODEL.STATE.uri)) {
                    try {
                        m_obj.setState(DOTranslationUtility
                                       .readStateAttribute(grab(a, FOXML.uri, "VALUE")));
                    } catch (ParseException e) {
                        throw new SAXException("Could not read state", e);
                    }
                } else if (m_objPropertyName.equals(MODEL.LABEL.uri)) {
                    m_obj.setLabel(grab(a, FOXML.uri, "VALUE"));
                } else if (m_objPropertyName.equals(MODEL.OWNER.uri)) {
                    m_obj.setOwnerId(grab(a, FOXML.uri, "VALUE"));
                } else if (m_objPropertyName.equals(MODEL.CREATED_DATE.uri)) {
                    m_obj.setCreateDate(DateUtility
                            .convertStringToDate(grab(a, FOXML.uri, "VALUE")));
                } else if (m_objPropertyName
                        .equals(VIEW.LAST_MODIFIED_DATE.uri)) {
                    m_obj.setLastModDate(DateUtility
                            .convertStringToDate(grab(a, FOXML.uri, "VALUE")));
                } else {
                    // Legacy object properties from FOXML 1.0, if present,
                    // will be retained here as external properties in the
                    // DigitalObject.  This includes fedora-model:contentModel
                    // and rdf:type.
                    m_obj.setExtProperty(m_objPropertyName, grab(a,
                                                                 FOXML.uri,
                                                                 "VALUE"));
                }
                //===============
                // DATASTREAMS...
                //===============
            } else if (localName.equals("datastream")) {
                // get datastream container-level attributes...
                // These are common for all versions of the datastream.
                m_dsId = grab(a, FOXML.uri, "ID");
                m_dsState = grab(a, FOXML.uri, "STATE");
                m_dsControlGrp = grab(a, FOXML.uri, "CONTROL_GROUP");
                String versionable = grab(a, FOXML.uri, "VERSIONABLE");
                // If dsVersionable is null or missing, default to true.
                if (versionable == null || versionable.isEmpty()) {
                    m_dsVersionable = true;
                } else {
                    m_dsVersionable = Boolean.parseBoolean(versionable);
                }
                // Never allow the AUDIT datastream to be versioned
                // since it naturally represents a system-controlled
                // view of changes over time.
                if (m_dsId.equals("AUDIT")) {
                    m_dsVersionable = false;
                }
            } else if (localName.equals("datastreamVersion")) {
                // get datastream version-level attributes...
                m_dsVersId = grab(a, FOXML.uri, "ID");
                m_dsLabel = grab(a, FOXML.uri, "LABEL");
                m_dsCreateDate =
                        DateUtility.convertStringToDate(grab(a,
                                                             FOXML.uri,
                                                             "CREATED"));
                String altIDsString = grab(a, FOXML.uri, "ALT_IDS");
                if (altIDsString.length() == 0) {
                    m_dsAltIds = new String[0];
                } else {
                    m_dsAltIds = altIDsString.split(" ");
                }
                m_dsFormatURI = grab(a, FOXML.uri, "FORMAT_URI");
                if (m_dsFormatURI.length() == 0) {
                    m_dsFormatURI = null;
                }
                checkMETSFormat(m_dsFormatURI);
                m_dsMimeType = grab(a, FOXML.uri, "MIMETYPE");
                String sizeString = grab(a, FOXML.uri, "SIZE");
                if (sizeString != null && !sizeString.isEmpty()) {
                    try {
                        m_dsSize = Long.parseLong(sizeString);
                    } catch (NumberFormatException nfe) {
                        throw new SAXException("If specified, a datastream's "
                                + "SIZE attribute must be an xsd:long.");
                    }
                } else {
                    m_dsSize = -1;
                }
                if (m_dsVersId.equals("AUDIT.0")) {
                    m_gotAudit = true;
                }
                m_dsChecksumType = (Datastream.autoChecksum)
                        ? Datastream.getDefaultChecksumType()
                        : Datastream.CHECKSUMTYPE_DISABLED;
                m_dsChecksum = Datastream.CHECKSUM_NONE;
            } else if (localName.equals("contentDigest")) {
                m_dsChecksumType = grab(a, FOXML.uri, "TYPE");
                m_dsChecksum = grab(a, FOXML.uri, "DIGEST");
            }
            //======================
            // DATASTREAM CONTENT...
            //======================
            // inside a datastreamVersion element, it's either going to be
            // xmlContent (inline xml), contentLocation (a reference) or binaryContent
            else if (localName.equals("xmlContent")) {
                m_dsXMLBuffer = new ReadableCharArrayWriter();
                m_xmlDataLevel = 0;
                m_inXMLMetadata = true;
            } else if (localName.equals("contentLocation")) {
                String dsLocation = grab(a, FOXML.uri, "REF");
                if (dsLocation == null || dsLocation.isEmpty()) {
                    throw new SAXException("REF attribute must be specified in contentLocation element");
                }
                // check if datastream is ExternalReferenced
                if (m_dsControlGrp.equalsIgnoreCase("E")
                        || m_dsControlGrp.equalsIgnoreCase("R")) {

                    // URL FORMAT VALIDATION for dsLocation:
                    // make sure we have a properly formed URL
                    try {
                        ValidationUtility.validateURL(dsLocation, m_dsControlGrp);
                    } catch (ValidationException ve) {
                        throw new SAXException(ve.getMessage());
                    }
                    // system will set dsLocationType for E and R datastreams...
                    m_dsLocationType = Datastream.DS_LOCATION_TYPE_URL;
                    m_dsLocation = dsLocation;
                    instantiateDatastream(new DatastreamReferencedContent());
                    // check if datastream is ManagedContent
                } else if (m_dsControlGrp.equalsIgnoreCase("M")) {
                    // URL FORMAT VALIDATION for dsLocation:
                    // For Managed Content the URL is only checked when we are parsing a
                    // a NEW ingest file because the URL is replaced with an internal identifier
                    // once the repository has sucked in the content for storage.
                    if (m_obj.isNew()) {
                        try {
                            ValidationUtility.validateURL(dsLocation, m_dsControlGrp);
                            m_dsLocationType = Datastream.DS_LOCATION_TYPE_URL;
                        } catch (ValidationException ve) {
                            throw new SAXException(ve.getMessage());
                        }
                    }
                    else {
                        m_dsLocationType = Datastream.DS_LOCATION_TYPE_INTERNAL;
                    }
                    m_dsLocation = dsLocation;
                    instantiateDatastream(new DatastreamManagedContent());
                }
            } else if (localName.equals("binaryContent")) {
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
            } else if (m_format.equals(FOXML1_0)) {
                //==================
                // DISSEMINATORS...
                //==================
                startDisseminators(localName, a);
            }
        } else {
            //===============
            // INLINE XML...
            //===============
            if (m_inXMLMetadata) {
                // we are inside an xmlContent element.
                // just output it, remembering the number of foxml:xmlContent elements we see,
                appendElementStart(uri, localName, qName, a, m_dsXMLBuffer);

                // FOXML INSIDE FOXML! we have an inline XML datastream
                // that is itself FOXML.  We do not want to parse this!
                if (uri.equals(FOXML.uri) && localName.equals("xmlContent")) {
                    m_xmlDataLevel++;
                }

                // if AUDIT datastream, initialize new audit record object
                if (m_gotAudit) {
                    if (localName.equals("record")) {
                        m_auditRec = new AuditRecord();
                        m_auditRec.id = grab(a, uri, "ID");
                    } else if (localName.equals("process")) {
                        m_auditProcessType = grab(a, uri, "type");
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
    public void characters(char[] ch, int start, int length) 
        throws SAXException {
        // read entire inline XML metadata chunks into a buffer
        if (m_inXMLMetadata && !m_gotAudit) {
            // since this data is encoded straight back to xml,
            // we need to make sure special characters &, <, >, ", and '
            // are re-converted to the xml-acceptable equivalents.
            StreamUtility.enc(ch, start, length, m_dsXMLBuffer);
        } else if (m_gotAudit || m_readingBinaryContent){
            // Use a separate buffer to deal with the special case
            // of AUDIT datastreams, which may be inline, but need to
            // retrieve individual element content to deserialize correctly
            // append element content into a byte buffer; or b64-encoded
            // binary content
            if (m_elementContent == null) {
                m_elementContent = new ReadableByteArrayOutputStream();
            }
            CharBuffer chars = CharBuffer.wrap(ch, start, length);
            ByteBuffer bytes = Charset.forName(m_characterEncoding).encode(chars);
            m_elementContent.write(bytes.array(), bytes.arrayOffset(), bytes.limit());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        //==================
        // INLINE XML...
        //==================
        if (m_inXMLMetadata) {
            //=====================
            // AUDIT DATASTREAM...
            //=====================
            if (m_gotAudit) {
                // Pick up audit records from the current ds version
                // and instantiate audit records array in digital object.
                if (localName.equals("action")) {
                    m_auditAction = (m_elementContent != null) ?
                            m_elementContent.toString() : "";
                    //} else if (localName.equals("recordID")) {
                    //    m_auditRecordID=m_elementContent.toString();
                } else if (localName.equals("componentID")) {
                    m_auditComponentID = (m_elementContent != null) ?
                            m_elementContent.toString() : "";
                } else if (localName.equals("responsibility")) {
                    m_auditResponsibility = (m_elementContent != null) ?
                            m_elementContent.toString() : "";
                } else if (localName.equals("date")) {
                    m_auditDate = (m_elementContent != null) ?
                            m_elementContent.toString() : "";
                } else if (localName.equals("justification")) {
                    m_auditJustification = (m_elementContent != null) ?
                            m_elementContent.toString() : "";
                } else if (localName.equals("record")) {
                    //m_auditRec.id=m_auditRecordID;
                    m_auditRec.processType = m_auditProcessType;
                    m_auditRec.action = m_auditAction;
                    m_auditRec.componentID = m_auditComponentID;
                    m_auditRec.responsibility = m_auditResponsibility;
                    m_auditRec.date =
                            DateUtility.convertStringToDate(m_auditDate);
                    m_auditRec.justification = m_auditJustification;
                    // add the audit records to the digital object
                    m_obj.getAuditRecords().add(m_auditRec);
                    // reinit variables for next audit record
                    m_auditProcessType = "";
                    m_auditAction = "";
                    m_auditComponentID = "";
                    m_auditResponsibility = "";
                    m_auditDate = "";
                    m_auditJustification = "";
                } else if (localName.equals("auditTrail")) {
                    m_gotAudit = false;
                }
                // process end of xmlContent ONLY if it is NOT embedded within inline XML!
            } else if (uri.equals(FOXML.uri) && localName.equals("xmlContent")
                    && m_xmlDataLevel == 0) {
                //=====================
                // AUDIT DATASTREAM...
                //=====================
                if (m_dsId.equals("AUDIT")) {
                    // if we are in the inline XML of the AUDIT datastream just
                    // end processing and move on.  Audit datastream handled elsewhere.
                    m_inXMLMetadata = false; // other stuff is re-initted upon
                    // startElement for next xml metadata
                    // element
                    //========================
                    // ALL OTHER INLINE XML...
                    //========================
                } else {
                    // Create the right kind of datastream and add to the object
                    DatastreamXMLMetadata ds = new DatastreamXMLMetadata();
                    instantiateXMLDatastream(ds);
                    m_inXMLMetadata = false;
                    m_localPrefixMap.clear();
                }
            } else {
                // finished an element within inline xml metadata
                m_dsXMLBuffer.append("</").append(qName).append('>');
                // make sure we know when to pay attention to FOXML again
                if (uri.equals(FOXML.uri) && localName.equals("xmlContent")) {
                    m_xmlDataLevel--;
                }
            }
            //========================================
            // ALL OTHER ELEMENTS (NOT INLINE XML)...
            //========================================
        } else if (uri.equals(FOXML.uri) && localName.equals("binaryContent")) {
            if (m_binaryContentTempFile != null) {
                try {
                    FileOutputStream os =
                            new FileOutputStream(m_binaryContentTempFile);
                    // remove all spaces and newlines, this might not be necessary.
                    byte elementBytes[] = Base64.decode(m_elementContent.toInputStream());
                    os.write(elementBytes);
                    os.close();
                    m_dsLocationType = Datastream.DS_LOCATION_TYPE_INTERNAL;
                    m_dsLocation =
                        DatastreamManagedContent.TEMP_SCHEME
                                    + m_binaryContentTempFile.getAbsolutePath();
                    instantiateDatastream(new DatastreamManagedContent());
                } catch (FileNotFoundException fnfe) {
                    throw new SAXException(new StreamIOException("Unable to open temporary file created for binary content"));
                } catch (IOException fnfe) {
                    throw new SAXException(new StreamIOException("Error writing to temporary file created for binary content"));
                }
            }
            m_binaryContentTempFile = null;
            m_readingBinaryContent = false;
        } else if (uri.equals(FOXML.uri)
                && localName.equals("datastreamVersion")) {
            // reinitialize datastream version-level attributes...
            m_dsVersId = "";
            m_dsLabel = "";
            m_dsCreateDate = null;
            m_dsAltIds = new String[0];
            m_dsFormatURI = "";
            m_dsMimeType = "";
            m_dsSize = -1;
            //m_dsAdmIds=new HashMap();
            //m_dsDmdIds=null;
        } else if (uri.equals(FOXML.uri) && localName.equals("datastream")) {
            // reinitialize datastream attributes ...
            m_dsId = "";
            m_dsVersionable = true;
            m_dsState = "";
            m_dsInfoType = "";
            m_dsOtherInfoType = "";
            m_dsMDClass = 0;
        } else if (m_format.equals(FOXML1_0)) {
            endDisseminators(uri, localName);
        }
    }

    //---
    // Instance helpers
    //---

    private void startDisseminators(String localName, Attributes a) {
        if (localName.equals("disseminator")) {
            m_dissID = grab(a, FOXML.uri, "ID");
            m_sDefID = grab(a, FOXML.uri, "BDEF_CONTRACT_PID");
            m_dissState = grab(a, FOXML.uri, "STATE");
            String versionable = grab(a, FOXML.uri, "VERSIONABLE");
            // disseminator versioning is defaulted to true
            if (versionable == null || versionable.isEmpty()) {
                m_dissVersionable = true;
            } else {
                m_dissVersionable = Boolean.parseBoolean(versionable);
            }
        } else if (localName.equals("disseminatorVersion")) {
            m_diss = new Disseminator();
            m_diss.dissID = m_dissID;
            m_diss.bDefID = m_sDefID;
            m_diss.dissState = m_dissState;
            String versionable = grab(a, FOXML.uri, "VERSIONABLE");
            // disseminator versioning is defaulted to true
            if (versionable == null || versionable.isEmpty()) {
                m_dissVersionable = true;
            } else {
                m_dissVersionable = Boolean.parseBoolean(versionable);
            }
            m_diss.dissVersionID = grab(a, FOXML.uri, "ID");
            m_diss.dissLabel = grab(a, FOXML.uri, "LABEL");
            m_diss.sDepID = grab(a, FOXML.uri, "BMECH_SERVICE_PID");
            m_diss.dissCreateDT =
                    DateUtility.convertStringToDate(grab(a,
                                                         FOXML.uri,
                                                         "CREATED"));
        } else if (localName.equals("serviceInputMap")) {
            m_diss.dsBindMap = new DSBindingMap();
            m_dsBindings = new ArrayList<DSBinding>();
            // Note that the dsBindMapID is not really necessary from the
            // FOXML standpoint, but it was necessary in METS since the
            // structMap was outside the disseminator.
            // Also, the rest of the attributes on the DSBindingMap are not
            // really necessary since they are inherited from the disseminator.
            // I just use the values picked up from disseminatorVersion.
            m_diss.dsBindMapID = m_diss.dissVersionID + "b";
            m_diss.dsBindMap.dsBindMapID = m_diss.dsBindMapID;
            m_diss.dsBindMap.dsBindMechanismPID = m_diss.sDepID;
            m_diss.dsBindMap.dsBindMapLabel = ""; // does not exist in FOXML
            m_diss.dsBindMap.state = m_diss.dissState;
        } else if (localName.equals("datastreamBinding")) {
            DSBinding dsb = new DSBinding();
            dsb.bindKeyName = grab(a, FOXML.uri, "KEY");
            dsb.bindLabel = grab(a, FOXML.uri, "LABEL");
            dsb.datastreamID = grab(a, FOXML.uri, "DATASTREAM_ID");
            dsb.seqNo = grab(a, FOXML.uri, "ORDER");
            m_dsBindings.add(dsb);
        }
    }

    private void endDisseminators(String uri, String localName) {
        if (localName.equals("serviceInputMap")) {
            m_diss.dsBindMap.dsBindings =
                    m_dsBindings.toArray(new DSBinding[0]);
            m_dsBindings = null;
        } else if (uri.equals(FOXML.uri)
                && localName.equals("disseminatorVersion")) {
            m_obj.disseminators(m_diss.dissID).add(m_diss);
            m_diss = null;
        } else if (uri.equals(FOXML.uri) && localName.equals("disseminator")) {
            m_dissID = "";
            m_sDefID = "";
            m_dissState = "";
            m_dissVersionable = true;
        }
    }

    private void appendElementStart(String uri,
                                    String localName,
                                    String qName,
                                    Attributes a,
                                    ReadableCharArrayWriter out) {
        out.append('<').append(qName);
        // add the current qName's namespace to m_localPrefixMap
        // and m_prefixList if it's not already in m_localPrefixMap
        // This ensures that all namespaces used in inline XML are declared within,
        // since it's supposed to be a standalone chunk.
        int colon = qName.indexOf(':');
        if (colon > -1) {
            String prefix = qName.substring(0, colon);
            String nsuri = m_localPrefixMap.get(prefix);
            if (nsuri == null) {
                m_localPrefixMap.put(prefix, qName.substring(colon+1));
                m_prefixList.add(prefix);
            }
        }
        // do we have any newly-mapped namespaces?
        while (m_prefixList.size() > 0) {
            String prefix = m_prefixList.remove(0);
            out.append(" xmlns");
            if (prefix.length() > 0) {
                out.append(':');
            }
            out.append(prefix).append("=\"");
            StreamUtility.enc(m_prefixMap.get(prefix), out);
            out.append('"');
        }
        for (int i = 0; i < a.getLength(); i++) {
            out.append(' ').append(a.getQName(i)).append("=\"");
            StreamUtility.enc(a.getValue(i), out);
            out.append('"');
        }
        out.append('>');
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

    private void instantiateDatastream(Datastream ds) throws SAXException {

        // set datastream variables with values grabbed from the SAX parse
        ds.DatastreamID = m_dsId;
        ds.DatastreamAltIDs = m_dsAltIds;
        ds.DSVersionable = m_dsVersionable;
        ds.DSFormatURI = m_dsFormatURI;
        ds.DSVersionID = m_dsVersId;
        ds.DSLabel = m_dsLabel;
        ds.DSCreateDT = m_dsCreateDate;
        ds.DSMIME = m_dsMimeType;
        ds.DSControlGrp = m_dsControlGrp;
        ds.DSState = m_dsState;
        if (m_dsControlGrp.equals("M"))
            ds.DSSize = m_dsSize; // X datastreams have their own instantiator; E and R don't have (persisted) SIZE
        ds.DSLocation = m_dsLocation;
        ds.DSLocationType = m_dsLocationType;
        ds.DSInfoType = ""; // METS legacy
        ds.DSChecksumType = m_dsChecksumType;
        logger.debug(
                "instantiate datastream: dsid = {} checksumType = {} checksum = {}",
                m_dsId, m_dsChecksumType, m_dsChecksum);
        if (m_obj.isNew()) {
            logger.debug("New Object: checking supplied checksum");
            if (m_dsChecksum != null && !m_dsChecksum.isEmpty()
                    && !m_dsChecksum.equals(Datastream.CHECKSUM_NONE)) {
                String tmpChecksum = ds.getChecksum();
                logger.debug("checksum = {}", tmpChecksum);
                if (!m_dsChecksum.equals(tmpChecksum)) {
                    {
                        throw new SAXException(new ValidationException("Checksum Mismatch: "
                                + tmpChecksum));
                    }
                }
            }

            ds.DSChecksumType = ds.getChecksumType();
            ds.DSChecksum = m_dsChecksum;
        } else {
            ds.DSChecksum = m_dsChecksum;
        }

        // Normalize the dsLocation for the deserialization context
        ds.DSLocation =
                (DOTranslationUtility.normalizeDSLocationURLs(m_obj.getPid(),
                                                              ds,
                                                              m_transContext)).DSLocation;

        // SDP: this is METS specific stuff.  What to do?
        /*
         * if (m_dsDmdIds!=null) { for (int idi=0; idi<m_dsDmdIds.length;
         * idi++) { drc.metadataIdList().add(m_dsDmdIds[idi]); } }
         */

        // FINALLLY! add the datastream to the digital object instantiation
        m_obj.addDatastreamVersion(ds, true);

    }

    private void instantiateXMLDatastream(DatastreamXMLMetadata ds)
            throws SAXException {

        // set the attrs common to all datastream versions
        ds.DatastreamID = m_dsId;
        ds.DatastreamAltIDs = m_dsAltIds;
        ds.DSVersionable = m_dsVersionable;
        ds.DSFormatURI = m_dsFormatURI;
        ds.DSVersionID = m_dsVersId;
        ds.DSLabel = m_dsLabel;
        ds.DSCreateDT = m_dsCreateDate;
        if (m_dsMimeType == null || m_dsMimeType.isEmpty()) {
            ds.DSMIME = "text/xml";
        } else {
            ds.DSMIME = m_dsMimeType;
        }
        // set the attrs specific to datastream version
        ds.DSControlGrp = "X";
        ds.DSState = m_dsState;
        ds.DSLocation = m_obj.getPid() + "+" + m_dsId + "+" + m_dsVersId;
        ds.DSLocationType = m_dsLocationType;
        ds.DSInfoType = m_dsInfoType; // METS legacy
        ds.DSMDClass = m_dsMDClass; // METS legacy

        // now set the xml content stream itself...

        ByteBuffer bytes = Charset.forName(m_characterEncoding).
                encode(m_dsXMLBuffer.toBuffer());
        ds.xmlContent = new byte[bytes.limit()];
        System.arraycopy(
                bytes.array(), bytes.arrayOffset(),
                ds.xmlContent, 0, bytes.limit());
        if (logger.isDebugEnabled()) {
            StringBuilder rels = new StringBuilder();
            if (m_dsId.equals("WSDL")) {
                if (m_obj.hasContentModel(Models.SERVICE_DEPLOYMENT_3_0)){
                    rels.append(Models.SERVICE_DEPLOYMENT_3_0 ).append('\n');
                }

                logger.debug("Not processing WSDL from {} with models:\n{}", m_obj.getPid(), rels);
            }
        }
        //LOOK! this sets bytes, not characters.  Do we want to set this?
        ds.DSSize = ds.xmlContent.length;

        logger.debug(
                "instantiate XML datastream: dsid = {} checksumType = {} checksum = {}",
                m_dsId, m_dsChecksumType, m_dsChecksum);
        ds.DSChecksumType = m_dsChecksumType;
        if (m_obj.isNew()) {
            if (m_dsChecksum != null && !m_dsChecksum.isEmpty()
                    && !m_dsChecksum.equals(Datastream.CHECKSUM_NONE)) {
                String tmpChecksum = ds.getChecksum();
                logger.debug("checksum = {}", tmpChecksum);
                if (!m_dsChecksum.equals(tmpChecksum)) {
                    throw new SAXException(new ValidationException("Checksum Mismatch: "
                            + tmpChecksum));
                }
            }

            ds.DSChecksumType = ds.getChecksumType();
            ds.DSChecksum = m_dsChecksum;
        } else {
            ds.DSChecksum = m_dsChecksum;
        }
        // FINALLY! add the xml datastream to the digitalObject
        m_obj.addDatastreamVersion(ds, true);
    }

    private void checkMETSFormat(String formatURI) {
        if (formatURI != null && !formatURI.isEmpty()) {
            Matcher m = metsPattern.matcher(formatURI);
            //Matcher m = metsURI.matcher(formatURI);
            if (m.lookingAt()) {
                int index = m.end();
                StringTokenizer st =
                        new StringTokenizer(formatURI.substring(index), ".");
                String mdClass = st.nextToken();
                if (st.hasMoreTokens()) {
                    m_dsInfoType = st.nextToken();
                }
                if (st.hasMoreTokens()) {
                    m_dsOtherInfoType = st.nextToken();
                }
                if (mdClass.equals("techMD")) {
                    m_dsMDClass = 1;
                } else if (mdClass.equals("sourceMD")) {
                    m_dsMDClass = 2;
                } else if (mdClass.equals("rightsMD")) {
                    m_dsMDClass = 3;
                } else if (mdClass.equals("digiprovMD")) {
                    m_dsMDClass = 4;
                } else if (mdClass.equals("descMD")) {
                    m_dsMDClass = 5;
                }
                if (m_dsInfoType.equals("OTHER")) {
                    m_dsInfoType = m_dsOtherInfoType;
                }
            }
        }
    }

    private void initialize() {

        // temporary variables and state variables
        m_rootElementFound = false;
        m_objPropertyName = "";
        m_readingBinaryContent = false; // indicates reading base64-encoded content
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

        // temporary variables for processing disseminators
        m_diss = null;
        m_dissID = "";
        m_sDefID = "";
        m_dissState = "";
        m_dissVersionable = true;
        m_dsBindings = null;

        // temporary variables for processing audit records
        m_auditRec = null;
        m_gotAudit = false;
        m_auditComponentID = "";
        m_auditProcessType = "";
        m_auditAction = "";
        m_auditResponsibility = "";
        m_auditDate = "";
        m_auditJustification = "";
    }
}
