/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import fedora.common.Constants;
import fedora.common.rdf.RDFName;
import fedora.common.xml.format.XMLFormat;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.storage.types.DSBinding;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.Disseminator;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.StreamUtility;
import fedora.server.utilities.StringUtility;

import fedora.utilities.Base64;

import static fedora.common.Models.SERVICE_DEPLOYMENT_3_0;

/**
 * Serializes objects in the constructor-provided version of FOXML.
 *
 * @author Sandy Payette
 * @author Chris Wilper
 */
@SuppressWarnings("deprecation")
public class FOXMLDOSerializer
        implements DOSerializer, Constants {

    /**
     * The format this serializer will write if unspecified at construction.
     * This defaults to the latest FOXML format.
     */
    public static final XMLFormat DEFAULT_FORMAT = FOXML1_1;

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(FOXMLDOSerializer.class);

    /** The format this serializer writes. */
    private final XMLFormat m_format;

    /** The current translation context. */
    private int m_transContext;

    /**
     * Creates a serializer that writes the default FOXML format.
     */
    public FOXMLDOSerializer() {
        m_format = DEFAULT_FORMAT;
    }

    /**
     * Creates a serializer that writes the given FOXML format.
     *
     * @param format
     *        the version-specific FOXML format.
     * @throws IllegalArgumentException
     *         if format is not a known FOXML format.
     */
    public FOXMLDOSerializer(XMLFormat format) {
        if (format.equals(FOXML1_0) || format.equals(FOXML1_1)) {
            m_format = format;
        } else {
            throw new IllegalArgumentException("Not a FOXML format: "
                    + format.uri);
        }
    }

    //---
    // DOSerializer implementation
    //---

    /**
     * {@inheritDoc}
     */
    public DOSerializer getInstance() {
        return new FOXMLDOSerializer(m_format);
    }

    /**
     * {@inheritDoc}
     */
    public void serialize(DigitalObject obj,
                          OutputStream out,
                          String encoding,
                          int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedEncodingException {
        LOG.debug("Serializing " + m_format.uri + " for transContext: "
                + transContext);
        m_transContext = transContext;
        OutputStreamWriter osWriter = new OutputStreamWriter(out, encoding);
        PrintWriter writer = new PrintWriter(new BufferedWriter(osWriter));
        try {
            appendXMLDeclaration(obj, encoding, writer);
            appendRootElementStart(obj, writer);
            appendProperties(obj, writer, encoding);
            appendAudit(obj, writer, encoding);
            appendDatastreams(obj, writer, encoding);
            if (m_format.equals(FOXML1_0)) {
                appendDisseminators(obj, writer);
            }
            appendRootElementEnd(writer);
        } finally {
            writer.close();
        }
    }

    //---
    // Instance helpers
    //---

    private void appendXMLDeclaration(DigitalObject obj,
                                      String encoding,
                                      PrintWriter writer) {
        writer.print("<?xml version=\"1.0\" encoding=\"");
        writer.print(encoding);
        writer.print("\"?>\n");
    }

    private void appendRootElementStart(DigitalObject obj, PrintWriter writer)
            throws ObjectIntegrityException {
        writer.print("<");
        writer.print(FOXML.DIGITAL_OBJECT.qName);
        if (m_format.equals(FOXML1_1)) {
            writer.print(" ");
            writer.print(FOXML.VERSION.localName);
            writer.print("=\"1.1\"");
        }
        writer.print(" ");
        writer.print(FOXML.PID.localName);
        writer.print("=\"");
        writer.print(obj.getPid());
        writer.print("\"");
        if (m_transContext == DOTranslationUtility.SERIALIZE_EXPORT_PUBLIC) {
            writer.print(" ");
            writer.print(FOXML.FEDORA_URI.localName);
            writer.print("=\"info:fedora/");
            writer.print(obj.getPid());
            writer.print("\"");
        }
        writer.print("\nxmlns:");
        writer.print(FOXML.prefix);
        writer.print("=\"");
        writer.print(FOXML.uri);
        writer.print("\"\nxmlns:");
        writer.print(XSI.prefix);
        writer.print("=\"");
        writer.print(XSI.uri);
        writer.print("\"\n");
        writer.print(XSI.SCHEMA_LOCATION.qName);
        writer.print("=\"");
        writer.print(FOXML.uri);
        writer.print(" ");
        writer.print(m_format.xsdLocation);
        writer.print("\">\n");
    }

    private void appendProperties(DigitalObject obj,
                                  PrintWriter writer,
                                  String encoding)
            throws ObjectIntegrityException {

        writer.print("<");
        writer.print(FOXML.prefix);
        writer.print(":objectProperties>\n");

        /*
         * fType is eliminated in foxml 1.1+, so choose the best reasonable
         * value for 1.0 serializations
         */
        if (m_format.equals(FOXML1_0)) {
            RDFName ftype = DOTranslationUtility.getTypeAttribute(obj);
            if (ftype != null) {
                appendProperty(RDF.TYPE.uri, ftype.uri, writer, false);
            }
        }

        appendProperty(MODEL.STATE.uri,
                       DOTranslationUtility.getStateAttribute(obj),
                       writer,
                       false);
        appendProperty(MODEL.LABEL.uri, obj.getLabel(), writer, false);
        appendProperty(MODEL.OWNER.uri, obj.getOwnerId(), writer, false);
        appendProperty(MODEL.CREATED_DATE.uri, obj.getCreateDate(), writer);
        appendProperty(VIEW.LAST_MODIFIED_DATE.uri,
                       obj.getLastModDate(),
                       writer);

        Iterator<String> iter = obj.getExtProperties().keySet().iterator();
        while (iter.hasNext()) {
            String name = iter.next();
            appendProperty(name, obj.getExtProperty(name), writer, true);
        }
        writer.print("</");
        writer.print(FOXML.prefix);
        writer.print(":objectProperties>\n");
    }

    private static void appendProperty(String uri,
                                       String value,
                                       PrintWriter writer,
                                       boolean extProperty) {
        if (value != null) {
            writer.print("<");
            writer.print(FOXML.prefix);
            writer.print(':');
            if (extProperty) {
                writer.print("ext");
            }
            writer.print("property NAME=\"");
            writer.print(uri);
            writer.print("\" VALUE=\"");
            writer.print(StreamUtility.enc(value));
            writer.print("\"/>\n");
        }
    }

    private static void appendProperty(String uri,
                                       Date value,
                                       PrintWriter writer) {
        if (value != null) {
            appendProperty(uri,
                           DateUtility.convertDateToString(value),
                           writer,
                           false);
        }
    }

    private void appendDatastreams(DigitalObject obj,
                                   PrintWriter writer,
                                   String encoding)
            throws ObjectIntegrityException, UnsupportedEncodingException,
            StreamIOException {
        Iterator<String> iter = obj.datastreamIdIterator();
        while (iter.hasNext()) {
            String dsid = iter.next();
            boolean haveWrittenCommonAttributes = false;

            // AUDIT datastream is rebuilt from the latest in-memory audit trail
            // which is a separate array list in the DigitalObject class.
            // So, ignore it here.
            if (dsid.equals("AUDIT") || dsid.equals("FEDORA-AUDITTRAIL")) {
                continue;
            }
            // Given a datastream ID, get all the datastream versions.
            // Use the first version to pick up the attributes common to all versions.

            for (Datastream v : obj.datastreams(dsid)) {
                Datastream vds = DOTranslationUtility.setDatastreamDefaults(v);

                // insert the ds attributes common to all versions, when necessary
                if (!haveWrittenCommonAttributes) {
                    writer.print("<");
                    writer.print(FOXML.prefix);
                    writer.print(":datastream ID=\"");
                    writer.print(vds.DatastreamID);
                    writer.print("\"");
                    if (m_transContext == DOTranslationUtility.SERIALIZE_EXPORT_PUBLIC) {
                        writer.print(" FEDORA_URI=\"");
                        writer.print("info:fedora/");
                        writer.print(obj.getPid());
                        writer.print("/");
                        writer.print(vds.DatastreamID);
                        writer.print("\"");
                    }
                    writer.print(" STATE=\"");
                    writer.print(vds.DSState);
                    writer.print("\"");
                    writer.print(" CONTROL_GROUP=\"");
                    writer.print(vds.DSControlGrp);
                    writer.print("\"");
                    writer.print(" VERSIONABLE=\"");
                    writer.print(vds.DSVersionable);
                    writer.print("\">\n");
                    haveWrittenCommonAttributes = true;
                }

                // insert the ds version elements
                writer.print("<");
                writer.print(FOXML.prefix);
                writer.print(":datastreamVersion ID=\"");
                writer.print(vds.DSVersionID);
                writer.print("\"");
                writer.print(" LABEL=\"");
                writer.print(StreamUtility.enc(vds.DSLabel));
                writer.print("\"");
                if (vds.DSCreateDT != null) {
                    writer.print(" CREATED=\"");
                    writer.print(DateUtility.convertDateToString(vds.DSCreateDT));
                    writer.print("\"");
                }
                String altIds =
                        DOTranslationUtility.oneString(vds.DatastreamAltIDs);
                if (altIds != null && !altIds.equals("")) {
                    writer.print(" ALT_IDS=\"");
                    writer.print(StreamUtility.enc(altIds));
                    writer.print("\"");
                }
                writer.print(" MIMETYPE=\"");
                writer.print(StreamUtility.enc(vds.DSMIME));
                writer.print("\"");
                if (vds.DSFormatURI != null && !vds.DSFormatURI.equals("")) {
                    writer.print(" FORMAT_URI=\"");
                    writer.print(StreamUtility.enc(vds.DSFormatURI));
                    writer.print("\"");
                }
                // include size if it's non-zero
                if (vds.DSSize != 0) {
                    writer.print(" SIZE=\"");
                    writer.print(vds.DSSize);
                    writer.print("\"");
                }
                writer.print(">\n");

                // include checksum if it has a value
                String csType = vds.getChecksumType();
                if (csType != null && csType.length() > 0
                        && !csType.equals(Datastream.CHECKSUMTYPE_DISABLED)) {
                    writer.print("<");
                    writer.print(FOXML.prefix);
                    writer.print(":contentDigest TYPE=\"");
                    writer.print(csType);
                    writer.print("\"");
                    writer.print(" DIGEST=\"");
                    writer.print(vds.getChecksum());
                    writer.print("\"/>\n");
                }

                // if E or R insert ds content location as URL
                if (vds.DSControlGrp.equalsIgnoreCase("E")
                        || vds.DSControlGrp.equalsIgnoreCase("R")) {
                    writer.print("<");
                    writer.print(FOXML.prefix);
                    writer.print(":contentLocation TYPE=\"");
                    writer.print("URL\"");
                    writer.print(" REF=\"");
                    String urls = DOTranslationUtility.normalizeDSLocationURLs(
                            obj.getPid(),
                            vds,
                            m_transContext).DSLocation;
                    writer.print(StreamUtility.enc(urls));
                    writer.print("\"/>\n");
                    // if M insert ds content location as an internal identifier
                } else if (vds.DSControlGrp.equalsIgnoreCase("M")) {
                    if (m_transContext == DOTranslationUtility.SERIALIZE_EXPORT_ARCHIVE) {
                        writer.print("<");
                        writer.print(FOXML.prefix);
                        writer.print(":binaryContent> \n");
                        String encoded = Base64.encodeToString(vds.getContentStream());
                        writer.print(StringUtility.splitAndIndent(encoded,
                                                                  14,
                                                                  80));
                        writer.print("</");
                        writer.print(FOXML.prefix);
                        writer.print(":binaryContent> \n");
                    } else {
                        writer.print("<");
                        writer.print(FOXML.prefix);
                        writer.print(":contentLocation TYPE=\"");
                        writer.print("INTERNAL_ID\" REF=\"");
                        String urls = DOTranslationUtility.normalizeDSLocationURLs(
                                obj.getPid(),
                                vds,
                                m_transContext).DSLocation;
                        writer.print(StreamUtility.enc(urls));
                        writer.print("\"/>\n");
                    }
                    // if X insert inline XML
                } else if (vds.DSControlGrp.equalsIgnoreCase("X")) {
                    appendInlineXML(obj,
                                    (DatastreamXMLMetadata) vds,
                                    writer,
                                    encoding);
                }
                writer.print("</");
                writer.print(FOXML.prefix);
                writer.print(":datastreamVersion>\n");
            }
            writer.print("</");
            writer.print(FOXML.prefix);
            writer.print(":datastream>\n");
        }
    }

    private void appendAudit(DigitalObject obj,
                             PrintWriter writer,
                             String encoding) throws ObjectIntegrityException {

        if (obj.getAuditRecords().size() > 0) {
            // Audit trail datastream re-created from audit records.
            // There is only ONE version of the audit trail datastream!
            writer.print("<");
            writer.print(FOXML.prefix);
            writer.print(":datastream ID=\"");
            writer.print("AUDIT\"");
            if (m_transContext == DOTranslationUtility.SERIALIZE_EXPORT_PUBLIC) {
                writer.print(" FEDORA_URI=\"info:fedora/");
                writer.print(obj.getPid());
                writer.print("/AUDIT\"");
            }
            writer.print(" STATE=\"A\" CONTROL_GROUP=\"X\" VERSIONABLE=\"false\">\n");
            // insert the ds version-level elements
            writer.print("<");
            writer.print(FOXML.prefix);
            writer.print(":datastreamVersion ID=\"AUDIT.0\" LABEL=\"");
            writer.print("Audit Trail for this object\" CREATED=\"");
            writer.print(DateUtility.convertDateToString(obj.getCreateDate()));
            writer.print("\" MIMETYPE=\"text/xml\" FORMAT_URI=\"");
            writer.print(AUDIT1_0.uri);
            writer.print("\">\n");
            writer.print("<");
            writer.print(FOXML.prefix);
            writer.print(":xmlContent>\n");
            DOTranslationUtility.appendAuditTrail(obj, writer);
            writer.print("</");
            writer.print(FOXML.prefix);
            writer.print(":xmlContent>\n");
            writer.print("</");
            writer.print(FOXML.prefix);
            writer.print(":datastreamVersion>\n");
            writer.print("</");
            writer.print(FOXML.prefix);
            writer.print(":datastream>\n");
        }
    }

    private void appendInlineXML(DigitalObject obj,
                                 DatastreamXMLMetadata ds,
                                 PrintWriter writer,
                                 String encoding)
            throws ObjectIntegrityException, UnsupportedEncodingException,
            StreamIOException {

        writer.print("<");
        writer.print(FOXML.prefix);
        writer.print(":xmlContent>\n");

        // Relative Repository URLs: If it's a WSDL or SERVICE-PROFILE datastream
        // in a SDep object search for any embedded URLs that are relative to
        // the local repository (like internal service URLs) and make sure they
        // are converted appropriately for the translation context.
        if (obj.hasContentModel(SERVICE_DEPLOYMENT_3_0)
                && (ds.DatastreamID.equals("SERVICE-PROFILE") || ds.DatastreamID
                        .equals("WSDL"))) {
            // FIXME! We need a more efficient way than to search
            // the whole block of inline XML. We really only want to
            // look at service URLs in the XML.
            writer.print(DOTranslationUtility
                    .normalizeInlineXML(new String(ds.xmlContent, "UTF-8")
                            .trim(), m_transContext));
        } else {
            DOTranslationUtility.appendXMLStream(ds.getContentStream(),
                                                 writer,
                                                 encoding);
        }
        writer.print("\n</");
        writer.print(FOXML.prefix);
        writer.print(":xmlContent>\n");
    }

    private void appendDisseminators(DigitalObject obj, PrintWriter writer)
            throws ObjectIntegrityException {

        Iterator<String> dissIdIter = obj.disseminatorIdIterator();
        while (dissIdIter.hasNext()) {
            String did = dissIdIter.next();
            List<Disseminator> dissList = obj.disseminators(did);

            for (int i = 0; i < dissList.size(); i++) {
                Disseminator vdiss =
                        DOTranslationUtility
                                .setDisseminatorDefaults(obj
                                        .disseminators(did).get(i));
                // insert the disseminator elements common to all versions.
                if (i == 0) {
                    writer.print("<");
                    writer.print(FOXML.prefix);
                    writer.print(":disseminator ID=\"");
                    writer.print(did);
                    writer.print("\" BDEF_CONTRACT_PID=\"");
                    writer.print(vdiss.bDefID);
                    writer.print("\" STATE=\"");
                    writer.print(vdiss.dissState);
                    writer.print("\" VERSIONABLE=\"");
                    writer.print(vdiss.dissVersionable);
                    writer.print("\">\n");
                }
                // insert the disseminator version-level elements
                writer.print("<");
                writer.print(FOXML.prefix);
                writer.print(":disseminatorVersion ID=\"");
                writer.print(vdiss.dissVersionID);
                writer.print("\"");
                if (vdiss.dissLabel != null && !vdiss.dissLabel.equals("")) {
                    writer.print(" LABEL=\"");
                    writer.print(StreamUtility.enc(vdiss.dissLabel));
                    writer.print("\"");
                }
                writer.print(" BMECH_SERVICE_PID=\"");
                writer.print(vdiss.sDepID);
                writer.print("\"");
                if (vdiss.dissCreateDT != null) {
                    writer.print(" CREATED=\"");
                    writer.print(DateUtility.convertDateToString(vdiss.dissCreateDT));
                    writer.print("\"");
                }
                writer.print(">\n");

                // datastream bindings...
                DSBinding[] bindings = vdiss.dsBindMap.dsBindings;
                writer.print("<");
                writer.print(FOXML.prefix);
                writer.print(":serviceInputMap>\n");
                for (int j = 0; j < bindings.length; j++) {
                    if (bindings[j].seqNo == null) {
                        bindings[j].seqNo = "";
                    }
                    writer.print("<");
                    writer.print(FOXML.prefix);
                    writer.print(":datastreamBinding KEY=\"");
                    writer.print(bindings[j].bindKeyName);
                    writer.print("\" DATASTREAM_ID=\"");
                    writer.print(bindings[j].datastreamID);
                    writer.print("\"");
                    if (bindings[j].bindLabel != null
                            && !bindings[j].bindLabel.equals("")) {
                        writer.print(" LABEL=\"");
                        writer.print(StreamUtility.enc(bindings[j].bindLabel));
                        writer.print("\"");
                    }
                    if (bindings[j].seqNo != null
                            && !bindings[j].seqNo.equals("")) {
                        writer.print(" ORDER=\"");
                        writer.print(bindings[j].seqNo);
                        writer.print("\"");
                    }
                    writer.print("/>\n");
                }
                writer.print("</");
                writer.print(FOXML.prefix);
                writer.print(":serviceInputMap>\n");
                writer.print("</");
                writer.print(FOXML.prefix);
                writer.print(":disseminatorVersion>\n");
            }
            writer.print("</");
            writer.print(FOXML.prefix);
            writer.print(":disseminator>\n");
        }
    }

    private void appendRootElementEnd(PrintWriter writer) {
        writer.print("</");
        writer.print(FOXML.prefix);
        writer.print(":digitalObject>");
    }
}
