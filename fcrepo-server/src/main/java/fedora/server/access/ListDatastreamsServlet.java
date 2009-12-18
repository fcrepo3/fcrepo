/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;

import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.GeneralException;
import fedora.server.errors.InitializationException;
import fedora.server.errors.ObjectNotFoundException;
import fedora.server.errors.ObjectNotInLowlevelStorageException;
import fedora.server.errors.ServerException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.errors.servletExceptionExtensions.BadRequest400Exception;
import fedora.server.errors.servletExceptionExtensions.InternalError500Exception;
import fedora.server.errors.servletExceptionExtensions.NotFound404Exception;
import fedora.server.errors.servletExceptionExtensions.RootException;
import fedora.server.storage.types.DatastreamDef;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.StreamUtility;

import fedora.utilities.XmlTransformUtility;

/**
 * Implements listDatastreams method of Fedora Access LITE (API-A-LITE)
 * interface using a java servlet front end.
 * <ol>
 * <li>ListDatastreams URL syntax:
 * <p>
 * protocol://hostname:port/fedora/listDatastreams/PID[/dateTime][?xml=BOOLEAN]
 * </p>
 * <p>
 * This syntax requests a list of datastreams for the specified digital object.
 * The xml parameter determines the type of output returned. If the parameter is
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
 * @version $Id: ListDatastreamsServlet.java 7781 2008-10-15 20:03:30Z pangloss
 *          $
 */
public class ListDatastreamsServlet
        extends HttpServlet
        implements Constants {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(ListDatastreamsServlet.class.getName());

    private static final long serialVersionUID = 1L;

    /** Content type for html. */
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";

    /** Content type for xml. */
    private static final String CONTENT_TYPE_XML = "text/xml; charset=UTF-8";

    /** Instance of the Fedora server. */
    private static Server s_server = null;

    /** Instance of the access subsystem. */
    private static Access s_access = null;

    /** Portion of initial request URL from protocol up to query string */
    private String requestURI = null;

    /** HTTP protocol * */
    private static String HTTP = "http";

    /** HTTPS protocol * */
    private static String HTTPS = "https";

    public static final String ACTION_LABEL = "List Datastreams";

    /** Configured Fedora server hostname */
    private static String fedoraServerHost = null;

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
            // Request is either unversioned or versioned listDatastreams
            // request
            try {
                PID = Server.getPID(URIArray[5]).toString(); // normalize PID
            } catch (Throwable th) {
                LOG.error("Bad pid syntax in request", th);
                throw new BadRequest400Exception(request,
                                                 ACTION_LABEL,
                                                 "",
                                                 new String[0]);
            }
            if (URIArray.length == 7) {
                // Request is a versioned listDatastreams request
                versDateTime = DateUtility.convertStringToDate(URIArray[6]);
                if (versDateTime == null) {
                    LOG.error("Bad date format in request");
                    throw new BadRequest400Exception(request,
                                                     ACTION_LABEL,
                                                     "",
                                                     new String[0]);
                } else {
                    asOfDateTime = versDateTime;
                }
            }
            LOG.debug("Listing datastreams (PID=" + PID + ", asOfDate="
                    + versDateTime + ")");
        } else {
            LOG.error("Bad syntax (expected 6 or 7 parts) in request");
            throw new BadRequest400Exception(request,
                                             ACTION_LABEL,
                                             "",
                                             new String[0]);
        }

        if (request.getParameter("xml") != null) {
            xml = new Boolean(request.getParameter("xml")).booleanValue();
        }

        try {
            Context context =
                    ReadOnlyContext.getContext(HTTP_REQUEST.REST.uri, request);
            listDatastreams(context, PID, asOfDateTime, xml, request, response);
            LOG.debug("Finished listing datastreams");
        } catch (ObjectNotFoundException e) {
            LOG.error("Object not found for request: " + requestURI
                    + " (actionLabel=" + ACTION_LABEL + ")", e);
            throw new NotFound404Exception(request,
                                           ACTION_LABEL,
                                           "",
                                           new String[0]);
        } catch (ObjectNotInLowlevelStorageException e) {
            LOG.error("Object not found for request: " + requestURI
                    + " (actionLabel=" + ACTION_LABEL + ")", e);
            throw new NotFound404Exception(request,
                                           ACTION_LABEL,
                                           "",
                                           new String[0]);
        } catch (AuthzException ae) {
            LOG.error("Authorization failed while listing datastreams", ae);
            throw RootException.getServletException(ae,
                                                    request,
                                                    ACTION_LABEL,
                                                    new String[0]);
        } catch (Throwable th) {
            LOG.error("Error listing datastreams", th);
            throw new InternalError500Exception("Error listing datastreams",
                                                th,
                                                request,
                                                ACTION_LABEL,
                                                "",
                                                new String[0]);
        }
    }

    public void listDatastreams(Context context,
                                String PID,
                                Date asOfDateTime,
                                boolean xml,
                                HttpServletRequest request,
                                HttpServletResponse response)
            throws ServerException {

        OutputStreamWriter out = null;
        Date versDateTime = asOfDateTime;
        DatastreamDef[] dsDefs = null;
        PipedWriter pw = null;
        PipedReader pr = null;

        try {
            pw = new PipedWriter();
            pr = new PipedReader(pw);
            dsDefs = s_access.listDatastreams(context, PID, asOfDateTime);

            // Object Profile found.
            // Serialize the ObjectProfile object into XML
            new DatastreamDefSerializerThread(context,
                                              PID,
                                              dsDefs,
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
                        new File(s_server.getHomeDir(),
                                 "access/listDatastreams.xslt");
                TransformerFactory factory =
                        XmlTransformUtility.getTransformerFactory();
                Templates template =
                        factory.newTemplates(new StreamSource(xslFile));

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
            String message = "Error listing datastreams";
            LOG.error(message, th);
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
                String message = "Error closing output";
                LOG.error(message, th);
                throw new StreamIOException(message);
            }
        }
    }

    /**
     * <p>
     * A Thread to serialize a DatastreamDef object into XML.
     * </p>
     */
    public class DatastreamDefSerializerThread
            extends Thread {

        private PipedWriter pw = null;

        private String PID = null;

        private DatastreamDef[] dsDefs = null;

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
         * @param dsDefs
         *        An array of DatastreamDefs.
         * @param versDateTime
         *        The version datetime stamp of the request.
         * @param pw
         *        A PipedWriter to which the serialization info is written.
         */
        public DatastreamDefSerializerThread(Context context,
                                             String PID,
                                             DatastreamDef[] dsDefs,
                                             Date versDateTime,
                                             PipedWriter pw) {
            this.pw = pw;
            this.PID = PID;
            this.dsDefs = dsDefs;
            this.versDateTime = versDateTime;
            fedoraServerPort =
                    context.getEnvironmentValue(HTTP_REQUEST.SERVER_PORT.uri);
            fedoraAppServerContext =
                    context.getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME);

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
                    pw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    pw.write("<objectDatastreams pid=\"" + PID + "\"");
                    if (versDateTime != null) {
                        pw.write(" asOfDateTime=\"");
                        pw.write(DateUtility.convertDateToString(versDateTime));
                        pw.write("\"");
                    }
                    final String baseURL =
                            fedoraServerProtocol + "://" + fedoraServerHost
                                    + ":" + fedoraServerPort + "/"
                                    + fedoraAppServerContext + "/";
                    pw.write(" baseURL=\"" + baseURL + "\"");
                    pw.write(" xmlns:xsi=\"" + XSI.uri + "\"");
                    pw.write(" xsi:schemaLocation=\"" + ACCESS.uri);
                    pw.write(" " + OBJ_DATASTREAMS1_0.xsdLocation + "\">");
                    // DatastreamDef SERIALIZATION
                    for (DatastreamDef element : dsDefs) {
                        pw.write("    <datastream " + "dsid=\""
                                + StreamUtility.enc(element.dsID) + "\" "
                                + "label=\""
                                + StreamUtility.enc(element.dsLabel) + "\" "
                                + "mimeType=\""
                                + StreamUtility.enc(element.dsMIME) + "\" />");
                    }
                    pw.write("</objectDatastreams>");
                    pw.flush();
                    pw.close();
                } catch (IOException ioe) {
                    LOG.error("WriteThread error", ioe);
                } finally {
                    try {
                        if (pw != null) {
                            pw.close();
                        }
                    } catch (IOException ioe) {
                        LOG.error("WriteThread error", ioe);
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
    public void init() throws ServletException {
        try {
            s_server = Server.getInstance(new File(FEDORA_HOME), false);
            fedoraServerHost = s_server.getParameter("fedoraServerHost");
            s_access =
                    (Access) s_server.getModule("fedora.server.access.Access");
        } catch (InitializationException ie) {
            throw new ServletException("Unable to get Fedora Server instance."
                    + ie.getMessage());
        }

    }

    /**
     * <p>
     * Cleans up servlet resources.
     * </p>
     */
    @Override
    public void destroy() {
    }

}
