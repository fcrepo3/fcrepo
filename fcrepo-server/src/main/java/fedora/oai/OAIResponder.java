/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.Server;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.errors.authorization.AuthzOperationalException;
import fedora.server.security.Authorization;

/**
 * OAIResponder.
 * 
 * @author Chris Wilper
 */
public class OAIResponder
        implements Constants {

    private final OAIProvider m_provider;

    private DateGranularitySupport m_granularity;

    private Authorization m_authorization;

    public OAIResponder(OAIProvider provider) {
        m_provider = provider;
    }

    public void respond(Context context, Map args, OutputStream outStream)
            throws RepositoryException, AuthzException {
        if (m_authorization == null) {
            Server server;
            try {
                server = Server.getInstance(new File(FEDORA_HOME));
            } catch (Throwable e) {
                throw new AuthzOperationalException("couldn't attempt authz", e);
            }
            m_authorization =
                    (Authorization) server
                            .getModule("fedora.server.security.Authorization");
        }
        m_authorization.enforceOAIRespond(context);
        m_granularity = m_provider.getDateGranularitySupport();
        PrintWriter out = null;
        try {
            out =
                    new PrintWriter(new BufferedWriter(new OutputStreamWriter(outStream,
                                                                              "UTF-8")));
        } catch (UnsupportedEncodingException uee) {
            // not going to happen... all java impls support UTF-8
        }
        String verb = (String) args.get("verb");

        String baseURL =
                m_provider
                        .getBaseURL(context
                                            .getEnvironmentValue(HTTP_REQUEST.SECURITY.uri)
                                            .equals(HTTP_REQUEST.SECURE.uri) ? "https"
                                            : "http",
                                    context
                                            .getEnvironmentValue(HTTP_REQUEST.SERVER_PORT.uri));
        try {
            if (verb == null) {
                throw new BadVerbException("Request did not specify a verb.");
            }
            if (verb.equals("GetRecord")) {
                String identifier = (String) args.get("identifier");
                if (identifier == null) {
                    throw new BadArgumentException("GetRecord request did not specify 'identifier' argument.");
                }
                String metadataPrefix = (String) args.get("metadataPrefix");
                if (metadataPrefix == null) {
                    throw new BadArgumentException("GetRecord request did not specify 'metadataPrefix' argument.");
                }
                if (args.size() > 3) {
                    throw new BadArgumentException("GetRecord request specified illegal argument(s).");
                }
                Record record =
                        m_provider.getRecord(identifier, metadataPrefix);
                respondToGetRecord(args, baseURL, record, out);
            } else if (verb.equals("Identify")) {
                if (args.size() > 1) {
                    throw new BadArgumentException("Identify request specified illegal argument(s).");
                }
                String repositoryName = m_provider.getRepositoryName();
                String protocolVersion = m_provider.getProtocolVersion();
                Date earliestDatestamp = m_provider.getEarliestDatestamp();
                DeletedRecordSupport deletedRecord =
                        m_provider.getDeletedRecordSupport();
                Set adminEmails = m_provider.getAdminEmails();
                Set compressions =
                        m_provider.getSupportedCompressionEncodings();
                Set descriptions = m_provider.getDescriptions();
                respondToIdentify(args,
                                  baseURL,
                                  repositoryName,
                                  protocolVersion,
                                  earliestDatestamp,
                                  deletedRecord,
                                  adminEmails,
                                  compressions,
                                  descriptions,
                                  out);
            } else if (verb.equals("ListIdentifiers")) {
                String rToken = (String) args.get("resumptionToken");
                List headers;
                if (rToken != null) {
                    if (args.size() > 2) {
                        throw new BadArgumentException("ListIdentifiers request specified resumptionToken with other arguments.");
                    }
                    headers = m_provider.getHeaders(rToken);
                } else {
                    Iterator iter = args.keySet().iterator();
                    boolean badParam = false;
                    Date from = null;
                    Date until = null;
                    String metadataPrefix = null;
                    String set = null;
                    while (iter.hasNext()) {
                        String name = (String) iter.next();
                        if (name.equals("metadataPrefix")) {
                            metadataPrefix = (String) args.get(name);
                        } else if (name.equals("set")) {
                            set = (String) args.get(name);
                        } else if (name.equals("from")) {
                            from = getUTCDate((String) args.get(name), false);
                        } else if (name.equals("until")) {
                            until = getUTCDate((String) args.get(name), true);
                        } else if (!name.equals("verb")) {
                            badParam = true;
                        }
                    }
                    if (from != null && until != null) {
                        assertSameGranularity((String) args.get("from"),
                                              (String) args.get("until"));
                    }
                    if (badParam) {
                        throw new BadArgumentException("ListIdentifiers request specified illegal argument(s).");
                    }
                    if (metadataPrefix == null) {
                        throw new BadArgumentException("ListIdentifiers request did not specify metadataPrefix argument.");
                    }
                    headers =
                            m_provider.getHeaders(from,
                                                  until,
                                                  metadataPrefix,
                                                  set);
                }
                if (headers.size() == 0) {
                    throw new NoRecordsMatchException("No records match the providied criteria.");
                }
                ResumptionToken resumptionToken = null;
                if (m_provider.getMaxHeaders() > 0) {
                    if (headers.size() > m_provider.getMaxHeaders()) {
                        resumptionToken =
                                (ResumptionToken) headers
                                        .get(headers.size() - 1);
                        headers = headers.subList(0, headers.size() - 1);
                    }
                }
                respondToListIdentifiers(args,
                                         baseURL,
                                         headers,
                                         resumptionToken,
                                         out);
            } else if (verb.equals("ListMetadataFormats")) {
                String identifier = (String) args.get("identifier");
                if (identifier == null) {
                    if (args.size() > 1) {
                        throw new BadArgumentException("ListMetadataFormats request specified illegal argument(s).");
                    }
                } else {
                    if (args.size() > 2) {
                        throw new BadArgumentException("ListMetadataFormats request specified illegal argument(s).");
                    }
                }
                respondToListMetadataFormats(args, baseURL, m_provider
                        .getMetadataFormats(identifier), out);
            } else if (verb.equals("ListRecords")) {
                String rToken = (String) args.get("resumptionToken");
                List records;
                if (rToken != null) {
                    if (args.size() > 2) {
                        throw new BadArgumentException("ListRecords request specified resumptionToken with other arguments.");
                    }
                    records = m_provider.getRecords(rToken);
                } else {
                    Iterator iter = args.keySet().iterator();
                    boolean badParam = false;
                    Date from = null;
                    Date until = null;
                    String metadataPrefix = null;
                    String set = null;
                    while (iter.hasNext()) {
                        String name = (String) iter.next();
                        if (name.equals("metadataPrefix")) {
                            metadataPrefix = (String) args.get(name);
                        } else if (name.equals("set")) {
                            set = (String) args.get(name);
                        } else if (name.equals("from")) {
                            from = getUTCDate((String) args.get(name), false);
                        } else if (name.equals("until")) {
                            until = getUTCDate((String) args.get(name), true);
                        } else if (!name.equals("verb")) {
                            badParam = true;
                        }
                    }
                    if (from != null && until != null) {
                        assertSameGranularity((String) args.get("from"),
                                              (String) args.get("until"));
                    }
                    if (badParam) {
                        throw new BadArgumentException("ListRecords request specified illegal argument(s).");
                    }
                    if (metadataPrefix == null) {
                        throw new BadArgumentException("ListRecords request did not specify metadataPrefix argument.");
                    }
                    records =
                            m_provider.getRecords(from,
                                                  until,
                                                  metadataPrefix,
                                                  set);
                }
                if (records.size() == 0) {
                    throw new NoRecordsMatchException("No records match the providied criteria.");
                }
                ResumptionToken resumptionToken = null;
                if (m_provider.getMaxRecords() > 0) {
                    if (records.size() > m_provider.getMaxRecords()) {
                        resumptionToken =
                                (ResumptionToken) records
                                        .get(records.size() - 1);
                        records = records.subList(0, records.size() - 1);
                    }
                }
                respondToListRecords(args,
                                     baseURL,
                                     records,
                                     resumptionToken,
                                     out);
            } else if (verb.equals("ListSets")) {
                String rToken = (String) args.get("resumptionToken");
                List sets;
                if (rToken == null) {
                    if (args.size() > 1) {
                        throw new BadArgumentException("ListSets request specified illegal argument(s).");
                    }
                    sets = m_provider.getSets();
                } else {
                    if (args.size() > 2) {
                        throw new BadArgumentException("ListSets request specified illegal argument(s).");
                    }
                    sets = m_provider.getSets(rToken);
                }
                ResumptionToken resumptionToken = null;
                if (m_provider.getMaxSets() > 0) {
                    if (sets.size() > m_provider.getMaxSets()) {
                        resumptionToken =
                                (ResumptionToken) sets.get(sets.size() - 1);
                        sets = sets.subList(0, sets.size() - 1);
                    }
                }
                respondToListSets(args, baseURL, sets, resumptionToken, out);
            } else {
                throw new BadVerbException("Unrecognized verb, '" + verb + "'.");
            }
        } catch (OAIException oaie) {
            respondWithError(oaie, verb, baseURL, out);
        } finally {
            out.flush();
        }
    }

    private void assertSameGranularity(String from, String until)
            throws BadArgumentException {
        if (from.endsWith("Z") && !until.endsWith("Z") || until.endsWith("Z")
                && !from.endsWith("Z")) {
            throw new BadArgumentException("Date granularities of from and until arguments do not match.");
        }
    }

    private void respondToGetRecord(Map args,
                                    String baseURL,
                                    Record record,
                                    PrintWriter out) {
        appendTop(out);
        appendRequest(args, baseURL, out);
        out.println("  <GetRecord>");
        appendRecord("    ", record, out);
        out.println("  </GetRecord>");
        appendBottom(out);
    }

    private void respondToIdentify(Map args,
                                   String baseURL,
                                   String repositoryName,
                                   String protocolVersion,
                                   Date earliestDatestamp,
                                   DeletedRecordSupport deletedRecord,
                                   Set adminEmails,
                                   Set compressions,
                                   Set descriptions,
                                   PrintWriter out) {
        appendTop(out);
        appendRequest(args, baseURL, out);
        out.println("  <Identify>");
        out.println("    <repositoryName>" + OAIResponder.enc(repositoryName)
                + "</repositoryName>");
        out.println("    <baseURL>" + OAIResponder.enc(baseURL) + "</baseURL>");
        out.println("    <protocolVersion>" + protocolVersion
                + "</protocolVersion>");
        Iterator iter = adminEmails.iterator();
        while (iter.hasNext()) {
            out.println("    <adminEmail>"
                    + OAIResponder.enc((String) iter.next()) + "</adminEmail>");
        }
        out.println("    <earliestDatestamp>"
                + getUTCString(earliestDatestamp,
                               m_granularity == DateGranularitySupport.SECONDS)
                + "</earliestDatestamp>");
        out.println("    <deletedRecord>" + deletedRecord.toString()
                + "</deletedRecord>");
        out.println("    <granularity>" + m_granularity.toString()
                + "</granularity>");
        iter = compressions.iterator();
        while (iter.hasNext()) {
            out
                    .println("    <compression>"
                            + OAIResponder.enc((String) iter.next())
                            + "</compression>");
        }
        iter = descriptions.iterator();
        while (iter.hasNext()) {
            out.println("    <description>");
            out.println((String) iter.next());
            out.println("    </description>");
        }
        out.println("  </Identify>");
        appendBottom(out);
    }

    // resumptionToken may be null
    private void respondToListIdentifiers(Map args,
                                          String baseURL,
                                          List headers,
                                          ResumptionToken resumptionToken,
                                          PrintWriter out) {
        appendTop(out);
        appendRequest(args, baseURL, out);
        out.println("  <ListIdentifiers>");
        for (int i = 0; i < headers.size(); i++) {
            Header header = (Header) headers.get(i);
            appendHeader("    ", header, out);
        }
        appendResumptionToken(resumptionToken, out);
        out.println("  </ListIdentifiers>");
        appendBottom(out);
    }

    private void respondToListMetadataFormats(Map args,
                                              String baseURL,
                                              Set metadataFormats,
                                              PrintWriter out) {
        appendTop(out);
        appendRequest(args, baseURL, out);
        out.println("  <ListMetadataFormats>");
        Iterator iter = metadataFormats.iterator();
        while (iter.hasNext()) {
            MetadataFormat f = (MetadataFormat) iter.next();
            out.println("    <metadataFormat>");
            out.println("      <metadataPrefix>" + f.getPrefix()
                    + "</metadataPrefix>");
            out.println("      <schema>" + f.getSchemaLocation() + "</schema>");
            out.println("      <metadataNamespace>" + f.getNamespaceURI()
                    + "</metadataNamespace>");
            out.println("    </metadataFormat>");
        }
        out.println("  </ListMetadataFormats>");
        appendBottom(out);
    }

    // resumptionToken may be null
    private void respondToListRecords(Map args,
                                      String baseURL,
                                      List records,
                                      ResumptionToken resumptionToken,
                                      PrintWriter out) {
        appendTop(out);
        appendRequest(args, baseURL, out);
        out.println("  <ListRecords>");
        for (int i = 0; i < records.size(); i++) {
            appendRecord("    ", (Record) records.get(i), out);
        }
        appendResumptionToken(resumptionToken, out);
        out.println("  </ListRecords>");
        appendBottom(out);
    }

    // resumptionToken may be null
    private void respondToListSets(Map args,
                                   String baseURL,
                                   List sets,
                                   ResumptionToken resumptionToken,
                                   PrintWriter out) {
        appendTop(out);
        appendRequest(args, baseURL, out);
        out.println("  <ListSets>");
        for (int i = 0; i < sets.size(); i++) {
            SetInfo s = (SetInfo) sets.get(i);
            out.println("    <set>");
            out.println("      <setSpec>" + s.getSpec() + "</setSpec>");
            out.println("      <setName>" + s.getName() + "</setName>");
            Iterator iter = s.getDescriptions().iterator();
            while (iter.hasNext()) {
                out.println("      <setDescription>\n");
                out.println((String) iter.next());
                out.println("      </setDescription>\n");
            }
            out.println("    </set>");
        }
        appendResumptionToken(resumptionToken, out);
        out.println("  </ListSets>");
        appendBottom(out);
    }

    private void appendRecord(String indent, Record record, PrintWriter out) {
        Header header = record.getHeader();
        String metadata = record.getMetadata();
        Set abouts = record.getAbouts();
        out.println(indent + "<record>");
        appendHeader(indent + "  ", header, out);
        if (header.isAvailable()) {
            out.println(indent + "  <metadata>");
            out.println(metadata);
            out.println(indent + "  </metadata>");
            Iterator iter = abouts.iterator();
            while (iter.hasNext()) {
                out.println(indent + "  <about>");
                out.println((String) iter.next());
                out.println(indent + "  </about>");
            }
        }
        out.println(indent + "</record>");
    }

    private void appendHeader(String indent, Header header, PrintWriter out) {
        String identifier = header.getIdentifier();
        Date datestamp = header.getDatestamp();
        Set setSpecs = header.getSetSpecs();
        boolean isAvailable = header.isAvailable();
        out.print(indent + "<header");
        if (!isAvailable) {
            out.print(" status=\"deleted\"");
        }
        out.println(">");
        out.println(indent + "  <identifier>" + OAIResponder.enc(identifier)
                + "</identifier>");
        out.println(indent
                + "  <datestamp>"
                + getUTCString(datestamp,
                               m_granularity == DateGranularitySupport.SECONDS)
                + "</datestamp>");
        Iterator iter = setSpecs.iterator();
        while (iter.hasNext()) {
            out.println(indent + "  <setSpec>"
                    + OAIResponder.enc((String) iter.next()) + "</setSpec>");
        }
        out.println(indent + "</header>");
    }

    private void appendResumptionToken(ResumptionToken token, PrintWriter out) {
        if (token != null) {
            out.print("    <resumptionToken");
            if (token.getExpirationDate() != null) {
                out.print(" expirationDate=\""
                        + getUTCString(token.getExpirationDate(), true) + "\"");
            }
            if (token.getCompleteListSize() >= 0) {
                out.print(" completeListSize=\"" + token.getCompleteListSize()
                        + "\"");
            }
            if (token.getCursor() >= 0) {
                out.print(" cursor=\"" + token.getCursor() + "\"");
            }
            if (token.getValue() != null) {
                out.println(">" + token.getValue() + "</resumptionToken>");
            } else {
                out.println("/>");
            }
        }
    }

    private void appendRequest(Map args, String baseURL, PrintWriter out) {
        out.print("  <request");
        Iterator iter = args.keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            String value = (String) args.get(name);
            out.print(" " + name + "=\"" + OAIResponder.enc(value) + "\"");
        }
        out.println(">" + OAIResponder.enc(baseURL) + "</request>");
    }

    private void appendTop(PrintWriter out) {
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<OAI-PMH xmlns=\"" + OAI_PMH.uri + "\"");
        out.println("         xmlns:xsi=\"" + XSI.uri + "\"");
        out.println("         xsi:schemaLocation=\"" + OAI_PMH.uri + " "
                + OAI_PMH2_0.xsdLocation + "\">");
        out.println("  <responseDate>"
                + getUTCString(getUTCDate(new Date()), true)
                + "</responseDate>");
    }

    private void appendBottom(PrintWriter out) {
        out.println("</OAI-PMH>");
    }

    private void respondWithError(OAIException e,
                                  String verb,
                                  String baseURL,
                                  PrintWriter out) {
        appendTop(out);
        out.print("  <request");
        if (!e.getCode().equals("badVerb")) {
            out.print(" verb=\"" + verb + "\"");
        }
        out.println(">" + OAIResponder.enc(baseURL) + "</request>");
        if (e.getMessage() == null) {
            out.println("  <error code=\"" + e.getCode() + "\"/>");
        } else {
            out.println("  <error code=\"" + e.getCode() + "\">"
                    + OAIResponder.enc(e.getMessage()) + "</error>");
        }
        appendBottom(out);
    }

    private static String getUTCString(Date utcDate, boolean fineGranularity) {
        SimpleDateFormat formatter;
        if (fineGranularity) {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        } else {
            formatter = new SimpleDateFormat("yyyy-MM-dd");
        }
        return formatter.format(utcDate);
    }

    private Date getUTCDate(String formattedDate, boolean isUntil)
            throws BadArgumentException {
        if (formattedDate == null) {
            return null;
        }
        try {
            if (formattedDate.endsWith("Z")) {
                if (m_granularity == DateGranularitySupport.SECONDS) {
                    SimpleDateFormat formatter =
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    return formatter.parse(formattedDate);
                } else {
                    throw new BadArgumentException("Repository does not support that granularity.");
                }
            } else {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                if (isUntil) {
                    SimpleDateFormat sdf =
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    return sdf.parse(formatter.format(formatter
                            .parse(formattedDate))
                            + "T23:59:59Z");
                } else {
                    return formatter.parse(formattedDate);
                }
            }
        } catch (ParseException pe) {
            throw new BadArgumentException("Error parsing date.");
        }
    }

    private Date getUTCDate(Date localDate) {
        Calendar cal = Calendar.getInstance();
        int tzOffset = cal.get(Calendar.ZONE_OFFSET);
        TimeZone tz = cal.getTimeZone();
        if (tz.inDaylightTime(localDate)) {
            tzOffset += cal.get(Calendar.DST_OFFSET);
        }
        Date UTCDate = new Date();
        UTCDate.setTime(localDate.getTime() + tzOffset);
        return UTCDate;
    }

    /**
     * Returns an XML-appropriate encoding of the given String.
     * 
     * @param in
     *        The String to encode.
     * @return A new, encoded String.
     */
    private static String enc(String in) {
        StringBuffer out = new StringBuffer();
        enc(in, out);
        return out.toString();
    }

    /**
     * Appends an XML-appropriate encoding of the given String to the given
     * StringBuffer.
     * 
     * @param in
     *        The String to encode.
     * @param buf
     *        The StringBuffer to write to.
     */
    private static void enc(String in, StringBuffer out) {
        for (int i = 0; i < in.length(); i++) {
            enc(in.charAt(i), out);
        }
    }

    /**
     * Appends an XML-appropriate encoding of the given range of characters to
     * the given StringBuffer.
     * 
     * @param in
     *        The char buffer to read from.
     * @param start
     *        The starting index.
     * @param length
     *        The number of characters in the range.
     * @param out
     *        The StringBuffer to write to.
     */
    private static void enc(char[] in, int start, int length, StringBuffer out) {
        for (int i = start; i < length + start; i++) {
            enc(in[i], out);
        }
    }

    /**
     * Appends an XML-appropriate encoding of the given character to the given
     * StringBuffer.
     * 
     * @param in
     *        The character.
     * @param out
     *        The StringBuffer to write to.
     */
    private static void enc(char in, StringBuffer out) {
        if (in == '&') {
            out.append("&amp;");
        } else if (in == '<') {
            out.append("&lt;");
        } else if (in == '>') {
            out.append("&gt;");
        } else if (in == '\"') {
            out.append("&quot;");
        } else if (in == '\'') {
            out.append("&apos;");
        } else {
            out.append(in);
        }
    }

}