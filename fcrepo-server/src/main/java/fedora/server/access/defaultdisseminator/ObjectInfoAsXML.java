/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access.defaultdisseminator;

import java.io.InputStream;

import java.util.Date;

import fedora.common.Constants;

import fedora.server.access.ObjectProfile;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.ServerException;
import fedora.server.storage.DOReader;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.MethodParmDef;
import fedora.server.storage.types.ObjectMethodsDef;
import fedora.server.utilities.DCFields;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.StreamUtility;

/**
 * Provide an XML encoding of various object components.
 *
 * @author Sandy Payette
 * @version $Id$
 */
public class ObjectInfoAsXML
        implements Constants {

    public ObjectInfoAsXML() {
    }

    public String getObjectProfile(String reposBaseURL,
                                   ObjectProfile objProfile,
                                   Date versDateTime) throws ServerException {
        StringBuffer out = new StringBuffer();
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.append("<objectProfile");
        out.append(" pid=\"" + objProfile.PID + "\"");
        if (versDateTime != null) {
            out.append(" dateTime=\"");
            out.append(DateUtility.convertDateToString(versDateTime));
            out.append("\"");
        }
        out.append(" xmlns:xsi=\"" + XSI.uri + "\"");
        out.append(" xsi:schemaLocation=\"" + ACCESS.uri + " ");
        out.append(OBJ_PROFILE1_0.xsdLocation + "\">");

        // PROFILE FIELDS SERIALIZATION
        out.append("<objLabel>" + StreamUtility.enc(objProfile.objectLabel)
                + "</objLabel>");

        String cDate =
                DateUtility.convertDateToString(objProfile.objectCreateDate);
        out.append("<objCreateDate>" + cDate + "</objCreateDate>");
        String mDate =
                DateUtility.convertDateToString(objProfile.objectLastModDate);
        out.append("<objLastModDate>" + mDate + "</objLastModDate>");
        out.append("<objDissIndexViewURL>"
                + StreamUtility.enc(objProfile.dissIndexViewURL)
                + "</objDissIndexViewURL>");
        out.append("<objItemIndexViewURL>"
                + StreamUtility.enc(objProfile.itemIndexViewURL)
                + "</objItemIndexViewURL>");
        out.append("</objectProfile>");
        return out.toString();
    }

    public String getItemIndex(String reposBaseURL,
                               String applicationContext,
                               DOReader reader,
                               Date versDateTime) throws ServerException {
        try {
            Datastream[] datastreams =
                    reader.GetDatastreams(versDateTime, null);
            StringBuffer out = new StringBuffer();

            out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            out.append("<objectItemIndex");
            out.append(" PID=\"" + reader.GetObjectPID() + "\"");
            if (versDateTime != null) {
                out.append(" dateTime=\"");
                out.append(DateUtility.convertDateToString(versDateTime));
                out.append("\"");
            }
            out.append(" xmlns:xsi=\"" + XSI.uri + "\"");
            out.append(" xsi:schemaLocation=\"" + ACCESS.uri + " ");
            out.append(OBJ_ITEMS1_0.xsdLocation + "\">");

            for (Datastream element : datastreams) {
                out.append("<item>\n");
                out.append("<itemId>" + StreamUtility.enc(element.DatastreamID)
                        + "</itemId>\n");
                String label = element.DSLabel;
                if (label == null) {
                    label = "";
                }
                out.append("<itemLabel>" + StreamUtility.enc(label)
                        + "</itemLabel>\n");

                String itemDissURL =
                        getItemDissURL(reposBaseURL,
                                       applicationContext,
                                       reader.GetObjectPID(),
                                       element.DatastreamID,
                                       versDateTime);
                out.append("<itemURL>" + StreamUtility.enc(itemDissURL)
                        + "</itemURL>\n");
                out.append("<itemMIMEType>" + StreamUtility.enc(element.DSMIME)
                        + "</itemMIMEType>\n");
                out.append("</item>\n");
            }
            out.append("</objectItemIndex>");
            return out.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ObjectIntegrityException(e.getMessage());
        }
    }

    public String getMethodIndex(String reposBaseURL,
                                 String PID,
                                 ObjectMethodsDef[] methods,
                                 Date versDateTime) throws ServerException {
        StringBuffer out = new StringBuffer();

        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.append("<objectMethods");
        out.append(" pid=\"" + PID + "\"");
        if (versDateTime != null) {
            out.append(" dateTime=\"");
            out.append(DateUtility.convertDateToString(versDateTime));
            out.append("\"");
        }
        out.append(" xmlns:xsi=\"" + XSI.uri + "\"");
        out.append(" xsi:schemaLocation=\"" + ACCESS.uri + " ");
        out.append(OBJ_METHODS1_0.xsdLocation + "\">");

        String nextSdef = "null";
        String currentSdef = "";
        for (int i = 0; i < methods.length; i++) {
            currentSdef = methods[i].sDefPID;
            if (!currentSdef.equalsIgnoreCase(nextSdef)) {
                if (i != 0) {
                    out.append("</sdef>");
                }
                out.append("<sdef pid=\""
                        + StreamUtility.enc(methods[i].sDefPID) + "\" >");
            }
            String versDate =
                    DateUtility.convertDateToString(methods[i].asOfDate);
            out.append("<method name=\""
                    + StreamUtility.enc(methods[i].methodName)
                    + "\" asOfDateTime=\"" + versDate + "\" >");
            MethodParmDef[] methodParms = methods[i].methodParmDefs;
            for (MethodParmDef element : methodParms) {
                out.append("<parm parmName=\""
                        + StreamUtility.enc(element.parmName)
                        + "\" parmDefaultValue=\""
                        + StreamUtility.enc(element.parmDefaultValue)
                        + "\" parmRequired=\"" + element.parmRequired
                        + "\" parmType=\""
                        + StreamUtility.enc(element.parmType)
                        + "\" parmLabel=\""
                        + StreamUtility.enc(element.parmLabel) + "\" >");
                if (element.parmDomainValues.length > 0) {
                    out.append("<parmDomainValues>");
                    for (String element2 : element.parmDomainValues) {
                        out.append("<value>" + StreamUtility.enc(element2)
                                + "</value>");
                    }
                    out.append("</parmDomainValues>");
                }
                out.append("</parm>");
            }

            out.append("</method>");
            nextSdef = currentSdef;
        }
        out.append("</sdef>");
        out.append("</objectMethods>");
        return out.toString();
    }

    public String getOAIDublinCore(DatastreamXMLMetadata dublinCore)
            throws ServerException {
        DCFields dc;
        if (dublinCore == null) {
            dc = new DCFields();
        } else {
            InputStream in = dublinCore.getContentStream();
            dc = new DCFields(in);
        }
        return dc.getAsXML();
    }

    private String getItemDissURL(String reposBaseURL,
                                  String applicationContext,
                                  String PID,
                                  String datastreamID,
                                  Date versDateTime) {
        String itemDissURL = null;

        if (versDateTime == null) {
            itemDissURL =
                    reposBaseURL + "/" + applicationContext + "/get/" + PID + "/" + datastreamID;
        } else {
            itemDissURL =
                    reposBaseURL + "/" + applicationContext + "/get/" + PID + "/" + datastreamID
                            + "/"
                            + DateUtility.convertDateToString(versDateTime);
        }
        return itemDissURL;
    }
}