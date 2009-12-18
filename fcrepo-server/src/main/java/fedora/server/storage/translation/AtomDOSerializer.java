/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.translation;

import fedora.common.Constants;
import fedora.common.Models;
import fedora.common.PID;
import fedora.common.xml.format.XMLFormat;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.StreamUtility;
import fedora.utilities.MimeTypeUtils;
import org.apache.abdera.Abdera;
import org.apache.abdera.ext.thread.ThreadHelper;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.util.MimeTypeHelper;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    private DigitalObject m_obj;

    private String m_encoding;

    /** The current translation context. */
    private int m_transContext;

    /** The format this serializer writes. */
    private final XMLFormat m_format;

    private PID m_pid;

    protected Feed m_feed;
    
    private ZipOutputStream m_zout;
    
    public AtomDOSerializer() {
        this(DEFAULT_FORMAT);
    }

    public AtomDOSerializer(XMLFormat format) {
        if (format.equals(ATOM1_1) || format.equals(ATOM_ZIP1_1)) {
            m_format = format;
        } else {
            throw new IllegalArgumentException("Not an ATOM format: "
                    + format.uri);
        }
    }

    /**
     * {@inheritDoc}
     */
    public DOSerializer getInstance() {
        return new AtomDOSerializer(m_format);
    }

    /**
     * {@inheritDoc}
     */
    public void serialize(DigitalObject obj,
                          OutputStream out,
                          String encoding,
                          int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedEncodingException {
        m_obj = obj;
        m_encoding = (encoding == null || encoding == "") ? "UTF-8" : encoding;
        m_transContext = transContext;
        m_pid = PID.getInstance(m_obj.getPid());
        m_feed = abdera.newFeed();
        
        if (m_format.equals(ATOM_ZIP1_1)) {
            m_zout = new ZipOutputStream(out);
        }

        addObjectProperties();
        m_feed
                .setIcon("http://www.fedora-commons.org/images/logo_vertical_transparent_200_251.png");
        addDatastreams();

        if (m_format.equals(ATOM_ZIP1_1)) {
            try {
                m_zout.putNextEntry(new ZipEntry("atommanifest.xml"));
                m_feed.writeTo("prettyxml", m_zout);
                m_zout.closeEntry();
                m_zout.close();
            } catch (IOException e) {
                throw new StreamIOException(e.getMessage(), e);
            }
        } else {
            try {
                m_feed.writeTo("prettyxml", out);
            } catch (IOException e) {
                throw new StreamIOException(e.getMessage(), e);
            }
        }
    }

    private void addObjectProperties() throws ObjectIntegrityException {
        String state = DOTranslationUtility.getStateAttribute(m_obj);
        String ownerId = m_obj.getOwnerId();
        String label = m_obj.getLabel();
        Date cdate = m_obj.getCreateDate();
        Date mdate = m_obj.getLastModDate();

        m_feed.setId(m_pid.toURI());
        m_feed.setTitle(label == null ? "" : label);
        m_feed.setUpdated(mdate);
        m_feed.addAuthor(ownerId == null ? "" : StreamUtility.enc(ownerId));

        m_feed.addCategory(MODEL.STATE.uri, state, null);

        if (cdate != null) {
            m_feed.addCategory(MODEL.CREATED_DATE.uri, DateUtility
                    .convertDateToString(cdate), null);
        }

        // TODO not sure I'm satisfied with this representation of extProperties
        for (String extProp : m_obj.getExtProperties().keySet()) {
            m_feed.addCategory(MODEL.EXT_PROPERTY.uri, extProp, m_obj
                    .getExtProperty(extProp));
        }
    }

    private void addDatastreams() throws ObjectIntegrityException,
            UnsupportedEncodingException, StreamIOException {
        Iterator<String> iter = m_obj.datastreamIdIterator();
        String dsid;
        while (iter.hasNext()) {
            dsid = iter.next();
            // AUDIT datastream is rebuilt from the latest in-memory audit trail
            // which is a separate array list in the DigitalObject class.
            // So, ignore it here.
            if (dsid.equals("AUDIT") || dsid.equals("FEDORA-AUDITTRAIL")) {
                continue;
            }

            Entry dsEntry = m_feed.addEntry();

            Datastream latestCreated = null;
            long latestCreateTime = -1;

            for (Datastream v : m_obj.datastreams(dsid)) {
                Datastream dsv = DOTranslationUtility.setDatastreamDefaults(v);

                // Keep track of the most recent datastream version
                if (dsv.DSCreateDT.getTime() > latestCreateTime) {
                    latestCreateTime = dsv.DSCreateDT.getTime();
                    latestCreated = dsv;
                }

                Entry dsvEntry = m_feed.addEntry();
                dsvEntry.setId(m_pid.toURI() + "/" + dsv.DatastreamID + "/"
                        + DateUtility.convertDateToString(dsv.DSCreateDT));
                dsvEntry.setTitle(dsv.DSVersionID);
                dsvEntry.setUpdated(dsv.DSCreateDT);

                ThreadHelper.addInReplyTo(dsvEntry, m_pid.toURI() + "/"
                        + dsv.DatastreamID);

                String altIds =
                        DOTranslationUtility.oneString(dsv.DatastreamAltIDs);
                if (altIds != null && !altIds.equals("")) {
                    dsvEntry.addCategory(MODEL.ALT_IDS.uri, altIds, null);
                }
                if (dsv.DSFormatURI != null && !dsv.DSFormatURI.equals("")) {
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
                setContent(dsvEntry, dsv);

            }

            // The "main" entry for the Datastream with a link to the atom:id
            // of the most recent datastream version
            dsEntry.setId(m_pid.toURI() + "/" + latestCreated.DatastreamID);
            dsEntry.setTitle(latestCreated.DatastreamID);
            dsEntry.setUpdated(latestCreated.DSCreateDT);
            dsEntry
                    .addLink(m_pid.toURI()
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
        addAuditDatastream();
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
    private void addAuditDatastream() throws ObjectIntegrityException, StreamIOException {
        if (m_obj.getAuditRecords().size() == 0) {
            return;
        }
        String dsId = m_pid.toURI() + "/AUDIT";
        String dsvId =
                dsId
                        + "/"
                        + DateUtility
                                .convertDateToString(m_obj.getCreateDate());

        Entry dsEntry = m_feed.addEntry();
        dsEntry.setId(dsId);
        dsEntry.setTitle("AUDIT");
        dsEntry.setUpdated(m_obj.getCreateDate()); // create date?

        dsEntry.addCategory(MODEL.STATE.uri, "A", null);
        dsEntry.addCategory(MODEL.CONTROL_GROUP.uri, "X", null);
        dsEntry.addCategory(MODEL.VERSIONABLE.uri, "false", null);

        dsEntry.addLink(dsvId, Link.REL_ALTERNATE);

        Entry dsvEntry = m_feed.addEntry();
        dsvEntry.setId(dsvId);
        dsvEntry.setTitle("AUDIT.0");
        dsvEntry.setUpdated(m_obj.getCreateDate());
        ThreadHelper.addInReplyTo(dsvEntry, m_pid.toURI() + "/AUDIT");
        dsvEntry.addCategory(MODEL.FORMAT_URI.uri, AUDIT1_0.uri, null);
        dsvEntry
                .addCategory(MODEL.LABEL.uri, "Audit Trail for this object", null);
        if (m_format.equals(ATOM_ZIP1_1)) {
            String name = "AUDIT.0.xml";
            try {
                m_zout.putNextEntry(new ZipEntry(name));
                Reader r = new StringReader(DOTranslationUtility.getAuditTrail(m_obj));
                IOUtils.copy(r, m_zout, m_encoding);
                m_zout.closeEntry();
                r.close();
            } catch(IOException e) {
                throw new StreamIOException(e.getMessage(), e);
            }
            IRI iri = new IRI(name);
            dsvEntry.setSummary("AUDIT.0");
            dsvEntry.setContent(iri, "text/xml");
        } else {
            dsvEntry.setContent(DOTranslationUtility.getAuditTrail(m_obj),
                            "text/xml");
        }
    }

    private void setContent(Entry entry, Datastream vds)
            throws UnsupportedEncodingException, StreamIOException {
        if (vds.DSControlGrp.equalsIgnoreCase("X")) {
            setInlineXML(entry, (DatastreamXMLMetadata) vds);
        } else if (vds.DSControlGrp.equalsIgnoreCase("E")
                || vds.DSControlGrp.equalsIgnoreCase("R")) {
            setReferencedContent(entry, vds);
        } else if (vds.DSControlGrp.equalsIgnoreCase("M")) {
            setManagedContent(entry, vds);
        }
    }

    private void setInlineXML(Entry entry, DatastreamXMLMetadata ds)
            throws UnsupportedEncodingException, StreamIOException {
        String content;

        if (m_obj.hasContentModel(
                                  Models.SERVICE_DEPLOYMENT_3_0)
                && (ds.DatastreamID.equals("SERVICE-PROFILE") || ds.DatastreamID
                        .equals("WSDL"))) {
            content =
                    DOTranslationUtility
                            .normalizeInlineXML(new String(ds.xmlContent,
                                                           m_encoding),
                                                m_transContext);
        } else {
            content = new String(ds.xmlContent, m_encoding);
        }
        
        if (m_format.equals(ATOM_ZIP1_1)) {
            String name = ds.DSVersionID + ".xml";
            try {
                m_zout.putNextEntry(new ZipEntry(name));
                InputStream is = new ByteArrayInputStream(content.getBytes(m_encoding));
                IOUtils.copy(is, m_zout);
                m_zout.closeEntry();
                is.close();
            } catch(IOException e) {
                throw new StreamIOException(e.getMessage(), e);
            }
            IRI iri = new IRI(name);
            entry.setSummary(ds.DSVersionID);
            entry.setContent(iri, ds.DSMIME);
        } else {
            entry.setContent(content, ds.DSMIME);
        }
    }

    private void setReferencedContent(Entry entry, Datastream vds)
            throws StreamIOException {
        entry.setSummary(vds.DSVersionID);
        String dsLocation =
                StreamUtility.enc(DOTranslationUtility
                        .normalizeDSLocationURLs(m_obj.getPid(),
                                                 vds,
                                                 m_transContext).DSLocation);
        IRI iri = new IRI(dsLocation);
        entry.setContent(iri, vds.DSMIME);
    }

    private void setManagedContent(Entry entry, Datastream vds)
            throws StreamIOException {
        // If the ARCHIVE context is selected, inline & base64 encode the content,
        // unless the format is ZIP.
        if (m_transContext == DOTranslationUtility.SERIALIZE_EXPORT_ARCHIVE &&
                !m_format.equals(ATOM_ZIP1_1)) {
            String mimeType = vds.DSMIME;
            if (MimeTypeHelper.isText(mimeType)
                    || MimeTypeHelper.isXml(mimeType)) {
                try {
                    entry.setContent(IOUtils.toString(vds.getContentStream(),
                                                      m_encoding), mimeType);
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
                    && m_transContext != DOTranslationUtility.AS_IS) {
                dsLocation = vds.DSVersionID + "." + MimeTypeUtils.fileExtensionForMIMEType(vds.DSMIME);
                try {
                    m_zout.putNextEntry(new ZipEntry(dsLocation));
                    IOUtils.copy(vds.getContentStream(), m_zout);
                    m_zout.closeEntry();
                } catch(IOException e) {
                    throw new StreamIOException(e.getMessage(), e);
                }
            } else {
                dsLocation =
                    StreamUtility.enc(DOTranslationUtility
                            .normalizeDSLocationURLs(m_obj.getPid(),
                                                     vds,
                                                     m_transContext).DSLocation);
                
            }
            iri = new IRI(dsLocation);
            entry.setSummary(vds.DSVersionID);
            entry.setContent(iri, vds.DSMIME);
        }
    }
}
