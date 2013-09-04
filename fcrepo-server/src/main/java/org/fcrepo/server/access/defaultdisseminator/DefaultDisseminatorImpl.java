/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access.defaultdisseminator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.fcrepo.common.Constants;
import org.fcrepo.common.Models;
import org.fcrepo.server.Context;
import org.fcrepo.server.access.Access;
import org.fcrepo.server.access.ObjectProfile;
import org.fcrepo.server.errors.DisseminationException;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.rest.DefaultSerializer;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.fcrepo.server.storage.types.MethodDef;
import org.fcrepo.server.storage.types.MethodParmDef;
import org.fcrepo.server.storage.types.ObjectMethodsDef;
import org.fcrepo.utilities.ReadableByteArrayOutputStream;
import org.fcrepo.utilities.ReadableCharArrayWriter;
import org.fcrepo.utilities.XmlTransformUtility;




/**
 * Implements the methods defined in the DefaultDisseminator interface. This is
 * the default Service Deployment that implements the "contract" of the default
 * Service Definition that is dynamically associated with every digital object
 * in the repository. This class is considered an "internal service" that is
 * built in to the Fedora system. Its purpose is to endow every digital object
 * with a set of generic behaviors. It is an implementation of what is known as
 * the Default Disseminator. Unlike other Service Definitions and Deployments,
 * there is no Service Definition Object or Service Deployment Object stored in
 * the repository.
 *
 * @author Sandy Payette
 * @version $Id: DefaultDisseminatorImpl.java 7973 2009-04-21 22:03:40Z kstrnad
 *          $
 */
public class DefaultDisseminatorImpl
        extends InternalService
        implements DefaultDisseminator {

    private final Context context;

    private final Date asOfDateTime;

    private final DOReader reader;

    private final String reposBaseURL;

    private final File reposHomeDir;

    private final Access m_access;

    public DefaultDisseminatorImpl(Context context,
                                   Date asOfDateTime,
                                   DOReader reader,
                                   Access access,
                                   String reposBaseURL,
                                   File reposHomeDir)
            throws ServerException {
        this.context = context;
        this.asOfDateTime = asOfDateTime;
        this.reader = reader;
        m_access = access;
        this.reposBaseURL = reposBaseURL;
        this.reposHomeDir = reposHomeDir;

    }

    /**
     * Returns an HTML rendering of the object profile which contains key
     * metadata from the object, plus URLs for the object's Dissemination Index
     * and Item Index. The data is returned as HTML in a presentation-oriented
     * format. This is accomplished by doing an XSLT transform on the XML that
     * is obtained from getObjectProfile in API-A.
     *
     * @return html packaged as a MIMETypedStream
     * @throws ServerException
     */
    public MIMETypedStream viewObjectProfile() throws ServerException {
        try {
            ObjectProfile profile =
                    m_access.getObjectProfile(context,
                                              reader.GetObjectPID(),
                                              asOfDateTime);
            Reader in = null;
            try {
                ReadableCharArrayWriter out = new ReadableCharArrayWriter(1024);
                DefaultSerializer.objectProfileToXML(profile, asOfDateTime, out);
                out.close();
                in = out.toReader();
            } catch (IOException ioe) {
                throw new GeneralException("[DefaultDisseminatorImpl] An error has occurred. "
                        + "The error was a \""
                        + ioe.getClass().getName()
                        + "\"  . The "
                        + "Reason was \""
                        + ioe.getMessage()
                        + "\"  .");
            }
            //InputStream in = getObjectProfile().getStream();
            ReadableByteArrayOutputStream bytes =
                    new ReadableByteArrayOutputStream(4096);
            PrintWriter out =
                    new PrintWriter(
                            new OutputStreamWriter(bytes, Charset.forName("UTF-8")));
            File xslFile =
                    new File(reposHomeDir, "access/viewObjectProfile.xslt");
            Templates template =
                    XmlTransformUtility.getTemplates(xslFile);
            Transformer transformer = template.newTransformer();
            transformer.setParameter("fedora", context
                    .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME));
            transformer.transform(new StreamSource(in), new StreamResult(out));
            out.close();
            return new MIMETypedStream("text/html", bytes.toInputStream(),
                    null, bytes.length());
        } catch (Exception e) {
            throw new DisseminationException("[DefaultDisseminatorImpl] had an error "
                    + "in transforming xml for viewObjectProfile. "
                    + "Underlying exception was: " + e.getMessage());
        }
    }

    /**
     * Returns an HTML rendering of the Dissemination Index for the object. The
     * Dissemination Index is a list of method definitions that represent all
     * disseminations possible on the object. The Dissemination Index is
     * returned as HTML in a presentation-oriented format. This is accomplished
     * by doing an XSLT transform on the XML that is obtained from listMethods
     * in API-A.
     *
     * @return html packaged as a MIMETypedStream
     * @throws ServerException
     */
    public MIMETypedStream viewMethodIndex() throws ServerException {
        // sdp: the dissemination index is disabled for service definition and deployment objects
        // so send back a message saying so.
        if (reader.hasContentModel(Models.SERVICE_DEFINITION_3_0)
                || reader.hasContentModel(Models.SERVICE_DEPLOYMENT_3_0)) {
            return noMethodIndexMsg();
        }

        // get xml expression of method definitions
        ObjectMethodsDef[] methods =
                m_access.listMethods(context,
                                     reader.GetObjectPID(),
                                     asOfDateTime);
        ReadableCharArrayWriter buffer = new ReadableCharArrayWriter(1024);
        ObjectInfoAsXML
        .getMethodIndex(reposBaseURL,
                reader.GetObjectPID(),
                methods,
                asOfDateTime,
                buffer);
        buffer.close();
        Reader in = buffer.toReader();

        // transform the method definitions xml to an html view
        try {
            ReadableByteArrayOutputStream bytes =
                    new ReadableByteArrayOutputStream(2048);
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(bytes, Charset.forName("UTF-8")));
            File xslFile =
                    new File(reposHomeDir, "access/listMethods.xslt");
            Templates template =
                    XmlTransformUtility.getTemplates(xslFile);
            Transformer transformer = template.newTransformer();
            transformer.setParameter("fedora", context
                    .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME));
            transformer.transform(new StreamSource(in), new StreamResult(out));
            out.close();
            return new MIMETypedStream("text/html", bytes.toInputStream(),
                    null, bytes.length());
        } catch (Exception e) {
            throw new DisseminationException("[DefaultDisseminatorImpl] had an error "
                    + "in transforming xml for viewItemIndex. "
                    + "Underlying exception was: " + e.getMessage());
        }
    }

    /**
     * Returns an HTML rendering of the Item Index for the object. The Item
     * Index is a list of all datastreams in the object. The datastream items
     * can be data or metadata. The Item Index is returned as HTML in a
     * presentation-oriented format. This is accomplished by doing an XSLT
     * transform on the XML that is obtained from listDatastreams in API-A.
     *
     * @return html packaged as a MIMETypedStream
     * @throws ServerException
     */
    public MIMETypedStream viewItemIndex() throws ServerException {
        // get the item index as xml
        Reader in = null;
        try {
            ReadableCharArrayWriter out = new ReadableCharArrayWriter(4096);
            ObjectInfoAsXML
                            .getItemIndex(reposBaseURL,
                                          context
                                                  .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME),
                                          reader,
                                          asOfDateTime, out);
            out.close();
            in = out.toReader();
        } catch (Exception e) {
            throw new GeneralException("[DefaultDisseminatorImpl] An error has occurred. "
                    + "The error was a \""
                    + e.getClass().getName()
                    + "\"  . The " + "Reason was \"" + e.getMessage() + "\"  .");
        }

        // convert the xml to an html view
        try {
            //InputStream in = getItemIndex().getStream();
            ReadableByteArrayOutputStream bytes =
                    new ReadableByteArrayOutputStream(2048);
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(bytes, Charset.forName("UTF-8")));
            File xslFile = new File(reposHomeDir, "access/viewItemIndex.xslt");
            Templates template =
                    XmlTransformUtility.getTemplates(xslFile);
            Transformer transformer = template.newTransformer();
            transformer.setParameter("fedora", context
                    .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME));
            transformer.transform(new StreamSource(in), new StreamResult(out));

            out.close();
            return new MIMETypedStream("text/html", bytes.toInputStream(),
                    null, bytes.length());
        } catch (Exception e) {
            throw new DisseminationException("[DefaultDisseminatorImpl] had an error "
                    + "in transforming xml for viewItemIndex. "
                    + "Underlying exception was: " + e.getMessage());
        }
    }

    /**
     * Returns the Dublin Core record for the object, if one exists. The record
     * is returned as HTML in a presentation-oriented format.
     *
     * @return html packaged as a MIMETypedStream
     * @throws ServerException
     */
    public MIMETypedStream viewDublinCore() throws ServerException {
        // get dublin core record as xml
        Datastream dcmd = null;
        Reader in = null;
        try {
            ReadableCharArrayWriter out = new ReadableCharArrayWriter(512);
            dcmd =
                    reader.GetDatastream("DC",
                                                                 asOfDateTime);
            ObjectInfoAsXML.getOAIDublinCore(dcmd, out);
            out.close();
            in = out.toReader();
        } catch (ClassCastException cce) {
            throw new ObjectIntegrityException("Object "
                    + reader.GetObjectPID()
                    + " has a DC datastream, but it's not inline XML.");

        }

        // convert the dublin core xml to an html view
        try {
            //InputStream in = getDublinCore().getStream();
            ReadableByteArrayOutputStream bytes =
                    new ReadableByteArrayOutputStream(1024);
            PrintWriter out =
                    new PrintWriter(
                            new OutputStreamWriter(bytes, Charset.forName("UTF-8")));
            File xslFile = new File(reposHomeDir, "access/viewDublinCore.xslt");
            Templates template =
                    XmlTransformUtility.getTemplates(xslFile);
            Transformer transformer = template.newTransformer();
            transformer.setParameter("fedora", context
                    .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME));
            transformer.transform(new StreamSource(in), new StreamResult(out));
            out.close();
            return new MIMETypedStream("text/html", bytes.toInputStream(),
                    null, bytes.length());
        } catch (Exception e) {
            throw new DisseminationException("[DefaultDisseminatorImpl] had an error "
                    + "in transforming xml for viewDublinCore. "
                    + "Underlying exception was: " + e.getMessage());
        }
    }

    private MIMETypedStream noMethodIndexMsg() throws GeneralException {
        ReadableByteArrayOutputStream bytes = new ReadableByteArrayOutputStream(1024);
        PrintWriter sb =
            new PrintWriter(
                        new OutputStreamWriter(bytes, Charset.forName("UTF-8")));
        sb
        .append("<html><head><title>Dissemination Index Not Available</title></head>"
                + "<body><center>"
                + "<table width=\"784\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td width=\"141\" height=\"134\" valign=\"top\"><img src=\"/")
                .append(context
                        .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME))
                        .append("/images/newlogo2.jpg\" width=\"141\" height=\"134\"/></td>"
                                + "<td width=\"643\" valign=\"top\">"
                                + "<center><h2>Fedora Repository</h2>"
                                + "<h3>Dissemination Index</h3>"
                                + "</center></td></tr></table><p>");
        sb.append("The Dissemination Index is not available"
                + " for Content Model objects, \n or Service Definition objects or Service Deployment objects.\n"
                + " The addition of this feature is not currently scheduled.");
        sb.append("</p></body></html>");
        sb.close();

        InputStream in = bytes.toInputStream();
        return new MIMETypedStream("text/html", in, null, bytes.length());
    }

    /**
     * Method implementation of reflectMethods from the InternalService
     * interface. This will return an array of method definitions that
     * constitute the behaviors of the Default Disseminator which is associated
     * with every Fedora object. These will be the methods promulgated by the
     * DefaultDisseminator interface.
     *
     * @return an array of method defintions
     */
    public static MethodDef[] reflectMethods() {
        ArrayList<MethodDef> methodList = new ArrayList<MethodDef>();
        MethodDef method = null;

        method = new MethodDef();
        method.methodName = "viewObjectProfile";
        method.methodLabel = "View description of the object";
        method.methodParms = new MethodParmDef[0];
        methodList.add(method);

        method = new MethodDef();
        method.methodName = "viewMethodIndex";
        method.methodLabel =
                "View a list of dissemination methods in the object";
        method.methodParms = new MethodParmDef[0];
        methodList.add(method);

        method = new MethodDef();
        method.methodName = "viewItemIndex";
        method.methodLabel = "View a list of items in the object";
        method.methodParms = new MethodParmDef[0];
        methodList.add(method);

        method = new MethodDef();
        method.methodName = "viewDublinCore";
        method.methodLabel = "View the Dublin Core record for the object";
        method.methodParms = new MethodParmDef[0];
        methodList.add(method);

        return methodList.toArray(new MethodDef[0]);
    }
}
