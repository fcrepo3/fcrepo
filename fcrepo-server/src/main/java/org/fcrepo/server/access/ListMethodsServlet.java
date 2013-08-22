/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.text.ParseException;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.DisseminationException;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.InitializationException;
import org.fcrepo.server.errors.ObjectNotFoundException;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.servletExceptionExtensions.BadRequest400Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.InternalError500Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.NotFound404Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.RootException;
import org.fcrepo.server.storage.types.MethodParmDef;
import org.fcrepo.server.storage.types.ObjectMethodsDef;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.utilities.DateUtility;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * Implements listMethods method of Fedora Access LITE (API-A-LITE) interface
 * using a java servlet front end.
 * <ol>
 * <li>ListMethods URL syntax:
 * <p>
 * protocol://hostname:port/fedora/listMethods/PID[/dateTime][?xml=BOOLEAN]
 * </p>
 * <p>
 * This syntax requests a list of methods for the specified digital object. The
 * xml parameter determines the type of output returned. If the parameter is
 * omitted or has a value of "false", a MIME-typed stream consisting of an html
 * table is returned providing a browser-savvy means of viewing the object
 * profile. If the value specified is "true", then a MIME-typed stream
 * consisting of XML is returned.
 * </p>
 * </li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>fedora - required name of the Fedora access service.</li>
 * <li>get - required verb of the Fedora service.</li>
 * <li>PID - required persistent identifier of the digital object.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time. (NOT currently
 * implemented.)
 * <li>xml - an optional parameter indicating the requested output format. A
 * value of "true" indicates a return type of text/xml; the absence of the xml
 * parameter or a value of "false" indicates format is to be text/html.</li>
 * </ul>
 * </ol>
 *
 * @author Ross Wayland
 * @version $Id$
 */
public class ListMethodsServlet
        extends SpringAccessServlet
        implements Constants {

    private static final Logger logger =
            LoggerFactory.getLogger(ListMethodsServlet.class);

    private static final long serialVersionUID = 1L;

    /** Content type for html. */
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";

    /** Content type for xml. */
    private static final String CONTENT_TYPE_XML = "text/xml; charset=UTF-8";

    /** Portion of initial request URL from protocol up to query string */
    private String requestURI = null;

    /** HTTP protocol * */
    private static String HTTP = "http";

    /** HTTPS protocol * */
    private static String HTTPS = "https";

    private static final String ACTION_LABEL = "List Methods";

    /** Configured Fedora server hostname */
    private String m_fedoraServerHost = null;

    /**
     * <p>
     * Process Fedora Access Request. Parse and validate the servlet input
     * parameters and then execute the specified request.
     * </p>
     *
     * @param request
     *        The servlet request.
     * @param response
     *        servlet The servlet response.
     * @throws ServletException
     *         If an error occurs that effects the servlet's basic operation.
     * @throws IOException
     *         If an error occurrs with an input or output operation.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String PID = null;
        Date asOfDateTime = null;
        Date versDateTime = null;
        boolean xml = false;
        requestURI =
                request.getRequestURL().toString() + "?"
                        + request.getQueryString();

        // Parse servlet URL.
        String[] URIArray = request.getRequestURL().toString().split("/");
        if (URIArray.length == 6 || URIArray.length == 7) {
            // Request is either unversioned or versioned listMethods request
            try {
                PID = Server.getPID(URIArray[5]).toString(); // normalize PID
            } catch (Throwable th) {
                logger.error("Bad pid syntax in request", th);
                throw new BadRequest400Exception(request,
                                                 ACTION_LABEL,
                                                 "",
                                                 new String[0]);
            }
            if (URIArray.length == 7) {
                // Request is a versioned listMethods request
                try {
                    versDateTime = DateUtility.parseDateStrict(URIArray[6]);
                } catch(ParseException e) {
                    logger.error("Bad date format in request");
                    throw new BadRequest400Exception(request,
                                                     ACTION_LABEL,
                                                     "",
                                                     new String[0]);
                }
                asOfDateTime = versDateTime;
            }
            logger.debug("Listing methods (PID={}, asOfDate={})",
                    PID, versDateTime);
        } else {
            logger.error("Bad syntax (expected 6 or 7 parts) in request");
            throw new BadRequest400Exception(request,
                                             ACTION_LABEL,
                                             "",
                                             new String[0]);
        }

        if (request.getParameter("xml") != null) {
            xml = Boolean.parseBoolean(request.getParameter("xml"));
        }

        try {
            Context context =
                    ReadOnlyContext.getContext(HTTP_REQUEST.REST.uri, request);
            listMethods(context, PID, asOfDateTime, xml, request, response);
            logger.debug("Finished listing methods");
        } catch (ObjectNotFoundException e) {
            logger.error("Object not found for request: " + requestURI
                    + " (actionLabel=" + ACTION_LABEL + ")", e);
            throw new NotFound404Exception(request,
                                           ACTION_LABEL,
                                           "",
                                           new String[0]);
        } catch (DisseminationException e) {
            logger.error("Error Listing Methods: " + requestURI + " (actionLabel="
                    + ACTION_LABEL + ")", e);
            throw new NotFound404Exception("Error Listing Methods",
                                           e,
                                           request,
                                           ACTION_LABEL,
                                           "",
                                           new String[0]);
        } catch (ObjectNotInLowlevelStorageException e) {
            logger.error("Object not found for request: " + requestURI
                    + " (actionLabel=" + ACTION_LABEL + ")", e);
            throw new NotFound404Exception(request,
                                           ACTION_LABEL,
                                           "",
                                           new String[0]);
        } catch (AuthzException ae) {
            logger.error("Authorization error listing methods", ae);
            throw RootException.getServletException(ae,
                                                    request,
                                                    ACTION_LABEL,
                                                    new String[0]);
        } catch (Throwable th) {
            logger.error("Error listing methods", th);
            throw new InternalError500Exception("Error listing methods",
                                                th,
                                                request,
                                                ACTION_LABEL,
                                                "",
                                                new String[0]);
        }
    }

    public void listMethods(Context context,
                            String PID,
                            Date asOfDateTime,
                            boolean xml,
                            HttpServletRequest request,
                            HttpServletResponse response)
            throws ServerException {

        OutputStreamWriter out = null;
        Date versDateTime = asOfDateTime;
        ObjectMethodsDef[] methodDefs = null;
        PipedWriter pw = null;
        PipedReader pr = null;

        try {
            pw = new PipedWriter();
            pr = new PipedReader(pw);
            methodDefs = m_access.listMethods(context, PID, asOfDateTime);

            // Object Profile found.
            // Serialize the ObjectProfile object into XML
            new ObjectMethodsDefSerializerThread(context,
                                                 PID,
                                                 methodDefs,
                                                 versDateTime,
                                                 pw).start();
            if (xml) {
                // Return results as raw XML
                response.setContentType(CONTENT_TYPE_XML);

                // Insures stream read from PipedReader correctly translates
                // utf-8
                // encoded characters to OutputStreamWriter.
                out =
                        new OutputStreamWriter(response.getOutputStream(),
                                               "UTF-8");
                int bufSize = 4096;
                char[] buf = new char[bufSize];
                int len = 0;
                while ((len = pr.read(buf, 0, bufSize)) != -1) {
                    out.write(buf, 0, len);
                }
                out.flush();
            } else {
                // Transform results into an html table
                response.setContentType(CONTENT_TYPE_HTML);
                out =
                        new OutputStreamWriter(response.getOutputStream(),
                                               "UTF-8");
                File xslFile =
                        new File(m_server.getHomeDir(),
                                 "access/listMethods.xslt");
                Templates template =
                        XmlTransformUtility.getTemplates(xslFile);
                Transformer transformer = template.newTransformer();
                transformer.setParameter("fedora", context
                        .getEnvironmentValue(FEDORA_APP_CONTEXT_NAME));
                transformer.transform(new StreamSource(pr),
                                      new StreamResult(out));
            }
            out.flush();
        } catch (ServerException e) {
            throw e;
        } catch (Throwable th) {
            String message = "Error listing methods";
            logger.error(message, th);
            throw new GeneralException(message, th);
        } finally {
            try {
                if (pr != null) {
                    pr.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (Throwable th) {
                String message =
                        "[ListMethodsServlet] An error has occured. "
                                + " The error was a \" "
                                + th.getClass().getName() + " \". Reason: "
                                + th.getMessage();
                throw new StreamIOException(message);
            }
        }
    }

    /**
     * <p>
     * A Thread to serialize an ObjectMethodDef object into XML.
     * </p>
     */
    public class ObjectMethodsDefSerializerThread
            extends Thread {

        private PipedWriter pw = null;

        private String PID = null;

        private ObjectMethodsDef[] methodDefs = null;

        private Date versDateTime = null;

        private String fedoraServerProtocol = null;

        private String fedoraServerPort = null;

        private String fedoraAppServerContext = null;

        /**
         * <p>
         * Constructor for ProfileSerializeThread.
         * </p>
         *
         * @param PID
         *        The persistent identifier of the specified digital object.
         * @param methodDefs
         *        An array of ObjectMethodsDefs.
         * @param versDateTime
         *        The version datetime stamp of the request.
         * @param pw
         *        A PipedWriter to which the serialization info is written.
         */
        public ObjectMethodsDefSerializerThread(Context context,
                                                String PID,
                                                ObjectMethodsDef[] methodDefs,
                                                Date versDateTime,
                                                PipedWriter pw) {
            this.pw = pw;
            this.PID = PID;
            this.methodDefs = methodDefs;
            this.versDateTime = versDateTime;
            fedoraServerPort =
                    context.getEnvironmentValue(HTTP_REQUEST.SERVER_PORT.uri);

            fedoraAppServerContext =
                    context
                            .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME);
            if (HTTP_REQUEST.SECURE.uri.equals(context
                    .getEnvironmentValue(HTTP_REQUEST.SECURITY.uri))) {
                fedoraServerProtocol = HTTPS;
            } else if (HTTP_REQUEST.INSECURE.uri.equals(context
                    .getEnvironmentValue(HTTP_REQUEST.SECURITY.uri))) {
                fedoraServerProtocol = HTTP;
            }
        }

        /**
         * <p>
         * This method executes the thread.
         * </p>
         */
        @Override
        public void run() {
            if (pw != null) {
                try {
                    pw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    pw.write("<objectMethods");
                    pw.write(" pid=\"" + PID + "\"");
                    if (versDateTime != null) {
                        pw.write(" asOfDateTime=\"");
                        pw.write(DateUtility.convertDateToString(versDateTime));
                        pw.write("\"");
                    }
                    pw.write(" baseURL=\""
                            + StreamUtility.enc(fedoraServerProtocol) + "://"
                            + StreamUtility.enc(m_fedoraServerHost) + ":"
                            + StreamUtility.enc(fedoraServerPort) + "/"
                            + fedoraAppServerContext + "/\"");
                    pw.write(" xmlns=\"" + OBJ_METHODS1_0.namespace.uri + "\" ");
                    pw.write(" xmlns:xsi=\"" + XSI.uri + "\" ");
                    pw.write(" xsi:schemaLocation=\"" + OBJ_METHODS1_0.namespace.uri);
                    pw.write(" " + OBJ_METHODS1_0.xsdLocation + "\">");

                    // ObjectMethodsDef SERIALIZATION
                    String nextSdef = "null";
                    String currentSdef = "";
                    for (int i = 0; i < methodDefs.length; i++) {
                        currentSdef = methodDefs[i].sDefPID;
                        if (!currentSdef.equalsIgnoreCase(nextSdef)) {
                            if (i != 0) {
                                pw.write("</sDef>");
                            }
                            pw.write("<sDef pid=\""
                                    + StreamUtility.enc(methodDefs[i].sDefPID)
                                    + "\" >");
                        }
                        pw.write("<method name=\""
                                + StreamUtility.enc(methodDefs[i].methodName)
                                + "\" >");
                        MethodParmDef[] methodParms =
                                methodDefs[i].methodParmDefs;
                        for (MethodParmDef element : methodParms) {
                            pw.write("<methodParm parmName=\""
                                    + StreamUtility.enc(element.parmName)
                                    + "\" parmDefaultValue=\""
                                    + StreamUtility
                                            .enc(element.parmDefaultValue)
                                    + "\" parmRequired=\""
                                    + element.parmRequired + "\" parmLabel=\""
                                    + StreamUtility.enc(element.parmLabel)
                                    + "\" >");
                            if (element.parmDomainValues.length > 0) {
                                pw.write("<methodParmDomain>");
                                for (String element2 : element.parmDomainValues) {
                                    pw.write("<methodParmValue>"
                                            + StreamUtility.enc(element2)
                                            + "</methodParmValue>");
                                }
                                pw.write("</methodParmDomain>");
                            }
                            pw.write("</methodParm>");
                        }

                        pw.write("</method>");
                        nextSdef = currentSdef;
                    }
                    pw.write("</sDef>");
                    pw.write("</objectMethods>");

                    pw.flush();
                    pw.close();
                } catch (IOException ioe) {
                    logger.error("WriteThread error", ioe);
                } finally {
                    try {
                        if (pw != null) {
                            pw.close();
                        }
                    } catch (IOException ioe) {
                        logger.error("WriteThread error", ioe);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * For now, treat a HTTP POST request just like a GET request.
     * </p>
     *
     * @param request
     *        The servet request.
     * @param response
     *        The servlet response.
     * @throws ServletException
     *         If thrown by <code>doGet</code>.
     * @throws IOException
     *         If thrown by <code>doGet</code>.
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * <p>
     * Initialize servlet.
     * </p>
     *
     * @throws ServletException
     *         If the servet cannot be initialized.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        m_fedoraServerHost = m_server.getParameter("fedoraServerHost");
    }

}
