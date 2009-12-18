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

import java.net.URLDecoder;

import java.util.Enumeration;
import java.util.Hashtable;

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

import fedora.utilities.XmlTransformUtility;

/**
 * Implements the "getObjectHistory" functionality of the Fedora Access LITE
 * (API-A-LITE) interface using a java servlet front end. The syntax defined by
 * API-A-LITE has for getting a description of the repository has the following
 * binding:
 * <ol>
 * <li>getObjectHistory URL syntax:
 * protocol://hostname:port/fedora/getObjectHistory/pid[?xml=BOOLEAN] This
 * syntax requests information about the repository. The xml parameter
 * determines the type of output returned. If the parameter is omitted or has a
 * value of "false", a MIME-typed stream consisting of an html table is returned
 * providing a browser-savvy means of viewing the object profile. If the value
 * specified is "true", then a MIME-typed stream consisting of XML is returned.</li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>fedora - required name of the Fedora access service.</li>
 * <li>getObjectHistory - required verb of the Fedora service.</li>
 * <li>pid - the persistent identifier of the digital object.
 * <li>xml - an optional parameter indicating the requested output format. A
 * value of "true" indicates a return type of text/xml; the absence of the xml
 * parameter or a value of "false" indicates format is to be text/html.</li>
 * </ul>
 *
 * @author Ross Wayland
 * @version $Id$
 */
public class GetObjectHistoryServlet
        extends HttpServlet
        implements Constants {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(GetObjectHistoryServlet.class.getName());

    private static final long serialVersionUID = 1L;

    /** Content type for html. */
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";

    /** Content type for xml. */
    private static final String CONTENT_TYPE_XML = "text/xml; charset=UTF-8";

    /** Instance of the Fedora server. */
    private static Server s_server = null;

    /** Instance of the access subsystem. */
    private static Access s_access = null;

    public static final String ACTION_LABEL = "Get Object History";

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
        boolean xml = false;

        // Parse servlet URL.
        String[] URIArray = request.getRequestURL().toString().split("/");
        if (URIArray.length != 6 || !URIArray[4].equals("getObjectHistory")) {
            throw new BadRequest400Exception(request,
                                             ACTION_LABEL,
                                             "",
                                             new String[0]);
        }

        PID = URIArray[5];
        LOG.debug("Servicing getObjectHistory request (PID=" + PID + ")");

        // Check for xml encoding parameter; ignore any other parameters
        Hashtable<String, String> h_userParms = new Hashtable<String, String>();
        for (Enumeration<?> e = request.getParameterNames(); e.hasMoreElements();) {
            String name = URLDecoder.decode((String) e.nextElement(), "UTF-8");
            String value =
                    URLDecoder.decode(request.getParameter(name), "UTF-8");
            if (name.equalsIgnoreCase("xml")) {
                xml = new Boolean(request.getParameter(name)).booleanValue();
            }
            h_userParms.put(name, value);
        }

        Context context =
                ReadOnlyContext.getContext(HTTP_REQUEST.REST.uri, request);
        try {
            getObjectHistory(context, PID, xml, response);
        } catch (ObjectNotFoundException e) {
            LOG.error("Object not found for request: "
                    + request.getRequestURI() + " (actionLabel=" + ACTION_LABEL
                    + ")", e);
            throw new NotFound404Exception(request,
                                           ACTION_LABEL,
                                           "",
                                           new String[0]);
        } catch (ObjectNotInLowlevelStorageException e) {
            LOG.error("Object not found for request: "
                    + request.getRequestURI() + " (actionLabel=" + ACTION_LABEL
                    + ")", e);
            throw new NotFound404Exception(request,
                                           ACTION_LABEL,
                                           "",
                                           new String[0]);
        } catch (AuthzException ae) {
            throw RootException.getServletException(ae,
                                                    request,
                                                    ACTION_LABEL,
                                                    new String[0]);
        } catch (Throwable th) {
            LOG.error("Unexpected error servicing API-A request", th);
            throw new InternalError500Exception("",
                                                th,
                                                request,
                                                ACTION_LABEL,
                                                "",
                                                new String[0]);
        }
    }

    public void getObjectHistory(Context context,
                                 String PID,
                                 boolean xml,
                                 HttpServletResponse response)
            throws ServerException {

        OutputStreamWriter out = null;
        String[] objectHistory = new String[0];
        PipedWriter pw = null;
        PipedReader pr = null;

        try {
            pw = new PipedWriter();
            pr = new PipedReader(pw);
            objectHistory = s_access.getObjectHistory(context, PID);
            if (objectHistory.length > 0) {
                // Object history.
                // Serialize the ObjectHistory object into XML
                new ObjectHistorySerializerThread(context,
                                                  objectHistory,
                                                  PID,
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
                                     "access/viewObjectHistory.xslt");
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

            } else {
                throw new GeneralException("No object history returned");
            }
        } catch (ServerException e) {
            throw e;
        } catch (Throwable th) {
            String msg = "Error getting object history";
            throw new GeneralException(msg, th);
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
                        "[GetObjectHistoryServlet] An error has occured. "
                                + " The error was a \" "
                                + th.getClass().getName() + " \". Reason: "
                                + th.getMessage();
                throw new StreamIOException(message);
            }
        }
    }

    /**
     * <p>
     * A Thread to serialize an ObjectProfile object into XML.
     * </p>
     */
    public class ObjectHistorySerializerThread
            extends Thread {

        private PipedWriter pw = null;

        private String[] objectHistory = new String[0];

        private String PID = null;

        /**
         * <p>
         * Constructor for ObjectHistorySerializerThread.
         * </p>
         *
         * @param objectHistory
         *        An object history data structure.
         * @param PID
         *        The pid of the digital object.
         * @param pw
         *        A PipedWriter to which the serialization info is written.
         */
        public ObjectHistorySerializerThread(Context context,
                                             String[] objectHistory,
                                             String PID,
                                             PipedWriter pw) {
            this.pw = pw;
            this.objectHistory = objectHistory;
            this.PID = PID;
            if (HTTP_REQUEST.SECURE.uri.equals(context
                    .getEnvironmentValue(HTTP_REQUEST.SECURITY.uri))) {
            } else if (HTTP_REQUEST.INSECURE.uri.equals(context
                    .getEnvironmentValue(HTTP_REQUEST.SECURITY.uri))) {
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
                    pw.write("<fedoraObjectHistory" + " pid=\"" + PID + "\""
                            + " xmlns:xsd=\"" + XML_XSD.uri + "\""
                            + " xmlns:xsi=\"" + XSI.uri + "\""
                            + " xsi:schemaLocation=\"" + ACCESS.uri + " "
                            + OBJ_HISTORY1_0.xsdLocation + "\">");
                    // Object History Serialization
                    for (String element : objectHistory) {
                        pw.write("<objectChangeDate>" + element
                                + "</objectChangeDate>");
                    }
                    pw.write("</fedoraObjectHistory>");
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
            s_access =
                    (Access) s_server.getModule("fedora.server.access.Access");
        } catch (InitializationException ie) {
            throw new ServletException("Unable to get Fedora Server instance."
                    + ie.getMessage());
        }
    }

}
