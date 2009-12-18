/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.Server;
import fedora.server.errors.GeneralException;
import fedora.server.errors.InitializationException;
import fedora.server.errors.QueryParseException;
import fedora.server.errors.ServerException;
import fedora.server.search.Condition;
import fedora.server.search.FieldSearchQuery;
import fedora.server.search.FieldSearchResult;
import fedora.server.search.ObjectFields;
import fedora.server.utilities.StreamUtility;

import fedora.utilities.XmlTransformUtility;

/**
 * Implements reporting functionality.
 *
 * @author Bill Niebel
 */
public class Report {

    /** Instance of the Server */
    private static Server s_server = null;

    /** Instance of the access subsystem */
    private static Access s_access = null;

    private static final String getFieldValue(ObjectFields f, String name) {
        SimpleDateFormat formatter =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String value = null;
        if ("pid".equalsIgnoreCase(name)) {
            value = f.getPid();
        } else if ("label".equalsIgnoreCase(name)) {
            value = f.getLabel();
        } else if ("state".equalsIgnoreCase(name)) {
            value = f.getState();
        } else if ("ownerId".equalsIgnoreCase(name)) {
            value = f.getOwnerId();
        } else if ("cDate".equalsIgnoreCase(name)) {
            value = formatter.format(f.getCDate());
        } else if ("mDate".equalsIgnoreCase(name)) {
            value = formatter.format(f.getMDate());
        } else if ("dcmDate".equalsIgnoreCase(name)) {
            value = formatter.format(f.getDCMDate());
        }
        return value;
    }

    private static final List getFieldValues(ObjectFields f, String name) {
        List values = null;
        if ("title".equalsIgnoreCase(name)) {
            values = f.titles();
        } else if ("creator".equalsIgnoreCase(name)) {
            values = f.creators();
        } else if ("subject".equalsIgnoreCase(name)) {
            values = f.subjects();
        } else if ("description".equalsIgnoreCase(name)) {
            values = f.descriptions();
        } else if ("publisher".equalsIgnoreCase(name)) {
            values = f.publishers();
        } else if ("contributor".equalsIgnoreCase(name)) {
            values = f.contributors();
        } else if ("date".equalsIgnoreCase(name)) {
            values = f.dates();
        } else if ("type".equalsIgnoreCase(name)) {
            values = f.types();
        } else if ("format".equalsIgnoreCase(name)) {
            values = f.formats();
        } else if ("identifier".equalsIgnoreCase(name)) {
            values = f.identifiers();
        } else if ("source".equalsIgnoreCase(name)) {
            values = f.sources();
        } else if ("language".equalsIgnoreCase(name)) {
            values = f.languages();
        } else if ("relation".equalsIgnoreCase(name)) {
            values = f.relations();
        } else if ("coverage".equalsIgnoreCase(name)) {
            values = f.coverages();
        } else if ("rights".equalsIgnoreCase(name)) {
            values = f.rights();
        }
        return values;
    }

    private static final HashSet<String> multivalued = new HashSet<String>();
    static {
        String[] temp =
                {"title", "creator", "subject", "description",
                        "publisher", "contributor", "date", "type", "format",
                        "identifier", "source", "language", "relation",
                        "coverage", "rights"};
        for (String element : temp) {
            multivalued.add(element);
        }
    }

    private static final boolean isMultivalued(String name) {
        return multivalued.contains(name);
    }

    protected static final HashSet<String> allFields =
            new HashSet<String>(multivalued);
    static {
        String[] temp =
                {"pid", "label", "state", "ownerId", "cDate", "mDate",
                        "dcmDate"};
        for (String element : temp) {
            allFields.add(element);
        }
    }

    protected static final HashSet<String> parms = new HashSet<String>();
    static {
        String[] temp = {"sessionToken", "maxResults", "newBase"};
        for (String element : temp) {
            parms.add(element);
        }
    }

    private static final HashSet<String> needsEnc = new HashSet<String>();
    static {
        String[] temp = {"label", "cModel"};
        for (String element : temp) {
            needsEnc.add(element);
        }
    }

    private static final int MAXRESULTS = Integer.MAX_VALUE;

    private String[] fieldsArray = null;

    private String query = null;

    private int maxResults = MAXRESULTS;

    private String sessionToken = null;

    private String reportName = null;

    private String xslt = null;

    private int newBase = 0;

    private String prefix = null;

    private String dateRange = null;

    private FieldSearchResult fsr = null;

    private static final String OBJECTS = "all objects";

    private static final String ACTIVEOBJECTS = "active objects";

    private static final String INACTIVEOBJECTS = "inactive objects";

    protected static final String HTMLFORM = "htmlform";

    private static final String FORM_TITLE = "Repository Reports";

    private static final String FORM_SUBTITLE = "select a report";

    private static final String REPORT_TITLE = "Report on Repository";

    private static final HashSet<String> reportNames = new HashSet<String>();
    static {
        final String[] temp =
                {HTMLFORM, OBJECTS, ACTIVEOBJECTS, INACTIVEOBJECTS};
        for (String element : temp) {
            reportNames.add(element);
        }
    }

    private static final boolean isPredefinedReport(String name) {
        return reportNames.contains(name);
    }

    private static final String OBJECTSQUERY = "";

    private static final String[] OBJECTSFIELDSARRAY =
            {"mDate", "pid", "label", "state"};

    private static final String ACTIVEOBJECTSQUERY = "state='A'";

    private static final String[] ACTIVEOBJECTSFIELDSARRAY =
            {"mDate", "pid", "label"};

    private static final String INACTIVEOBJECTSQUERY = "state='I'";

    private static final String[] INACTIVEOBJECTSFIELDSARRAY =
            {"mDate", "pid", "label"};


    private static final Hashtable<String, String> queries =
            new Hashtable<String, String>();
    static {
        queries.put(OBJECTS, OBJECTSQUERY);
        queries.put(ACTIVEOBJECTS, ACTIVEOBJECTSQUERY);
        queries.put(INACTIVEOBJECTS, INACTIVEOBJECTSQUERY);
    }

    private static final String getQuery(String name) {
        return queries.get(name);
    }

    private static final Hashtable<String, String[]> fieldArrays =
            new Hashtable<String, String[]>();
    static {
        fieldArrays.put(OBJECTS, OBJECTSFIELDSARRAY);
        fieldArrays.put(ACTIVEOBJECTS, ACTIVEOBJECTSFIELDSARRAY);
        fieldArrays.put(INACTIVEOBJECTS, INACTIVEOBJECTSFIELDSARRAY);
    }

    private static final String[] getFieldsArray(String name) {
        return fieldArrays.get(name);
    }

    private static final int UNKNOWN = 0;

    private static final int HTMLFORM_ONLY = 1;

    private static final int PREDEFINED_REPORT = 2;

    private static final int ADHOC_REPORT = 3;

    private static final int CONTINUED_REPORT = 4;

    private static final String NONE = "none";

    private static final String CREATED_LT_24_HRS_AGO = "cltd";

    private static final String MODIFIED_LT_24_HRS_AGO = "mltd";

    private static final String CREATED_GT_24_HRS_AGO = "cgtd";

    private static final String MODIFIED_GT_24_HRS_AGO = "mgtd";

    private static final String CREATED_LT_1_WK_AGO = "cltw";

    private static final String MODIFIED_LT_1_WK_AGO = "mltw";

    private static final String CREATED_GT_1_WK_AGO = "cgtw";

    private static final String MODIFIED_GT_1_WK_AGO = "mgtw";

    private static final String CREATED_LT_1_MO_AGO = "cltm";

    private static final String MODIFIED_LT_1_MO_AGO = "mltm";

    private static final String CREATED_GT_1_MO_AGO = "cgtm";

    private static final String MODIFIED_GT_1_MO_AGO = "mgtm";

    private static final String CREATED_LT_1_YR_AGO = "clty";

    private static final String MODIFIED_LT_1_YR_AGO = "mlty";

    private static final String CREATED_GT_1_YR_AGO = "cgty";

    private static final String MODIFIED_GT_1_YR_AGO = "mgty";

    private static final Hashtable<String, String> dateRangeLabels;
    static {
        Hashtable<String, String> t = new Hashtable<String, String>();
        t.put(NONE, "(regardless of when created or last modified)");
        t.put(CREATED_LT_24_HRS_AGO, "created within past 24 hours");
        t.put(MODIFIED_LT_24_HRS_AGO, "last modified within past 24 hours");
        t.put(CREATED_GT_24_HRS_AGO, "created more than 24 hours ago");
        t.put(MODIFIED_GT_24_HRS_AGO, "last modified more than 24 hours ago");

        t.put(CREATED_LT_1_WK_AGO, "created within past 7 days");
        t.put(MODIFIED_LT_1_WK_AGO, "last modified within past 7 days");
        t.put(CREATED_GT_1_WK_AGO, "created more than 7 days ago");
        t.put(MODIFIED_GT_1_WK_AGO, "last modified more than 7 days ago");

        t.put(CREATED_LT_1_MO_AGO, "created within past 30 days");
        t.put(MODIFIED_LT_1_MO_AGO, "last modified within past 30 days");
        t.put(CREATED_GT_1_MO_AGO, "created more than 30 days ago");
        t.put(MODIFIED_GT_1_MO_AGO, "last modified more than 30 days ago");

        t.put(CREATED_LT_1_YR_AGO, "created within past 1 year");
        t.put(MODIFIED_LT_1_YR_AGO, "last modified within past 1 year");
        t.put(CREATED_GT_1_YR_AGO, "created more than 1 year ago");
        t.put(MODIFIED_GT_1_YR_AGO, "last modified more than 1 year ago");
        dateRangeLabels = t;
    }

    int requestType = UNKNOWN;

    private static final long MILLISECS_IN_DAY = 1000 * 60 * 60 * 24;

    private Report(Context context,
                   String _reportName,
                   String _xslt,
                   String[] _fieldsArray,
                   String _query,
                   String _remoteAddr,
                   String _maxResults,
                   String _sessionToken,
                   String _newBase,
                   String _prefix,
                   String _dateRange)
            throws QueryParseException, ServerException {
        try {
            s_server = Server.getInstance(new File(Constants.FEDORA_HOME));
            s_access =
                    (Access) s_server.getModule("fedora.server.access.Access");
        } catch (InitializationException ie) {
            throw new GeneralException("Error getting Fedora Server instance: "
                    + ie.getMessage());
        }
        reportName = _reportName;
        if (_sessionToken != null) {
            if (_newBase == null) {
                throw new GeneralException("new base missing");
            }
            sessionToken = _sessionToken;
            try {
                fieldsArray = getFieldsArray(reportName);
            } catch (NullPointerException e) {
                fieldsArray = _fieldsArray;
                if (_fieldsArray == null) {
                    throw new GeneralException("fields array missing");
                }
            }
            prefix = _prefix;
            dateRange = _dateRange;
            newBase = Integer.parseInt(_newBase);
            requestType = CONTINUED_REPORT;
        } else if (isPredefinedReport(reportName)) {
            fieldsArray = getFieldsArray(reportName);
            query = getQuery(reportName);
            prefix = _prefix;
            dateRange = _dateRange;

            if (query != null && !"".equals(query)) {
                query += " ";
            }
            if (prefix == null || "".equals(prefix)) {
                query += "pid~*";
            } else {
                query += "pid~" + prefix + ":*";
            }

            if (dateRange != null && dateRange != "" && dateRange.length() != 4) {
                throw new GeneralException("bad date range a " + dateRange);
            }

            if (dateRange != null && !"none".equals(dateRange)) {
                String op = "";
                String st = dateRange.substring(1, 3);
                if ("lt".equals(st)) {
                    op = ">=";
                } else if ("gt".equals(st)) {
                    op = "<";
                } else {
                    throw new GeneralException("bad date range b");
                }

                String field = "";
                char ch = dateRange.charAt(0);
                switch (ch) {
                    case 'c':
                        field = "cDate";
                        break;
                    case 'm':
                        field = "mDate";
                        break;
                    default:
                        throw new GeneralException("bad date range c");
                }

                long days;
                ch = dateRange.charAt(3);
                switch (ch) {
                    case 'd':
                        days = 1;
                        break;
                    case 'w':
                        days = 7;
                        break;
                    case 'm':
                        days = 30;
                        break;
                    case 'y':
                        days = 365;
                        break;
                    default:
                        throw new GeneralException("bad date range d");
                }

                long compTime =
                        (new Date()).getTime() - days * MILLISECS_IN_DAY;
                DateFormat df =
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                String compString = df.format(new Date(compTime));
                if (dateRange != null && !"".equals(dateRange)) {
                    query += " " + field + op + compString;
                }
            }
            requestType = PREDEFINED_REPORT;

        } else if ((reportName == null || "".equals(reportName))
                && _fieldsArray == null && _query == null) {
            requestType = HTMLFORM_ONLY;
        } else if (_fieldsArray != null && _query != null) {
            requestType = ADHOC_REPORT;
            fieldsArray = _fieldsArray;
            query = _query;
        } else {
            throw new GeneralException("parameters don't agree");
        }

        if ("".equals(_xslt)) { // override default with no transform at all
        } else if (_xslt != null) { // use stated
            xslt = xslts.get(_xslt);
        } else { // use default transform for this report
            switch (requestType) {
                case HTMLFORM_ONLY:
                    xslt = REQUEST_HTML_XSLT;
                    break;
                default:
                    if (OBJECTS.equals(reportName)) {
                        xslt = HTML_XSLT;
                    } else {
                        xslt = HTML_XSLT;
                    }
            }
        }

        switch (requestType) {
            case PREDEFINED_REPORT:
            case ADHOC_REPORT:
            case CONTINUED_REPORT:
                try {
                    if ("*".equals(_maxResults)) {
                        maxResults = Integer.MAX_VALUE;
                    } else {
                        maxResults = Integer.parseInt(_maxResults);
                    }
                } catch (NullPointerException npe) {
                } catch (NumberFormatException nfe) {
                }
                if (sessionToken != null) {
                    fsr = s_access.resumeFindObjects(context, sessionToken);
                } else {
                    fsr =
                            s_access
                                    .findObjects(context,
                                                 fieldsArray,
                                                 maxResults,
                                                 new FieldSearchQuery(Condition
                                                         .getConditions(query)));
                }
                if (fsr == null) {
                    throw new GeneralException("no fsr");
                }
                break;
        }
    }

    protected static final Report getInstance(Context context,
                                              String remoteAddr,
                                              String sessionToken,
                                              String reportName,
                                              String xslt,
                                              String maxResults,
                                              String newBase,
                                              String prefix,
                                              String dateRange)
            throws QueryParseException, ServerException {
        return getInstance(context,
                           remoteAddr,
                           sessionToken,
                           reportName,
                           null,
                           null,
                           xslt,
                           maxResults,
                           newBase,
                           prefix,
                           dateRange);
    }

    private static final String XSLT_DIR = "access";

    protected static final String REQUEST_HTML_XSLT = "reportRequestHtml.xslt";

    protected static final String HTML_XSLT = "reportHtml.xslt";

    protected static final String XML_XSLT = "reportXml.xslt";

    private static final Hashtable<String, String> xslts =
            new Hashtable<String, String>();
    static {
        xslts.put("REQUEST_HTML_XSLT", REQUEST_HTML_XSLT);
        xslts.put("HTML_XSLT", HTML_XSLT);
        xslts.put("XML_XSLT", XML_XSLT);
    }

    protected final String getContentType() {
        String contentType = "text/xml";
        if (REQUEST_HTML_XSLT.equals(xslt) || HTML_XSLT.equals(xslt)) {
            contentType = "text/html";
        }
        return contentType;
    }

    protected static final Report getInstance(Context context,
                                              String _remoteAddr,
                                              String _sessionToken,
                                              String _reportName,
                                              String[] _fieldsArray,
                                              String _query,
                                              String _xslt,
                                              String _maxResults,
                                              String newBase,
                                              String prefix,
                                              String dateRange)
            throws QueryParseException, ServerException {
        Report report =
                new Report(context,
                           _reportName,
                           _xslt,
                           _fieldsArray,
                           _query,
                           _remoteAddr,
                           _maxResults,
                           _sessionToken,
                           newBase,
                           prefix,
                           dateRange);
        return report;
    }

    private static final void putOr(Hashtable<String, String> hashtable,
                                    String key,
                                    String value) {
        if (value != null) {
            hashtable.put(key, value);
        }
    }

    protected final void writeOut(OutputStream ultOut)
            throws QueryParseException, ServerException, IOException,
            TransformerException { // PrintWriter
        PrintWriter out = null;
        Writer x = null;
        if (xslt == null) {
            out = new PrintWriter(new OutputStreamWriter(ultOut, "UTF-8"));
            x = new DirectWriter(out);
        } else {
            Hashtable<String, String> params = new Hashtable<String, String>();
            params.put("REQUEST-TYPE", Integer.toString(requestType));
            switch (requestType) {
                case HTMLFORM_ONLY:
                    putOr(params, "GENERAL-TITLE", FORM_TITLE);
                    putOr(params, "SPECIFIC-TITLE", FORM_SUBTITLE);
                    break;
                case PREDEFINED_REPORT:
                case ADHOC_REPORT:
                case CONTINUED_REPORT:
                    String viewingStart = null;
                    String viewingEnd = null;
                    String newToken = fsr.getToken();
                    if (fsr.objectFieldsList().size() <= 0) {
                        viewingStart = "0";
                        viewingEnd = "0";
                    } else {
                        viewingStart = Long.toString(newBase + 1);
                        viewingEnd =
                                Long.toString(newBase
                                        + fsr.objectFieldsList().size());
                        if (newToken != null && !"".equals(newToken)) {
                            newBase += fsr.objectFieldsList().size();
                        }
                    }
                    putOr(params, "GENERAL-TITLE", REPORT_TITLE);
                    putOr(params, "SPECIFIC-TITLE", reportName);
                    putOr(params, "FIELDARRAY-LENGTH", Integer
                            .toString(fieldsArray.length));
                    putOr(params, "VIEWINGSTART", viewingStart);
                    putOr(params, "VIEWINGEND", viewingEnd);
                    putOr(params, "MAXRESULTS", Integer.toString(maxResults));
                    putOr(params, "REPORTNAME", reportName);
                    putOr(params, "SESSIONTOKEN", newToken);
                    putOr(params, "NEWBASE", Integer.toString(newBase));
                    putOr(params, "PREFIX", prefix);
                    putOr(params, "DATERANGE", dateRange);
                    if (dateRange != null && !"".equals(dateRange)) {
                        putOr(params,
                              "DATERANGELABEL",
                              dateRangeLabels.get(dateRange));
                    } else {
                        putOr(params, "DATERANGELABEL", "");
                    }
                    break;
            }
            out = new PrintWriter(new OutputStreamWriter(ultOut, "UTF-8"));
            x = new InMemoryWriter(out, xslt, params);
        }

        StringBuffer outBuf = x.getStringBuffer();
        outBuf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        switch (requestType) {
            case HTMLFORM_ONLY:
                outBuf.append("<X />\n");
                x.flush();
                break;
            case PREDEFINED_REPORT:
            case ADHOC_REPORT:
            case CONTINUED_REPORT:
                outBuf.append("<result>\n");

                outBuf.append("\t<listSession>\n");
                if (fsr.getCursor() != -1) {
                    outBuf.append("\t\t<cursor>" + fsr.getCursor()
                            + "</cursor>\n");
                }
                if (fsr.getToken() != null) {
                    outBuf
                            .append("\t\t<token>" + fsr.getToken()
                                    + "</token>\n");
                }
                if (fsr.getCompleteListSize() != -1) {
                    outBuf.append("\t\t<completeListSize>"
                            + fsr.getCompleteListSize()
                            + "</completeListSize>\n");
                } else if (maxResults == Integer.MAX_VALUE) {
                    outBuf.append("\t\t<completeListSize>"
                            + fsr.objectFieldsList().size()
                            + "</completeListSize>\n");
                }
                if (fsr.getExpirationDate() != null) {
                    outBuf
                            .append("\t\t<expirationDate>"
                                    + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                            .format(fsr.getExpirationDate())
                                    + "</expirationDate>\n");
                }
                outBuf.append("\t</listSession>\n");

                outBuf.append("\t<fieldNames>\n");
                for (String element : fieldsArray) {
                    outBuf.append("\t\t<fieldName>" + element
                            + "</fieldName>\n");
                }
                outBuf.append("\t</fieldNames>\n");
                outBuf.append("\t<resultList>\n");
                x.flush();
                if ("text/html".equals(getContentType())) {
                    //header fields
                }
                List<ObjectFields> searchResults = fsr.objectFieldsList();
                for (int i = 0; i < searchResults.size(); i++) {
                    ObjectFields f = searchResults.get(i);
                    outBuf.append("\t\t<objectFields>\n");
                    for (String name : fieldsArray) {
                        if (isMultivalued(name)) {
                            appendXML(name, getFieldValues(f, name), outBuf);
                        } else {
                            appendXML(name, getFieldValue(f, name), outBuf);
                        }
                    }
                    outBuf.append("\t\t</objectFields>\n");
                    x.flush();
                }
                outBuf.append("\t</resultList>");
                outBuf.append("</result>");
                break;
            default:
                throw new GeneralException("requestType out-of-bounds");
        }
        x.flush();
        x.close();
    }

    private static void appendXML(String name, String value, StringBuffer out) {
        if (value != null) {
            out.append("\t\t\t<" + name + ">" + StreamUtility.enc(value) + "</"
                    + name + ">\n");
        }
    }

    private static void appendXML(String name, List values, StringBuffer out) {
        for (int i = 0; i < values.size(); i++) {
            appendXML(name, (String)values.get(i), out);
        }
    }

    abstract class Writer {

        PrintWriter out = null;

        StringBuffer buffer = null;

        public StringBuffer getStringBuffer() {
            return buffer;
        }

        public void flush() {
            //default, no op
        }

        public void close() {
            buffer = null;
        }

    }

    abstract class TransformerWriter
            extends Writer {

        String xslt = null;

        Transformer transformer = null;

        Hashtable<String, String> params = null;

        protected void xform(Source source) throws TransformerException {
            TransformerFactory factory = XmlTransformUtility.getTransformerFactory();
            transformer =
                    factory.newTransformer(new StreamSource(s_server
                            .getHomeDir()
                            + "/" + XSLT_DIR + "/" + xslt));
            Enumeration<String> keys = params.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String value = params.get(key);
                transformer.setParameter(key, value);
            }
            transformer.transform(source, new StreamResult(out));
        }

    }

    class DirectWriter
            extends Writer {

        PrintWriter out = null;

        DirectWriter(PrintWriter out) {
            this.out = out;
            buffer = new StringBuffer();
        }

        @Override
        public void flush() {
            out.print(buffer.toString());
            buffer.setLength(0);
        }

    }

    class InMemoryWriter
            extends TransformerWriter {

        InMemoryWriter(PrintWriter out, String xslt, Hashtable<String, String> params) {
            this.out = out;
            this.xslt = xslt;
            this.params = params;
            buffer = new StringBuffer();
        }

        @Override
        public void close() {
            StringReader sr = new StringReader(buffer.toString());
            StreamSource streamSource = new StreamSource(sr);
            try {
                xform(streamSource);
            } catch (TransformerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            super.close();
        }
    }

}
