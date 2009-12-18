/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access.defaultdisseminator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Date;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import fedora.common.Constants;
import fedora.common.Models;

import fedora.server.Context;
import fedora.server.access.Access;
import fedora.server.access.ObjectProfile;
import fedora.server.errors.DisseminationException;
import fedora.server.errors.GeneralException;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.ServerException;
import fedora.server.storage.DOReader;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.MethodParmDef;
import fedora.server.storage.types.ObjectMethodsDef;

import fedora.utilities.XmlTransformUtility;

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
            InputStream in = null;
            try {
                in =
                        new ByteArrayInputStream(new ObjectInfoAsXML()
                                .getObjectProfile(reposBaseURL,
                                                  profile,
                                                  asOfDateTime)
                                .getBytes("UTF-8"));
            } catch (UnsupportedEncodingException uee) {
                throw new GeneralException("[DefaultDisseminatorImpl] An error has occurred. "
                        + "The error was a \""
                        + uee.getClass().getName()
                        + "\"  . The "
                        + "Reason was \""
                        + uee.getMessage()
                        + "\"  .");
            }
            //InputStream in = getObjectProfile().getStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            File xslFile =
                    new File(reposHomeDir, "access/viewObjectProfile.xslt");
            TransformerFactory factory =
                    XmlTransformUtility.getTransformerFactory();
            Templates template =
                    factory.newTemplates(new StreamSource(xslFile));
            Transformer transformer = template.newTransformer();
            transformer.setParameter("fedora", context
                    .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME));
            transformer.transform(new StreamSource(in), new StreamResult(out));
            in = new ByteArrayInputStream(out.toByteArray());
            return new MIMETypedStream("text/html", in, null);
        } catch (TransformerException e) {
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
        InputStream in = null;
        try {
            in =
                    new ByteArrayInputStream(new ObjectInfoAsXML()
                            .getMethodIndex(reposBaseURL,
                                            reader.GetObjectPID(),
                                            methods,
                                            asOfDateTime).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            throw new GeneralException("[DefaultDisseminatorImpl] An error has occurred. "
                    + "The error was a \""
                    + uee.getClass().getName()
                    + "\"  . The "
                    + "Reason was \""
                    + uee.getMessage()
                    + "\"  .");
        }

        // transform the method definitions xml to an html view
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            File xslFile =
                    new File(reposHomeDir, "access/listMethods.xslt");
            TransformerFactory factory =
                    XmlTransformUtility.getTransformerFactory();
            Templates template =
                    factory.newTemplates(new StreamSource(xslFile));
            Transformer transformer = template.newTransformer();
            transformer.setParameter("fedora", context
                    .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME));
            transformer.transform(new StreamSource(in), new StreamResult(out));
            in = new ByteArrayInputStream(out.toByteArray());
            return new MIMETypedStream("text/html", in, null);
        } catch (TransformerException e) {
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
        InputStream in = null;
        try {
            in =
                    new ByteArrayInputStream(new ObjectInfoAsXML()
                            .getItemIndex(reposBaseURL,
                                          context
                                                  .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME),
                                          reader,
                                          asOfDateTime).getBytes("UTF-8"));
        } catch (Exception e) {
            throw new GeneralException("[DefaultDisseminatorImpl] An error has occurred. "
                    + "The error was a \""
                    + e.getClass().getName()
                    + "\"  . The " + "Reason was \"" + e.getMessage() + "\"  .");
        }

        // convert the xml to an html view
        try {
            //InputStream in = getItemIndex().getStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            File xslFile = new File(reposHomeDir, "access/viewItemIndex.xslt");
            TransformerFactory factory =
                    XmlTransformUtility.getTransformerFactory();
            Templates template =
                    factory.newTemplates(new StreamSource(xslFile));
            Transformer transformer = template.newTransformer();
            transformer.setParameter("fedora", context
                    .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME));
            transformer.transform(new StreamSource(in), new StreamResult(out));
            in = new ByteArrayInputStream(out.toByteArray());
            return new MIMETypedStream("text/html", in, null);
        } catch (TransformerException e) {
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
        DatastreamXMLMetadata dcmd = null;
        InputStream in = null;
        try {
            dcmd =
                    (DatastreamXMLMetadata) reader.GetDatastream("DC",
                                                                 asOfDateTime);
            in =
                    new ByteArrayInputStream(new ObjectInfoAsXML()
                            .getOAIDublinCore(dcmd).getBytes("UTF-8"));
        } catch (ClassCastException cce) {
            throw new ObjectIntegrityException("Object "
                    + reader.GetObjectPID()
                    + " has a DC datastream, but it's not inline XML.");

        } catch (UnsupportedEncodingException uee) {
            throw new GeneralException("[DefaultDisseminatorImpl] An error has occurred. "
                    + "The error was a \""
                    + uee.getClass().getName()
                    + "\"  . The "
                    + "Reason was \""
                    + uee.getMessage()
                    + "\"  .");
        }

        // convert the dublin core xml to an html view
        try {
            //InputStream in = getDublinCore().getStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            File xslFile = new File(reposHomeDir, "access/viewDublinCore.xslt");
            TransformerFactory factory =
                    XmlTransformUtility.getTransformerFactory();
            Templates template =
                    factory.newTemplates(new StreamSource(xslFile));
            Transformer transformer = template.newTransformer();
            transformer.setParameter("fedora", context
                    .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME));
            transformer.transform(new StreamSource(in), new StreamResult(out));
            in = new ByteArrayInputStream(out.toByteArray());
            return new MIMETypedStream("text/html", in, null);
        } catch (TransformerException e) {
            throw new DisseminationException("[DefaultDisseminatorImpl] had an error "
                    + "in transforming xml for viewDublinCore. "
                    + "Underlying exception was: " + e.getMessage());
        }
    }

    private MIMETypedStream noMethodIndexMsg() throws GeneralException {
        String msg =
                new String("The Dissemination Index is not available"
                        + " for Content Model objects, \n or Service Definition objects or Service Deployment objects.\n"
                        + " The addition of this feature is not currently scheduled.");
        StringBuffer sb = new StringBuffer();
        sb
                .append("<html><head><title>Dissemination Index Not Available</title></head>");
        sb.append("<body><center>");
        sb
                .append("<table width=\"784\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
        sb
                .append("<tr><td width=\"141\" height=\"134\" valign=\"top\"><img src=\"/")
                .append(context
                        .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME))
                .append("/images/newlogo2.jpg\" width=\"141\" height=\"134\"/></td>");
        sb.append("<td width=\"643\" valign=\"top\">");
        sb.append("<center><h2>Fedora Repository</h2>");
        sb.append("<h3>Dissemination Index</h3>");
        sb.append("</center></td></tr></table>");
        sb.append("<p>" + msg + "</p>");
        sb.append("</body>");
        sb.append("</html>");
        String msgOut = sb.toString();
        ByteArrayInputStream in = null;
        try {
            in = new ByteArrayInputStream(msgOut.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            throw new GeneralException("[DefaultDisseminatorImpl] An error has occurred. "
                    + "The error was a \""
                    + uee.getClass().getName()
                    + "\"  . The "
                    + "Reason was \""
                    + uee.getMessage()
                    + "\"  .");
        }
        return new MIMETypedStream("text/html", in, null);
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
