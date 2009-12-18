/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai.sample;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fedora.common.Constants;

import fedora.oai.BadResumptionTokenException;
import fedora.oai.DateGranularitySupport;
import fedora.oai.DeletedRecordSupport;
import fedora.oai.IDDoesNotExistException;
import fedora.oai.OAIProvider;
import fedora.oai.Record;
import fedora.oai.SimpleHeader;
import fedora.oai.SimpleMetadataFormat;
import fedora.oai.SimpleRecord;
import fedora.oai.SimpleSetInfo;

/**
 * A sample implementation of OAIProvider for testing and demonstration
 * purposes.
 * 
 * @author Chris Wilper
 */
public class SampleOAIProvider
        implements Constants, OAIProvider {

    private static String s_rec1_identifier = "sample:1";

    private static String s_rec1_metadata =
            "        <oai_dc:dc\n" + "           xmlns:oai_dc=\""
                    + OAI_DC.uri
                    + "\"\n"
                    + "           xmlns:dc=\""
                    + DC.uri
                    + "\"\n"
                    + "           xmlns:xsi=\""
                    + XSI.uri
                    + "\"\n"
                    + "           xsi:schemaLocation=\""
                    + OAI_DC.uri
                    + "\n"
                    + "           "
                    + OAI_DC2_0.xsdLocation
                    + "\">\n"
                    + "          <dc:title>Using Structural Metadata to Localize Experience of \n"
                    + "                    Digital Content</dc:title>\n"
                    + "          <dc:creator>Dushay, Naomi</dc:creator>\n"
                    + "          <dc:subject>Digital Libraries</dc:subject>\n"
                    + "          <dc:description>With the increasing technical sophistication of\n"
                    + "              both information consumers and providers, there is\n"
                    + "              increasing demand for more meaningful experiences of digital\n"
                    + "              information. We present a framework that separates digital\n"
                    + "              object experience, or rendering, from digital object storage\n"
                    + "              and manipulation, so the rendering can be tailored to\n"
                    + "              particular communities of users.\n"
                    + "          </dc:description>\n"
                    + "          <dc:description>Comment: 23 pages including 2 appendices,\n"
                    + "              8 figures</dc:description>\n"
                    + "          <dc:date>2001-12-14</dc:date>\n"
                    + "        </oai_dc:dc>";

    private static String s_rec1_about =
            "        <provenance\n"
                    + "         xmlns=\""
                    + OAI_PROV.uri
                    + "\"\n"
                    + "         xmlns:xsi=\""
                    + XSI.uri
                    + "\"\n"
                    + "         xsi:schemaLocation=\""
                    + OAI_PROV.uri
                    + "\n"
                    + "         "
                    + OAI_PROV2_0.xsdLocation
                    + "\">\n"
                    + "         <originDescription harvestDate=\"2002-01-01T11:10:01Z\" altered=\"true\">\n"
                    + "          <baseURL>http://some.oa.org</baseURL>\n"
                    + "          <identifier>oai:r2.org:klik001</identifier>\n"
                    + "          <datestamp>2001-01-01</datestamp>\n"
                    + "          <metadataNamespace>" + OAI_DC.uri
                    + "</metadataNamespace>\n"
                    + "          </originDescription>\n"
                    + "        </provenance>";

    private final SimpleHeader m_head1;

    private final SimpleRecord m_rec1;

    public SampleOAIProvider() {
        HashSet<String> s = new HashSet<String>();
        s.add("cs");
        s.add("cornell");
        m_head1 = new SimpleHeader(s_rec1_identifier, new Date(), s, true);
        HashSet<String> a = new HashSet<String>();
        a.add(s_rec1_about);
        m_rec1 = new SimpleRecord(m_head1, s_rec1_metadata, a);
    }

    public String getRepositoryName() {
        return "My Repository";
    }

    public String getBaseURL(String protocol, String port) {
        return protocol + "://localhost:" + port + "/path/to/servlet";
    }

    public String getProtocolVersion() {
        return "2.0";
    }

    public Date getEarliestDatestamp() {
        return new Date();
    }

    public DeletedRecordSupport getDeletedRecordSupport() {
        return DeletedRecordSupport.NO;
    }

    public DateGranularitySupport getDateGranularitySupport() {
        return DateGranularitySupport.SECONDS;
    }

    public Set getAdminEmails() {
        HashSet<String> s = new HashSet<String>();
        s.add("nobody@nowhere.com");
        return s;
    }

    public Set getSupportedCompressionEncodings() {
        return new HashSet();
    }

    public Set getDescriptions() {
        return new HashSet();
    }

    public Record getRecord(String identifier, String metadataPrefix)
            throws IDDoesNotExistException {
        // throws CannotDisseminateFormatException, IDDoesNotExistException;
        if (identifier.equals("sample:1")) {
            return m_rec1;
        } else {
            throw new IDDoesNotExistException("An item with that id was not found.");
        }
    }

    public List getRecords(Date from,
                           Date until,
                           String metadataPrefix,
                           String set) {
        // throws CannotDisseminateFormatException,
        // NoRecordsMatchException, NoSetHierarchyException;
        ArrayList<SimpleRecord> a = new ArrayList<SimpleRecord>();
        a.add(m_rec1);
        return a;
    }

    public List getRecords(String resumptionToken)
            throws BadResumptionTokenException {
        throw new BadResumptionTokenException("Sample doesn't support resumptionTokens.");
    }

    public List getHeaders(Date from,
                           Date until,
                           String metadataPrefix,
                           String set) {
        ArrayList<SimpleHeader> a = new ArrayList<SimpleHeader>();
        a.add(m_head1);
        return a;
    }

    public List getHeaders(String resumptionToken)
            throws BadResumptionTokenException {
        throw new BadResumptionTokenException("Sample doesn't support resumptionTokens.");
    }

    public List getSets() {
        ArrayList<SimpleSetInfo> a = new ArrayList<SimpleSetInfo>();
        a.add(new SimpleSetInfo("Computer Science", "cs", new HashSet()));
        a
                .add(new SimpleSetInfo("Cornell University",
                                       "cornell",
                                       new HashSet()));
        return a;
    }

    public List getSets(String resumptionToken)
            throws BadResumptionTokenException {
        throw new BadResumptionTokenException("Sample doesn't support resumptionTokens.");
    }

    public Set getMetadataFormats(String id) {
        HashSet<SimpleMetadataFormat> s = new HashSet<SimpleMetadataFormat>();
        s.add(new SimpleMetadataFormat(OAI_DC.prefix,
                                       OAI_DC2_0.xsdLocation,
                                       OAI_DC.uri));
        return s;
    }

    public long getMaxSets() {
        return 10;
    }

    public long getMaxRecords() {
        return 10;
    }

    public long getMaxHeaders() {
        return 10;
    }
}