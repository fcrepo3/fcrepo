/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.servletExceptionExtensions.BadRequest400Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.InternalError500Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.RootException;
import org.fcrepo.server.search.Condition;
import org.fcrepo.server.search.FieldSearchQuery;
import org.fcrepo.server.search.FieldSearchResult;
import org.fcrepo.server.search.ObjectFields;
import org.fcrepo.server.utilities.DCField;
import org.fcrepo.server.utilities.StreamUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * REST interface for API-A's FieldSearch functionality.
 *
 * @author Chris Wilper
 */
@SuppressWarnings("serial")
public class FieldSearchServlet
        extends SpringAccessServlet
        implements Constants {

    private static final Logger logger =
            LoggerFactory.getLogger(FieldSearchServlet.class);

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
    
    private static String CHECKED = " checked=\"checked\"";

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
                    logger.error("Bad request (maxResults not an integer)", nfe);
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

            FieldSearchResult fsr = null;
            if (fieldsArray != null && fieldsArray.length > 0
                    || sessionToken != null) {
                if (sessionToken != null) {
                    fsr = m_access.resumeFindObjects(context, sessionToken);
                } else {
                    if (terms != null && terms.length() != 0) {
                        fsr =
                                m_access
                                        .findObjects(context,
                                                     fieldsArray,
                                                     maxResults,
                                                     new FieldSearchQuery(terms));
                    } else {
                        fsr =
                                m_access
                                        .findObjects(context,
                                                     fieldsArray,
                                                     maxResults,
                                                     new FieldSearchQuery(Condition
                                                             .getConditions(query)));
                    }
                }
            }
            if (!xml) {
                SimpleDateFormat formatter =
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
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
                printSearchFormToHtml(fieldHash, terms, query, out);
                printFieldsArrayTableHeader(fieldsArray, out);
                List<ObjectFields> searchResults = fsr.objectFieldsList();
                for (int i = 0; i < searchResults.size(); i++) {
                    ObjectFields f = searchResults.get(i);
                    printObjectFieldsToHtml(f, fieldsArray, formatter, out);
                }
                    out.append("</table>");
                    printHiddenFieldsFormToHtml(fsr, fieldHash, maxResults, out);
                    out.append("</center>\n");
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
                    SimpleDateFormat formatter =
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
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
                                        + formatter.format(fsr.getExpirationDate())
                                        + "</expirationDate>");
                    }
                    out.println("  </listSession>");
                    out.println("<resultList>");
                    List<ObjectFields> searchResults = fsr.objectFieldsList();
                    for (int i = 0; i < searchResults.size(); i++) {
                        ObjectFields f = searchResults.get(i);
                        printObjectFieldsToXml(f, formatter, out);
                    }
                    out.println("</resultList>");
                } else {
                    out.println("<resultList />");
                }
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
    
    private void printObjectFieldsToXml(
            ObjectFields objFields, SimpleDateFormat formatter, PrintWriter xmlBuf) {
        xmlBuf.append("  <objectFields>\n");
        appendXML("pid", objFields.getPid(), xmlBuf);
        appendXML("label", objFields.getLabel(), xmlBuf);
        appendXML("state", objFields.getState(), xmlBuf);
        appendXML("ownerId", objFields.getOwnerId(), xmlBuf);
        appendXML("cDate", objFields.getCDate(), formatter, xmlBuf);
        appendXML("mDate", objFields.getMDate(), formatter, xmlBuf);
        appendXML("dcmDate", objFields.getDCMDate(), formatter, xmlBuf);
        appendXML("title", objFields.titles(), xmlBuf);
        appendXML("creator", objFields.creators(), xmlBuf);
        appendXML("subject", objFields.subjects(), xmlBuf);
        appendXML("description", objFields.descriptions(), xmlBuf);
        appendXML("publisher", objFields.publishers(), xmlBuf);
        appendXML("contributor", objFields.contributors(), xmlBuf);
        appendXML("date", objFields.dates(), xmlBuf);
        appendXML("type", objFields.types(), xmlBuf);
        appendXML("format", objFields.formats(), xmlBuf);
        appendXML("identifier", objFields.identifiers(), xmlBuf);
        appendXML("source", objFields.sources(), xmlBuf);
        appendXML("language", objFields.languages(), xmlBuf);
        appendXML("relation", objFields.relations(), xmlBuf);
        appendXML("coverage", objFields.coverages(), xmlBuf);
        appendXML("rights", objFields.rights(), xmlBuf);
        xmlBuf.append("  </objectFields>\n");
    }
    
    private void printFieldsArrayTableHeader(String[] fieldsArray, PrintWriter html) {
        html.append("<center><table width=\"90%\" border=\"1\" cellpadding=\"5\" cellspacing=\"5\" bgcolor=\"silver\">\n"
                + "<tr>");
        for (String element : fieldsArray) {
            html
            .append("<td valign=\"top\"><strong>");
            html.append(element);
            html.append("</strong></td>");
        }
        html.append("</tr>");
    }
    
    private void printSearchFormToHtml(HashSet<String> fieldHash, String terms, String query, PrintWriter html) {
        html.append("<form method=\"post\" action=\"search\">"
                + "<center><table border=0 cellpadding=6 cellspacing=0>\n"
                + "<tr><td colspan=3 valign=top><i>Fields to display:</i></td><td></td></tr>"
                + "<tr><td valign=top><font size=-1>"
                + "<input type=\"checkbox\" name=\"pid\" value=\"true\" checked> <a href=\"#\" onClick=\"javascript:alert('Persistent Identfier\\n\\nThe globally unique identifier of the resource.')\">pid</a><br>"
                + "<input type=\"checkbox\" name=\"label\" value=\"true\"");
        html.append(fieldHash.contains("label") ? " checked"
                                : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Label\\n\\nThe label of the object')\">label</a><br>"
                + "<input type=\"checkbox\" name=\"state\" value=\"true\"");
        html.append(fieldHash.contains("state") ? CHECKED
                                : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('State\\n\\nThe state of the object.\\nThis will be:\\n  A - Active')\">state</a><br>"
                + "<input type=\"checkbox\" name=\"ownerId\" value=\"true\"");
        html.append(fieldHash.contains("ownerId") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Owner Id\\n\\nThe userId of the user who owns the object.')\">ownerId</a><br>"
                + "<input type=\"checkbox\" name=\"cDate\" value=\"true\"");
        html.append(fieldHash.contains("cDate") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Creation Date\\n\\nThe UTC date the object was created,\\nin YYYY-MM-DDTHH:MM:SS.SSSZ format')\">cDate</a><br>"
                 + "<input type=\"checkbox\" name=\"mDate\" value=\"true\"");
        html.append(fieldHash.contains("mDate") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Modified Date\\n\\nThe UTC date the object was last modified,\\nin YYYY-MM-DDTHH:MM:SS.SSSZ format')\">mDate</a><br>"
                + "<input type=\"checkbox\" name=\"dcmDate\" value=\"true\"");
        html.append(fieldHash.contains("dcmDate") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Dublin Core Modified Date\\n\\nThe UTC date the DC datastream was last modified,\\nin YYYY-MM-DDTHH:MM:SS.SSSZ format')\">dcmDate</a><br>"
                + "</font></td><td valign=top><font size=-1>"
                + "<input type=\"checkbox\" name=\"title\" value=\"true\" checked> <a href=\"#\" onClick=\"javascript:alert('Title\\n\\nA name given to the resource.\\nThis is a repeating field.')\">title</a><br>"
                + "<input type=\"checkbox\" name=\"creator\" value=\"true\"");
        html.append(fieldHash.contains("creator") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Creator\\n\\nAn entity primarily responsible for making\\nthe content of the resource.\\nThis is a repeating field.')\">creator</a><br>"
                + "<input type=\"checkbox\" name=\"subject\" value=\"true\"");
        html.append(fieldHash.contains("subject") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Subject and Keywords\\n\\nA topic of the content of the resource.\\nThis is a repeating field.')\">subject</a><br>"
                + "<input type=\"checkbox\" name=\"description\" value=\"true\"");
        html.append(fieldHash.contains("description") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Description\\n\\nAn account of the content of the resource.\\nThis is a repeating field.')\">description</a><br>"
                + "<input type=\"checkbox\" name=\"publisher\" value=\"true\"");
        html.append(fieldHash.contains("publisher") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Publisher\\n\\nAn entity responsible for making the resource available.\\nThis is a repeating field.')\">publisher</a><br>"
                + "<input type=\"checkbox\" name=\"contributor\" value=\"true\"");
        html.append(fieldHash.contains("contributor") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Contributor\\n\\nAn entity responsible for making contributions\\nto the content of the resource.\\nThis is a repeating field.')\">contributor</a><br>"
                + "<input type=\"checkbox\" name=\"date\" value=\"true\"");
        html.append(fieldHash.contains("date") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Date\\n\\nA date of an event in the lifecycle of the resource.\\nThis is a repeating field.')\">date</a><br>"
                + "</font></td><td valign=top><font size=-1>"
                + "<input type=\"checkbox\" name=\"type\" value=\"true\"");
        html.append(fieldHash.contains("type") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Resource Type\\n\\nThe nature or genre of the resource.\\nThis is a repeating field.')\">type</a><br>"
                + "<input type=\"checkbox\" name=\"format\" value=\"true\"");
        html.append(fieldHash.contains("format") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Format\\n\\nThe physical or digital manifestation of the resource.\\nThis is a repeating field.')\">format</a><br>"
                + "<input type=\"checkbox\" name=\"identifier\" value=\"true\"");
        html.append(fieldHash.contains("identifier") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Resource Identifier\\n\\nAn unambiguous reference to the resource within a given context.\\nThis is a repeating field.')\">identifier</a><br>"
                + "<input type=\"checkbox\" name=\"source\" value=\"true\"");
        html.append(fieldHash.contains("source") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Source\\n\\nA reference to a resource from which the present resource is derived.\\nThis is a repeating field.')\">source</a><br>"
                + "<input type=\"checkbox\" name=\"language\" value=\"true\"");
        html.append(fieldHash.contains("language") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Language\\n\\nA language of the intellectual content of the resource.\\nThis is a repeating field.')\">language</a><br>"
                + "<input type=\"checkbox\" name=\"relation\" value=\"true\"");
        html.append(fieldHash.contains("relation") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Relation\\n\\nA reference to a related resource.\\nThis is a repeating field.')\">relation</a><br>"
                + "<input type=\"checkbox\" name=\"coverage\" value=\"true\"");
        html.append(fieldHash.contains("coverage") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Coverage\\n\\nThe extent or scope of the content of the resource.\\nThis is a repeating field.')\">coverage</a><br>"
                + "<input type=\"checkbox\" name=\"rights\" value=\"true\"");
        html.append(fieldHash.contains("rights") ? CHECKED : "");
        html.append("> <a href=\"#\" onClick=\"javascript:alert('Rights Management\\n\\nInformation about rights held in and over the resource.\\nThis is a repeating field.')\">rights</a><br>"
                + "</font></td><td bgcolor=silver valign=top>&nbsp;&nbsp;&nbsp;</td><td valign=top>"
                + "Search all fields for phrase: <input type=\"text\" name=\"terms\" size=\"15\" value=\"");
        if (terms != null) StreamUtility.enc(terms, html);
        html.append("\"> <a href=\"#\" onClick=\"javascript:alert('Search All Fields\\n\\nEnter a phrase.  Objects where any field contains the phrase will be returned.\\nThis is a case-insensitive search, and you may use the * or ? wildcards.\\n\\nExamples:\\n\\n  *o*\\n    finds objects where any field contains the letter o.\\n\\n  ?edora\\n    finds objects where a word starts with any letter and ends with edora.')\"><i>help</i></a><p> "
                +"Or search specific field(s): <input type=\"text\" name=\"query\" size=\"15\" value=\"");
        if (query != null) StreamUtility.enc(query, html);
        html.append("\"> <a href=\"#\" onClick=\"javascript:alert('Search Specific Field(s)\\n\\nEnter one or more conditions, separated by space.  Objects matching all conditions will be returned.\\nA condition is a field (choose from the field names on the left) followed by an operator, followed by a value.\\nThe = operator will match if the field\\'s entire value matches the value given.\\nThe ~ operator will match on phrases within fields, and accepts the ? and * wildcards.\\nThe &lt;, &gt;, &lt;=, and &gt;= operators can be used with numeric values, such as dates.\\n\\nExamples:\\n\\n  pid~demo:* description~fedora\\n    Matches all demo objects with a description containing the word fedora.\\n\\n  cDate&gt;=1976-03-04 creator~*n*\\n    Matches objects created on or after March 4th, 1976 where at least one of the creators has an n in their name.\\n\\n  mDate&gt;2002-10-2 mDate&lt;2002-10-2T12:00:00\\n    Matches objects modified sometime before noon (UTC) on October 2nd, 2002')\"><i>help</i></a><p> ");
        html.append("Maximum Results: <select name=\"maxResults\"><option value=\"20\">20</option><option value=\"40\">40</option><option value=\"60\">60</option><option value=\"80\">80</option></select> "
                + "<p><input type=\"submit\" value=\"Search\"> "
                + "</td></tr></table></center>"
                + "</form><hr size=1>");
    }
    
    private void printObjectFieldsToHtml(ObjectFields f, String[] fieldsArray,
            SimpleDateFormat formatter, PrintWriter html) {
        html.append("<tr>");
        for (String l : fieldsArray) {
            html.append("<td valign=\"top\">");
            if (l.equalsIgnoreCase("pid")) {
                html.append("<a href=\"objects/");
                html.append(f.getPid().replace("%", "%25"));
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
                getList(f.titles(), html);
            } else if (l.equalsIgnoreCase("creator")) {
                getList(f.creators(), html);
            } else if (l.equalsIgnoreCase("subject")) {
                getList(f.subjects(), html);
            } else if (l.equalsIgnoreCase("description")) {
                getList(f.descriptions(), html);
            } else if (l.equalsIgnoreCase("publisher")) {
                getList(f.publishers(), html);
            } else if (l.equalsIgnoreCase("contributor")) {
                getList(f.contributors(), html);
            } else if (l.equalsIgnoreCase("date")) {
                getList(f.dates(), html);
            } else if (l.equalsIgnoreCase("type")) {
                getList(f.types(), html);
            } else if (l.equalsIgnoreCase("format")) {
                getList(f.formats(), html);
            } else if (l.equalsIgnoreCase("identifier")) {
                getList(f.identifiers(), html);
            } else if (l.equalsIgnoreCase("source")) {
                getList(f.sources(), html);
            } else if (l.equalsIgnoreCase("language")) {
                getList(f.languages(), html);
            } else if (l.equalsIgnoreCase("relation")) {
                getList(f.relations(), html);
            } else if (l.equalsIgnoreCase("coverage")) {
                getList(f.coverages(), html);
            } else if (l.equalsIgnoreCase("rights")) {
                getList(f.rights(), html);
            }
            html.append("</td>");
        }
        html.append("</tr><tr><td colspan=\"");
        html.append(Integer.toString(fieldsArray.length));
        html.append("\"></td></tr>");
    }
    
    private void printHiddenFieldsFormToHtml(FieldSearchResult fsr, 
            HashSet<String> fieldHash, long maxResults, PrintWriter html) {
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
    }

    private void appendXML(String name, String value, PrintWriter out) {
        if (value != null) {
            out.append("      <");
            out.append(name);
            out.append(">");
            StreamUtility.enc(value, out);
            out.append("</");
            out.append(name);
            out.append(">\n");
        }
    }

    private void appendXML(String name, List<DCField> values, PrintWriter out) {
        for (int i = 0; i < values.size(); i++) {
            appendXML(name, values.get(i).getValue(), out);
        }
    }

    private void appendXML(String name,
                           Date dt,
                           SimpleDateFormat formatter,
                           PrintWriter out) {
        if (dt != null) {
            appendXML(name, formatter.format(dt), out);
        }
    }

    private void getList(List<DCField> l, PrintWriter ret) {
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) {
                ret.append(", ");
            }
            StreamUtility.enc(l.get(i).getValue(), ret);
        }
    }

    /** Exactly the same behavior as doGet. */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

}
