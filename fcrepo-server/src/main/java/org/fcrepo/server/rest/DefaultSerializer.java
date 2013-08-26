/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.rest;

import static org.fcrepo.server.utilities.StreamUtility.enc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultSerializer {
    private static final Logger logger =
            LoggerFactory.getLogger(DefaultManagement.class);

    String fedoraServerHost;
    String fedoraServerPort;
    String fedoraServerProtocol;
    String fedoraAppServerContext;

    public DefaultSerializer(String fedoraServerHost, Context context) {
        this.fedoraServerHost = fedoraServerHost;
        this.fedoraServerPort = context.getEnvironmentValue(Constants.HTTP_REQUEST.SERVER_PORT.attributeId);
        this.fedoraAppServerContext = context.getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME);

        if (Constants.HTTP_REQUEST.SECURE.uri
                .equals(context.getEnvironmentValue(Constants.HTTP_REQUEST.SECURITY.attributeId))) {
            this.fedoraServerProtocol = "https";
        } else {
            this.fedoraServerProtocol = "http";
        }
    }

    String pidsToXml(String[] pidList) {
        StringBuilder xml = new StringBuilder(512);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<pidList  xmlns=\"")
        .append(Constants.PID_LIST1_0.namespace.uri)
        .append("\"  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"")
        .append(Constants.PID_LIST1_0.namespace.uri)
        .append(' ')
        .append(Constants.PID_LIST1_0.xsdLocation)
        .append("\">");

        // PID array serialization
        for (int i = 0; i < pidList.length; i++) {
            xml.append("  <pid>")
            .append(pidList[i])
            .append("</pid>\n");
        }
        xml.append("</pidList>\n");
        return xml.toString();
    }

    public static String objectProfileToXML(
            ObjectProfile objProfile,
            Date versDateTime)  {
        StringBuilder buffer = new StringBuilder(1024);

        String pid = objProfile.PID;
        String dateString = "";
        if (versDateTime != null) {
            String tmp = DateUtility.convertDateToString(versDateTime);
            if (tmp != null) {
                dateString = String.format("dateTime=\"%s\" ", tmp);
            }
        }

        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<objectProfile  xmlns=\"")
        .append(Constants.OBJ_PROFILE1_0.namespace.uri)
        .append("\"  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"")
        .append(Constants.OBJ_PROFILE1_0.namespace.uri)
        .append(' ')
        .append(Constants.OBJ_PROFILE1_0.xsdLocation)
        .append("\" pid=\"");
        enc(pid, buffer);
        buffer.append("\" ");
        buffer.append(dateString);
        buffer.append('>');

        // PROFILE FIELDS SERIALIZATION
        buffer.append("<objLabel>");
        enc(objProfile.objectLabel, buffer);
        buffer.append("</objLabel><objOwnerId>");
        enc(objProfile.objectOwnerId, buffer);
        buffer.append("</objOwnerId><objModels>");
        for (String model : objProfile.objectModels) {
            buffer.append("<model>");
            enc(model, buffer);
            buffer.append("</model>");
        }
        buffer.append("</objModels>");
        String cDate = DateUtility.convertDateToString(objProfile.objectCreateDate);
        buffer.append("<objCreateDate>")
        .append(cDate)
        .append("</objCreateDate>");
        String mDate = DateUtility.convertDateToString(objProfile.objectLastModDate);
        buffer.append("<objLastModDate>")
        .append(mDate)
        .append("</objLastModDate><objDissIndexViewURL>");
        enc(objProfile.dissIndexViewURL, buffer);
        buffer.append("</objDissIndexViewURL><objItemIndexViewURL>");
        enc(objProfile.itemIndexViewURL, buffer);
        buffer.append("</objItemIndexViewURL><objState>");
        enc(objProfile.objectState, buffer);
        buffer.append("</objState></objectProfile>");

        return buffer.toString();
    }

    private void datastreamFieldSerialization(Datastream dsProfile, String prefix,
            boolean validateChecksum, StringBuilder buffer) {
        appendXML(null, prefix, "dsLabel", dsProfile.DSLabel, buffer, true);
        appendXML(null, prefix, "dsVersionID", dsProfile.DSVersionID, buffer, true);

        String cDate = DateUtility.convertDateToString(dsProfile.DSCreateDT);
        appendXML(null, prefix, "dsCreateDate", cDate, buffer, true);
        appendXML(null, prefix, "dsState", dsProfile.DSState, buffer, true);
        appendXML(null, prefix, "dsMIME", dsProfile.DSMIME, buffer, true);
        appendXML(null, prefix, "dsFormatURI", dsProfile.DSFormatURI, buffer, true);
        appendXML(null, prefix, "dsControlGroup", dsProfile.DSControlGrp, buffer, true);
        appendXML(null, prefix, "dsSize", Long.toString(dsProfile.DSSize), buffer, true);
        appendXML(null, prefix, "dsVersionable", Boolean.toString(dsProfile.DSVersionable), buffer, true);
        appendXML(null, prefix, "dsInfoType", dsProfile.DSInfoType, buffer, true);
        appendXML(null, prefix, "dsLocation", dsProfile.DSLocation, buffer, true);
        appendXML(null, prefix, "dsLocationType", dsProfile.DSLocationType, buffer, true);
        appendXML(null, prefix, "dsChecksumType", dsProfile.DSChecksumType, buffer, true);
        appendXML(null, prefix, "dsChecksum", dsProfile.DSChecksum, buffer, true);

        if (validateChecksum) {
            String valid = dsProfile.compareChecksum() ? "true" : "false";
            appendXML(null, prefix, "dsChecksumValid", valid, buffer);
        }
        String[] dsAltIDs = dsProfile.DatastreamAltIDs;
        for (int i = 0; i < dsAltIDs.length; i++) {
            appendXML(null, prefix, "dsAltID", dsAltIDs[i], buffer);
        }
    }

    String datastreamProfileToXML(String pid, String dsID, Datastream dsProfile, Date versDateTime,
                                  boolean validateChecksum) {
        StringBuilder buffer = new StringBuilder(512);

        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<datastreamProfile  xmlns=\"")
        .append(Constants.MANAGEMENT.uri)
        .append("\"  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/management/ "
                + "http://www.fedora.info/definitions/1/0/datastreamProfile.xsd\" pid=\"");
        enc(pid, buffer);
        buffer.append("\" dsID=\"");
        enc(dsID, buffer);
        buffer.append('"');
        if (versDateTime != null &&
            !DateUtility.convertDateToString(versDateTime).equalsIgnoreCase("")) {
            buffer.append(" dateTime=\"" + DateUtility.convertDateToString(versDateTime) + "\"");
        }
        buffer.append('>');
        // ADD PROFILE FIELDS SERIALIZATION
        datastreamFieldSerialization(dsProfile, "", validateChecksum, buffer);

        buffer.append("</datastreamProfile>");
        return buffer.toString();
    }

    String datastreamProfilesToXML(String pid, Datastream[] dsProfiles, Date versDateTime,
                                  boolean validateChecksum){
        StringBuilder builder = new StringBuilder(2048);
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<objectDatastreams xmlns=\"")
        .append(Constants.ACCESS.uri)
        .append("\" xmlns:apim=\"")
        .append(Constants.MANAGEMENT.uri)
        .append("\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/ ");
        baseUrl(builder);
        builder.append("/schema/listDatastreams.xsd\" pid=\"");
        enc(pid, builder);
        builder.append('"');
        if (versDateTime != null) {
            String tmp = DateUtility.convertDateToString(versDateTime);
            if (tmp != null) {
                builder.append(String.format(" asOfDateTime=\"%s\"", tmp));
            }
        }
        builder.append(" baseURL=\"");
        baseUrl(builder);
        builder.append("/\" >");
        for (Datastream ds:dsProfiles){
            builder.append("<datastreamProfile pid=\"");
            enc(pid, builder);
            builder.append("\" dsID=\"");
            enc(ds.DatastreamID, builder);
            builder.append("\" >");
            datastreamFieldSerialization(ds, "apim", validateChecksum, builder);
            builder.append("</datastreamProfile>");
        }
        builder.append("</objectDatastreams>");
        return builder.toString();
    }

    static String objectHistoryToXml(
            String[] objectHistory,
            String pid)  {
        StringBuilder buffer = new StringBuilder(1024);
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><fedoraObjectHistory  xmlns=\"")
        .append(Constants.OBJ_HISTORY1_0.namespace.uri)
        .append("\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"")
        .append(Constants.OBJ_HISTORY1_0.namespace.uri)
        .append(' ')
        .append(Constants.OBJ_HISTORY1_0.xsdLocation)
        .append("\" pid=\"");
        enc(pid, buffer);
        buffer.append("\" >");

        for (String ts : objectHistory) {
            buffer.append("<objectChangeDate>")
            .append(ts)
            .append("</objectChangeDate>");
        }
        buffer.append("</fedoraObjectHistory>");
        return buffer.toString();
    }

    String datastreamHistoryToXml(String pid, String dsID, Datastream[] history) {
        StringBuilder buffer = new StringBuilder(1024);
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<datastreamHistory  xmlns=\"")
        .append(Constants.MANAGEMENT.uri)
        .append("\"  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/management/ "
                + "http://www.fedora.info/definitions/1/0/datastreamHistory.xsd" + "\" pid=\"");
        enc(pid, buffer);
        buffer.append("\" dsID=\"");
        enc(dsID, buffer);
        buffer.append("\">");

        for (Datastream ds : history) {
            buffer.append("<datastreamProfile pid=\"");
            enc(pid, buffer);
            buffer.append("\" dsID=\"");
            enc(dsID, buffer);
            buffer.append("\">");
            datastreamFieldSerialization(ds, "", false, buffer);
            buffer.append("</datastreamProfile>");
        }

        buffer.append("</datastreamHistory>");
        return buffer.toString();

    }

    public String objectMethodsToXml(
            ObjectMethodsDef[] methodDefs,
            String pid,
            String sDef,
            Date versDateTime) {
        StringBuilder urlBuf = new StringBuilder(128);
        baseUrl(urlBuf);
        return objectMethodsToXml(urlBuf.toString(), methodDefs, pid, sDef, versDateTime);
    }

    public static String objectMethodsToXml(
            String baseUrl,
            ObjectMethodsDef[] methodDefs,
            String pid,
            String sDef,
            Date versDateTime) {
        StringBuilder buffer = new StringBuilder(1024);

        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<objectMethods xmlns=\"")
        .append(Constants.OBJ_METHODS1_0.namespace.uri)
        .append("\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"")
        .append(Constants.OBJ_METHODS1_0.namespace.uri)
        .append(' ')
        .append(Constants.OBJ_METHODS1_0.xsdLocation)
        .append("\"  pid=\"");
        enc(pid, buffer);
        buffer.append('"');
        if (versDateTime != null) {
            buffer.append(" asOfDateTime=\"")
            .append(DateUtility.convertDateToString(versDateTime))
            .append('"');
        }
        if (sDef != null) {
            buffer.append(" sDef=\"");
            enc(sDef, buffer);
            buffer.append("\"");
        }
        buffer.append(" baseURL=\"");
        buffer.append(baseUrl);
        buffer.append("/\" >");

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
                    buffer.append("<sDef pid=\"");
                    enc(methodDefs[i].sDefPID, buffer);
                    buffer.append("\" >");
                }
                buffer.append("<method name=\"");
                enc(methodDefs[i].methodName, buffer);
                buffer.append("\" >");
                MethodParmDef[] methodParms = methodDefs[i].methodParmDefs;
                for (int j = 0; j < methodParms.length; j++) {
                    buffer.append("<methodParm parmName=\"");
                    enc(methodParms[j].parmName, buffer);
                    buffer.append("\" parmDefaultValue=\"");
                    enc(methodParms[j].parmDefaultValue, buffer);
                    buffer.append("\" parmRequired=\"");
                    buffer.append(methodParms[j].parmRequired);
                    buffer.append("\" parmLabel=\"");
                    enc(methodParms[j].parmLabel, buffer);
                    buffer.append("\" >");
                    if (methodParms[j].parmDomainValues.length > 0) {
                        buffer.append("<methodParmDomain>");
                        for (int k = 0; k < methodParms[j].parmDomainValues.length; k++) {
                            buffer.append("<methodParmValue>");
                            enc(methodParms[j].parmDomainValues[k], buffer);
                            buffer.append("</methodParmValue>");
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
        StringBuilder html = new StringBuilder(2048);
        HashSet<String> fieldHash = new HashSet<String>();

        if (wantedFields != null) {
            for (String wf : wantedFields) {
                fieldHash.add(wf);
            }
        }

        html.append("<html><head><title>Search Repository</title></head>"
                + "<body><center>"
                + "<table width=\"784\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td width=\"141\" height=\"134\" valign=\"top\"><img src=\"" + "/");
        html.append(fedoraAppServerContext);
        html.append("/images/newlogo2.jpg\" width=\"141\" height=\"134\"/></td>"
                + "<td width=\"643\" valign=\"top\">"
                + "<center><h2>Fedora Repository</h2>"
                + "<h3>Find Objects</h3>"
                + "</center></td></tr></table>\n"
                + "<form method=\"get\">"
                + "<center><table border=\"0\" cellpadding=\"6\" cellspacing=\"0\">\n"
                + "<tr><td colspan=\"3\" valign=\"top\"><i>Fields to display:</i></td><td></td></tr>"
                + "<tr>");

        int fieldPerCol = (searchableFields.length / 3) + 1;
        for (int i = 0; i < searchableFields.length; i++) {
            boolean everOtherNFields = (i % fieldPerCol == 0);

            if (everOtherNFields) {
                if (i > 0) {
                    html.append("</font></td>\n");
                }

                html.append("<td valign=\"top\"><font size=\"-1\">\n");
            }

            html.append("<input type=\"checkbox\" name=\"");
            html.append(searchableFields[i]);
            html.append("\" value=\"true\" ");
            html.append((RestParam.PID.equals(searchableFields[i])
                            || "title".equals(searchableFields[i]) || fieldHash.contains(searchableFields[i])) ?
                           "checked=\"checked\"" : "");
            html.append("> <a href='#'>");
            html.append(searchableFields[i]);
            html.append("</a><br/>\n");
        }
        html.append("</font></td><td bgcolor=\"silver\" valign=\"top\">&nbsp;&nbsp;&nbsp;</td><td valign=\"top\">"
                + "Search all fields for phrase: <input type=\"text\" name=\"terms\" size=\"15\" value=\"");
        if (terms != null) enc(terms, html);
        html.append("\"> <a href=\"#\" onClick=\"javascript:alert('Search All Fields\\n\\nEnter a phrase.  Objects where any field contains the phrase will be returned.\\nThis is a case-insensitive search, and you may use the * or ? wildcards.\\n\\nExamples:\\n\\n  *o*\\n    finds objects where any field contains the letter o.\\n\\n  ?edora\\n    finds objects where a word starts with any letter and ends with edora.')\"><i>help</i></a><p> ");
        html.append("Or search specific field(s): <input type=\"text\" name=\"query\" size=\"15\" value=\"");
        if (query != null) enc(query, html);
        html.append("\"> <a href=\"#\" onClick=\"javascript:alert('Search Specific Field(s)\\n\\nEnter one or more conditions, separated by space.  Objects matching all conditions will be returned.\\nA condition is a field (choose from the field names on the left) followed by an operator, followed by a value.\\nThe = operator will match if the field\\'s entire value matches the value given.\\nThe ~ operator will match on phrases within fields, and accepts the ? and * wildcards.\\nThe &lt;, &gt;, &lt;=, and &gt;= operators can be used with numeric values, such as dates.\\n\\nExamples:\\n\\n  pid~demo:* description~fedora\\n    Matches all demo objects with a description containing the word fedora.\\n\\n  cDate&gt;=1976-03-04 creator~*n*\\n    Matches objects created on or after March 4th, 1976 where at least one of the creators has an n in their name.\\n\\n  mDate&gt;2002-10-2 mDate&lt;2002-10-2T12:00:00\\n    Matches objects modified sometime before noon (UTC) on October 2nd, 2002')\"><i>help</i></a><p> "
                + "Maximum Results: <select name=\"maxResults\"><option value=\"20\">20</option><option value=\"40\">40</option><option value=\"60\">60</option><option value=\"80\">80</option></select> "
                + "<p><input type=\"submit\" value=\"Search\"> "
                + "</td></tr></table></center>"
                + "</form><hr size=\"1\">");

        if (result != null) {
            List<ObjectFields> objectFieldList = result.objectFieldsList();

            html.append(
                    "<center><table width=\"90%\" border=\"1\" cellpadding=\"5\" cellspacing=\"5\" bgcolor=\"silver\">\n"
                    + "<tr>");
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
                        html.append("<a href=\"/" + fedoraAppServerContext + "/objects/");
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
                            enc(f.getLabel(), html);
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
                        join(f.titles(), html);
                    } else if (l.equalsIgnoreCase("creator")) {
                        join(f.creators(), html);
                    } else if (l.equalsIgnoreCase("subject")) {
                        join(f.subjects(), html);
                    } else if (l.equalsIgnoreCase("description")) {
                        join(f.descriptions(), html);
                    } else if (l.equalsIgnoreCase("publisher")) {
                        join(f.publishers(), html);
                    } else if (l.equalsIgnoreCase("contributor")) {
                        join(f.contributors(), html);
                    } else if (l.equalsIgnoreCase("date")) {
                        join(f.dates(), html);
                    } else if (l.equalsIgnoreCase("type")) {
                        join(f.types(), html);
                    } else if (l.equalsIgnoreCase("format")) {
                        join(f.formats(), html);
                    } else if (l.equalsIgnoreCase("identifier")) {
                        join(f.identifiers(), html);
                    } else if (l.equalsIgnoreCase("source")) {
                        join(f.sources(), html);
                    } else if (l.equalsIgnoreCase("language")) {
                        join(f.languages(), html);
                    } else if (l.equalsIgnoreCase("relation")) {
                        join(f.relations(), html);
                    } else if (l.equalsIgnoreCase("coverage")) {
                        join(f.coverages(), html);
                    } else if (l.equalsIgnoreCase("rights")) {
                        join(f.rights(), html);
                    }
                    html.append("</td>\n");
                }
                html.append("</tr>\n<tr><td colspan=\"");
                html.append(Integer.toString(wantedFields.length));
                html.append("\"></td></tr>\n");
            }
            html.append("</table>\n");

            if (result != null && result.getToken() != null) {
                if (result.getCursor() != -1) {
                    long viewingStart = result.getCursor() + 1;
                    long viewingEnd = result.objectFieldsList().size() + viewingStart - 1;
                    html.append("<p>Viewing results ");
                    html.append(Long.toString(viewingStart));
                    html.append(" to ");
                    html.append(Long.toString(viewingEnd));
                    if (result.getCompleteListSize() != -1) {
                        html.append(" of " + result.getCompleteListSize());
                    }
                    html.append("</p>\n");
                }
                html.append("<form method=\"get\" action=\"\">\n");
                for (String field : wantedFields) {
                    html.append("<input type=\"hidden\" name=\"");
                    html.append(field);
                    html.append("\" value=\"true\">\n");
                }

                html.append("\n\n<input type=\"hidden\" name=\"sessionToken\" value=\"");
                html.append(result.getToken());
                html.append("\">\n"
                        + "\n<input type=\"hidden\" name=\"maxResults\" value=\"");
                html.append(Integer.toString(maxResults));
                html.append("\">\n<input type=\"submit\" value=\"More Results &gt;\"></form>");
            }
            html.append("</center>\n");
        }

        html.append("</center></body></html>");

        return html.toString();
    }

    String searchResultToXml(
            FieldSearchResult result) {

        StringBuilder xmlBuf = new StringBuilder(2048);
        xmlBuf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<result xmlns=\"http://www.fedora.info/definitions/1/0/types/\" "
                + "xmlns:types=\"http://www.fedora.info/definitions/1/0/types/\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/types/ ");
        baseUrl(xmlBuf);
        xmlBuf.append("/schema/findObjects.xsd\">\n");
        if ((result != null) && (result.getToken() != null)) {
            xmlBuf.append("  <listSession>\n    <token>")
            .append(result.getToken())
            .append("</token>\n");
            if (result.getCursor() != -1) {
                xmlBuf.append("    <cursor>");
                xmlBuf.append(Long.toString(result.getCursor()));
                xmlBuf.append("</cursor>\n");
            }
            if (result.getCompleteListSize() != -1) {
                xmlBuf.append("    <completeListSize>");
                xmlBuf.append(Long.toString(result.getCompleteListSize()));
                xmlBuf.append("</completeListSize>\n");
            }
            if (result.getExpirationDate() != null) {
                xmlBuf.append("    <expirationDate>");
                xmlBuf.append(DateUtility.convertDateToString(result.getExpirationDate()));
                xmlBuf.append("</expirationDate>\n");
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
        xmlBuf.append("  </resultList>\n</result>\n");

        return xmlBuf.toString();
    }
    
    private void baseUrl(StringBuilder baseUrlBuf) {
        enc(fedoraServerProtocol, baseUrlBuf);
        baseUrlBuf.append("://");
        enc(fedoraServerHost, baseUrlBuf);
        baseUrlBuf.append(':');
        enc(fedoraServerPort, baseUrlBuf);
        baseUrlBuf.append('/').append(fedoraAppServerContext);
    }

    static private void join(
            List<DCField> l, StringBuilder ret) {
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) {
                ret.append(", ");
            }
            enc(l.get(i).getValue(), ret);
        }
    }

    private static void appendXML(String name, String value, StringBuilder out) {
        appendXML("      ", null, name, value, out);
    }

    private static void appendXML(String indent, String prefix, String name, String value, StringBuilder out) {
        appendXML(indent, prefix, name, value, out, false);
    }
    
    private static void appendXML(String indent, String prefix, String name,
            String value, StringBuilder out, boolean force) {
        if (value != null || force) {
            if (indent != null) out.append(indent);
            out.append('<');
            if (prefix != null && !"".equals(prefix)) {
                out.append(prefix);
                out.append(':');
            }
            out.append(name);
            out.append('>');
            enc(value, out);
            out.append("</");
            if (prefix != null && !"".equals(prefix)) {
                out.append(prefix);
                out.append(':');
            }
            out.append(name);
            out.append(">\n");
        }
    }

    private static void appendXML(String name, List<DCField> values, StringBuilder out) {
        for (DCField value: values) {
            appendXML(name, value.getValue(), out);
        }
    }

    private static void appendXML(
            String name,
            Date dt,
            StringBuilder out) {
        if (dt != null) {
            appendXML(name, DateUtility.convertDateToString(dt), out);
        }
    }

    public String dataStreamsToXML(
            String pid,
            Date asOfDateTime,
            DatastreamDef[] dsDefs) {
        StringBuilder xml = new StringBuilder(1024);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<objectDatastreams  xmlns=\"")
        .append(Constants.OBJ_DATASTREAMS1_0.namespace.uri)
        .append("\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"")
        .append(Constants.OBJ_DATASTREAMS1_0.namespace.uri)
        .append(' ')
        .append(Constants.OBJ_DATASTREAMS1_0.xsdLocation)
        .append("\" pid=\"");
        enc(pid, xml);
        xml.append('"');
        if (asOfDateTime != null) {
            String tmp = DateUtility.convertDateToString(asOfDateTime);
            if (tmp != null) {
                xml.append(String.format(" asOfDateTime=\"%s\"", tmp));
            }
        }
        xml.append(" baseURL=\"");
        baseUrl(xml);
        xml.append("/\" >");

        // DatastreamDef SERIALIZATION
        for (int i = 0; i < dsDefs.length; i++) {
            xml.append("    <datastream dsid=\"");
            enc(dsDefs[i].dsID, xml);
            xml.append("\" label=\"");
            enc(dsDefs[i].dsLabel, xml);
            xml.append("\" mimeType=\"");
            enc(dsDefs[i].dsMIME, xml);
            xml.append("\" />");
        }
        xml.append("</objectDatastreams>");

        return xml.toString();
    }

    public String objectValidationToXml(Validation validation) {
        StringBuilder buffer = new StringBuilder(1024);
        String pid = validation.getPid();
        Date date = validation.getAsOfDateTime();
        String dateString = null;
        boolean valid = validation.isValid();
        if (date != null) {
            dateString = DateUtility.convertDateToString(date);
        }
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        buffer.append("<management:validation "
                + " xmlns:management=\"" + Constants.OBJ_VALIDATION1_0.namespace.uri + "\""
                + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"" + Constants.OBJ_VALIDATION1_0.namespace.uri
                + " " + Constants.OBJ_VALIDATION1_0.xsdLocation + "\" pid=\"");
        enc(pid, buffer);
        buffer.append("\"  valid=\"")
        .append(valid)
        .append("\">\n");
        if (date != null) {
        	buffer.append("  <management:asOfDateTime>" + dateString + "</management:asOfDateTime>\n");
        }
        buffer.append("  <management:contentModels>\n");
        for (String model : validation.getContentModels()) {
            buffer.append("    <management:model>");
            enc(model, buffer);
            buffer.append("</management:model>\n");
        }
        buffer.append("  </management:contentModels>\n  <management:problems>\n");
        for (String problem : validation.getObjectProblems()) {
            buffer.append("    <management:problem>");
            buffer.append(problem);
            buffer.append("</management:problem>\n");
        }
        buffer.append("  </management:problems>\n  <management:datastreamProblems>\n");
        Map<String, List<String>> dsprobs = validation.getDatastreamProblems();
        for (String ds : dsprobs.keySet()) {
            List<String> problems = dsprobs.get(ds);
            buffer.append("    <management:datastream datastreamID=\"");
            buffer.append(ds);
            buffer.append("\">\n");
            for (String problem : problems) {
                buffer.append("      <management:problem>");
                buffer.append(problem);
                buffer.append("</management:problem>\n");
            }
            buffer.append("    </management:datastream>");
        }
        buffer.append("  </management:datastreamProblems>\n</management:validation>");
        return buffer.toString();
    }


}
