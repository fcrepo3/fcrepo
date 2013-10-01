/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.oai.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fcrepo.common.Constants;
import org.fcrepo.oai.BadResumptionTokenException;
import org.fcrepo.oai.DateGranularitySupport;
import org.fcrepo.oai.DeletedRecordSupport;
import org.fcrepo.oai.IDDoesNotExistException;
import org.fcrepo.oai.MetadataFormat;
import org.fcrepo.oai.OAIProvider;
import org.fcrepo.oai.Record;
import org.fcrepo.oai.SetInfo;
import org.fcrepo.oai.SimpleHeader;
import org.fcrepo.oai.SimpleMetadataFormat;
import org.fcrepo.oai.SimpleRecord;
import org.fcrepo.oai.SimpleSetInfo;



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

    public Set<String> getAdminEmails() {
        return Collections.singleton("nobody@nowhere.com");
    }

    public Set<String> getSupportedCompressionEncodings() {
        return Collections.emptySet();
    }

    public Set<String> getDescriptions() {
        return Collections.emptySet();
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

    public List<?> getRecords(Date from,
                           Date until,
                           String metadataPrefix,
                           String set) {
        // throws CannotDisseminateFormatException,
        // NoRecordsMatchException, NoSetHierarchyException;
        return Collections.singletonList(m_rec1);
    }

    public List<?> getRecords(String resumptionToken)
            throws BadResumptionTokenException {
        throw new BadResumptionTokenException("Sample doesn't support resumptionTokens.");
    }

    public List<?> getHeaders(Date from,
                           Date until,
                           String metadataPrefix,
                           String set) {
        return Collections.singletonList(m_head1);
    }

    public List<?> getHeaders(String resumptionToken)
            throws BadResumptionTokenException {
        throw new BadResumptionTokenException("Sample doesn't support resumptionTokens.");
    }

    public List<SetInfo> getSets() {
        ArrayList<SetInfo> a = new ArrayList<SetInfo>();
        a.add(new SimpleSetInfo("Computer Science", "cs", new HashSet<String>()));
        a
                .add(new SimpleSetInfo("Cornell University",
                                       "cornell",
                                       new HashSet<String>()));
        return a;
    }

    public List<?> getSets(String resumptionToken)
            throws BadResumptionTokenException {
        throw new BadResumptionTokenException("Sample doesn't support resumptionTokens.");
    }

    public Set<MetadataFormat> getMetadataFormats(String id) {
        MetadataFormat mdf = (new SimpleMetadataFormat(OAI_DC.prefix,
                                       OAI_DC2_0.xsdLocation,
                                       OAI_DC.uri));
        return Collections.singleton(mdf);
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