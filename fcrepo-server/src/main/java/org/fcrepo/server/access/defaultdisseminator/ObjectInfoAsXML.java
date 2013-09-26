/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access.defaultdisseminator;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Date;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.access.ObjectProfile;
import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.rest.DefaultSerializer;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.ObjectMethodsDef;
import org.fcrepo.server.utilities.DCFields;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.utilities.DateUtility;
import org.fcrepo.utilities.ReadableCharArrayWriter;



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

    public static String getObjectProfile(String reposBaseURL,
                                   ObjectProfile objProfile,
                                   Date versDateTime) throws ServerException {
        ReadableCharArrayWriter buffer = new ReadableCharArrayWriter(1024);
        getObjectProfile(reposBaseURL, objProfile, versDateTime, buffer);
        return buffer.getString();
    }

    public static void getObjectProfile(String reposBaseURL,
                ObjectProfile objProfile,
                Date versDateTime, Writer out) throws ServerException {
        // use REST serializer
        try {
            DefaultSerializer.objectProfileToXML(objProfile, versDateTime, out);
        } catch (IOException e) {
            throw new StreamIOException(e.getMessage(), e);
        }
    }

    public static String getItemIndex(String reposBaseURL,
                               String applicationContext,
                               DOReader reader,
                               Date versDateTime) throws ServerException {
        ReadableCharArrayWriter out = new ReadableCharArrayWriter(512);
        getItemIndex(reposBaseURL, applicationContext, reader, versDateTime, out);
        return out.getString();
    }
    
    public static void getItemIndex(String reposBaseURL,
                String applicationContext,
                DOReader reader,
                Date versDateTime,
                Writer out) throws ServerException {
        try {
            Datastream[] datastreams =
                    reader.GetDatastreams(versDateTime, null);

            out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<objectItemIndex PID=\"");
            out.append(reader.GetObjectPID());
            out.append('"');
            if (versDateTime != null) {
                out.append(" dateTime=\"");
                out.append(DateUtility.convertDateToString(versDateTime));
                out.append('"');
            }
            out.append(" xmlns:xsi=\"");
            out.append(XSI.uri);
            out.append("\" xsi:schemaLocation=\"");
            out.append(ACCESS.uri);
            out.append(' ');
            out.append(OBJ_ITEMS1_0.xsdLocation);
            out.append("\">");

            for (Datastream element : datastreams) {
                out.append("<item>\n<itemId>");
                StreamUtility.enc(element.DatastreamID, out);
                out.append("</itemId>\n");
                String label = element.DSLabel;
                out.append("<itemLabel>");
                StreamUtility.enc(label, out);
                out.append("</itemLabel>\n");

                String itemDissURL =
                        getItemDissURL(reposBaseURL,
                                       applicationContext,
                                       reader.GetObjectPID(),
                                       element.DatastreamID,
                                       versDateTime);
                out.append("<itemURL>");
                StreamUtility.enc(itemDissURL, out);
                out.append("</itemURL>\n<itemMIMEType>");
                StreamUtility.enc(element.DSMIME, out);
                out.append("</itemMIMEType>\n</item>\n");
            }
            out.append("</objectItemIndex>");
        } catch (Exception e) {
            e.printStackTrace();
            throw new ObjectIntegrityException(e.getMessage());
        }
    }

    public static String getMethodIndex(String reposBaseURL,
                                 String PID,
                                 ObjectMethodsDef[] methods,
                                 Date versDateTime) throws ServerException {
        ReadableCharArrayWriter buffer =
                new ReadableCharArrayWriter(1024);
        getMethodIndex(reposBaseURL, PID, methods, versDateTime, buffer);
        return buffer.getString();
    }

    public static void getMethodIndex(String reposBaseURL,
                String PID,
                ObjectMethodsDef[] methods,
                Date versDateTime,
                Writer buffer) throws ServerException {
        // use REST serializer
        try {
            DefaultSerializer.objectMethodsToXml(reposBaseURL, methods, PID, null, versDateTime, buffer);
        } catch (IOException e) {
            throw new StreamIOException(e.getMessage(), e);
        }

    }

    public static String getOAIDublinCore(Datastream dublinCore)
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
    
    public static void getOAIDublinCore(Datastream dublinCore, Writer out)
            throws ServerException {
        DCFields dc;
        if (dublinCore == null) {
            dc = new DCFields();
        } else {
            InputStream in = dublinCore.getContentStream();
            dc = new DCFields(in);
        }
        try {
            dc.getAsXML(out);
        } catch (IOException e) {
            throw new StreamIOException(e.getMessage(), e);
        }
    }

    private static String getItemDissURL(String reposBaseURL,
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