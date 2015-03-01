/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.translation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.thread.ThreadHelper;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.util.MimeTypeHelper;
import org.apache.commons.io.IOUtils;
import org.fcrepo.common.Constants;
import org.fcrepo.common.Models;
import org.fcrepo.common.PID;
import org.fcrepo.common.xml.format.XMLFormat;
import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamXMLMetadata;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.utilities.DateUtility;
import org.fcrepo.utilities.MimeTypeUtils;
import org.fcrepo.utilities.ReadableCharArrayWriter;

/**
 * <p>Serializes a Fedora Object in Atom with Threading Extensions.</p>
 *
 * <p>A Fedora Digital Object is represented as an atom:feed and
 * Datastreams are represented as an atom:entries.</p>
 *
 * <p>The hierarchy of Datastreams their Datastream Versions is
 * represented via the Atom Threading Extensions.
 * For convenience, a datastream entry references its latest datastream
 * version entry with an atom:link element. For example, a DC datastream
 * entry with a reference to its most recent version: <br/>
 * <code>&lt;link href="info:fedora/demo:foo/DC/2008-04-01T12:30:15.123" rel="alternate"/&gt</code></p>
 *
 * <p>Each datastream version refers to its parent datastream via a
 * thr:in-reply-to element. For example, the entry for a DC datastream
 * version would include:<br/>
 * <code>&lt;thr:in-reply-to ref="info:fedora/demo:foo/DC"/&gt;</code></p>
 *
 * @see <a href="http://atomenabled.org/developers/syndication/atom-format-spec.php">The Atom Syndication Format</a>
 * @see <a href="http://www.ietf.org/rfc/rfc4685.txt">Atom Threading Extensions</a>
 *
 * @author Edwin Shin
 * @since 3.0
 * @version $Id$
 */
public class AtomDOSerializer
        implements DOSerializer, Constants {

    /**
     * The format this serializer will write if unspecified at construction.
     * This defaults to the latest ATOM format.
     */
    public static final XMLFormat DEFAULT_FORMAT = ATOM1_1;

    private final static Abdera abdera = Abdera.getInstance();

    /** The format this serializer writes. */
    private final XMLFormat m_format;

    /** The translation utility is use */
    private DOTranslationUtility m_translator;

    public AtomDOSerializer() {
        this(DEFAULT_FORMAT);
    }

    public AtomDOSerializer(XMLFormat format) {
        this(format, null);
    }
    public AtomDOSerializer(XMLFormat format, DOTranslationUtility translator) {
        if (format.equals(ATOM1_1) || format.equals(ATOM_ZIP1_1)) {
            m_format = format;
        } else {
            throw new IllegalArgumentException("Not an ATOM format: "
                    + format.uri);
        }
        m_translator = (translator == null) ? DOTranslationUtility.defaultInstance() : translator;
    }

    /**
     * {@inheritDoc}
     */
    public DOSerializer getInstance() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void serialize(DigitalObject obj,
                          OutputStream out,
                          String encoding,
                          int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedEncodingException {
        if (encoding == null || encoding == "")encoding = "UTF-8";
        Feed feed = abdera.newFeed();
        ZipOutputStream zout = null;
        if (m_format.equals(ATOM_ZIP1_1)) {
            zout = new ZipOutputStream(out);
        }

        addObjectProperties(obj, feed);
        feed.setIcon("http://www.fedora-commons.org/images/logo_vertical_transparent_200_251.png");
        addDatastreams(feed, obj, zout, encoding, transContext);

        if (m_format.equals(ATOM_ZIP1_1)) {
            try {
                zout.putNextEntry(new ZipEntry("atommanifest.xml"));
                feed.writeTo("prettyxml", zout);
                zout.closeEntry();
                zout.close();
            } catch (IOException e) {
                throw new StreamIOException(e.getMessage(), e);
            }
        } else {
            try {
                feed.writeTo("prettyxml", out);
            } catch (IOException e) {
                throw new StreamIOException(e.getMessage(), e);
            }
        }
    }

    private void addObjectProperties(DigitalObject obj, Feed feed) throws ObjectIntegrityException {
        String state = DOTranslationUtility.getStateAttribute(obj);
        String ownerId = obj.getOwnerId();
        String label = obj.getLabel();
        Date cdate = obj.getCreateDate();
        Date mdate = obj.getLastModDate();

        feed.setId(PID.toURI(obj.getPid()));
        feed.setTitle(label == null ? "" : label);
        feed.setUpdated(mdate);
        feed.addAuthor(ownerId == null ? "" : StreamUtility.enc(ownerId));

        feed.addCategory(MODEL.STATE.uri, state, null);

        if (cdate != null) {
            feed.addCategory(MODEL.CREATED_DATE.uri, DateUtility
                    .convertDateToString(cdate), null);
        }

        // TODO not sure I'm satisfied with this representation of extProperties
        for (String extProp : obj.getExtProperties().keySet()) {
            feed.addCategory(MODEL.EXT_PROPERTY.uri, extProp, obj
                    .getExtProperty(extProp));
        }
    }

    private void addDatastreams(Feed feed, DigitalObject obj, ZipOutputStream zout, String encoding, int transContext) throws ObjectIntegrityException,
            UnsupportedEncodingException, StreamIOException {
        Iterator<String> iter = obj.datastreamIdIterator();
        String dsid;
        while (iter.hasNext()) {
            dsid = iter.next();
            // AUDIT datastream is rebuilt from the latest in-memory audit trail
            // which is a separate array list in the DigitalObject class.
            // So, ignore it here.
            if (dsid.equals("AUDIT") || dsid.equals("FEDORA-AUDITTRAIL")) {
                continue;
            }

            Entry dsEntry = feed.addEntry();

            Datastream latestCreated = null;
            long latestCreateTime = -1;

            for (Datastream v : obj.datastreams(dsid)) {
                Datastream dsv = DOTranslationUtility.setDatastreamDefaults(v);

                // Keep track of the most recent datastream version
                if (dsv.DSCreateDT.getTime() > latestCreateTime) {
                    latestCreateTime = dsv.DSCreateDT.getTime();
                    latestCreated = dsv;
                }

                Entry dsvEntry = feed.addEntry();
                dsvEntry.setId(PID.toURI(obj.getPid()) + "/" + dsv.DatastreamID + "/"
                        + DateUtility.convertDateToString(dsv.DSCreateDT));
                dsvEntry.setTitle(dsv.DSVersionID);
                dsvEntry.setUpdated(dsv.DSCreateDT);

                ThreadHelper.addInReplyTo(dsvEntry, PID.toURI(obj.getPid()) + "/"
                        + dsv.DatastreamID);

                String altIds =
                        DOTranslationUtility.oneString(dsv.DatastreamAltIDs);
                if (altIds != null && !altIds.isEmpty()) {
                    dsvEntry.addCategory(MODEL.ALT_IDS.uri, altIds, null);
                }
                if (dsv.DSFormatURI != null && !dsv.DSFormatURI.isEmpty()) {
                    dsvEntry.addCategory(MODEL.FORMAT_URI.uri,
                                         dsv.DSFormatURI,
                                         null);
                }

                dsvEntry.addCategory(MODEL.LABEL.uri, dsv.DSLabel == null ? ""
                        : dsv.DSLabel, null);

                // include checksum if it has a value
                String csType = dsv.getChecksumType();
                if (csType != null && csType.length() > 0
                        && !csType.equals(Datastream.CHECKSUMTYPE_DISABLED)) {
                    dsvEntry.addCategory(MODEL.DIGEST_TYPE.uri, csType, null);
                    dsvEntry.addCategory(MODEL.DIGEST.uri,
                                         dsv.getChecksum(),
                                         null);
                }

                // include size if it's non-zero
                if (dsv.DSSize != 0) {
                    dsvEntry.addCategory(MODEL.LENGTH.uri, Long
                            .toString(dsv.DSSize), null);
                }
                setContent(dsvEntry, obj, dsv, zout, encoding, transContext);

            }

            // The "main" entry for the Datastream with a link to the atom:id
            // of the most recent datastream version
            dsEntry.setId(PID.toURI(obj.getPid()) + "/" + latestCreated.DatastreamID);
            dsEntry.setTitle(latestCreated.DatastreamID);
            dsEntry.setUpdated(latestCreated.DSCreateDT);
            dsEntry
                    .addLink(PID.toURI(obj.getPid())
                                     + "/"
                                     + latestCreated.DatastreamID
                                     + "/"
                                     + DateUtility
                                             .convertDateToString(latestCreated.DSCreateDT),
                             Link.REL_ALTERNATE);
            dsEntry.addCategory(MODEL.STATE.uri, latestCreated.DSState, null);
            dsEntry.addCategory(MODEL.CONTROL_GROUP.uri,
                                latestCreated.DSControlGrp,
                                null);
            dsEntry.addCategory(MODEL.VERSIONABLE.uri, Boolean
                    .toString(latestCreated.DSVersionable), null);
        }
        addAuditDatastream(feed, obj, zout, encoding);
    }

    /**
     * AUDIT datastream is rebuilt from the latest in-memory audit trail which
     * is a separate array list in the DigitalObject class. Audit trail
     * datastream re-created from audit records. There is only ONE version of
     * the audit trail datastream
     *
     * @throws ObjectIntegrityException
     * @throws StreamIOException
     */
    private void addAuditDatastream(Feed feed, DigitalObject obj, ZipOutputStream zout, String encoding) throws ObjectIntegrityException, StreamIOException {
        if (obj.getAuditRecords().size() == 0) {
            return;
        }
        String dsId = PID.toURI(obj.getPid()) + "/AUDIT";
        String dsvId =
                dsId
                        + "/"
                        + DateUtility
                                .convertDateToString(obj.getCreateDate());

        Entry dsEntry = feed.addEntry();
        dsEntry.setId(dsId);
        dsEntry.setTitle("AUDIT");
        dsEntry.setUpdated(obj.getCreateDate()); // create date?

        dsEntry.addCategory(MODEL.STATE.uri, "A", null);
        dsEntry.addCategory(MODEL.CONTROL_GROUP.uri, "X", null);
        dsEntry.addCategory(MODEL.VERSIONABLE.uri, "false", null);

        dsEntry.addLink(dsvId, Link.REL_ALTERNATE);

        Entry dsvEntry = feed.addEntry();
        dsvEntry.setId(dsvId);
        dsvEntry.setTitle("AUDIT.0");
        dsvEntry.setUpdated(obj.getCreateDate());
        ThreadHelper.addInReplyTo(dsvEntry, PID.toURI(obj.getPid()) + "/AUDIT");
        dsvEntry.addCategory(MODEL.FORMAT_URI.uri, AUDIT1_0.uri, null);
        dsvEntry
                .addCategory(MODEL.LABEL.uri, "Audit Trail for this object", null);
        if (m_format.equals(ATOM_ZIP1_1)) {
            String name = "AUDIT.0.xml";
            try {
                zout.putNextEntry(new ZipEntry(name));
                ReadableCharArrayWriter buf =
                        new ReadableCharArrayWriter(512);
                PrintWriter pw  = new PrintWriter(buf);
                DOTranslationUtility.appendAuditTrail(obj, pw);
                pw.close();
                IOUtils.copy(buf.toReader(), zout, encoding);
                zout.closeEntry();
            } catch(IOException e) {
                throw new StreamIOException(e.getMessage(), e);
            }
            IRI iri = new IRI(name);
            dsvEntry.setSummary("AUDIT.0");
            dsvEntry.setContent(iri, "text/xml");
        } else {
            dsvEntry.setContent(DOTranslationUtility.getAuditTrail(obj),
                            "text/xml");
        }
    }

    private void setContent(Entry entry, DigitalObject obj, Datastream vds, ZipOutputStream zout, String encoding, int transContext)
            throws UnsupportedEncodingException, StreamIOException {
        if (vds.DSControlGrp.equalsIgnoreCase("X")) {
            setInlineXML(entry, obj,(DatastreamXMLMetadata) vds, zout, encoding, transContext);
        } else if (vds.DSControlGrp.equalsIgnoreCase("E")
                || vds.DSControlGrp.equalsIgnoreCase("R")) {
            setReferencedContent(entry, obj, vds, transContext);
        } else if (vds.DSControlGrp.equalsIgnoreCase("M")) {
            setManagedContent(entry, obj, vds, zout, encoding, transContext);
        }
    }

    private void setInlineXML(Entry entry, DigitalObject obj, DatastreamXMLMetadata ds,
            ZipOutputStream zout, String encoding, int transContext)
            throws UnsupportedEncodingException, StreamIOException {
        byte[] content;

        if (obj.hasContentModel(
                                  Models.SERVICE_DEPLOYMENT_3_0)
                && (ds.DatastreamID.equals("SERVICE-PROFILE") || ds.DatastreamID
                        .equals("WSDL"))) {
            content =
                    m_translator
                            .normalizeInlineXML(new String(ds.xmlContent,
                                                           encoding),
                                                transContext).getBytes(encoding);
        } else {
            content = ds.xmlContent;
        }

        if (m_format.equals(ATOM_ZIP1_1)) {
            String name = ds.DSVersionID + ".xml";
            try {
                zout.putNextEntry(new ZipEntry(name));
                InputStream is = new ByteArrayInputStream(content);
                IOUtils.copy(is, zout);
                zout.closeEntry();
                is.close();
            } catch(IOException e) {
                throw new StreamIOException(e.getMessage(), e);
            }
            IRI iri = new IRI(name);
            entry.setSummary(ds.DSVersionID);
            entry.setContent(iri, ds.DSMIME);
        } else {
            entry.setContent(new String(content, encoding), ds.DSMIME);
        }
    }

    private void setReferencedContent(Entry entry, DigitalObject obj, Datastream vds, int transContext)
            throws StreamIOException {
        entry.setSummary(vds.DSVersionID);
        String dsLocation =
                StreamUtility.enc(m_translator
                        .normalizeDSLocationURLs(obj.getPid(),
                                                 vds,
                                                 transContext).DSLocation);
        IRI iri = new IRI(dsLocation);
        entry.setContent(iri, vds.DSMIME);
    }

    private void setManagedContent(Entry entry, DigitalObject obj, Datastream vds,
            ZipOutputStream zout, String encoding, int transContext)
            throws StreamIOException {
        // If the ARCHIVE context is selected, inline & base64 encode the content,
        // unless the format is ZIP.
        if (transContext == DOTranslationUtility.SERIALIZE_EXPORT_ARCHIVE &&
                !m_format.equals(ATOM_ZIP1_1)) {
            String mimeType = vds.DSMIME;
            if (MimeTypeHelper.isText(mimeType)
                    || MimeTypeHelper.isXml(mimeType)) {
                try {
                    entry.setContent(IOUtils.toString(vds.getContentStream(),
                                                      encoding), mimeType);
                } catch (IOException e) {
                    throw new StreamIOException(e.getMessage(), e);
                }
            } else {
                entry.setContent(vds.getContentStream(), mimeType);
            }
        } else {
            String dsLocation;
            IRI iri;
            if (m_format.equals(ATOM_ZIP1_1)
                    && transContext != DOTranslationUtility.AS_IS) {
                dsLocation = vds.DSVersionID + "." + MimeTypeUtils.fileExtensionForMIMEType(vds.DSMIME);
                try {
                    zout.putNextEntry(new ZipEntry(dsLocation));
                    InputStream is = vds.getContentStream();
                    IOUtils.copy(is, zout);
                    is.close();
                    zout.closeEntry();
                } catch(IOException e) {
                    throw new StreamIOException(e.getMessage(), e);
                }
            } else {
                dsLocation =
                    StreamUtility.enc(m_translator
                            .normalizeDSLocationURLs(obj.getPid(),
                                                     vds,
                                                     transContext).DSLocation);

            }
            iri = new IRI(dsLocation);
            entry.setSummary(vds.DSVersionID);
            entry.setContent(iri, vds.DSMIME);
        }
    }
}
