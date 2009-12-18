/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.management;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;

import java.net.URLDecoder;

import java.util.Enumeration;

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
import fedora.server.errors.ServerException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.errors.servletExceptionExtensions.InternalError500Exception;
import fedora.server.errors.servletExceptionExtensions.RootException;

import fedora.utilities.XmlTransformUtility;

/**
 * Implements the "getNextPID" functionality of the Fedora Management LITE
 * (API-M-LITE) interface using a java servlet front end. The syntax defined by
 * API-M-LITE for getting a list of the next available PIDs has the following
 * binding:
 * <ol>
 * <li>getNextPID URL syntax:
 * protocol://hostname:port/fedora/management/getNextPID[?numPIDs=NUMPIDS&namespace=NAMESPACE&xml=BOOLEAN]
 * This syntax requests a list of next available PIDS. The parameter numPIDs
 * determines the number of requested PIDS to generate. If omitted, numPIDs
 * defaults to 1. The namespace parameter determines the namespace to be used in
 * generating the PIDs. If omitted, namespace defaults to the namespace defined
 * in the fedora.fcfg configuration file for the parameter pidNamespace. The xml
 * parameter determines the type of output returned. If the parameter is omitted
 * or has a value of "false", a MIME-typed stream consisting of an html table is
 * returned providing a browser-savvy means of viewing the object profile. If
 * the value specified is "true", then a MIME-typed stream consisting of XML is
 * returned.</li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>fedora - required name of the Fedora access service.</li>
 * <li>describe - required verb of the Fedora service.</li>
 * <li>numPIDs - an optional parameter indicating the number of PIDs to be
 * generated. If omitted, it defaults to 1.</li>
 * <li>namespace - an optional parameter indicating the namesapce to be used in
 * generating the PIDs. If omitted, it defaults to the namespace defined in the
 * <code>fedora.fcfg</code> configuration file for the parameter pidNamespace.</li>
 * <li>xml - an optional parameter indicating the requested output format. A
 * value of "true" indicates a return type of text/xml; the absence of the xml
 * parameter or a value of "false" indicates format is to be text/html.</li>
 * </ul>
 * 
 * @author Ross Wayland
 */
public class GetNextPIDServlet
        extends HttpServlet
        implements Constants {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(GetNextPIDServlet.class.getName());

    private static final long serialVersionUID = 1L;

    /** Content type for html. */
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";

    /** Content type for xml. */
    private static final String CONTENT_TYPE_XML = "text/xml; charset=UTF-8";

    /** Instance of the Fedora server. */
    private static Server s_server = null;

    /** Instance of the Management subsystem. */
    private static Management s_management = null;

    public static final String ACTION_LABEL = "Get Pid";

    /**
     * <p>
     * Process the Fedora API-M-LITE request to generate a list of next
     * available PIDs. Parse and validate the servlet input parameters and then
     * execute the specified request.
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
        boolean xml = false;
        int numPIDs = 1;
        String namespace = null;

        Context context =
                ReadOnlyContext.getContext(HTTP_REQUEST.REST.uri, request);

        // Get optional supplied parameters.
        for (Enumeration<?> e = request.getParameterNames(); e.hasMoreElements();) {
            String name = URLDecoder.decode((String) e.nextElement(), "UTF-8");
            if (name.equalsIgnoreCase("xml")) {
                xml = new Boolean(request.getParameter(name)).booleanValue();
            }
            if (name.equalsIgnoreCase("numPIDs")) {
                numPIDs =
                        new Integer(URLDecoder.decode(request
                                .getParameter(name), "UTF-8")).intValue();
            }
            if (name.equalsIgnoreCase("namespace")) {
                namespace =
                        URLDecoder.decode(request.getParameter(name), "UTF-8");
            }
        }
        try {
            getNextPID(context, numPIDs, namespace, xml, response);
        } catch (AuthzException ae) {
            throw RootException.getServletException(ae,
                                                    request,
                                                    ACTION_LABEL,
                                                    new String[0]);
        } catch (Throwable th) {
            final String msg = "Unexpected error getting next PID";
            LOG.error(msg, th);
            throw new InternalError500Exception(msg,
                                                th,
                                                request,
                                                ACTION_LABEL,
                                                "Internal Error",
                                                new String[0]);
        }
    }

    /**
     * <p>
     * Get the requested list of next Available PIDs by invoking the approriate
     * method from the Management subsystem.
     * </p>
     * 
     * @param context
     *        The context of this request.
     * @param numPIDs
     *        The number of PIDs requested.
     * @param namespace
     *        The namespace of the requested PIDs.
     * @param xml
     *        Boolean that determines format of response; true indicates
     *        response format is xml; false indicates response format is html.
     * @param response
     *        The servlet response.
     * @throws ServerException
     *         If an error occurred while accessing the Fedora Management
     *         subsystem.
     */
    public void getNextPID(Context context,
                           int numPIDs,
                           String namespace,
                           boolean xml,
                           HttpServletResponse response) throws ServerException {

        OutputStreamWriter out = null;
        PipedWriter pw = null;
        PipedReader pr = null;

        try {
            pw = new PipedWriter();
            pr = new PipedReader(pw);
            String[] pidList =
                    s_management.getNextPID(context, numPIDs, namespace);
            if (pidList.length > 0) {
                // Repository info obtained.
                // Serialize the RepositoryInfo object into XML
                new GetNextPIDSerializerThread(context, pidList, pw).start();
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
                                     "management/getNextPIDInfo.xslt");
                    TransformerFactory factory =
                            XmlTransformUtility.getTransformerFactory();
                    Templates template =
                            factory.newTemplates(new StreamSource(xslFile));
                    Transformer transformer = template.newTransformer();
                    transformer.transform(new StreamSource(pr),
                                          new StreamResult(out));
                }
                out.flush();

            } else {
                // GetNextPID request returned no PIDs.
                String message = "[GetNextPIDServlet] No PIDs returned.";
                LOG.error(message);
            }
        } catch (ServerException e) {
            throw e;
        } catch (Throwable th) {
            throw new GeneralException("Error while getting next PID", th);
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
                        "[GetNextPIDServlet] An error has occured. "
                                + " The error was a \" "
                                + th.getClass().getName() + " \". Reason: "
                                + th.getMessage();
                throw new StreamIOException(message);
            }
        }
    }

    /**
     * <p>
     * A Thread to serialize an array of PIDs into XML.
     * </p>
     */
    public class GetNextPIDSerializerThread
            extends Thread {

        private PipedWriter pw = null;

        private String[] pidList = null;

        /**
         * <p>
         * Constructor for GetNextPIDSerializerThread.
         * </p>
         * 
         * @param pidList
         *        An array of the requested next available PIDs.
         * @param pw
         *        A PipedWriter to which the serialization info is written.
         */
        public GetNextPIDSerializerThread(Context context,
                                          String[] pidList,
                                          PipedWriter pw) {
            this.pw = pw;
            this.pidList = pidList;
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
                    pw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    pw.write("<pidList");
                    pw.write(" xmlns:xsi=\"" + XSI.uri + "\"");
                    pw.write(" xsi:schemaLocation=\"" + MANAGEMENT.uri);
                    pw.write(" " + PID_LIST1_0.xsdLocation + "\">\n");

                    // PID array serialization
                    for (String element : pidList) {
                        pw.write("  <pid>" + element + "</pid>\n");
                    }
                    pw.write("</pidList>\n");
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
                        LOG.warn("WriteThread error", ioe);
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
            s_management =
                    (Management) s_server
                            .getModule("fedora.server.management.Management");
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
