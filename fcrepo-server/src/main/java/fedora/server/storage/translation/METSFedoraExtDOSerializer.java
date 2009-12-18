/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import java.util.Date;
import java.util.Iterator;

import org.apache.log4j.Logger;

import fedora.common.Constants;
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
 * Serializes objects in the constructor-provider version of the METS Fedora
 * Extension format.
 *
 * @author Sandy Payette
 * @author Chris Wilper
 */
@SuppressWarnings("deprecation")
public class METSFedoraExtDOSerializer
        implements Constants, DOSerializer {

    /**
     * The format this serializer will write if unspecified at construction.
     * This defaults to the latest METS Fedora Extension format.
     */
    public static final XMLFormat DEFAULT_FORMAT = METS_EXT1_1;

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(METSFedoraExtDOSerializer.class);

    /** The format this serializer writes. */
    private final XMLFormat m_format;

    /** The current translation context. */
    private int m_transContext;

    /**
     * Creates a serializer that writes the default METS Fedora Extension
     * format.
     */
    public METSFedoraExtDOSerializer() {
        m_format = DEFAULT_FORMAT;
    }

    /**
     * Creates a serializer that writes the given METS Fedora Extension format.
     *
     * @param format
     *        the version-specific METS Fedora Extension format.
     * @throws IllegalArgumentException
     *         if format is not a known METS Fedora extension format.
     */
    public METSFedoraExtDOSerializer(XMLFormat format) {
        if (format.equals(METS_EXT1_0) || format.equals(METS_EXT1_1)) {
            m_format = format;
        } else {
            throw new IllegalArgumentException("Not a METS Fedora Extension "
                    + "format: " + format.uri);
        }
    }

    //---
    // DOSerializer implementation
    //---

    /**
     * {@inheritDoc}
     */
    public DOSerializer getInstance() {
        return new METSFedoraExtDOSerializer(m_format);
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
        PrintWriter writer = new PrintWriter(osWriter);
        try {
            appendXMLDeclaration(obj, encoding, writer);
            appendRootElementStart(obj, writer);
            appendHdr(obj, writer);
            appendDescriptiveMD(obj, writer, encoding);
            appendAuditRecordAdminMD(obj, writer);
            appendOtherAdminMD(obj, writer, encoding);
            appendFileSecs(obj, writer);
            if (m_format.equals(METS_EXT1_0)) {
                appendStructMaps(obj, writer);
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
        writer.print("\" ?>\n");
    }

    private void appendRootElementStart(DigitalObject obj, PrintWriter writer)
            throws ObjectIntegrityException {
        writer.print("<");
        writer.print(METS.METS.qName);
        if (m_format.equals(METS_EXT1_1)) {
            writer.print(" ");
            writer.print(METS_EXT.EXT_VERSION.localName);
            writer.print("=\"1.1\"");
        }
        writer.print(" ");
        writer.print(METS.OBJID.localName);
        writer.print("=\"");
        writer.print(obj.getPid());
        writer.print("\"");

        if (m_format.equals(METS_EXT1_0)
                && DOTranslationUtility.getTypeAttribute(obj) != null) {
            writer.print(" ");
            writer.print(METS.TYPE.localName);
            writer.print("=\"");
            writer.print(DOTranslationUtility.getTypeAttribute(obj).localName);
            writer.print("\"");
        }

        writer.print("\n");
        String label = obj.getLabel();
        if (label != null && label.length() > 0) {
            writer.print(METS.LABEL.localName);
            writer.print("=\"");
            writer.print(StreamUtility.enc(label));
            writer.print("\"\n");
        }
        writer.print("xmlns:");
        writer.print(METS.prefix);
        writer.print("=\"");
        writer.print(METS.uri);
        writer.print("\"\n");
        if (m_format.equals(METS_EXT1_0)) {
            writer.print("xmlns:");
            writer.print(XLINK.prefix);
            writer.print("=\"");
            writer.print(OLD_XLINK.uri);
            writer.print("\"\n");
        } else {
            writer.print("xmlns:");
            writer.print(XLINK.prefix);
            writer.print("=\"");
            writer.print(XLINK.uri);
            writer.print("\"\n");
        }
        writer.print("xmlns:");
        writer.print(XSI.prefix);
        writer.print("=\"");
        writer.print(XSI.uri);
        writer.print("\"\n");
        writer.print(XSI.SCHEMA_LOCATION.qName);
        writer.print("=\"");
        writer.print(METS.uri);
        writer.print(" ");
        writer.print(m_format.xsdLocation);
        writer.print("\">\n");
    }

    private void appendHdr(DigitalObject obj, PrintWriter writer) throws ObjectIntegrityException {
        writer.print("<");
        writer.print(METS.prefix);
        writer.print(":metsHdr");
        Date cDate = obj.getCreateDate();
        if (cDate != null) {
            writer.print(" CREATEDATE=\"");
            writer.print(DateUtility.convertDateToString(cDate));
            writer.print("\"");
        }
        Date mDate = obj.getLastModDate();
        if (mDate != null) {
            writer.print(" LASTMODDATE=\"");
            writer.print(DateUtility.convertDateToString(mDate));
            writer.print("\"");
        }

        writer.print(" RECORDSTATUS=\"");
        writer.print(DOTranslationUtility.getStateAttribute(obj));
        writer.print("\"");

        writer.print(">\n");
        // use agent to identify the owner of the digital object
        String ownerId = obj.getOwnerId();
        if (ownerId != null && !ownerId.equals("")) {
            writer.print("<");
            writer.print(METS.prefix);
            writer.print(":agent");
            writer.print(" ROLE=\"IPOWNER\">\n");
            writer.print("<");
            writer.print(METS.prefix);
            writer.print(":name>");
            writer.print(ownerId);
            writer.print("</");
            writer.print(METS.prefix);
            writer.print(":name>\n");
            writer.print("</");
            writer.print(METS.prefix);
            writer.print(":agent>\n");
        }
        writer.print("</");
        writer.print(METS.prefix);
        writer.print(":metsHdr>\n");
    }

    private void appendDescriptiveMD(DigitalObject obj,
                                     PrintWriter writer,
                                     String encoding)
            throws ObjectIntegrityException, UnsupportedEncodingException,
            StreamIOException {
        Iterator<String> iter = obj.datastreamIdIterator();
        while (iter.hasNext()) {
            String id = iter.next();
            Datastream firstDS = obj.datastreams(id).iterator().next();
            if (firstDS.DSControlGrp.equals("X")
                    && ((DatastreamXMLMetadata) firstDS).DSMDClass == DatastreamXMLMetadata.DESCRIPTIVE) {
                appendMDSec(obj,
                            "dmdSecFedora",
                            "descMD",
                            obj.datastreams(id),
                            writer,
                            encoding);
            }
        }
    }

    private void appendMDSec(DigitalObject obj,
                             String outerName,
                             String innerName,
                             Iterable<Datastream> XMLMetadata,
                             PrintWriter writer,
                             String encoding) throws ObjectIntegrityException,
            UnsupportedEncodingException, StreamIOException {
        DatastreamXMLMetadata first =
                (DatastreamXMLMetadata) DOTranslationUtility
                        .setDatastreamDefaults(XMLMetadata
                                .iterator().next());
        writer.print("<");
        writer.print(METS.prefix);
        writer.print(":");
        writer.print(outerName);
        writer.print(" ID=\"");
        writer.print(first.DatastreamID);
        writer.print("\" STATUS=\"");
        writer.print(first.DSState);
        writer.print("\" VERSIONABLE=\"");
        writer.print(first.DSVersionable);
        writer.print("\">\n");
        for (Datastream d : XMLMetadata) {
            DatastreamXMLMetadata ds =
                    (DatastreamXMLMetadata) DOTranslationUtility
                            .setDatastreamDefaults(d);

            writer.print("<");
            writer.print(METS.prefix);
            writer.print(":");
            writer.print(innerName);
            writer.print(" ID=\"");
            writer.print(ds.DSVersionID);
            writer.print("\"");
            if (ds.DSCreateDT != null) {
                writer.print(" CREATED=\"");
                writer.print(DateUtility.convertDateToString(ds.DSCreateDT));
                writer.print("\"");
            }
            writer.print(">\n");




            writer.print("<");
            writer.print(METS.prefix);
            writer.print(":mdWrap MIMETYPE=\"");
            writer.print(StreamUtility.enc(ds.DSMIME));
            writer.print("\" MDTYPE=\"");
            String mdType = ds.DSInfoType;
            if (!mdType.equals("MARC") && !mdType.equals("EAD")
                    && !mdType.equals("DC") && !mdType.equals("NISOIMG")
                    && !mdType.equals("LC-AV") && !mdType.equals("VRA")
                    && !mdType.equals("TEIHDR") && !mdType.equals("DDI")
                    && !mdType.equals("FGDC")) {
                writer.print("OTHER\" OTHERMDTYPE=\"");
                writer.print(StreamUtility.enc(mdType));
            } else {
                writer.print(mdType);
            }
            writer.print("\" ");

            if (ds.DSLabel != null && !ds.DSLabel.equals("")) {
                writer.print(" LABEL=\"");
                writer.print(StreamUtility.enc(ds.DSLabel));
                writer.print("\"");
            }

            if (ds.DSFormatURI != null && !ds.DSFormatURI.equals("")) {
                writer.print(" FORMAT_URI=\"");
                writer.print(StreamUtility.enc(ds.DSFormatURI));
                writer.print("\"");
            }

            String altIds = DOTranslationUtility.oneString(ds.DatastreamAltIDs);
            if (altIds != null && !altIds.equals("")) {
                writer.print(" ALT_IDS=\"");
                writer.print(StreamUtility.enc(altIds));
                writer.print("\"");
            }

            // CHECKSUM and CHECKSUMTYPE are also optional
            String csType = ds.DSChecksumType;
            if (csType != null
                    && csType.length() > 0
                    && !csType.equals(Datastream.CHECKSUMTYPE_DISABLED)) {
                writer.print(" CHECKSUM=\"");
                writer.print(StreamUtility.enc(ds.DSChecksum));
                writer.print("\" CHECKSUMTYPE=\"");
                writer.print(StreamUtility.enc(csType));
                writer.print("\"");
            }

            writer.print(">\n");
            writer.print("<");
            writer.print(METS.prefix);
            writer.print(":xmlData>\n");

            // If WSDL or SERVICE-PROFILE datastream (in SDep)
            // make sure that any embedded URLs are encoded
            // appropriately for either EXPORT or STORE.
            if (obj.hasContentModel(SERVICE_DEPLOYMENT_3_0)
                    && ds.DatastreamID.equals("SERVICE-PROFILE")
                    || ds.DatastreamID.equals("WSDL")) {
                writer.print(DOTranslationUtility
                        .normalizeInlineXML(new String(ds.xmlContent, "UTF-8")
                                .trim(), m_transContext));
            } else {
                DOTranslationUtility.appendXMLStream(ds.getContentStream(),
                                                     writer,
                                                     encoding);
            }
            writer.print("\n</");
            writer.print(METS.prefix);
            writer.print(":xmlData>");
            writer.print("</");
            writer.print(METS.prefix);
            writer.print(":mdWrap>\n");
            writer.print("</");
            writer.print(METS.prefix);
            writer.print(":");
            writer.print(innerName);
            writer.print(">\n");
        }
        writer.print("</");
        writer.print(METS.prefix);
        writer.print(":");
        writer.print(outerName);
        writer.print(">\n");
    }

    private void appendAuditRecordAdminMD(DigitalObject obj, PrintWriter writer)
            throws ObjectIntegrityException {
        if (obj.getAuditRecords().size() > 0) {
            writer.print("<");
            writer.print(METS.prefix);
            writer.print(":amdSec ID=\"AUDIT\" STATUS=\"A\" VERSIONABLE=\"false\">\n");
            writer.print("<");
            writer.print(METS.prefix);
            writer.print(":digiprovMD ID=\"AUDIT.0\" CREATED=\"");
            writer.print(DateUtility.convertDateToString(obj.getCreateDate()));
            writer.print("\">\n");
            writer.print("<");
            writer.print(METS.prefix);
            writer.print(":mdWrap MIMETYPE=\"text/xml\" MDTYPE=\"OTHER\"");
            writer.print(" OTHERMDTYPE=\"FEDORA-AUDIT\"");
            writer.print(" LABEL=\"Audit Trail for this object\"");
            writer.print(" FORMAT_URI=\"");
            writer.print(AUDIT1_0.uri);
            writer.print("\">\n");
            writer.print("<");
            writer.print(METS.prefix);
            writer.print(":xmlData>\n");
            DOTranslationUtility.appendAuditTrail(obj, writer);
            writer.print("</");
            writer.print(METS.prefix);
            writer.print(":xmlData>\n");
            writer.print("</");
            writer.print(METS.prefix);
            writer.print(":mdWrap>\n");
            writer.print("</");
            writer.print(METS.prefix);
            writer.print(":digiprovMD>\n");
            writer.print("</");
            writer.print(METS.prefix);
            writer.print(":amdSec>\n");
        }
    }

    private void appendOtherAdminMD(DigitalObject obj,
                                    PrintWriter writer,
                                    String encoding)
            throws ObjectIntegrityException, UnsupportedEncodingException,
            StreamIOException {
        Iterator<String> iter = obj.datastreamIdIterator();
        while (iter.hasNext()) {
            String id = iter.next();
            Datastream firstDS =
                    obj.datastreams(id).iterator().next();
            // First, work with the first version to get the mdClass set to
            // a proper value required in the METS XML Schema.
            if (firstDS.DSControlGrp.equals("X")
                    && ((DatastreamXMLMetadata) firstDS).DSMDClass != DatastreamXMLMetadata.DESCRIPTIVE) {
                DatastreamXMLMetadata md = (DatastreamXMLMetadata) firstDS;
                // Default mdClass to techMD when a valid one does not appear
                // (say because the object was born as FOXML)
                String mdClass = "techMD";
                if (md.DSMDClass == DatastreamXMLMetadata.TECHNICAL) {
                    mdClass = "techMD";
                } else if (md.DSMDClass == DatastreamXMLMetadata.SOURCE) {
                    mdClass = "sourceMD";
                } else if (md.DSMDClass == DatastreamXMLMetadata.RIGHTS) {
                    mdClass = "rightsMD";
                } else if (md.DSMDClass == DatastreamXMLMetadata.DIGIPROV) {
                    mdClass = "digiprovMD";
                }
                // Then, pass everything along to do the actual serialization
                appendMDSec(obj,
                            "amdSec",
                            mdClass,
                            obj.datastreams(id),
                            writer,
                            encoding);
            }
        }
    }

    private void appendFileSecs(DigitalObject obj, PrintWriter writer)
            throws ObjectIntegrityException, StreamIOException {
        Iterator<String> iter = obj.datastreamIdIterator();
        boolean didFileSec = false;
        while (iter.hasNext()) {
            Datastream ds =
                    DOTranslationUtility.setDatastreamDefaults(obj
                            .datastreams(iter.next()).iterator().next());
            if (!ds.DSControlGrp.equals("X")) {
                if (!didFileSec) {
                    didFileSec = true;
                    writer.print("<");
                    writer.print(METS.prefix);
                    writer.print(":fileSec>\n");
                    writer.print("<");
                    writer.print(METS.prefix);
                    writer.print(":fileGrp ID=\"DATASTREAMS\">\n");
                }
                writer.print("<");
                writer.print(METS.prefix);
                writer.print(":fileGrp ID=\"");
                writer.print(ds.DatastreamID);
                writer.print("\" STATUS=\"");
                writer.print(ds.DSState);
                writer.print("\" VERSIONABLE=\"");
                writer.print(ds.DSVersionable);
                writer.print("\">\n");
                Iterator<Datastream> contentIter =
                        obj.datastreams(ds.DatastreamID).iterator();
                while (contentIter.hasNext()) {
                    Datastream dsc =
                            DOTranslationUtility
                                    .setDatastreamDefaults(contentIter.next());


                    writer.print("<");
                    writer.print(METS.prefix);
                    writer.print(":file ID=\"");
                    writer.print(dsc.DSVersionID);
                    writer.print("\"");
                    if (dsc.DSCreateDT != null) {
                        writer.print(" CREATED=\"");
                        writer.print(DateUtility.convertDateToString(dsc.DSCreateDT));
                        writer.print("\"");
                    }
                    writer.print(" MIMETYPE=\"");
                    writer.print(StreamUtility.enc(dsc.DSMIME));
                    writer.print("\"");
                    if (dsc.DSSize != 0) {
                        writer.print(" SIZE=\"" + dsc.DSSize + "\"");
                    }
                    if (dsc.DSFormatURI != null && !dsc.DSFormatURI.equals("")) {
                        writer.print(" FORMAT_URI=\"");
                        writer.print(StreamUtility.enc(dsc.DSFormatURI));
                        writer.print("\"");
                    }
                    String altIds =
                            DOTranslationUtility
                                    .oneString(dsc.DatastreamAltIDs);
                    if (altIds != null && !altIds.equals("")) {
                        writer.print(" ALT_IDS=\"");
                        writer.print(StreamUtility.enc(altIds));
                        writer.print("\"");
                    }
                    String csType = ds.DSChecksumType;
                    if (csType != null
                            && csType.length() > 0
                            && !csType.equals(Datastream.CHECKSUMTYPE_DISABLED)) {
                        writer.print(" CHECKSUM=\"");
                        writer.print(StreamUtility.enc(ds.DSChecksum));
                        writer.print("\"");
                        writer.print(" CHECKSUMTYPE=\"");
                        writer.print(StreamUtility.enc(csType));
                        writer.print("\"");
                    }
                    writer.print(" OWNERID=\"");
                    writer.print(dsc.DSControlGrp);
                    writer.print("\">\n");
                    if (m_transContext == DOTranslationUtility.SERIALIZE_EXPORT_ARCHIVE
                            && dsc.DSControlGrp.equalsIgnoreCase("M")) {
                        writer.print("<");
                        writer.print(METS.prefix);
                        writer.print(":FContent> \n");
                        String encoded = Base64.encodeToString(dsc.getContentStream());
                        writer.print(StringUtility.splitAndIndent(encoded,
                                                                  14,
                                                                  80));
                        writer.print("</");
                        writer.print(METS.prefix);
                        writer.print(":FContent>\n");
                    } else {
                        writer.print("<");
                        writer.print(METS.prefix);
                        writer.print(":FLocat");
                        if (dsc.DSLabel != null && !dsc.DSLabel.equals("")) {
                            writer.print(" ");
                            writer.print(XLINK.prefix);
                            writer.print(":title=\"");
                            writer.print(StreamUtility.enc(dsc.DSLabel));
                            writer.print("\"");
                        }
                        writer.print(" LOCTYPE=\"URL\" ");
                        writer.print(XLINK.prefix);
                        writer.print(":href=\"");
                        writer.print(StreamUtility.enc(
                                DOTranslationUtility.normalizeDSLocationURLs(
                                        obj.getPid(),
                                        dsc,
                                        m_transContext).DSLocation));
                        writer.print("\"/>\n");
                    }
                    writer.print("</");
                    writer.print(METS.prefix);
                    writer.print(":file>\n");
                }
                writer.print("</");
                writer.print(METS.prefix);
                writer.print(":fileGrp>\n");
            }
        }
        if (didFileSec) {
            writer.print("</");
            writer.print(METS.prefix);
            writer.print(":fileGrp>\n");
            writer.print("</");
            writer.print(METS.prefix);
            writer.print(":fileSec>\n");
        }
    }

    private void appendStructMaps(DigitalObject obj, PrintWriter writer)
            throws ObjectIntegrityException {
        Iterator<String> dissIdIter = obj.disseminatorIdIterator();
        while (dissIdIter.hasNext()) {
            String did = dissIdIter.next();
            Iterator<Disseminator> dissIter = obj.disseminators(did).iterator();
            while (dissIter.hasNext()) {
                Disseminator diss =
                        DOTranslationUtility.setDisseminatorDefaults(dissIter
                                .next());
                writer.print("<");
                writer.print(METS.prefix);
                writer.print(":structMap ID=\"");
                writer.print(diss.dsBindMapID);
                writer.print("\" TYPE=\"fedora:dsBindingMap\">\n");
                writer.print("<");
                writer.print(METS.prefix);
                writer.print(":div TYPE=\"");
                writer.print(diss.sDepID);
                writer.print("\"");
                if (diss.dsBindMap.dsBindMapLabel != null
                        && !diss.dsBindMap.dsBindMapLabel.equals("")) {
                    writer.print(" LABEL=\"");
                    writer.print(StreamUtility.enc(diss.dsBindMap.dsBindMapLabel));
                    writer.print("\"");
                }
                writer.print(">\n");
                DSBinding[] bindings = diss.dsBindMap.dsBindings;
                for (int i = 0; i < bindings.length; i++) {
                    if (bindings[i].bindKeyName == null
                            || bindings[i].bindKeyName.equals("")) {
                        throw new ObjectIntegrityException("Object's disseminator"
                                + " binding map binding must have a binding key name.");
                    }
                    writer.print("<");
                    writer.print(METS.prefix);
                    writer.print(":div TYPE=\"");
                    writer.print(bindings[i].bindKeyName);
                    if (bindings[i].bindLabel != null
                            && !bindings[i].bindLabel.equals("")) {
                        writer.print("\" LABEL=\"");
                        writer.print(StreamUtility.enc(bindings[i].bindLabel));
                    }
                    if (bindings[i].seqNo != null
                            && !bindings[i].seqNo.equals("")) {
                        writer.print("\" ORDER=\"");
                        writer.print(bindings[i].seqNo);
                    }
                    if (bindings[i].datastreamID == null
                            || bindings[i].datastreamID.equals("")) {
                        throw new ObjectIntegrityException("Object's disseminator"
                                + " binding map binding must point to a datastream.");
                    }
                    writer.print("\">\n<");
                    writer.print(METS.prefix);
                    writer.print(":fptr FILEID=\"");
                    writer.print(bindings[i].datastreamID);
                    writer.print("\"/>\n");
                    writer.print("</");
                    writer.print(METS.prefix);
                    writer.print(":div>\n");
                }
                writer.print("</");
                writer.print(METS.prefix);
                writer.print(":div>\n");
                writer.print("</");
                writer.print(METS.prefix);
                writer.print(":structMap>\n");
            }
        }
    }

    private void appendDisseminators(DigitalObject obj, PrintWriter writer)
            throws ObjectIntegrityException {
        Iterator<String> dissIdIter = obj.disseminatorIdIterator();
        while (dissIdIter.hasNext()) {
            String did = dissIdIter.next();
            Disseminator diss =
                    DOTranslationUtility.setDisseminatorDefaults(obj
                            .disseminators(did).get(0));
            writer.print("<");
            writer.print(METS.prefix);
            writer.print(":behaviorSec ID=\"");
            writer.print(did);
            writer.print("\" STATUS=\"");
            writer.print(diss.dissState);
            writer.print("\">\n");
            for (int i = 0; i < obj.disseminators(did).size(); i++) {
                diss =
                        DOTranslationUtility
                                .setDisseminatorDefaults(obj
                                        .disseminators(did).get(i));
                writer.print("<");
                writer.print(METS.prefix);
                writer.print(":serviceBinding ID=\"");
                writer.print(diss.dissVersionID);
                writer.print("\" STRUCTID=\"");
                writer.print(diss.dsBindMapID);
                writer.print("\" BTYPE=\"");
                writer.print(diss.bDefID);
                writer.print("\" CREATED=\"");
                writer.print(DateUtility.convertDateToString(diss.dissCreateDT));
                writer.print("\"");
                if (diss.dissLabel != null && !diss.dissLabel.equals("")) {
                    writer.print(" LABEL=\"");
                    writer.print(StreamUtility.enc(diss.dissLabel));
                    writer.print("\"");
                }
                writer.print(">\n");
                writer.print("<");
                writer.print(METS.prefix);
                writer.print(":interfaceMD LOCTYPE=\"URN\" ");
                writer.print(XLINK.prefix);
                writer.print(":href=\"");
                writer.print(diss.bDefID);
                writer.print("\"/>\n");
                writer.print("<");
                writer.print(METS.prefix);
                writer.print(":serviceBindMD LOCTYPE=\"URN\" ");
                writer.print(XLINK.prefix);
                writer.print(":href=\"");
                writer.print(diss.sDepID);
                writer.print("\"/>\n");

                writer.print("</");
                writer.print(METS.prefix);
                writer.print(":serviceBinding>\n");
            }
            writer.print("</");
            writer.print(METS.prefix);
            writer.print(":behaviorSec>\n");
        }
    }

    private void appendRootElementEnd(PrintWriter writer) {
        writer.print("</");
        writer.print(METS.prefix);
        writer.print(":mets>");
    }

}
