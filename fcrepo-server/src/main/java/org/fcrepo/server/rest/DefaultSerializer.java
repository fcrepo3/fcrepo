/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.rest;

import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.Context;
import org.fcrepo.server.access.ObjectProfile;
import org.fcrepo.server.management.DefaultManagement;
import org.fcrepo.server.search.FieldSearchResult;
import org.fcrepo.server.search.ObjectFields;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamDef;
import org.fcrepo.server.storage.types.MethodParmDef;
import org.fcrepo.server.storage.types.ObjectMethodsDef;
import org.fcrepo.server.storage.types.Validation;
import org.fcrepo.server.utilities.DCField;

import org.fcrepo.utilities.DateUtility;

import static org.fcrepo.server.utilities.StreamUtility.enc;


public class DefaultSerializer {
    private static final Logger logger =
            LoggerFactory.getLogger(DefaultManagement.class);


    String fedoraServerHost;
    String fedoraServerPort;
    String fedoraServerProtocol;
    String fedoraAppServerContext;

    public DefaultSerializer(String fedoraServerHost, Context context) {
        this.fedoraServerHost = fedoraServerHost;
        this.fedoraServerPort = context.getEnvironmentValue(Constants.HTTP_REQUEST.SERVER_PORT.uri);
        this.fedoraAppServerContext = context.getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME);

        if (Constants.HTTP_REQUEST.SECURE.uri
                .equals(context.getEnvironmentValue(Constants.HTTP_REQUEST.SECURITY.uri))) {
            this.fedoraServerProtocol = "https";
        } else {
            this.fedoraServerProtocol = "http";
        }
    }

    String pidsToXml(
            String[] pidList) {
        StringBuffer xml = new StringBuffer();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<pidList "
                + " xmlns=\"" + Constants.PID_LIST1_0.namespace.uri + "\" "
                + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"" + Constants.PID_LIST1_0.namespace.uri + " "
                + Constants.PID_LIST1_0.xsdLocation + "\">");

        // PID array serialization
        for (int i = 0; i < pidList.length; i++) {
            xml.append("  <pid>" + pidList[i] + "</pid>\n");
        }
        xml.append("</pidList>\n");
        return xml.toString();
    }

    public String objectProfileToXML(
            ObjectProfile objProfile,
            Date versDateTime)  {
        StringBuilder buffer = new StringBuilder();

        String pid = objProfile.PID;
        String dateString = "";
        if (versDateTime != null) {
            String tmp = DateUtility.convertDateToString(versDateTime);
            if (tmp != null) {
                dateString = String.format("dateTime=\"%s\" ", tmp);
            }
        }

        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buffer.append("<objectProfile "
                      + " xmlns=\"" + Constants.OBJ_PROFILE1_0.namespace.uri + "\" "
                      + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                      + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                      + " xsi:schemaLocation=\"" + Constants.OBJ_PROFILE1_0.namespace.uri + " "
                      + Constants.OBJ_PROFILE1_0.xsdLocation + "\""
                      + " pid=\"" + enc(pid) + "\" " + dateString + ">");

        // PROFILE FIELDS SERIALIZATION
        buffer.append("<objLabel>" + enc(objProfile.objectLabel)
                      + "</objLabel>");
        buffer.append("<objOwnerId>" + enc(objProfile.objectOwnerId)
                      + "</objOwnerId>");
        buffer.append("<objModels>");
        for (String model : objProfile.objectModels) {
            buffer.append("<model>");
            buffer.append(enc(model));
            buffer.append("</model>");
        }
        buffer.append("</objModels>");
        String cDate = DateUtility.convertDateToString(objProfile.objectCreateDate);
        buffer.append("<objCreateDate>" + cDate + "</objCreateDate>");
        String mDate = DateUtility.convertDateToString(objProfile.objectLastModDate);
        buffer.append("<objLastModDate>" + mDate + "</objLastModDate>");
        buffer.append("<objDissIndexViewURL>"
                      + enc(objProfile.dissIndexViewURL)
                      + "</objDissIndexViewURL>");
        buffer.append("<objItemIndexViewURL>"
                      + enc(objProfile.itemIndexViewURL)
                      + "</objItemIndexViewURL>");
        buffer.append("<objState>"
                      + enc(objProfile.objectState)
                      + "</objState>");
        buffer.append("</objectProfile>");

        return buffer.toString();
    }

    private String datastreamFieldSerialization(Datastream dsProfile, boolean validateChecksum) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<dsLabel>" + enc(dsProfile.DSLabel) + "</dsLabel>");
        buffer.append("<dsVersionID>" + enc(dsProfile.DSVersionID) + "</dsVersionID>");

        String cDate = DateUtility.convertDateToString(dsProfile.DSCreateDT);
        buffer.append("<dsCreateDate>" + enc(cDate) + "</dsCreateDate>");

        buffer.append("<dsState>" + enc(dsProfile.DSState) + "</dsState>");
        buffer.append("<dsMIME>" + enc(dsProfile.DSMIME) + "</dsMIME>");
        buffer.append("<dsFormatURI>" + enc(dsProfile.DSFormatURI) + "</dsFormatURI>");
        buffer.append("<dsControlGroup>" + enc(dsProfile.DSControlGrp) + "</dsControlGroup>");
        buffer.append("<dsSize>" + enc(Long.valueOf(dsProfile.DSSize).toString()) + "</dsSize>");
        buffer.append(
                "<dsVersionable>" + enc(Boolean.valueOf(dsProfile.DSVersionable).toString()) + "</dsVersionable>");
        buffer.append("<dsInfoType>" + enc(dsProfile.DSInfoType) + "</dsInfoType>");
        buffer.append("<dsLocation>" + enc(dsProfile.DSLocation) + "</dsLocation>");
        buffer.append("<dsLocationType>" + enc(dsProfile.DSLocationType) + "</dsLocationType>");
        buffer.append("<dsChecksumType>" + enc(dsProfile.DSChecksumType) + "</dsChecksumType>");
        buffer.append("<dsChecksum>" + enc(dsProfile.DSChecksum) + "</dsChecksum>");
        if (validateChecksum) {
            String valid = dsProfile.compareChecksum() ? "true" : "false";
            buffer.append("<dsChecksumValid>" + valid + "</dsChecksumValid>");
        }
        String[] dsAltIDs = dsProfile.DatastreamAltIDs;
        for (int i = 0; i < dsAltIDs.length; i++) {
            buffer.append("<dsAltID>" + enc(dsAltIDs[i]) + "</dsAltID>");
        }
        return buffer.toString();
    }

    String datastreamProfileToXML(String pid, String dsID, Datastream dsProfile, Date versDateTime,
                                  boolean validateChecksum) {
        StringBuffer buffer = new StringBuffer();

        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buffer.append("<datastreamProfile "
                      + " xmlns=\"" + Constants.MANAGEMENT.uri + "\" "
                      + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                      + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                      + " xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/management/ "
                      + "http://www.fedora.info/definitions/1/0/datastreamProfile.xsd" + "\""
                      + " pid=\"" + enc(pid) + "\""
                      + " dsID=\"" + enc(dsID) + "\"");
        if (versDateTime != null &&
            !DateUtility.convertDateToString(versDateTime).equalsIgnoreCase("")) {
            buffer.append(" dateTime=\"" + DateUtility.convertDateToString(versDateTime) + "\"");
        }
        buffer.append(" >");
        // ADD PROFILE FIELDS SERIALIZATION
        buffer.append(datastreamFieldSerialization(dsProfile, validateChecksum));


        buffer.append("</datastreamProfile>");

        return buffer.toString();
    }

    String objectHistoryToXml(
            String[] objectHistory,
            String pid)  {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buffer.append("<fedoraObjectHistory "
                      + " xmlns=\"" + Constants.OBJ_HISTORY1_0.namespace.uri + "\""
                      + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                      + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                      + " xsi:schemaLocation=\"" + Constants.OBJ_HISTORY1_0.namespace.uri
                      + " " + Constants.OBJ_HISTORY1_0.xsdLocation + "\""
                      + " pid=\"" + pid + "\" >");

        for (String ts : objectHistory) {
            buffer.append("<objectChangeDate>" + ts + "</objectChangeDate>");
        }
        buffer.append("</fedoraObjectHistory>");
        return buffer.toString();
    }

    String datastreamHistoryToXml(String pid, String dsID, Datastream[] history) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buffer.append("<datastreamHistory "
                      + " xmlns=\"" + Constants.MANAGEMENT.uri + "\" "
                      + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                      + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                      + " xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/management/ "
                      + "http://www.fedora.info/definitions/1/0/datastreamHistory.xsd" + "\""
                      + " pid=\"" + enc(pid) + "\""
                      + " dsID=\"" + enc(dsID) + "\">");

        for (Datastream ds : history) {
            buffer.append("<datastreamProfile ").append("pid=\"").append(
                    enc(pid)).append("\"").append(" dsID=\"").append(enc(dsID))
                    .append("\">");
            buffer.append(datastreamFieldSerialization(ds, false));
            buffer.append("</datastreamProfile>");

        }

        buffer.append("</datastreamHistory>");
        logger.error(buffer.toString());
        return buffer.toString();

    }

    public String objectMethodsToXml(
            ObjectMethodsDef[] methodDefs,
            String pid,
            String sDef,
            Date versDateTime) {
        StringBuffer buffer = new StringBuffer();

        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        String asOfDateTimeElement = "";
        if (versDateTime != null) {
            asOfDateTimeElement = "asOfDateTime=\""
                                  + DateUtility.convertDateToString(versDateTime) + "\" ";
        }
        String sDefElement = "";
        if (sDef != null) {
            sDefElement = "sDef=\"" + enc(sDef) + "\" ";
        }
        buffer.append("<objectMethods "
                      + "xmlns=\"" + Constants.OBJ_METHODS1_0.namespace.uri + "\" "
                      + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                      + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                      + "xsi:schemaLocation=\"" + Constants.OBJ_METHODS1_0.namespace.uri + " "
                      + Constants.OBJ_METHODS1_0.xsdLocation + "\" "
                      + " pid=\"" + enc(pid) + "\" "
                      + asOfDateTimeElement + sDefElement + "baseURL=\""
                      + enc(fedoraServerProtocol) + "://"
                      + enc(fedoraServerHost) + ":"
                      + enc(fedoraServerPort) + "/" + fedoraAppServerContext + "/\" >");

        // ObjectMethodsDef SERIALIZATION
        String nextSdef = "null";
        String currentSdef = "";
        for (int i = 0; i < methodDefs.length; i++) {
            currentSdef = methodDefs[i].sDefPID;
            if (sDef == null || currentSdef.equals(sDef)) {
                if (!currentSdef.equalsIgnoreCase(nextSdef)) {
                    if (!nextSdef.equals("null")) {
                        buffer.append("</sDef>");
                    }
                    buffer.append("<sDef pid=\"" + enc(methodDefs[i].sDefPID)
                                  + "\" >");
                }
                buffer.append("<method name=\"" + enc(methodDefs[i].methodName)
                              + "\" >");
                MethodParmDef[] methodParms = methodDefs[i].methodParmDefs;
                for (int j = 0; j < methodParms.length; j++) {
                    buffer.append("<methodParm parmName=\""
                                  + enc(methodParms[j].parmName)
                                  + "\" parmDefaultValue=\""
                                  + enc(methodParms[j].parmDefaultValue)
                                  + "\" parmRequired=\"" + methodParms[j].parmRequired
                                  + "\" parmLabel=\"" + enc(methodParms[j].parmLabel)
                                  + "\" >");
                    if (methodParms[j].parmDomainValues.length > 0) {
                        buffer.append("<methodParmDomain>");
                        for (int k = 0; k < methodParms[j].parmDomainValues.length; k++) {
                            buffer.append("<methodParmValue>"
                                          + enc(methodParms[j].parmDomainValues[k])
                                          + "</methodParmValue>");
                        }
                        buffer.append("</methodParmDomain>");
                    }
                    buffer.append("</methodParm>");
                }

                buffer.append("</method>");
                nextSdef = currentSdef;
            }
        }
        if (!nextSdef.equals("null")) {
            buffer.append("</sDef>");
        }
        buffer.append("</objectMethods>");

        return buffer.toString();
    }

    String searchResultToHtml(
            String query,
            String terms,
            String[] searchableFields,
            String[] wantedFields,
            int maxResults,
            FieldSearchResult result) {
        StringBuffer html = new StringBuffer();
        HashSet<String> fieldHash = new HashSet<String>();

        if (wantedFields != null) {
            for (String wf : wantedFields) {
                fieldHash.add(wf);
            }
        }

        html.append("<html><head><title>Search Repository</title></head>");
        html.append("<body><center>");
        html.append("<table width=\"784\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
        html.append("<tr><td width=\"141\" height=\"134\" valign=\"top\"><img src=\"" + "/" + fedoraAppServerContext +
                    "/images/newlogo2.jpg\" width=\"141\" height=\"134\"/></td>");
        html.append("<td width=\"643\" valign=\"top\">");
        html.append("<center><h2>Fedora Repository</h2>");
        html.append("<h3>Find Objects</h3>");
        html.append("</center></td></tr></table>");
        html.append("\n");

        html.append("<form method=\"get\">");
        html.append("<center><table border=0 cellpadding=6 cellspacing=0>\n");
        html.append("<tr><td colspan=3 valign=top><i>Fields to display:</i></td><td></td></tr>");
        html.append("<tr>");

        int fieldPerCol = (searchableFields.length / 3) + 1;
        for (int i = 0; i < searchableFields.length; i++) {
            boolean everOtherNFields = (i % fieldPerCol == 0);

            if (everOtherNFields) {
                if (i > 0) {
                    html.append("</font></td>");
                    html.append("\n");
                }

                html.append("<td valign=top><font size=-1>");
                html.append("\n");
            }

            html.append("<input type='checkbox' name='"
                        + searchableFields[i]
                        + "' value='true' "
                        + ((RestParam.PID.equals(searchableFields[i])
                            || "title".equals(searchableFields[i]) || fieldHash.contains(searchableFields[i])) ?
                           "checked"
                                                                                                               : "") +
                        "> <a href='#'>" + searchableFields[i] + "</a><br>");
            html.append("\n");
        }
        html.append("</font></td><td bgcolor=silver valign=top>&nbsp;&nbsp;&nbsp;</td><td valign=top>");
        html.append("Search all fields for phrase: <input type=\"text\" name=\"terms\" size=\"15\" value=\""
                    + (terms == null ? "" : enc(terms))
                    +
                    "\"> <a href=\"#\" onClick=\"javascript:alert('Search All Fields\\n\\nEnter a phrase.  Objects where any field contains the phrase will be returned.\\nThis is a case-insensitive search, and you may use the * or ? wildcards.\\n\\nExamples:\\n\\n  *o*\\n    finds objects where any field contains the letter o.\\n\\n  ?edora\\n    finds objects where a word starts with any letter and ends with edora.')\"><i>help</i></a><p> ");
        html.append("Or search specific field(s): <input type=\"text\" name=\"query\" size=\"15\" value=\""
                    + (query == null ? "" : enc(query))
                    +
                    "\"> <a href=\"#\" onClick=\"javascript:alert('Search Specific Field(s)\\n\\nEnter one or more conditions, separated by space.  Objects matching all conditions will be returned.\\nA condition is a field (choose from the field names on the left) followed by an operator, followed by a value.\\nThe = operator will match if the field\\'s entire value matches the value given.\\nThe ~ operator will match on phrases within fields, and accepts the ? and * wildcards.\\nThe &lt;, &gt;, &lt;=, and &gt;= operators can be used with numeric values, such as dates.\\n\\nExamples:\\n\\n  pid~demo:* description~fedora\\n    Matches all demo objects with a description containing the word fedora.\\n\\n  cDate&gt;=1976-03-04 creator~*n*\\n    Matches objects created on or after March 4th, 1976 where at least one of the creators has an n in their name.\\n\\n  mDate&gt;2002-10-2 mDate&lt;2002-10-2T12:00:00\\n    Matches objects modified sometime before noon (UTC) on October 2nd, 2002')\"><i>help</i></a><p> ");
        html.append(
                "Maximum Results: <select name=\"maxResults\"><option value=\"20\">20</option><option value=\"40\">40</option><option value=\"60\">60</option><option value=\"80\">80</option></select> ");
        html.append("<p><input type=\"submit\" value=\"Search\"> ");
        html.append("</td></tr></table></center>");
        html.append("</form><hr size=1>");

        if (result != null) {
            List<ObjectFields> objectFieldList = result.objectFieldsList();

            html.append(
                    "<center><table width=\"90%\" border=\"1\" cellpadding=\"5\" cellspacing=\"5\" bgcolor=\"silver\">\n");
            html.append("<tr>");
            for (int i = 0; i < wantedFields.length; i++) {
                html.append("<td valign=\"top\"><strong>");
                html.append(wantedFields[i]);
                html.append("</strong></td>");
            }
            html.append("</tr>");

            for (int i = 0; i < objectFieldList.size(); i++) {
                ObjectFields f = objectFieldList.get(i);
                html.append("<tr>");
                for (int j = 0; j < wantedFields.length; j++) {
                    String l = wantedFields[j];
                    html.append("<td valign=\"top\">");
                    if (l.equalsIgnoreCase("pid")) {
                        html.append("<a href=\"objects/");
                        try {
                            html.append(URLEncoder.encode(f.getPid(), "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            // should never happen (UTF-8)
                            throw new RuntimeException(e);
                        }
                        html.append("\">");
                        html.append(f.getPid());
                        html.append("</a>");
                    } else if (l.equalsIgnoreCase("label")) {
                        if (f.getLabel() != null) {
                            html.append(enc(f.getLabel()));
                        }
                    } else if (l.equalsIgnoreCase("state")) {
                        html.append(f.getState());
                    } else if (l.equalsIgnoreCase("ownerId")) {
                        if (f.getOwnerId() != null) {
                            html.append(f.getOwnerId());
                        }
                    } else if (l.equalsIgnoreCase("cDate")) {
                        html.append(DateUtility.convertDateToString(f.getCDate()));
                    } else if (l.equalsIgnoreCase("mDate")) {
                        html.append(DateUtility.convertDateToString(f.getMDate()));
                    } else if (l.equalsIgnoreCase("dcmDate")) {
                        if (f.getDCMDate() != null) {
                            html.append(DateUtility.convertDateToString(f.getDCMDate()));
                        }
                    } else if (l.equalsIgnoreCase("title")) {
                        html.append(join(f.titles()));
                    } else if (l.equalsIgnoreCase("creator")) {
                        html.append(join(f.creators()));
                    } else if (l.equalsIgnoreCase("subject")) {
                        html.append(join(f.subjects()));
                    } else if (l.equalsIgnoreCase("description")) {
                        html.append(join(f.descriptions()));
                    } else if (l.equalsIgnoreCase("publisher")) {
                        html.append(join(f.publishers()));
                    } else if (l.equalsIgnoreCase("contributor")) {
                        html.append(join(f.contributors()));
                    } else if (l.equalsIgnoreCase("date")) {
                        html.append(join(f.dates()));
                    } else if (l.equalsIgnoreCase("type")) {
                        html.append(join(f.types()));
                    } else if (l.equalsIgnoreCase("format")) {
                        html.append(join(f.formats()));
                    } else if (l.equalsIgnoreCase("identifier")) {
                        html.append(join(f.identifiers()));
                    } else if (l.equalsIgnoreCase("source")) {
                        html.append(join(f.sources()));
                    } else if (l.equalsIgnoreCase("language")) {
                        html.append(join(f.languages()));
                    } else if (l.equalsIgnoreCase("relation")) {
                        html.append(join(f.relations()));
                    } else if (l.equalsIgnoreCase("coverage")) {
                        html.append(join(f.coverages()));
                    } else if (l.equalsIgnoreCase("rights")) {
                        html.append(join(f.rights()));
                    }
                    html.append("</td>");
                    html.append("\n");
                }
                html.append("</tr>");
                html.append("\n");
                html.append("<tr><td colspan=\"");
                html.append(wantedFields.length);
                html.append("\"></td></tr>");
                html.append("\n");
            }
            html.append("</table>");
            html.append("\n");

            if (result != null && result.getToken() != null) {
                if (result.getCursor() != -1) {
                    long viewingStart = result.getCursor() + 1;
                    long viewingEnd = result.objectFieldsList().size() + viewingStart - 1;
                    html.append("<p>Viewing results " + viewingStart + " to "
                                + viewingEnd);
                    if (result.getCompleteListSize() != -1) {
                        html.append(" of " + result.getCompleteListSize());
                    }
                    html.append("</p>\n");
                }
                html.append("<form method=\"get\" action=\"\">");
                html.append("\n");
                for (String field : wantedFields) {
                    html.append("<input type=\"hidden\" name=\"" + field
                                + "\" value=\"true\">");
                    html.append("\n");
                }

                html.append("\n");
                html.append("\n<input type=\"hidden\" name=\"sessionToken\" value=\""
                            + result.getToken() + "\">\n");
                html.append("\n<input type=\"hidden\" name=\"maxResults\" value=\""
                            + maxResults + "\">\n");
                html.append("<input type=\"submit\" value=\"More Results &gt;\"></form>");
            }
            html.append("</center>\n");
        }

        html.append("</center>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    String searchResultToXml(
            FieldSearchResult result) {
        StringBuffer xmlBuf = new StringBuffer();

        xmlBuf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlBuf.append("<result xmlns=\"http://www.fedora.info/definitions/1/0/types/\">\n");
        if ((result != null) && (result.getToken() != null)) {
            xmlBuf.append("  <listSession>\n");
            xmlBuf.append("    <token>" + result.getToken() + "</token>\n");
            if (result.getCursor() != -1) {
                xmlBuf.append("    <cursor>" + result.getCursor() + "</cursor>\n");
            }
            if (result.getCompleteListSize() != -1) {
                xmlBuf.append("    <completeListSize>" + result.getCompleteListSize()
                              + "</completeListSize>\n");
            }
            if (result.getExpirationDate() != null) {
                xmlBuf.append("    <expirationDate>"
                              + DateUtility.convertDateToString(result.getExpirationDate())
                              + "</expirationDate>\n");
            }
            xmlBuf.append("  </listSession>\n");
        }
        xmlBuf.append("  <resultList>\n");

        if (result != null) {
            List<ObjectFields> fieldList = result.objectFieldsList();

            for (int i = 0; i < fieldList.size(); i++) {
                ObjectFields f = fieldList.get(i);
                xmlBuf.append("  <objectFields>\n");
                appendXML("pid", f.getPid(), xmlBuf);
                appendXML("label", f.getLabel(), xmlBuf);
                appendXML("state", f.getState(), xmlBuf);
                appendXML("ownerId", f.getOwnerId(), xmlBuf);
                appendXML("cDate", f.getCDate(), xmlBuf);
                appendXML("mDate", f.getMDate(), xmlBuf);
                appendXML("dcmDate", f.getDCMDate(), xmlBuf);
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
            }
        }
        xmlBuf.append("  </resultList>\n");
        xmlBuf.append("</result>\n");

        return xmlBuf.toString();
    }

    static private String join(
            List<DCField> l) {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) {
                ret.append(", ");
            }
            ret.append(enc(l.get(i).getValue()));
        }
        return ret.toString();
    }

    private static void appendXML(
            String name,
            String value,
            StringBuffer out) {
        if (value != null) {
            out.append("      <" + name + ">" + enc(value) + "</" + name
                       + ">\n");
        }
    }

    private void appendXML(String name, List<DCField> values, StringBuffer out) {
        for (int i = 0; i < values.size(); i++) {
            appendXML(name, values.get(i).getValue(), out);
        }
    }

    private static void appendXML(
            String name,
            Date dt,
            StringBuffer out) {
        if (dt != null) {
            appendXML(name, DateUtility.convertDateToString(dt), out);
        }
    }

    public String dataStreamsToXML(
            String pid,
            Date asOfDateTime,
            DatastreamDef[] dsDefs) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        String dateString = "";
        if (asOfDateTime != null) {
            String tmp = DateUtility.convertDateToString(asOfDateTime);
            if (tmp != null) {
                dateString = String.format("asOfDateTime=\"%s\" ", tmp);
            }
        }

        xml.append("<objectDatastreams "
                   + " xmlns=\"" + Constants.OBJ_DATASTREAMS1_0.namespace.uri + "\" "
                   + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                   + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                   + " xsi:schemaLocation=\"" + Constants.OBJ_DATASTREAMS1_0.namespace.uri + " "
                   + Constants.OBJ_DATASTREAMS1_0.xsdLocation + "\""
                   + " pid=\"" + enc(pid) + "\" " + dateString
                   + " baseURL=\"" + enc(fedoraServerProtocol) + "://"
                   + enc(fedoraServerHost) + ":"
                   + enc(fedoraServerPort) + "/" + fedoraAppServerContext + "/\" >");

        // DatastreamDef SERIALIZATION
        for (int i = 0; i < dsDefs.length; i++) {
            xml.append("    <datastream " + "dsid=\"" + enc(dsDefs[i].dsID)
                       + "\" " + "label=\"" + enc(dsDefs[i].dsLabel) + "\" "
                       + "mimeType=\"" + enc(dsDefs[i].dsMIME) + "\" />");
        }
        xml.append("</objectDatastreams>");

        return xml.toString();
    }

    public String objectValidationToXml(Validation validation) {
        StringBuilder buffer = new StringBuilder();
        String pid = validation.getPid();
        Date date = validation.getAsOfDateTime();
        String dateString = null;
        boolean valid = validation.isValid();
        if (date != null) {
            dateString = DateUtility.convertDateToString(date);
        }
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        buffer.append("<validation "
                + " xmlns=\"" + Constants.OBJ_VALIDATION1_0.namespace.uri + "\""
                + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"" + Constants.OBJ_VALIDATION1_0.namespace.uri
                + " " + Constants.OBJ_VALIDATION1_0.xsdLocation + "\""
                + " pid=\"" + enc(pid) + "\" "
                + " valid=\"" + valid + "\">\n");
        if (date != null) {
        	buffer.append("  <asOfDateTime>" + dateString + "</asOfDateTime>\n");
        }
        buffer.append("  <contentModels>\n");
        for (String model : validation.getContentModels()) {
            buffer.append("    <model>");
            buffer.append(enc(model));
            buffer.append("</model>\n");
        }
        buffer.append("  </contentModels>\n");

        buffer.append("  <problems>\n");
        for (String problem : validation.getObjectProblems()) {
            buffer.append("    <problem>");
            buffer.append(problem);
            buffer.append("</problem>\n");
        }
        buffer.append("  </problems>\n");

        buffer.append("  <datastreamProblems>\n");
        Map<String, List<String>> dsprobs = validation.getDatastreamProblems();
        for (String ds : dsprobs.keySet()) {
            List<String> problems = dsprobs.get(ds);
            buffer.append("    <datastream");
            buffer.append(" datastreamID=\"");
            buffer.append(ds);
            buffer.append("\">\n");
            for (String problem : problems) {
                buffer.append("      <problem>");
                buffer.append(problem);
                buffer.append("</problem>\n");
            }
            buffer.append("    </datastream>");
        }
        buffer.append("  </datastreamProblems>\n");
        buffer.append("</validation>");
        return buffer.toString();
    }


}
