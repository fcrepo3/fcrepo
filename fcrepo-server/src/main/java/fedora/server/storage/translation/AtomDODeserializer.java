/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.translation;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
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

import java.text.ParseException;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLStreamException;

import javax.activation.MimeType;

import org.apache.commons.io.IOUtils;

import org.apache.log4j.Logger;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.thread.ThreadHelper;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Person;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.util.MimeTypeHelper;
import org.apache.abdera.xpath.XPath;

import fedora.common.Constants;
import fedora.common.MalformedPIDException;
import fedora.common.PID;
import fedora.common.xml.format.XMLFormat;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.ValidationException;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamManagedContent;
import fedora.server.storage.types.DatastreamReferencedContent;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;
import fedora.server.utilities.DateUtility;
import fedora.server.validation.ValidationUtility;

import fedora.utilities.FileUtils;
import fedora.utilities.NormalizedURI;


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

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(AtomDODeserializer.class);

    /** The object to deserialize to. */
    private DigitalObject m_obj;

    private String m_encoding;

    /** The current translation context. */
    private int m_transContext;

    /** The format this deserializer reads. */
    private final XMLFormat m_format;

    private Abdera abdera = Abdera.getInstance();

    private Feed m_feed;

    private XPath m_xpath;
    
    private ZipInputStream m_zin;
    
    /**
     * Temporary directory for the unpacked contents of an Atom Zip archive.
     */
    private File m_tempDir;

    public AtomDODeserializer() {
        this(DEFAULT_FORMAT);
    }

    public AtomDODeserializer(XMLFormat format) {
        if (format.equals(ATOM1_1) || format.equals(ATOM_ZIP1_1)) {
            m_format = format;
        } else {
            throw new IllegalArgumentException("Not an Atom format: "
                    + format.uri);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deserialize(InputStream in,
                            DigitalObject obj,
                            String encoding,
                            int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedEncodingException {
        if (m_format.equals(ATOM_ZIP1_1)) {
            try {
                m_tempDir = FileUtils.createTempDir("atomzip", null);
                m_zin = new ZipInputStream(new BufferedInputStream(in));
                ZipEntry entry;
                while ((entry = m_zin.getNextEntry()) != null) {
                    FileUtils.copy(m_zin, new FileOutputStream(new File(m_tempDir, entry.getName())));
                }
                in = new FileInputStream(new File(m_tempDir, "atommanifest.xml"));
            } catch (FileNotFoundException e) {
                throw new StreamIOException(e.getMessage(), e);
            } catch (IOException e) {
                throw new StreamIOException(e.getMessage(), e);
            }
        }
        
        Parser parser = abdera.getParser();
        Document<Feed> feedDoc = parser.parse(in);
        m_feed = feedDoc.getRoot();
        m_xpath = abdera.getXPath();

        m_obj = obj;
        m_encoding = encoding;
        m_transContext = transContext;
        addObjectProperties();
        addDatastreams();

        DOTranslationUtility.normalizeDatastreams(m_obj,
                                                  m_transContext,
                                                  m_encoding);
        FileUtils.delete(m_tempDir);
    }

    /**
     * {@inheritDoc}
     */
    public DODeserializer getInstance() {
        return new AtomDODeserializer(m_format);
    }

    /**
     * Set the Fedora Object properties from the Feed metadata.
     * 
     * @throws ObjectIntegrityException
     */
    private void addObjectProperties() throws ObjectIntegrityException {
        PID pid;
        try {
            pid = new PID(m_feed.getId().toString());
        } catch (MalformedPIDException e) {
            throw new ObjectIntegrityException(e.getMessage(), e);
        }

        String label = m_feed.getTitle();
        String state =
                m_xpath.valueOf("/a:feed/a:category[@scheme='"
                        + MODEL.STATE.uri + "']/@term", m_feed);
        String createDate =
                m_xpath.valueOf("/a:feed/a:category[@scheme='"
                        + MODEL.CREATED_DATE.uri + "']/@term", m_feed);

        m_obj.setPid(pid.toString());

        try {
            m_obj.setState(DOTranslationUtility.readStateAttribute(state));
        } catch (ParseException e) {
            throw new ObjectIntegrityException("Could not read object state", e);
        }

        m_obj.setLabel(label);
        m_obj.setOwnerId(getOwnerId());
        m_obj.setCreateDate(DateUtility.convertStringToDate(createDate));
        m_obj.setLastModDate(m_feed.getUpdated());

        setExtProps();
    }

    private void addDatastreams() throws UnsupportedEncodingException,
            StreamIOException, ObjectIntegrityException {
        m_feed.sortEntries(new UpdatedIdComparator(true));
        List<Entry> entries = m_feed.getEntries();
        for (Entry entry : entries) {
            if (ThreadHelper.getInReplyTo(entry) != null) {
                addDatastreamVersion(entry);
            }
        }
    }

    private void addDatastreamVersion(Entry entry)
            throws UnsupportedEncodingException, StreamIOException,
            ObjectIntegrityException {
        IRI ref = ThreadHelper.getInReplyTo(entry).getRef();
        Entry parent = m_feed.getEntry(ref.toString());

        Datastream ds;
        String controlGroup = getDSControlGroup(parent);
        if (controlGroup.equals("X")) {
            ds = addInlineDatastreamVersion(entry);
        } else if (controlGroup.equals("M")) {
            ds = addManagedDatastreamVersion(entry);
        } else {
            ds = addExternalReferencedDatastreamVersion(entry);
        }
        m_obj.addDatastreamVersion(ds, true);
    }

    private Datastream addInlineDatastreamVersion(Entry entry)
            throws ObjectIntegrityException, StreamIOException {
        DatastreamXMLMetadata ds = new DatastreamXMLMetadata();
        setDSCommonProperties(ds, entry);
        String dsId = ds.DatastreamID;
        String dsvId = ds.DSVersionID;
        ds.DSLocation = m_obj.getPid() + "+" + dsId + "+" + dsvId;

        if (ds.DSVersionID.equals("AUDIT.0")) {
            addAuditDatastream(entry);
        } else {
            try {
                if (m_format.equals(ATOM_ZIP1_1)) {
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    FileUtils.copy(new FileInputStream(getContentSrcAsFile(entry.getContentSrc())), 
                            bout);
                    ds.xmlContent = bout.toByteArray();
                    
                } else {
                    ds.xmlContent = entry.getContent().getBytes(m_encoding); //IOUtils.toByteArray(entry.getContentStream());
                }
            } catch (UnsupportedEncodingException e) {
                throw new StreamIOException(e.getMessage(), e);
            } catch (FileNotFoundException e) {
                throw new ObjectIntegrityException(e.getMessage(), e);
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

    private Datastream addExternalReferencedDatastreamVersion(Entry entry)
            throws ObjectIntegrityException {
        Datastream ds = new DatastreamReferencedContent();
        setDSCommonProperties(ds, entry);
        ds.DSLocation = entry.getContentSrc().toString();
        // Normalize the dsLocation for the deserialization context
        ds.DSLocation =
                (DOTranslationUtility.normalizeDSLocationURLs(m_obj.getPid(),
                                                              ds,
                                                              m_transContext)).DSLocation;
        ds.DSLocationType = "URL";
        ds.DSMIME = entry.getContentMimeType().toString();

        return ds;
    }

    private Datastream addManagedDatastreamVersion(Entry entry)
            throws StreamIOException, ObjectIntegrityException {
        Datastream ds = new DatastreamManagedContent();
        setDSCommonProperties(ds, entry);
        ds.DSLocationType = "INTERNAL_ID";

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
            
            if (m_obj.isNew()) {
                ValidationUtility
                        .validateURL(contentLocation.toString(),ds.DSControlGrp);
            }
            
            if (m_format.equals(ATOM_ZIP1_1)) {
                if (!contentLocation.isAbsolute() && !contentLocation.isPathAbsolute()) {
                    File f = getContentSrcAsFile(contentLocation);
                    contentLocation = new IRI(DatastreamManagedContent.TEMP_SCHEME + 
                                              f.getAbsolutePath());
                }
            }
            
            ds.DSLocation = contentLocation.toString();
            ds.DSLocation =
                    (DOTranslationUtility.normalizeDSLocationURLs(m_obj
                            .getPid(), ds, m_transContext)).DSLocation;
            return ds;
        }

        try {
            File temp = File.createTempFile("binary-datastream", null);
            OutputStream out = new FileOutputStream(temp);
            if (MimeTypeHelper.isText(ds.DSMIME)
                    || MimeTypeHelper.isXml(ds.DSMIME)) {
                IOUtils.copy(new StringReader(entry.getContent()),
                             out,
                             m_encoding);
            } else {
                IOUtils.copy(entry.getContentStream(), out);
            }
            ds.DSLocation = DatastreamManagedContent.TEMP_SCHEME + temp.getAbsolutePath();
        } catch (IOException e) {
            throw new StreamIOException(e.getMessage(), e);
        }

        return ds;
    }

    private void addAuditDatastream(Entry entry)
            throws ObjectIntegrityException, StreamIOException {
        try {
            Reader auditTrail;
            if (m_format.equals(ATOM_ZIP1_1)) {
                File f = getContentSrcAsFile(entry.getContentSrc());
                auditTrail = new InputStreamReader(new FileInputStream(f), m_encoding);
            } else {
                auditTrail = new StringReader(entry.getContent());
            }
            m_obj.getAuditRecords().addAll(DOTranslationUtility
                    .getAuditRecords(auditTrail));
            auditTrail.close();
        } catch (XMLStreamException e) {
            throw new ObjectIntegrityException(e.getMessage(), e);
        } catch (IOException e) {
            throw new StreamIOException(e.getMessage(), e);
        }
    }

    private String getOwnerId() {
        Person owner = m_feed.getAuthor();
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
    private String getDatastreamId(Entry entry) {
        String entryId = entry.getId().toString();
        // matches info:fedora/pid/dsid/timestamp
        Pattern pattern =
                Pattern.compile("^" + Constants.FEDORA.uri + ".+?/([^/]+)/?.*");
        Matcher matcher = pattern.matcher(entryId);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return m_obj.newDatastreamID();
        }
    }

    private String getDatastreamVersionId(Entry entry) {
        String dsId = getDatastreamId(entry);
        String dsvId = entry.getTitle();
        // e.g. Match DS1.0 but not DS1
        if (dsvId.matches("^" + dsId + ".*\\.[\\w]")) {
            return dsvId;
        } else {
            if (!m_obj.datastreams(dsId).iterator().hasNext()) {
                return dsId + ".0";
            } else {
                return m_obj.newDatastreamID(dsId);
            }
        }
    }

    private String getDSControlGroup(Entry entry)
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
                        + m_obj.getPid());
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
    private boolean getDSVersionable(Entry entry) {
        if (getDatastreamId(entry).equals("AUDIT")) {
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
            return new String[0];
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
            return Datastream.CHECKSUMTYPE_DISABLED;
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

    private void setDSCommonProperties(Datastream dsVersion, Entry entry)
            throws ObjectIntegrityException {
        IRI ref = ThreadHelper.getInReplyTo(entry).getRef();
        Entry parent = m_feed.getEntry(ref.toString());
        dsVersion.DatastreamID = getDatastreamId(parent);
        dsVersion.DSControlGrp = getDSControlGroup(parent);
        dsVersion.DSState = getDSState(parent);
        dsVersion.DSVersionable = getDSVersionable(parent);
        setDatastreamVersionProperties(dsVersion, entry);
    }

    private void setDatastreamVersionProperties(Datastream ds, Entry entry)
            throws ValidationException {
        ds.DatastreamAltIDs = getDSAltIds(entry);
        ds.DSCreateDT = entry.getUpdated();
        ds.DSFormatURI = getDSFormatURI(entry);
        ds.DSLabel = getDSLabel(entry);
        ds.DSVersionID = getDatastreamVersionId(entry);
        ds.DSChecksumType = getDSChecksumType(entry);
        String checksum = getDSChecksum(entry);
        if (m_obj.isNew()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("New Object: checking supplied checksum");
            }
            if (checksum != null && !checksum.equals("")
                    && !checksum.equals(Datastream.CHECKSUM_NONE)) {
                String tmpChecksum = ds.getChecksum();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("checksum = " + tmpChecksum);
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

    private void setExtProps() {
        List<Category> epCategories =
                m_feed.getCategories(MODEL.EXT_PROPERTY.uri);
        for (Category epCategory : epCategories) {
            m_obj.setExtProperty(epCategory.getTerm(), epCategory.getLabel());
        }
    }

    /**
     * Returns the an Entry's contentSrc as a File relative to {@link #m_tempDir}.
     * 
     * @param contentSrc
     * @return the contentSrc as a File relative to m_tempDir.
     * @throws ObjectIntegrityException
     */
    protected File getContentSrcAsFile(IRI contentSrc) throws ObjectIntegrityException {
        if (contentSrc.isAbsolute() || contentSrc.isPathAbsolute()) {
            throw new ObjectIntegrityException("contentSrc must not be absolute");
        }
        try {
            // Normalize the IRI to resolve percent-encoding and 
            // backtracking (e.g. "../")
            NormalizedURI nUri = new NormalizedURI(m_tempDir.toURI().toString() + contentSrc.toString());
            nUri.normalize();

            File f = new File(nUri.toURI());
            if (f.getParentFile().equals(m_tempDir)) {
                return f;
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
