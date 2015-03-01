/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.translation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.activation.MimeType;
import javax.xml.stream.XMLStreamException;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.thread.ThreadHelper;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Person;
import org.apache.abdera.util.MimeTypeHelper;
import org.apache.abdera.xpath.XPath;
import org.apache.commons.io.IOUtils;
import org.fcrepo.common.Constants;
import org.fcrepo.common.MalformedPIDException;
import org.fcrepo.common.PID;
import org.fcrepo.common.xml.format.XMLFormat;
import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.errors.ValidationException;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamManagedContent;
import org.fcrepo.server.storage.types.DatastreamReferencedContent;
import org.fcrepo.server.storage.types.DatastreamXMLMetadata;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.validation.ValidationUtility;
import org.fcrepo.utilities.DateUtility;
import org.fcrepo.utilities.FileUtils;
import org.fcrepo.utilities.NormalizedURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;





/**
 * Deserializer for Fedora Objects in Atom format.
 *
 * @author Edwin Shin
 * @since 3.0
 * @version $Id$
 */
public class AtomDODeserializer
        implements DODeserializer, Constants {

    public static final XMLFormat DEFAULT_FORMAT = ATOM1_1;

    private static final Logger logger =
            LoggerFactory.getLogger(AtomDODeserializer.class);

    /** The format this deserializer reads. */
    private final XMLFormat m_format;

    /** The translation utility is use */
    private DOTranslationUtility m_translator;

    private static final Abdera abdera = Abdera.getInstance();

    private XPath m_xpath = abdera.getXPath();

    public AtomDODeserializer() {
        this(DEFAULT_FORMAT);
    }

    public AtomDODeserializer(XMLFormat format) {
        this(format, null);
    }

    public AtomDODeserializer(XMLFormat format, DOTranslationUtility translator) {
        if (format.equals(ATOM1_1) || format.equals(ATOM_ZIP1_1)) {
            m_format = format;
        } else {
            throw new IllegalArgumentException("Not an Atom format: "
                    + format.uri);
        }
        m_translator = (translator == null) ? DOTranslationUtility.defaultInstance() : translator;
    }

    /**
     * {@inheritDoc}
     */
    public void deserialize(InputStream in,
                            DigitalObject obj,
                            String encoding,
                            int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedEncodingException {
        File tempDir = null;
        if (m_format.equals(ATOM_ZIP1_1)) {
            try {
                tempDir = FileUtils.createTempDir("atomzip", null);
                ZipInputStream zin = new ZipInputStream(new BufferedInputStream(in));
                ZipEntry entry;
                // reusable this byte buffer
                byte[] buf = new byte[4096];
                while ((entry = zin.getNextEntry()) != null) {
                    FileUtils.copy(zin, new FileOutputStream(new File(tempDir, entry.getName())), buf);
                }
                in = new FileInputStream(new File(tempDir, "atommanifest.xml"));
            } catch (FileNotFoundException e) {
                throw new StreamIOException(e.getMessage(), e);
            } catch (IOException e) {
                throw new StreamIOException(e.getMessage(), e);
            }
        }

        Document<Feed> feedDoc = abdera.getParser().parse(in);
        Feed feed = feedDoc.getRoot();

        addObjectProperties(feed, obj);
        addDatastreams(feed, obj, encoding, transContext, tempDir);

        m_translator.normalizeDatastreams(obj, transContext, encoding);
        FileUtils.delete(tempDir);
    }

    /**
     * {@inheritDoc}
     */
    public DODeserializer getInstance() {
        return this;
    }

    /**
     * Set the Fedora Object properties from the Feed metadata.
     *
     * @throws ObjectIntegrityException
     */
    private void addObjectProperties(Feed feed, DigitalObject obj)
            throws ObjectIntegrityException {
        PID pid;
        try {
            pid = new PID(feed.getId().toString());
        } catch (MalformedPIDException e) {
            throw new ObjectIntegrityException(e.getMessage(), e);
        }

        String label = feed.getTitle();
        String state =
                m_xpath.valueOf("/a:feed/a:category[@scheme='"
                        + MODEL.STATE.uri + "']/@term", feed);
        String createDate =
                m_xpath.valueOf("/a:feed/a:category[@scheme='"
                        + MODEL.CREATED_DATE.uri + "']/@term", feed);

        obj.setPid(pid.toString());

        try {
            obj.setState(DOTranslationUtility.readStateAttribute(state));
        } catch (ParseException e) {
            throw new ObjectIntegrityException("Could not read object state", e);
        }

        obj.setLabel(label);
        obj.setOwnerId(getOwnerId(feed));
        obj.setCreateDate(DateUtility.convertStringToDate(createDate));
        obj.setLastModDate(feed.getUpdated());

        setExtProps(obj, feed);
    }

    private void addDatastreams(Feed feed, DigitalObject obj,
            String encoding, int transContext, File tempDir)
            throws UnsupportedEncodingException, StreamIOException, ObjectIntegrityException {
        feed.sortEntries(new UpdatedIdComparator(true));
        List<Entry> entries = feed.getEntries();
        for (Entry entry : entries) {
            if (ThreadHelper.getInReplyTo(entry) != null) {
                addDatastreamVersion(feed, entry, obj, encoding, transContext, tempDir);
            }
        }
    }

    private void addDatastreamVersion(Feed feed, Entry entry, DigitalObject obj,
            String encoding, int transContext, File tempDir)
            throws UnsupportedEncodingException, StreamIOException,
            ObjectIntegrityException {
        IRI ref = ThreadHelper.getInReplyTo(entry).getRef();
        Entry parent = feed.getEntry(ref.toString());

        Datastream ds;
        String controlGroup = getDSControlGroup(obj, parent);
        if (controlGroup.equals("X")) {
            ds = addInlineDatastreamVersion(feed, entry, obj, encoding, tempDir);
        } else if (controlGroup.equals("M")) {
            ds = addManagedDatastreamVersion(obj, entry, feed, encoding, transContext, tempDir);
        } else {
            try {
                ds = addExternalReferencedDatastreamVersion(obj, entry, feed, transContext);
            } catch (IOException e) {
                throw new StreamIOException(e.getMessage(), e);
            }
        }
        obj.addDatastreamVersion(ds, true);
    }

    private Datastream addInlineDatastreamVersion(Feed feed, Entry entry, DigitalObject obj, String encoding, File tempDir)
            throws ObjectIntegrityException, StreamIOException {
        DatastreamXMLMetadata ds = new DatastreamXMLMetadata();
        setDSCommonProperties(ds, obj, entry, feed);
        String dsId = ds.DatastreamID;
        String dsvId = ds.DSVersionID;
        ds.DSLocation = obj.getPid() + "+" + dsId + "+" + dsvId;

        if (ds.DSVersionID.equals("AUDIT.0")) {
            addAuditDatastream(obj, entry, encoding, tempDir);
        } else {
            try {
                if (m_format.equals(ATOM_ZIP1_1)) {
                    File entryContent = getContentSrcAsFile(entry.getContentSrc(), tempDir);
                    ByteBuffer byteBuffer = ByteBuffer.allocate((int)entryContent.length());
                    FileUtils.copy(new FileInputStream(entryContent),
                            byteBuffer);
                    ds.xmlContent = byteBuffer.array();

                } else {
                    ds.xmlContent = entry.getContent().getBytes(encoding);
                }
            } catch (UnsupportedEncodingException e) {
                throw new StreamIOException(e.getMessage(), e);
            } catch (FileNotFoundException e) {
                throw new ObjectIntegrityException(e.getMessage(), e);
            } catch (IOException e) {
                throw new StreamIOException(e.getMessage(), e);
            }
        }

        if (ds.xmlContent != null) {
            ds.DSSize = ds.xmlContent.length;
        }

        MimeType mimeType = entry.getContentMimeType();
        if (mimeType == null) {
            ds.DSMIME = "text/xml";
        } else {
            ds.DSMIME = mimeType.toString();
        }
        return ds;
    }

    private Datastream addExternalReferencedDatastreamVersion(DigitalObject obj, Entry entry,
            Feed feed, int transContext)
            throws ObjectIntegrityException, IOException {
        Datastream ds = new DatastreamReferencedContent();
        setDSCommonProperties(ds, obj, entry, feed);
        ds.DSLocation = entry.getContentSrc().toString();
        // Normalize the dsLocation for the deserialization context
        ds.DSLocation =
                (m_translator.normalizeDSLocationURLs(obj.getPid(),
                                                              ds,
                                                              transContext)).DSLocation;
        ds.DSLocationType = Datastream.DS_LOCATION_TYPE_URL;
        ds.DSMIME = entry.getContentMimeType().toString();

        return ds;
    }

    private Datastream addManagedDatastreamVersion(DigitalObject obj, Entry entry, Feed feed,
            String encoding, int transContext, File tempDir)
            throws StreamIOException, ObjectIntegrityException {
        Datastream ds = new DatastreamManagedContent();
        setDSCommonProperties(ds, obj, entry, feed);
        ds.DSLocationType = Datastream.DS_LOCATION_TYPE_INTERNAL;

        ds.DSMIME = getDSMimeType(entry);

        // Managed Content can take any of the following forms:
        // 1) inline text (plaintext, html, xml)
        // 2) inline Base64
        // 3) referenced content
        IRI contentLocation = entry.getContentSrc();
        if (contentLocation != null) {
            // URL FORMAT VALIDATION for dsLocation:
            // For Managed Content the URL is only checked when we are parsing a
            // a NEW ingest file because the URL is replaced with an internal identifier
            // once the repository has sucked in the content for storage.

        	// AtomZIP files can have a simple filename (nb, not file: )for the content location, so don't validate that
            if (obj.isNew() && !m_format.equals(ATOM_ZIP1_1)) {
                ValidationUtility
                        .validateURL(contentLocation.toString(),ds.DSControlGrp);
            }

            if (m_format.equals(ATOM_ZIP1_1)) {
                if (!contentLocation.isAbsolute() && !contentLocation.isPathAbsolute()) {
                    try {
                        File f = getContentSrcAsFile(contentLocation, tempDir);
                        contentLocation = new IRI(DatastreamManagedContent.TEMP_SCHEME +
                                              f.getAbsolutePath());
                    } catch (IOException e) {
                        throw new StreamIOException(e.getMessage(), e);
                    }
                }
            }

            ds.DSLocation = contentLocation.toString();
            ds.DSLocation =
                    (m_translator.normalizeDSLocationURLs(obj
                            .getPid(), ds, transContext)).DSLocation;
            return ds;
        }

        try {
            File temp = File.createTempFile("binary-datastream", null);
            OutputStream out = new FileOutputStream(temp);
            if (MimeTypeHelper.isText(ds.DSMIME)
                    || MimeTypeHelper.isXml(ds.DSMIME)) {
                IOUtils.copy(new StringReader(entry.getContent()),
                             out,
                             encoding);
            } else {
                IOUtils.copy(entry.getContentStream(), out);
            }
            ds.DSLocation = DatastreamManagedContent.TEMP_SCHEME + temp.getAbsolutePath();
        } catch (IOException e) {
            throw new StreamIOException(e.getMessage(), e);
        }

        return ds;
    }

    private void addAuditDatastream(DigitalObject obj, Entry entry, String encoding, File tempDir)
            throws ObjectIntegrityException, StreamIOException {
        try {
            Reader auditTrail;
            if (m_format.equals(ATOM_ZIP1_1)) {
                File f = getContentSrcAsFile(entry.getContentSrc(), tempDir);
                auditTrail = new InputStreamReader(new FileInputStream(f), encoding);
            } else {
                auditTrail = new StringReader(entry.getContent());
            }
            obj.getAuditRecords().addAll(DOTranslationUtility
                    .getAuditRecords(auditTrail));
            auditTrail.close();
        } catch (XMLStreamException e) {
            throw new ObjectIntegrityException(e.getMessage(), e);
        } catch (IOException e) {
            throw new StreamIOException(e.getMessage(), e);
        }
    }

    private String getOwnerId(Feed feed) {
        Person owner = feed.getAuthor();
        if (owner == null) {
            return "";
        } else {
            return owner.getName();
        }
    }

    /**
     * Parses the id to determine a datastreamId.
     *
     * @param id
     * @return
     */
    private String getDatastreamId(DigitalObject obj, Entry entry) {
        String entryId = entry.getId().toString();
        // matches info:fedora/pid/dsid/timestamp
        Pattern pattern =
                Pattern.compile("^" + Constants.FEDORA.uri + ".+?/([^/]+)/?.*");
        Matcher matcher = pattern.matcher(entryId);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return obj.newDatastreamID();
        }
    }

    private String getDatastreamVersionId(DigitalObject obj, Entry entry) {
        String dsId = getDatastreamId(obj, entry);
        String dsvId = entry.getTitle();
        // e.g. Match DS1.0 but not DS1
        if (dsvId.matches("^" + dsId + ".*\\.[\\w]")) {
            return dsvId;
        } else {
            if (!obj.datastreams(dsId).iterator().hasNext()) {
                return dsId + ".0";
            } else {
                return obj.newDatastreamID(dsId);
            }
        }
    }

    private String getDSControlGroup(DigitalObject obj, Entry entry)
            throws ObjectIntegrityException {
        List<Category> controlGroups =
                entry.getCategories(MODEL.CONTROL_GROUP.uri);

        // Try to infer the control group if not provided
        if (controlGroups.isEmpty() || controlGroups.size() > 1) {
            if (entry.getContentType() != null) {
                if (entry.getContentType().equals(Content.Type.XML)) {
                    return "X";
                } else {
                    // only XML can be inline
                    return "M";
                }
            }

            if (entry.getContentSrc() != null) {
                return "M";
            }
            // TODO other cases
            // link alts, link enclosures

            else {
                throw new ObjectIntegrityException("No control group provided by "
                        + obj.getPid());
            }
        } else {
            return controlGroups.get(0).getTerm();
        }
    }

    private String getDSState(Entry entry) {
        List<Category> state = entry.getCategories(MODEL.STATE.uri);
        if (state.isEmpty() || state.size() > 1) {
            return "A";
        } else {
            return state.get(0).getTerm();
        }
    }

    /**
     * Note: AUDIT datastreams always return false, otherwise defaults to true.
     *
     * @param entry
     * @return
     */
    private boolean getDSVersionable(DigitalObject obj, Entry entry) {
        if (getDatastreamId(obj, entry).equals("AUDIT")) {
            return false;
        }
        List<Category> versionable = entry.getCategories(MODEL.VERSIONABLE.uri);
        if (versionable.isEmpty() || versionable.size() > 1) {
            return true;
        } else {
            return Boolean.valueOf(versionable.get(0).getTerm());
        }
    }

    private String[] getDSAltIds(Entry entry) {
        List<Category> altIds = entry.getCategories(MODEL.ALT_IDS.uri);
        if (altIds.isEmpty()) {
            return EMPTY_STRING_ARRAY;
        } else {
            return altIds.get(0).getTerm().split(" ");
            // TODO we could handle size > 1
        }
    }

    private String getDSFormatURI(Entry entry) {
        List<Category> formatURI = entry.getCategories(MODEL.FORMAT_URI.uri);
        if (formatURI.isEmpty() || formatURI.size() > 1) {
            return null;
        } else {
            return formatURI.get(0).getTerm();
        }
    }

    private String getDSLabel(Entry entry) {
        List<Category> label = entry.getCategories(MODEL.LABEL.uri);
        if (label.isEmpty()) {
            return "";
        }
        return label.get(0).getTerm();
    }

    private String getDSMimeType(Entry entry) {
        String dsMimeType = "application/unknown";
        MimeType mimeType = entry.getContentMimeType();
        if (mimeType == null) {
            Content.Type type = entry.getContentType();
            if (type != null) {
                if (type == Content.Type.HTML) {
                    dsMimeType = "text/html";
                } else if (type == Content.Type.TEXT) {
                    dsMimeType = "text/plain";
                } else if (type == Content.Type.XHTML) {
                    dsMimeType = "application/xhtml+xml";
                } else if (type == Content.Type.XML) {
                    dsMimeType = "text/xml";
                }
            }
        } else {
            dsMimeType = mimeType.toString();
        }
        return dsMimeType;
    }

    private String getDSChecksumType(Entry entry) {
        List<Category> digestType = entry.getCategories(MODEL.DIGEST_TYPE.uri);
        if (digestType.isEmpty()) {
            String result = (Datastream.autoChecksum)
                ? Datastream.getDefaultChecksumType()
                : Datastream.CHECKSUMTYPE_DISABLED;
                return result;
        } else {
            return digestType.get(0).getTerm();
        }
    }

    private String getDSChecksum(Entry entry) {
        List<Category> digest = entry.getCategories(MODEL.DIGEST.uri);
        if (digest.isEmpty()) {
            return Datastream.CHECKSUM_NONE;
        } else {
            return digest.get(0).getTerm();
        }
    }

    private void setDSCommonProperties(Datastream dsVersion, DigitalObject obj, Entry entry, Feed feed)
            throws ObjectIntegrityException {
        IRI ref = ThreadHelper.getInReplyTo(entry).getRef();
        Entry parent = feed.getEntry(ref.toString());
        dsVersion.DatastreamID = getDatastreamId(obj, parent);
        dsVersion.DSControlGrp = getDSControlGroup(obj, parent);
        dsVersion.DSState = getDSState(parent);
        dsVersion.DSVersionable = getDSVersionable(obj, parent);
        setDatastreamVersionProperties(dsVersion, obj, entry);
    }

    private void setDatastreamVersionProperties(Datastream ds, DigitalObject obj, Entry entry)
            throws ValidationException {
        ds.DatastreamAltIDs = getDSAltIds(entry);
        ds.DSCreateDT = entry.getUpdated();
        ds.DSFormatURI = getDSFormatURI(entry);
        ds.DSLabel = getDSLabel(entry);
        ds.DSVersionID = getDatastreamVersionId(obj, entry);
        ds.DSChecksumType = getDSChecksumType(entry);
        String checksum = getDSChecksum(entry);
        if (obj.isNew()) {
            if (logger.isDebugEnabled()) {
                logger.debug("New Object: checking supplied checksum");
            }
            if (checksum != null && !checksum.isEmpty()
                    && !checksum.equals(Datastream.CHECKSUM_NONE)) {
                String tmpChecksum = ds.getChecksum();
                if (logger.isDebugEnabled()) {
                    logger.debug("checksum = {}", tmpChecksum);
                }
                if (!checksum.equals(tmpChecksum)) {
                    throw new ValidationException("Checksum Mismatch: "
                            + tmpChecksum);
                }
            }
            ds.DSChecksumType = ds.getChecksumType();
        } else {
            ds.DSChecksum = checksum;
        }
    }

    private void setExtProps(DigitalObject obj, Feed feed) {
        List<Category> epCategories =
                feed.getCategories(MODEL.EXT_PROPERTY.uri);
        for (Category epCategory : epCategories) {
            obj.setExtProperty(epCategory.getTerm(), epCategory.getLabel());
        }
    }

    /**
     * Returns the an Entry's contentSrc as a File relative to {@link #m_tempDir}.
     *
     * @param contentSrc
     * @return the contentSrc as a File relative to m_tempDir.
     * @throws ObjectIntegrityException
     */
    protected File getContentSrcAsFile(IRI contentSrc, File tempDir) throws ObjectIntegrityException, IOException {
        if (contentSrc.isAbsolute() || contentSrc.isPathAbsolute()) {
            throw new ObjectIntegrityException("contentSrc must not be absolute");
        }
        try {
            // Normalize the IRI to resolve percent-encoding and
            // backtracking (e.g. "../")
            NormalizedURI nUri = new NormalizedURI(tempDir.toURI().toString() + contentSrc.toString());
            nUri.normalize();

            File f = new File(nUri.toURI());
            if (f.getParentFile().equals(tempDir)) {
                File temp = File.createTempFile("binary-datastream", null);
                FileUtils.move(f, temp);
                temp.deleteOnExit();
                return temp;
                //return f;
            } else {
                throw new ObjectIntegrityException(contentSrc.toString()
                                                   + " is not a valid path.");
            }
        } catch (URISyntaxException e) {
            throw new ObjectIntegrityException(e.getMessage(), e);
        }
    }

    private static class UpdatedIdComparator
            implements Comparator<Entry> {

        private boolean ascending = true;

        UpdatedIdComparator(boolean ascending) {
            this.ascending = ascending;
        }

        public int compare(Entry o1, Entry o2) {
            Date d1 = o1.getUpdated();
            Date d2 = o2.getUpdated();
            String id1 = o1.getId().toString();
            String id2 = o2.getId().toString();

            int r = d1.compareTo(d2);
            if (d1.equals(d2)) {
                r = id1.compareTo(id2);
            }
            return (ascending) ? r : -r;
        }
    }
}
