/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.oai;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.fcrepo.common.Constants;
import org.fcrepo.oai.BadResumptionTokenException;
import org.fcrepo.oai.CannotDisseminateFormatException;
import org.fcrepo.oai.DateGranularitySupport;
import org.fcrepo.oai.DeletedRecordSupport;
import org.fcrepo.oai.Header;
import org.fcrepo.oai.IDDoesNotExistException;
import org.fcrepo.oai.MetadataFormat;
import org.fcrepo.oai.NoMetadataFormatsException;
import org.fcrepo.oai.NoRecordsMatchException;
import org.fcrepo.oai.NoSetHierarchyException;
import org.fcrepo.oai.OAIProvider;
import org.fcrepo.oai.Record;
import org.fcrepo.oai.RepositoryException;
import org.fcrepo.oai.SetInfo;
import org.fcrepo.oai.SimpleHeader;
import org.fcrepo.oai.SimpleMetadataFormat;
import org.fcrepo.oai.SimpleRecord;
import org.fcrepo.oai.SimpleResumptionToken;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.UnknownSessionTokenException;
import org.fcrepo.server.search.Condition;
import org.fcrepo.server.search.FieldSearch;
import org.fcrepo.server.search.FieldSearchQuery;
import org.fcrepo.server.search.FieldSearchResult;
import org.fcrepo.server.search.ObjectFields;
import org.fcrepo.server.utilities.DCFields;




/**
 * Simple FieldSearch-based OAI provider.
 * 
 * @author Chris Wilper
 */
public class FedoraOAIProvider
        implements Constants, OAIProvider {

    private final String m_repositoryName;

    private final String m_repositoryDomainName;

    private final String m_localname;

    private final String m_relpath;

    private final Set<String> m_adminEmails;

    private final Set<String> m_descriptions;

    private final List<SetInfo> m_setInfos;

    private final long m_maxSets;

    private final long m_maxRecords;

    private final long m_maxHeaders;

    private final FieldSearch m_fieldSearch;

    private final Set<MetadataFormat> m_formats;

    private static String[] s_headerFields =
            new String[] {"pid", "dcmDate"};

    private static String[] s_headerAndDCFields =
            new String[] {"pid", "dcmDate", "title", "creator",
                    "subject", "description", "publisher", "contributor",
                    "date", "type", "format", "identifier", "source",
                    "language", "relation", "coverage", "rights"};

    public FedoraOAIProvider(String repositoryName,
                             String repositoryDomainName,
                             String localname,
                             String relpath,
                             Set<String> adminEmails,
                             Set<String> friendBaseURLs,
                             String namespaceID,
                             long maxSets,
                             long maxRecords,
                             long maxHeaders,
                             FieldSearch fieldSearch) {
        m_repositoryName = repositoryName;
        m_repositoryDomainName = repositoryDomainName;
        m_localname = localname;
        m_relpath = relpath;
        m_adminEmails = adminEmails;
        m_maxSets = maxSets;
        m_maxRecords = maxRecords;
        m_maxHeaders = maxHeaders;
        m_fieldSearch = fieldSearch;
        m_descriptions = new HashSet<String>();
        StringBuffer buf = new StringBuffer();
        buf.append("      <oai-identifier xmlns=\"" + OAI_IDENTIFIER.uri
                + "\"\n");
        buf.append("          xmlns:xsi=\"" + XSI.uri + "\"\n");
        buf.append("          xsi:schemaLocation=\"" + OAI_IDENTIFIER.uri
                + "\n");
        buf.append("          " + OAI_IDENTIFIER2_0.xsdLocation + "\">\n");
        buf.append("        <scheme>oai</scheme>\n");
        buf.append("        <repositoryIdentifier>" + m_repositoryDomainName
                + "</repositoryIdentifier>\n");
        buf.append("        <delimiter>:</delimiter>\n");
        buf.append("        <sampleIdentifier>oai:" + m_repositoryDomainName
                + ":" + namespaceID + ":7654</sampleIdentifier>\n");
        buf.append("      </oai-identifier>");
        m_descriptions.add(buf.toString());
        if (friendBaseURLs != null && friendBaseURLs.size() > 0) {
            buf = new StringBuffer();
            buf.append("      <friends xmlns=\"" + OAI_FRIENDS.uri + "\"\n");
            buf.append("          xmlns:xsi=\"" + XSI.uri + "\"\n");
            buf.append("          xsi:schemaLocation=\"" + OAI_FRIENDS.uri
                    + "\n");
            buf.append("          " + OAI_FRIENDS2_0.xsdLocation + "\">\n");
            Iterator<String> iter = friendBaseURLs.iterator();
            while (iter.hasNext()) {
                buf.append("        <baseURL>" + iter.next()
                        + "</baseURL>\n");
            }
            buf.append("      </friends>");
            m_descriptions.add(buf.toString());
        }
        m_formats = new HashSet<MetadataFormat>();
        m_formats.add(new SimpleMetadataFormat("oai_dc",
                                               OAI_DC2_0.xsdLocation,
                                               OAI_DC.uri));
        m_setInfos = new ArrayList<SetInfo>();
    }

    public String getRepositoryName() {
        return m_repositoryName;
    }

    public String getBaseURL(String protocol, String port) {
        return protocol + "://" + m_localname + ":" + port + m_relpath;
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
        return m_adminEmails;
    }

    public Set<String> getSupportedCompressionEncodings() {
        return Collections.emptySet();
    }

    public Set<String> getDescriptions() {
        return m_descriptions;
    }

    public Record getRecord(String identifier, String metadataPrefix)
            throws CannotDisseminateFormatException, IDDoesNotExistException,
            RepositoryException {
        if (!metadataPrefix.equals("oai_dc")) {
            throw new CannotDisseminateFormatException("Repository does not provide that format in OAI-PMH responses.");
        }
        String pid = getPID(identifier);
        List<ObjectFields> l = null;
        try {
            //FIXME: use maxResults from... config instead of hardcoding 100?
            l =
                    m_fieldSearch
                            .findObjects(s_headerAndDCFields,
                                         100,
                                         new FieldSearchQuery(Condition
                                                 .getConditions("pid='"
                                                         + pid
                                                         + "' dcmDate>'2000-01-01'")))
                            .objectFieldsList();
        } catch (ServerException se) {
            throw new RepositoryException(se.getClass().getName() + ": "
                    + se.getMessage());
        }
        if (l.size() > 0) {
            ObjectFields f = (ObjectFields) l.get(0);
            Set<String> abouts = Collections.emptySet();
            return new SimpleRecord(getHeader(f), getDCXML(f), abouts);
        } else {
            // see if it exists
            try {
                l =
                        m_fieldSearch
                                .findObjects(new String[] {"pid"},
                                             1,
                                             new FieldSearchQuery(Condition
                                                     .getConditions("pid='"
                                                             + pid + "'")))
                                .objectFieldsList();
            } catch (ServerException se) {
                throw new RepositoryException(se.getClass().getName() + ": "
                        + se.getMessage());
            }
            if (l.size() == 0) {
                throw new IDDoesNotExistException("The provided id does not match any item in the repository.");
            } else {
                throw new CannotDisseminateFormatException("The item doesn't even have dc_oai metadata.");
            }
        }
    }

    public List<?> getRecords(Date from,
                           Date until,
                           String metadataPrefix,
                           String set) throws CannotDisseminateFormatException,
            NoRecordsMatchException, NoSetHierarchyException,
            RepositoryException {
        if (!metadataPrefix.equals("oai_dc")) {
            throw new CannotDisseminateFormatException("Repository does not provide that format in OAI-PMH responses.");
        }
        List<ObjectFields> l = null;
        FieldSearchResult fsr;
        try {
            fsr =
                    m_fieldSearch
                            .findObjects(s_headerAndDCFields,
                                         (int) getMaxRecords(),
                                         new FieldSearchQuery(Condition
                                                 .getConditions("dcmDate>'2000-01-01'"
                                                         + getDatePart(from,
                                                                       until))));
            l = fsr.objectFieldsList();
        } catch (ServerException se) {
            throw new RepositoryException(se.getClass().getName() + ": "
                    + se.getMessage());
        }
        if (l.size() == 0) {
            throw new NoRecordsMatchException("No records match the given criteria.");
        }
        ArrayList<Object> ret = new ArrayList<Object>();
        Set<String> abouts = Collections.emptySet();
        for (int i = 0; i < l.size(); i++) {
            ObjectFields f = (ObjectFields) l.get(i);
            ret.add(new SimpleRecord(getHeader(f), getDCXML(f), abouts));
        }
        if (fsr.getToken() != null) {
            // add resumptionToken stuff
            ret.add(new SimpleResumptionToken(fsr.getToken(), fsr
                    .getExpirationDate(), fsr.getCompleteListSize(), fsr
                    .getCursor()));
        }
        return ret;
    }

    private Header getHeader(ObjectFields f) {
        String identifier = "oai:" + m_repositoryDomainName + ":" + f.getPid();
        Date datestamp = f.getDCMDate();
        HashSet<String> setSpecs = new HashSet<String>();
        return new SimpleHeader(identifier, datestamp, setSpecs, true);
    }

    private String getDCXML(DCFields dc) {
        return dc.getAsXML();
    }

    public List<?> getRecords(String resumptionToken)
            throws CannotDisseminateFormatException, NoRecordsMatchException,
            NoSetHierarchyException, BadResumptionTokenException,
            RepositoryException {
        // this is the exact same as the other getRecords, except for the FieldSearch call,
        // and the fact that we re-throw UnknownSessionTokenException
        // as a BadResumptionTokenException
        List<ObjectFields> l = null;
        FieldSearchResult fsr;
        try {
            fsr = m_fieldSearch.resumeFindObjects(resumptionToken);
            l = fsr.objectFieldsList();
        } catch (UnknownSessionTokenException uste) {
            throw new BadResumptionTokenException("Not a known resumptionToken.");
        } catch (ServerException se) {
            throw new RepositoryException(se.getClass().getName() + ": "
                    + se.getMessage());
        }
        if (l.size() == 0) {
            throw new NoRecordsMatchException("No records match the given criteria.");
        }
        ArrayList<Object> ret = new ArrayList<Object>();
        Set<String> abouts = Collections.emptySet();
        for (int i = 0; i < l.size(); i++) {
            ObjectFields f = (ObjectFields) l.get(i);
            ret.add(new SimpleRecord(getHeader(f), getDCXML(f), abouts));
        }
        if (fsr.getToken() != null) {
            ret.add(new SimpleResumptionToken(fsr.getToken(), fsr
                    .getExpirationDate(), fsr.getCompleteListSize(), fsr
                    .getCursor()));
        }
        return ret;
    }

    private String getDatePart(Date from, Date until) {
        if (from == null && until == null) {
            return "";
        }
        StringBuffer out = new StringBuffer();
        // Note OAI only support ISO8601 dates to the seconds
        // and Fedora stores dates down to the millisecond level.
        // This should not matter since OAI requests specify
        // date ranges (from-until), and as long as the requests
        // are always in the same units, subsequent requests can pick 
        // up at the last end-point in time (in seconds) without
        // concern for millisecond granularity.
        SimpleDateFormat formatter =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        if (from != null) {
            out.append(" dcmDate>='");
            out.append(formatter.format(from));
            out.append("'");
        }
        if (until != null) {
            out.append(" dcmDate<='");
            out.append(formatter.format(until));
            out.append("'");
        }
        return out.toString();
    }

    public List<?> getHeaders(Date from,
                           Date until,
                           String metadataPrefix,
                           String set) throws CannotDisseminateFormatException,
            NoRecordsMatchException, NoSetHierarchyException,
            RepositoryException {
        if (!metadataPrefix.equals("oai_dc")) {
            throw new CannotDisseminateFormatException("Repository does not provide that format in OAI-PMH responses.");
        }
        List<ObjectFields> l = null;
        FieldSearchResult fsr;
        try {
            fsr =
                    m_fieldSearch
                            .findObjects(s_headerFields,
                                         (int) getMaxHeaders(),
                                         new FieldSearchQuery(Condition
                                                 .getConditions("dcmDate>'2000-01-01'"
                                                         + getDatePart(from,
                                                                       until))));
            l = fsr.objectFieldsList();
        } catch (ServerException se) {
            throw new RepositoryException(se.getClass().getName() + ": "
                    + se.getMessage());
        }
        if (l.size() == 0) {
            throw new NoRecordsMatchException("No records match the given criteria.");
        }
        ArrayList<Object> ret = new ArrayList<Object>();
        for (int i = 0; i < l.size(); i++) {
            ObjectFields f = (ObjectFields) l.get(i);
            String identifier =
                    "oai:" + m_repositoryDomainName + ":" + f.getPid();
            Date datestamp = f.getDCMDate();
            HashSet<String> setSpecs = new HashSet<String>();

            ret.add(new SimpleHeader(identifier, datestamp, setSpecs, true));
        }
        if (fsr.getToken() != null) {
            ret.add(new SimpleResumptionToken(fsr.getToken(), fsr
                    .getExpirationDate(), fsr.getCompleteListSize(), fsr
                    .getCursor()));
        }
        return ret;
    }

    public List<?> getHeaders(String resumptionToken)
            throws CannotDisseminateFormatException, NoRecordsMatchException,
            NoSetHierarchyException, BadResumptionTokenException,
            RepositoryException {
        // this is the exact same as the other getHeaders, except for the FieldSearch call,
        // and the fact that we re-throw UnknownSessionTokenException
        // as a BadResumptionTokenException
        List<ObjectFields> l = null;
        FieldSearchResult fsr;
        try {
            fsr = m_fieldSearch.resumeFindObjects(resumptionToken);
            l = fsr.objectFieldsList();
        } catch (UnknownSessionTokenException uste) {
            throw new BadResumptionTokenException("Not a known resumptionToken.");
        } catch (ServerException se) {
            throw new RepositoryException(se.getClass().getName() + ": "
                    + se.getMessage());
        }
        if (l.size() == 0) {
            throw new NoRecordsMatchException("No records match the given criteria.");
        }
        ArrayList<Object> ret = new ArrayList<Object>();
        for (int i = 0; i < l.size(); i++) {
            ObjectFields f = (ObjectFields) l.get(i);
            String identifier =
                    "oai:" + m_repositoryDomainName + ":" + f.getPid();
            Date datestamp = f.getDCMDate();
            HashSet<String> setSpecs = new HashSet<String>();
            ret.add(new SimpleHeader(identifier, datestamp, setSpecs, true));
        }
        if (fsr.getToken() != null) {
            ret.add(new SimpleResumptionToken(fsr.getToken(), fsr
                    .getExpirationDate(), fsr.getCompleteListSize(), fsr
                    .getCursor()));
        }
        return ret;
    }

    public List<?> getSets() throws NoSetHierarchyException, RepositoryException {
        return m_setInfos;
    }

    public List<?> getSets(String resumptionToken)
            throws BadResumptionTokenException, NoSetHierarchyException,
            RepositoryException {
        // no resumptionTokens are currently used on getSets since it's always so small
        throw new BadResumptionTokenException("Not a known resumptionToken.");
    }

    private String getPID(String id) throws IDDoesNotExistException {
        if (!id.startsWith("oai:" + m_repositoryDomainName + ":")) {
            throw new IDDoesNotExistException("For this repository, all identifiers in OAI requests should begin with oai:"
                    + m_repositoryDomainName + ":");
        }
        if (id.indexOf("'") != -1) {
            throw new IDDoesNotExistException("For this repository, no identifiers contain the apostrophe character.");
        }
        return id.substring(4 + m_repositoryDomainName.length() + 1);
    }

    public Set<MetadataFormat> getMetadataFormats(String id) throws NoMetadataFormatsException,
            IDDoesNotExistException, RepositoryException {
        if (id == null) {
            return m_formats;
        }
        String pid = getPID(id);
        List<ObjectFields> l = null;
        try {
            l =
                    m_fieldSearch
                            .findObjects(new String[] {"pid"},
                                         1,
                                         new FieldSearchQuery(Condition
                                                 .getConditions("pid='"
                                                         + pid
                                                         + "' dcmDate>'2000-01-01'")))
                            .objectFieldsList();
        } catch (ServerException se) {
            throw new RepositoryException(se.getClass().getName() + ": "
                    + se.getMessage());
        }
        if (l.size() > 0) {
            return m_formats;
        }
        try {
            l =
                    m_fieldSearch.findObjects(new String[] {"pid"},
                                              1,
                                              new FieldSearchQuery(Condition
                                                      .getConditions("pid='"
                                                              + pid + "'")))
                            .objectFieldsList();
        } catch (ServerException se) {
            throw new RepositoryException(se.getClass().getName() + ": "
                    + se.getMessage());
        }
        if (l.size() > 0) {
            throw new NoMetadataFormatsException("The item doesn't even have dc_oai metadata.");
        } else {
            throw new IDDoesNotExistException("The provided id does not match any item in the repository.");
        }
    }

    public long getMaxSets() throws RepositoryException {
        return m_maxSets;
    }

    public long getMaxRecords() throws RepositoryException {
        return m_maxRecords;
    }

    public long getMaxHeaders() throws RepositoryException {
        return m_maxHeaders;
    }

}