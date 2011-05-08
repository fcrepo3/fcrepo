/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access.defaultdisseminator;

import java.io.File;
import java.io.InputStream;

import java.util.Date;

import org.fcrepo.common.Constants;

import org.fcrepo.server.Context;
import org.fcrepo.server.Server;
import org.fcrepo.server.access.ObjectProfile;
import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.rest.DefaultSerializer;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.ObjectMethodsDef;
import org.fcrepo.server.utilities.DCFields;
import org.fcrepo.server.utilities.StreamUtility;

import org.fcrepo.utilities.DateUtility;



/**
 * Provide an XML encoding of various object components.
 *
 * @author Sandy Payette
 * @version $Id$
 */
public class ObjectInfoAsXML
        implements Constants {

    private final Context m_context;

    public ObjectInfoAsXML(Context context) {
        m_context = context;
    }

    public String getObjectProfile(String reposBaseURL,
                                   ObjectProfile objProfile,
                                   Date versDateTime) throws ServerException {

        // use REST serializer
        Server fedoraServer = Server.getInstance(new File(Constants.FEDORA_HOME), false);
        String fedoraServerHost = fedoraServer.getParameter("fedoraServerHost");
        DefaultSerializer ser = new DefaultSerializer(fedoraServerHost, m_context);
        return ser.objectProfileToXML(objProfile, versDateTime);
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

        // use REST serializer
        Server fedoraServer = Server.getInstance(new File(Constants.FEDORA_HOME), false);
        String fedoraServerHost = fedoraServer.getParameter("fedoraServerHost");
        DefaultSerializer ser = new DefaultSerializer(fedoraServerHost, m_context);
        return ser.objectMethodsToXml(methods, PID, null, versDateTime);

    }

    public String getOAIDublinCore(Datastream dublinCore)
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