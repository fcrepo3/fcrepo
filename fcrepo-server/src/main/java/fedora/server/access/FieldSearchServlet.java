/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.InitializationException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.errors.servletExceptionExtensions.BadRequest400Exception;
import fedora.server.errors.servletExceptionExtensions.InternalError500Exception;
import fedora.server.errors.servletExceptionExtensions.RootException;
import fedora.server.search.Condition;
import fedora.server.search.FieldSearchQuery;
import fedora.server.search.FieldSearchResult;
import fedora.server.search.ObjectFields;
import fedora.server.utilities.DCField;
import fedora.server.utilities.StreamUtility;

/**
 * REST interface for API-A's FieldSearch functionality.
 *
 * @author Chris Wilper
 */
public class FieldSearchServlet
        extends HttpServlet
        implements Constants {

    private static final long serialVersionUID = 1L;

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(FieldSearchServlet.class);

    /** Instance of the Server */
    private static Server s_server = null;

    /** Instance of the access subsystem */
    private static Access s_access = null;

    private String[] getFieldsArray(HttpServletRequest req) {
        ArrayList<String> l = new ArrayList<String>();
        if (req.getParameter("pid") != null
                && req.getParameter("pid").equalsIgnoreCase("true")) {
            l.add("pid");
        }
        if (req.getParameter("label") != null
                && req.getParameter("label").equalsIgnoreCase("true")) {
            l.add("label");
        }
        if (req.getParameter("state") != null
                && req.getParameter("state").equalsIgnoreCase("true")) {
            l.add("state");
        }
        if (req.getParameter("ownerId") != null
                && req.getParameter("ownerId").equalsIgnoreCase("true")) {
            l.add("ownerId");
        }
        if (req.getParameter("cDate") != null
                && req.getParameter("cDate").equalsIgnoreCase("true")) {
            l.add("cDate");
        }
        if (req.getParameter("mDate") != null
                && req.getParameter("mDate").equalsIgnoreCase("true")) {
            l.add("mDate");
        }
        if (req.getParameter("dcmDate") != null
                && req.getParameter("dcmDate").equalsIgnoreCase("true")) {
            l.add("dcmDate");
        }
        if (req.getParameter("title") != null
                && req.getParameter("title").equalsIgnoreCase("true")) {
            l.add("title");
        }
        if (req.getParameter("creator") != null
                && req.getParameter("creator").equalsIgnoreCase("true")) {
            l.add("creator");
        }
        if (req.getParameter("subject") != null
                && req.getParameter("subject").equalsIgnoreCase("true")) {
            l.add("subject");
        }
        if (req.getParameter("description") != null
                && req.getParameter("description").equalsIgnoreCase("true")) {
            l.add("description");
        }
        if (req.getParameter("publisher") != null
                && req.getParameter("publisher").equalsIgnoreCase("true")) {
            l.add("publisher");
        }
        if (req.getParameter("contributor") != null
                && req.getParameter("contributor").equalsIgnoreCase("true")) {
            l.add("contributor");
        }
        if (req.getParameter("date") != null
                && req.getParameter("date").equalsIgnoreCase("true")) {
            l.add("date");
        }
        if (req.getParameter("type") != null
                && req.getParameter("type").equalsIgnoreCase("true")) {
            l.add("type");
        }
        if (req.getParameter("format") != null
                && req.getParameter("format").equalsIgnoreCase("true")) {
            l.add("format");
        }
        if (req.getParameter("identifier") != null
                && req.getParameter("identifier").equalsIgnoreCase("true")) {
            l.add("identifier");
        }
        if (req.getParameter("source") != null
                && req.getParameter("source").equalsIgnoreCase("true")) {
            l.add("source");
        }
        if (req.getParameter("language") != null
                && req.getParameter("language").equalsIgnoreCase("true")) {
            l.add("language");
        }
        if (req.getParameter("relation") != null
                && req.getParameter("relation").equalsIgnoreCase("true")) {
            l.add("relation");
        }
        if (req.getParameter("coverage") != null
                && req.getParameter("coverage").equalsIgnoreCase("true")) {
            l.add("coverage");
        }
        if (req.getParameter("rights") != null
                && req.getParameter("rights").equalsIgnoreCase("true")) {
            l.add("rights");
        }
        String[] ret = new String[l.size()];
        for (int i = 0; i < l.size(); i++) {
            ret[i] = l.get(i);
        }
        return ret;
    }

    public static final String ACTION_LABEL = "Field Search";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String actionLabel = "Field Search";
        try {
            Context context =
                    ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                                               request);

            String[] fieldsArray = getFieldsArray(request);
            HashSet<String> fieldHash = new HashSet<String>();
            if (fieldsArray != null) {
                for (String element : fieldsArray) {
                    fieldHash.add(element);
                }
            }
            String terms = request.getParameter("terms");
            String query = request.getParameter("query");

            String sessionToken = request.getParameter("sessionToken");

            // default to 25 if not specified or specified incorrectly
            int maxResults = 25;
            if (request.getParameter("maxResults") != null) {
                try {
                    maxResults =
                            Integer
                                    .parseInt(request
                                            .getParameter("maxResults"));
                } catch (NumberFormatException nfe) {
                    LOG.error("Bad request (maxResults not an integer)", nfe);
                    throw new BadRequest400Exception(request,
                                                     ACTION_LABEL,
                                                     "",
                                                     new String[0]);
                }
            }

            String xmlOutput = request.getParameter("xml");
            boolean xml = false;
            if (xmlOutput != null
                    && (xmlOutput.toLowerCase().startsWith("t") || xmlOutput
                            .toLowerCase().startsWith("y"))) {
                xml = true;
            }
            StringBuffer xmlBuf = new StringBuffer();
            StringBuffer html = new StringBuffer();
            if (!xml) {
                html.append("<form method=\"post\" action=\"search\">");
                html
                        .append("<center><table border=0 cellpadding=6 cellspacing=0>\n");
                html
                        .append("<tr><td colspan=3 valign=top><i>Fields to display:</i></td><td></td></tr>");
                html.append("<tr><td valign=top><font size=-1>");
                html
                        .append("<input type=\"checkbox\" name=\"pid\" value=\"true\" checked> <a href=\"#\" onClick=\"javascript:alert('Persistent Identfier\\n\\nThe globally unique identifier of the resource.')\">pid</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"label\" value=\"true\""
                                + (fieldHash.contains("label") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Label\\n\\nThe label of the object')\">label</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"state\" value=\"true\""
                                + (fieldHash.contains("state") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('State\\n\\nThe state of the object.\\nThis will be:\\n  A - Active')\">state</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"ownerId\" value=\"true\""
                                + (fieldHash.contains("ownerId") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Owner Id\\n\\nThe userId of the user who owns the object.')\">ownerId</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"cDate\" value=\"true\""
                                + (fieldHash.contains("cDate") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Creation Date\\n\\nThe UTC date the object was created,\\nin YYYY-MM-DDTHH:MM:SS.SSSZ format')\">cDate</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"mDate\" value=\"true\""
                                + (fieldHash.contains("mDate") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Modified Date\\n\\nThe UTC date the object was last modified,\\nin YYYY-MM-DDTHH:MM:SS.SSSZ format')\">mDate</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"dcmDate\" value=\"true\""
                                + (fieldHash.contains("dcmDate") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Dublin Core Modified Date\\n\\nThe UTC date the DC datastream was last modified,\\nin YYYY-MM-DDTHH:MM:SS.SSSZ format')\">dcmDate</a><br>");
                html.append("</font></td><td valign=top><font size=-1>");
                html
                        .append("<input type=\"checkbox\" name=\"title\" value=\"true\" checked> <a href=\"#\" onClick=\"javascript:alert('Title\\n\\nA name given to the resource.\\nThis is a repeating field.')\">title</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"creator\" value=\"true\""
                                + (fieldHash.contains("creator") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Creator\\n\\nAn entity primarily responsible for making\\nthe content of the resource.\\nThis is a repeating field.')\">creator</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"subject\" value=\"true\""
                                + (fieldHash.contains("subject") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Subject and Keywords\\n\\nA topic of the content of the resource.\\nThis is a repeating field.')\">subject</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"description\" value=\"true\""
                                + (fieldHash.contains("description") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Description\\n\\nAn account of the content of the resource.\\nThis is a repeating field.')\">description</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"publisher\" value=\"true\""
                                + (fieldHash.contains("publisher") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Publisher\\n\\nAn entity responsible for making the resource available.\\nThis is a repeating field.')\">publisher</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"contributor\" value=\"true\""
                                + (fieldHash.contains("contributor") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Contributor\\n\\nAn entity responsible for making contributions\\nto the content of the resource.\\nThis is a repeating field.')\">contributor</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"date\" value=\"true\""
                                + (fieldHash.contains("date") ? " checked" : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Date\\n\\nA date of an event in the lifecycle of the resource.\\nThis is a repeating field.')\">date</a><br>");
                html.append("</font></td><td valign=top><font size=-1>");
                html
                        .append("<input type=\"checkbox\" name=\"type\" value=\"true\""
                                + (fieldHash.contains("type") ? " checked" : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Resource Type\\n\\nThe nature or genre of the resource.\\nThis is a repeating field.')\">type</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"format\" value=\"true\""
                                + (fieldHash.contains("format") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Format\\n\\nThe physical or digital manifestation of the resource.\\nThis is a repeating field.')\">format</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"identifier\" value=\"true\""
                                + (fieldHash.contains("identifier") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Resource Identifier\\n\\nAn unambiguous reference to the resource within a given context.\\nThis is a repeating field.')\">identifier</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"source\" value=\"true\""
                                + (fieldHash.contains("source") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Source\\n\\nA reference to a resource from which the present resource is derived.\\nThis is a repeating field.')\">source</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"language\" value=\"true\""
                                + (fieldHash.contains("language") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Language\\n\\nA language of the intellectual content of the resource.\\nThis is a repeating field.')\">language</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"relation\" value=\"true\""
                                + (fieldHash.contains("relation") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Relation\\n\\nA reference to a related resource.\\nThis is a repeating field.')\">relation</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"coverage\" value=\"true\""
                                + (fieldHash.contains("coverage") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Coverage\\n\\nThe extent or scope of the content of the resource.\\nThis is a repeating field.')\">coverage</a><br>");
                html
                        .append("<input type=\"checkbox\" name=\"rights\" value=\"true\""
                                + (fieldHash.contains("rights") ? " checked"
                                        : "")
                                + "> <a href=\"#\" onClick=\"javascript:alert('Rights Management\\n\\nInformation about rights held in and over the resource.\\nThis is a repeating field.')\">rights</a><br>");
                html
                        .append("</font></td><td bgcolor=silver valign=top>&nbsp;&nbsp;&nbsp;</td><td valign=top>");
                html
                        .append("Search all fields for phrase: <input type=\"text\" name=\"terms\" size=\"15\" value=\""
                                + (terms == null ? "" : StreamUtility
                                        .enc(terms))
                                + "\"> <a href=\"#\" onClick=\"javascript:alert('Search All Fields\\n\\nEnter a phrase.  Objects where any field contains the phrase will be returned.\\nThis is a case-insensitive search, and you may use the * or ? wildcards.\\n\\nExamples:\\n\\n  *o*\\n    finds objects where any field contains the letter o.\\n\\n  ?edora\\n    finds objects where a word starts with any letter and ends with edora.')\"><i>help</i></a><p> ");
                html
                        .append("Or search specific field(s): <input type=\"text\" name=\"query\" size=\"15\" value=\""
                                + (query == null ? "" : StreamUtility
                                        .enc(query))
                                + "\"> <a href=\"#\" onClick=\"javascript:alert('Search Specific Field(s)\\n\\nEnter one or more conditions, separated by space.  Objects matching all conditions will be returned.\\nA condition is a field (choose from the field names on the left) followed by an operator, followed by a value.\\nThe = operator will match if the field\\'s entire value matches the value given.\\nThe ~ operator will match on phrases within fields, and accepts the ? and * wildcards.\\nThe &lt;, &gt;, &lt;=, and &gt;= operators can be used with numeric values, such as dates.\\n\\nExamples:\\n\\n  pid~demo:* description~fedora\\n    Matches all demo objects with a description containing the word fedora.\\n\\n  cDate&gt;=1976-03-04 creator~*n*\\n    Matches objects created on or after March 4th, 1976 where at least one of the creators has an n in their name.\\n\\n  mDate&gt;2002-10-2 mDate&lt;2002-10-2T12:00:00\\n    Matches objects modified sometime before noon (UTC) on October 2nd, 2002')\"><i>help</i></a><p> ");
                html
                        .append("Maximum Results: <select name=\"maxResults\"><option value=\"20\">20</option><option value=\"40\">40</option><option value=\"60\">60</option><option value=\"80\">80</option></select> ");
                html.append("<p><input type=\"submit\" value=\"Search\"> ");
                html.append("</td></tr></table></center>");
                html.append("</form><hr size=1>");
            }
            FieldSearchResult fsr = null;
            if (fieldsArray != null && fieldsArray.length > 0
                    || sessionToken != null) {
                if (sessionToken != null) {
                    fsr = s_access.resumeFindObjects(context, sessionToken);
                } else {
                    if (terms != null && terms.length() != 0) {
                        fsr =
                                s_access
                                        .findObjects(context,
                                                     fieldsArray,
                                                     maxResults,
                                                     new FieldSearchQuery(terms));
                    } else {
                        fsr =
                                s_access
                                        .findObjects(context,
                                                     fieldsArray,
                                                     maxResults,
                                                     new FieldSearchQuery(Condition
                                                             .getConditions(query)));
                    }
                }
                List<ObjectFields> searchResults = fsr.objectFieldsList();
                if (!xml) {
                    html
                            .append("<center><table width=\"90%\" border=\"1\" cellpadding=\"5\" cellspacing=\"5\" bgcolor=\"silver\">\n");
                    html.append("<tr>");
                    for (String element : fieldsArray) {
                        html
                                .append("<td valign=\"top\"><strong>");
                        html.append(element);
                        html.append("</strong></td>");
                    }
                    html.append("</tr>");
                }
                SimpleDateFormat formatter =
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                for (int i = 0; i < searchResults.size(); i++) {
                    ObjectFields f = searchResults.get(i);
                    if (xml) {
                        xmlBuf.append("  <objectFields>\n");
                        appendXML("pid", f.getPid(), xmlBuf);
                        appendXML("label", f.getLabel(), xmlBuf);
                        appendXML("state", f.getState(), xmlBuf);
                        appendXML("ownerId", f.getOwnerId(), xmlBuf);
                        appendXML("cDate", f.getCDate(), formatter, xmlBuf);
                        appendXML("mDate", f.getMDate(), formatter, xmlBuf);
                        appendXML("dcmDate", f.getDCMDate(), formatter, xmlBuf);
                        appendXML("title", f.titles(), xmlBuf);
                        appendXML("creator", f.creators(), xmlBuf);
                        appendXML("subject", f.subjects(), xmlBuf);
                        appendXML("description", f.descriptions(), xmlBuf);
                        appendXML("publisher", f.publishers(), xmlBuf);
                        appendXML("contributor", f.contributors(), xmlBuf);
                        appendXML("date", f.dates(), xmlBuf);
                        appendXML("type", f.types(), xmlBuf);
                        appendXML("format", f.formats(), xmlBuf);
                        appendXML("identifier", f.identifiers(), xmlBuf);
                        appendXML("source", f.sources(), xmlBuf);
                        appendXML("language", f.languages(), xmlBuf);
                        appendXML("relation", f.relations(), xmlBuf);
                        appendXML("coverage", f.coverages(), xmlBuf);
                        appendXML("rights", f.rights(), xmlBuf);
                        xmlBuf.append("  </objectFields>\n");
                    } else {
                        html.append("<tr>");
                        for (String l : fieldsArray) {
                            html.append("<td valign=\"top\">");
                            if (l.equalsIgnoreCase("pid")) {
                                html.append("<a href=\"get/");
                                html.append(f.getPid());
                                html.append("\">");
                                html.append(f.getPid());
                                html.append("</a>");
                            } else if (l.equalsIgnoreCase("label")) {
                                if (f.getLabel() != null) {
                                    html
                                            .append(StreamUtility.enc(f
                                                    .getLabel()));
                                }
                            } else if (l.equalsIgnoreCase("state")) {
                                html.append(f.getState());
                            } else if (l.equalsIgnoreCase("ownerId")) {
                                if (f.getOwnerId() != null) {
                                    html.append(f.getOwnerId());
                                }
                            } else if (l.equalsIgnoreCase("cDate")) {
                                html.append(formatter.format(f.getCDate()));
                            } else if (l.equalsIgnoreCase("mDate")) {
                                html.append(formatter.format(f.getMDate()));
                            } else if (l.equalsIgnoreCase("dcmDate")) {
                                if (f.getDCMDate() != null) {
                                    html.append(formatter
                                            .format(f.getDCMDate()));
                                }
                            } else if (l.equalsIgnoreCase("title")) {
                                html.append(getList(f.titles()));
                            } else if (l.equalsIgnoreCase("creator")) {
                                html.append(getList(f.creators()));
                            } else if (l.equalsIgnoreCase("subject")) {
                                html.append(getList(f.subjects()));
                            } else if (l.equalsIgnoreCase("description")) {
                                html.append(getList(f.descriptions()));
                            } else if (l.equalsIgnoreCase("publisher")) {
                                html.append(getList(f.publishers()));
                            } else if (l.equalsIgnoreCase("contributor")) {
                                html.append(getList(f.contributors()));
                            } else if (l.equalsIgnoreCase("date")) {
                                html.append(getList(f.dates()));
                            } else if (l.equalsIgnoreCase("type")) {
                                html.append(getList(f.types()));
                            } else if (l.equalsIgnoreCase("format")) {
                                html.append(getList(f.formats()));
                            } else if (l.equalsIgnoreCase("identifier")) {
                                html.append(getList(f.identifiers()));
                            } else if (l.equalsIgnoreCase("source")) {
                                html.append(getList(f.sources()));
                            } else if (l.equalsIgnoreCase("language")) {
                                html.append(getList(f.languages()));
                            } else if (l.equalsIgnoreCase("relation")) {
                                html.append(getList(f.relations()));
                            } else if (l.equalsIgnoreCase("coverage")) {
                                html.append(getList(f.coverages()));
                            } else if (l.equalsIgnoreCase("rights")) {
                                html.append(getList(f.rights()));
                            }
                            html.append("</td>");
                        }
                        html.append("</tr>");
                        html.append("<tr><td colspan=\"");
                        html.append(fieldsArray.length);
                        html.append("\"></td></tr>");
                    }
                }
                if (!xml) {
                    html.append("</table>");
                    if (fsr != null && fsr.getToken() != null) {
                        if (fsr.getCursor() != -1) {
                            long viewingStart = fsr.getCursor() + 1;
                            long viewingEnd =
                                    fsr.objectFieldsList().size()
                                            + viewingStart - 1;
                            html.append("<p>Viewing results " + viewingStart
                                    + " to " + viewingEnd);
                            if (fsr.getCompleteListSize() != -1) {
                                html.append(" of " + fsr.getCompleteListSize());
                            }
                            html.append("</p>\n");
                        }
                        html.append("<form method=\"post\" action=\"search\">");
                        if (fieldHash.contains("pid")) {
                            html
                                    .append("<input type=\"hidden\" name=\"pid\" value=\"true\">");
                        }
                        if (fieldHash.contains("label")) {
                            html
                                    .append("<input type=\"hidden\" name=\"label\" value=\"true\">");
                        }
                        if (fieldHash.contains("state")) {
                            html
                                    .append("<input type=\"hidden\" name=\"state\" value=\"true\">");
                        }
                        if (fieldHash.contains("ownerId")) {
                            html
                                    .append("<input type=\"hidden\" name=\"ownerId\" value=\"true\">");
                        }
                        if (fieldHash.contains("cDate")) {
                            html
                                    .append("<input type=\"hidden\" name=\"cDate\" value=\"true\">");
                        }
                        if (fieldHash.contains("mDate")) {
                            html
                                    .append("<input type=\"hidden\" name=\"mDate\" value=\"true\">");
                        }
                        if (fieldHash.contains("dcmDate")) {
                            html
                                    .append("<input type=\"hidden\" name=\"dcmDate\" value=\"true\">");
                        }
                        if (fieldHash.contains("title")) {
                            html
                                    .append("<input type=\"hidden\" name=\"title\" value=\"true\">");
                        }
                        if (fieldHash.contains("creator")) {
                            html
                                    .append("<input type=\"hidden\" name=\"creator\" value=\"true\">");
                        }
                        if (fieldHash.contains("subject")) {
                            html
                                    .append("<input type=\"hidden\" name=\"subject\" value=\"true\">");
                        }
                        if (fieldHash.contains("description")) {
                            html
                                    .append("<input type=\"hidden\" name=\"description\" value=\"true\">");
                        }
                        if (fieldHash.contains("publisher")) {
                            html
                                    .append("<input type=\"hidden\" name=\"publisher\" value=\"true\">");
                        }
                        if (fieldHash.contains("contributor")) {
                            html
                                    .append("<input type=\"hidden\" name=\"contributor\" value=\"true\">");
                        }
                        if (fieldHash.contains("date")) {
                            html
                                    .append("<input type=\"hidden\" name=\"date\" value=\"true\">");
                        }
                        if (fieldHash.contains("type")) {
                            html
                                    .append("<input type=\"hidden\" name=\"type\" value=\"true\">");
                        }
                        if (fieldHash.contains("format")) {
                            html
                                    .append("<input type=\"hidden\" name=\"format\" value=\"true\">");
                        }
                        if (fieldHash.contains("identifier")) {
                            html
                                    .append("<input type=\"hidden\" name=\"identifier\" value=\"true\">");
                        }
                        if (fieldHash.contains("source")) {
                            html
                                    .append("<input type=\"hidden\" name=\"source\" value=\"true\">");
                        }
                        if (fieldHash.contains("language")) {
                            html
                                    .append("<input type=\"hidden\" name=\"language\" value=\"true\">");
                        }
                        if (fieldHash.contains("relation")) {
                            html
                                    .append("<input type=\"hidden\" name=\"relation\" value=\"true\">");
                        }
                        if (fieldHash.contains("coverage")) {
                            html
                                    .append("<input type=\"hidden\" name=\"coverage\" value=\"true\">");
                        }
                        if (fieldHash.contains("rights")) {
                            html
                                    .append("<input type=\"hidden\" name=\"rights\" value=\"true\">");
                        }
                        html
                                .append("\n<input type=\"hidden\" name=\"sessionToken\" value=\""
                                        + fsr.getToken() + "\">\n");
                        html
                                .append("\n<input type=\"hidden\" name=\"maxResults\" value=\""
                                        + maxResults + "\">\n");
                        html
                                .append("<input type=\"submit\" value=\"More Results &gt;\"></form>");
                    }
                    html.append("</center>\n");
                }
            }
            if (!xml) {
                response.setContentType("text/html; charset=UTF-8");
                PrintWriter out = response.getWriter();
                out
                        .print("<html><head><title>Search Repository</title></head>");
                out.print("<body><center>");
                out
                        .println("<table width=\"784\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
                out
                        .println("<tr><td width=\"141\" height=\"134\" valign=\"top\"><img src=\"images/newlogo2.jpg\" width=\"141\" height=\"134\"/></td>");
                out.println("<td width=\"643\" valign=\"top\">");
                out.println("<center><h2>Fedora Repository</h2>");
                out.println("<h3>Find Objects</h3>");
                out.println("</center></td></tr></table>");
                out.print(html.toString());
                out.print("</center>");
                out.print("</body>");
                out.print("</html>");
                out.flush();
                out.close();
            } else {
                response.setContentType("text/xml; charset=UTF-8");
                PrintWriter out =
                        new PrintWriter(new OutputStreamWriter(response
                                .getOutputStream(), "UTF-8"));
                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                out.println("<result xmlns=\"" + TYPES.uri + "\">");
                if (fsr != null && fsr.getToken() != null) {
                    out.println("  <listSession>");
                    out.println("    <token>" + fsr.getToken() + "</token>");
                    if (fsr.getCursor() != -1) {
                        out.println("    <cursor>" + fsr.getCursor()
                                + "</cursor>");
                    }
                    if (fsr.getCompleteListSize() != -1) {
                        out.println("    <completeListSize>"
                                + fsr.getCompleteListSize()
                                + "</completeListSize>");
                    }
                    if (fsr.getExpirationDate() != null) {
                        out
                                .println("    <expirationDate>"
                                        + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                                .format(fsr.getExpirationDate())
                                        + "</expirationDate>");
                    }
                    out.println("  </listSession>");
                }
                out.println("<resultList>");
                out.println(xmlBuf.toString());
                out.println("</resultList>");
                out.println("</result>");
                out.flush();
                out.close();
            }
        } catch (AuthzException ae) {
            throw RootException.getServletException(ae,
                                                    request,
                                                    ACTION_LABEL,
                                                    new String[0]);
        } catch (ServletException e) {
            throw e;
        } catch (Throwable th) {
            throw new InternalError500Exception("",
                                                th,
                                                request,
                                                actionLabel,
                                                "",
                                                new String[0]);
        }
    }

    private void appendXML(String name, String value, StringBuffer out) {
        if (value != null) {
            out.append("      <" + name + ">" + StreamUtility.enc(value) + "</"
                    + name + ">\n");
        }
    }

    private void appendXML(String name, List<DCField> values, StringBuffer out) {
        for (int i = 0; i < values.size(); i++) {
            appendXML(name, values.get(i).getValue(), out);
        }
    }

    private void appendXML(String name,
                           Date dt,
                           SimpleDateFormat formatter,
                           StringBuffer out) {
        if (dt != null) {
            appendXML(name, formatter.format(dt), out);
        }
    }

    private String getList(List<DCField> l) {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) {
                ret.append(", ");
            }
            ret.append(StreamUtility.enc(l.get(i).getValue()));
        }
        return ret.toString();
    }

    /** Exactly the same behavior as doGet. */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    /** Gets the Fedora Server instance. */
    @Override
    public void init() throws ServletException {
        try {
            s_server =
                    Server.getInstance(new File(Constants.FEDORA_HOME), false);
            s_access =
                    (Access) s_server.getModule("fedora.server.access.Access");
        } catch (InitializationException ie) {
            throw new ServletException("Error getting Fedora Server instance: "
                    + ie.getMessage());
        }
    }

}
