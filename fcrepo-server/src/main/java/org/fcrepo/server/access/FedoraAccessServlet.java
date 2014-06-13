/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.access;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.http.HttpStatus;
import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.errors.DatastreamNotFoundException;
import org.fcrepo.server.errors.DisseminationException;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.MethodNotFoundException;
import org.fcrepo.server.errors.ObjectNotFoundException;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.servletExceptionExtensions.InternalError500Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.NotFound404Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.RootException;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.fcrepo.server.storage.types.Property;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.utilities.DateUtility;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the three methods GetObjectProfile, GetDissemination, and
 * GetDatastreamDissemination of the Fedora Access LITE (API-A-LITE) interface
 * using a java servlet front end. The syntax defined by API-A-LITE defines
 * three bindings for these methods:
 * <ol>
 * <li>GetDissemination URL syntax:
 * <p>
 * protocol://hostname:port/fedora/get/PID/sDefPID/methodName[/dateTime][?
 * parmArray]
 * </p>
 * <p>
 * This syntax requests a dissemination of the specified object using the
 * specified method of the associated service definition object. The result is
 * returned as a MIME-typed stream.
 * </p>
 * </li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>fedora - required path name for the Fedora access service.</li>
 * <li>get - required path name for the Fedora service.</li>
 * <li>PID - required persistent idenitifer of the digital object.</li>
 * <li>sDefPID - required persistent identifier of the service definition object
 * to which the digital object subscribes.</li>
 * <li>methodName - required name of the method to be executed.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time.
 * <li>parmArray - optional array of method parameters consisting of name/value
 * pairs in the form parm1=value1&parm2=value2...</li>
 * </ul>
 * <li>GetObjectProfile URL syntax:
 * <p>
 * protocol://hostname:port/fedora/get/PID[/dateTime][?xml=BOOLEAN]
 * </p>
 * <p>
 * This syntax requests an object profile for the specified digital object. The
 * xml parameter determines the type of output returned. If the parameter is
 * omitted or has a value of "false", a MIME-typed stream consisting of an html
 * table is returned providing a browser-savvy means of viewing the object
 * profile. If the value specified is "true", then a MIME-typed stream
 * consisting of XML is returned.
 * </p>
 * </li>
 * <ul>
 * <li>protocol - either http or https</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>fedora - required name of the Fedora access service.</li>
 * <li>get - required verb of the Fedora service.</li>
 * <li>PID - required persistent identifier of the digital object.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time.
 * <li>xml - an optional parameter indicating the requested output format. A
 * value of "true" indicates a return type of text/xml; the absence of the xml
 * parameter or a value of "false" indicates format is to be text/html.</li>
 * </ul>
 * <li>GetDatastreamDissemination URL syntax:
 * <p>
 * protocol://hostname:port/fedora/get/PID/DSID[/dateTime]
 * </p>
 * This syntax requests a datastream dissemination for the specified digital
 * object. It is used to return the contents of a datastream.
 * <p></li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>fedora - required name of the Fedora access service.</li>
 * <li>get - required verb of the Fedora service.</li>
 * <li>PID - required persistent identifier of the digital object.</li>
 * <li>DSID - required datastream identifier for the datastream.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time.
 * </ul>
 * </ol>
 *
 * @author Ross Wayland
 */
public class FedoraAccessServlet
        extends SpringAccessServlet
        implements Constants {

    private static final Logger logger =
            LoggerFactory.getLogger(FedoraAccessServlet.class);

    private static final long serialVersionUID = 1L;

    /** Content type for html. */
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";

    /** Content type for xml. */
    private static final String CONTENT_TYPE_XML = "text/xml; charset=UTF-8";

    /** Portion of initial request URL from protocol up to query string */
    private String requestURI = null;

    /** 4K Buffer */
    private final static int BUF = 4096;

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
        String sDefPID = null;
        String methodName = null;
        String dsID = null;
        Date asOfDateTime = null;
        Date versDateTime = null;
        Property[] userParms = null;
        boolean isGetObjectProfileRequest = false;
        boolean isGetDisseminationRequest = false;
        boolean isGetDatastreamDisseminationRequest = false;
        boolean xml = false;

        requestURI = request.getQueryString() != null ?
                request.getRequestURL().toString() + "?" + request.getQueryString()
                : request.getRequestURL().toString();
        logger.info("Got request: {}", requestURI);

        // Parse servlet URL.
        // For the Fedora API-A-LITE "get" syntax, valid entries include:
        //
        // For dissemination requests:
        // http://host:port/fedora/get/pid/sDefPid/methodName
        // http://host:port/fedora/get/pid/sDefPid/methodName/timestamp
        // http://host:port/fedora/get/pid/sDefPid/methodName?parm=value[&parm=value]
        // http://host:port/fedora/get/pid/sDefPid/methodName/timestamp?parm=value[&parm=value]
        //
        // For object profile requests:
        // http://host:port/fedora/get/pid
        // http://host:port/fedora/get/pid/timestamp
        //
        // For datastream dissemination requests:
        // http://host:port/fedora/get/pid/dsID
        // http://host:port/fedora/get/pid/dsID/timestamp
        //
        // use substring to avoid an additional char array copy
        String[] URIArray = requestURI.substring(0, request.getRequestURL().length()).split("/");
        if (URIArray.length == 6 || URIArray.length == 7) {
            // Request is either an ObjectProfile request or a datastream
            // request
            if (URIArray.length == 7) {
                // They either specified a date/time or a datastream id.
                if (URIArray[6].indexOf(":") == -1) {
                    // If it doesn't contain a colon, they were after a
                    // datastream,
                    // so this is a DatastreamDissemination request
                    dsID = URLDecoder.decode(URIArray[6], "UTF-8");
                    isGetDatastreamDisseminationRequest = true;
                } else {
                    // If it DOES contain a colon, they were after a
                    // date/time-stamped object profile
                    try {
                        versDateTime = DateUtility.parseDateStrict(URIArray[6]);
                    } catch (ParseException e) {
                        String message =
                                "ObjectProfile Request Syntax Error: DateTime value "
                                        + "of \""
                                        + URIArray[6]
                                        + "\" is not a valid DateTime format. "
                                        + " <br></br> The expected format for DateTime is \""
                                        + "YYYY-MM-DDTHH:MM:SS.SSSZ\".  "
                                        + " <br></br> The expected syntax for "
                                        + "ObjectProfile requests is: \""
                                        + URIArray[0]
                                        + "//"
                                        + URIArray[2]
                                        + "/"
                                        + URIArray[3]
                                        + "/"
                                        + URIArray[4]
                                        + "/PID[/dateTime] \"  ."
                                        + " <br></br> Submitted request was: \""
                                        + requestURI + "\"  .  ";
                        logger.warn(message);
                        throw new ServletException("from FedoraAccessServlet"
                                + message);
                        /*
                         * commented out for exception.jsp test
                         * response.setStatus
                         * (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                         * response
                         * .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                         * , message); return; commented out for exception.jsp
                         * test
                         */
                    }
                    asOfDateTime = versDateTime;
                    isGetObjectProfileRequest = true;
                }
            } else {
                // URIArray.length==6 so this is a GetObjectProfile request
                isGetObjectProfileRequest = true;
            }
        } else if (URIArray.length > 7) {
            // Request is either dissemination request or timestamped get
            // datastream request
            methodName = URLDecoder.decode(URIArray[7], "UTF-8");
            if (URIArray.length == 8) {
                if (URIArray[6].indexOf(":") == -1) {
                    // If it doesn't contain a colon, they were after a
                    // timestamped
                    // datastream, so this is a GetDatastreamDissemination
                    // request.
                    dsID = URLDecoder.decode(URIArray[6], "UTF-8");

                    try {
                        versDateTime = DateUtility.parseDateStrict(URIArray[7]);
                    } catch (ParseException e) {
                        String message =
                                "GetDatastreamDissemination Request Syntax Error: DateTime value "
                                        + "of \""
                                        + URIArray[7]
                                        + "\" is not a valid DateTime format. "
                                        + " <br></br> The expected format for DateTime is \""
                                        + "YYYY-MM-DDTHH:MM:SS.SSSZ\".  "
                                        + " <br></br> The expected syntax for GetDatastreamDissemination requests is: \""
                                        + URIArray[0]
                                        + "//"
                                        + URIArray[2]
                                        + "/"
                                        + URIArray[3]
                                        + "/"
                                        + URIArray[4]
                                        + "/PID/dsID[/dateTime] \"  "
                                        + " <br></br> Submitted request was: \""
                                        + requestURI + "\"  .  ";
                        logger.warn(message);
                        throw new ServletException("from FedoraAccessServlet"
                                + message);
                    }

                    asOfDateTime = versDateTime;
                    isGetDatastreamDisseminationRequest = true;
                } else {
                    isGetDisseminationRequest = true;
                }
            } else if (URIArray.length == 9) {
                try {
                    versDateTime = DateUtility.parseDateStrict(URIArray[8]);
                } catch (ParseException e) {
                    String message =
                            "Dissemination Request Syntax Error: DateTime value "
                                    + "of \""
                                    + URIArray[8]
                                    + "\" is not a valid DateTime format. "
                                    + " <br></br> The expected format for DateTime is \""
                                    + "YYYY-MM-DDTHH:MM:SS.SSS\".  "
                                    + " <br></br> The expected syntax for Dissemination requests is: \""
                                    + URIArray[0]
                                    + "//"
                                    + URIArray[2]
                                    + "/"
                                    + URIArray[3]
                                    + "/"
                                    + URIArray[4]
                                    + "/PID/sDefPID/methodName[/dateTime][?ParmArray] \"  "
                                    + " <br></br> Submitted request was: \""
                                    + requestURI + "\"  .  ";
                    logger.warn(message);
                    throw new ServletException("from FedoraAccessServlet"
                            + message);
                    /*
                     * commented out for exception.jsp test
                     * response.setStatus(HttpServletResponse
                     * .SC_INTERNAL_SERVER_ERROR);
                     * response.sendError(HttpServletResponse
                     * .SC_INTERNAL_SERVER_ERROR, message); return; commented
                     * out for exception.jsp test
                     */
                }
                asOfDateTime = versDateTime;
                isGetDisseminationRequest = true;
            }
            if (URIArray.length > 9) {
                String message =
                        "Dissemination Request Syntax Error: The expected "
                                + "syntax for Dissemination requests is: \""
                                + URIArray[0]
                                + "//"
                                + URIArray[2]
                                + "/"
                                + URIArray[3]
                                + "/"
                                + URIArray[4]
                                + "/PID/sDefPID/methodName[/dateTime][?ParmArray] \"  "
                                + " <br></br> Submitted request was: \""
                                + requestURI + "\"  .  ";
                logger.warn(message);
                throw new ServletException("from FedoraAccessServlet" + message);
                /*
                 * commented out for exception.jsp test
                 * response.setStatus(HttpServletResponse
                 * .SC_INTERNAL_SERVER_ERROR);
                 * response.sendError(HttpServletResponse
                 * .SC_INTERNAL_SERVER_ERROR, message); return; commented out
                 * for exception.jsp test
                 */
            }
        } else {
            // Bad syntax; redirect to syntax documentation page.
            response
                    .sendRedirect("/userdocs/client/browser/apialite/index.html");
            return;
        }

        // Separate out servlet parameters from method parameters
        Hashtable<String, String> h_userParms = new Hashtable<String, String>();
        for (Enumeration<?> e = request.getParameterNames(); e
                .hasMoreElements();) {
            String name = URLDecoder.decode((String) e.nextElement(), "UTF-8");
            if (isGetObjectProfileRequest && name.equalsIgnoreCase("xml")) {
                xml = Boolean.parseBoolean(request.getParameter(name));
            } else {
                String value =
                        URLDecoder.decode(request.getParameter(name), "UTF-8");
                h_userParms.put(name, value);
            }
        }

        // API-A interface requires user-supplied parameters to be of type
        // Property[] so create Property[] from hashtable of user parameters.
        int userParmCounter = 0;
        userParms = new Property[h_userParms.size()];
        for (Enumeration<String> e = h_userParms.keys(); e.hasMoreElements();) {
            Property userParm = new Property();
            userParm.name = e.nextElement();
            userParm.value = h_userParms.get(userParm.name);
            userParms[userParmCounter] = userParm;
            userParmCounter++;
        }

        PID = URIArray[5];
        String actionLabel = "Access";

        try {
            if (isGetObjectProfileRequest) {
                logger.debug("Servicing getObjectProfile request (PID={}, asOfDate={})", PID, versDateTime);

                Context context =
                        ReadOnlyContext.getContext(HTTP_REQUEST.REST.uri,
                                                   request);
                getObjectProfile(context,
                                 PID,
                                 asOfDateTime,
                                 xml,
                                 request,
                                 response);

                logger.debug("Finished servicing getObjectProfile request");
            } else if (isGetDisseminationRequest) {
                sDefPID = URIArray[6];
                logger.debug("Servicing getDissemination request (PID={}, sDefPID={}, methodName={}, asOfDate={})",
                        PID, sDefPID, methodName, versDateTime);

                Context context =
                        ReadOnlyContext.getContext(HTTP_REQUEST.REST.uri,
                                                   request);
                getDissemination(context,
                                 PID,
                                 sDefPID,
                                 methodName,
                                 userParms,
                                 asOfDateTime,
                                 response,
                                 request);

                logger.debug("Finished servicing getDissemination request");
            } else if (isGetDatastreamDisseminationRequest) {
                logger.debug("Servicing getDatastreamDissemination request "
                        + "(PID={}, dsID={}, asOfDate={})",
                        PID, dsID, versDateTime);

                Context context =
                        ReadOnlyContext.getContext(HTTP_REQUEST.REST.uri,
                                                   request);
                getDatastreamDissemination(context,
                                           PID,
                                           dsID,
                                           asOfDateTime,
                                           response,
                                           request);

                logger.debug("Finished servicing getDatastreamDissemination "
                        + "request");
            }
        } catch (MethodNotFoundException e) {
            logger.error("Method not found for request: " + requestURI
                    + " (actionLabel=" + actionLabel + ")", e);
            throw new NotFound404Exception("", e, request, actionLabel, e
                    .getMessage(), EMPTY_STRING_ARRAY);
        } catch (DatastreamNotFoundException e) {
            logger.error("Datastream not found for request: " + requestURI
                    + " (actionLabel=" + actionLabel + ")", e);
            throw new NotFound404Exception("", e, request, actionLabel, e
                    .getMessage(), EMPTY_STRING_ARRAY);
        } catch (ObjectNotFoundException e) {
            logger.error("Object not found for request: " + requestURI
                    + " (actionLabel=" + actionLabel + ")", e);
            throw new NotFound404Exception("", e, request, actionLabel, e
                    .getMessage(), EMPTY_STRING_ARRAY);
        } catch (DisseminationException e) {
            logger.error("Dissemination failed: " + requestURI
                    + " (actionLabel=" + actionLabel + ")", e);
            throw new NotFound404Exception("", e, request, actionLabel, e
                    .getMessage(), EMPTY_STRING_ARRAY);
        } catch (ObjectNotInLowlevelStorageException e) {
            logger.error("Object or datastream not found for request: "
                    + requestURI + " (actionLabel=" + actionLabel + ")", e);
            throw new NotFound404Exception("", e, request, actionLabel, e
                    .getMessage(), EMPTY_STRING_ARRAY);
        } catch (AuthzException ae) {
            logger.error("Authorization failed for request: " + requestURI
                    + " (actionLabel=" + actionLabel + ")", ae);
            throw RootException.getServletException(ae,
                                                    request,
                                                    actionLabel,
                                                    EMPTY_STRING_ARRAY);
        } catch (Throwable th) {
            logger.error("Unexpected error servicing API-A request", th);
            throw new InternalError500Exception("",
                                                th,
                                                request,
                                                actionLabel,
                                                "",
                                                EMPTY_STRING_ARRAY);
        }
    }

    public void getObjectProfile(Context context,
                                 String PID,
                                 Date asOfDateTime,
                                 boolean xml,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
            throws ServerException {

        OutputStreamWriter out = null;
        Date versDateTime = asOfDateTime;
        ObjectProfile objProfile = null;
        PipedWriter pw = null;
        PipedReader pr = null;
        try {
            pw = new PipedWriter();
            pr = new PipedReader(pw);
            objProfile = m_access.getObjectProfile(context, PID, asOfDateTime);
            if (objProfile != null) {
                // Object Profile found.
                // Serialize the ObjectProfile object into XML
                new ProfileSerializerThread(context,
                                            PID,
                                            objProfile,
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
                    char[] buf = new char[BUF];
                    int len = 0;
                    while ((len = pr.read(buf, 0, BUF)) != -1) {
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
                                     "access/viewObjectProfile.xslt");
                    Templates template =
                            XmlTransformUtility.getTemplates(xslFile);
                    Transformer transformer = template.newTransformer();
                    transformer.setParameter("fedora", context
                            .getEnvironmentValue(FEDORA_APP_CONTEXT_NAME));
                    transformer.transform(new StreamSource(pr),
                                          new StreamResult(out));
                }
                out.flush();

            } else {
                throw new GeneralException("No object profile returned");
            }
        } catch (ServerException e) {
            throw e;
        } catch (Throwable th) {
            String message = "Error getting object profile";
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
                String message = "Error closing output";
                logger.error(message, th);
                throw new StreamIOException(message);
            }
        }
    }

    public void getDatastreamDissemination(Context context,
                                           String PID,
                                           String dsID,
                                           Date asOfDateTime,
                                           HttpServletResponse response,
                                           HttpServletRequest request)
            throws IOException, ServerException {
        ServletOutputStream out = null;
        MIMETypedStream dissemination = null;
        dissemination =
                m_access.getDatastreamDissemination(context,
                                                    PID,
                                                    dsID,
                                                    asOfDateTime);
        try {
            // testing to see what's in request header that might be of interest
            if (logger.isDebugEnabled()) {
                for (Enumeration<?> e = request.getHeaderNames(); e
                        .hasMoreElements();) {
                    String name = (String) e.nextElement();
                    Enumeration<?> headerValues = request.getHeaders(name);
                    StringBuffer sb = new StringBuffer();
                    while (headerValues.hasMoreElements()) {
                        sb.append((String) headerValues.nextElement());
                    }
                    String value = sb.toString();
                    logger.debug("FEDORASERVLET REQUEST HEADER CONTAINED: {} : {}",
                            name, value);
                }
            }

            // Dissemination was successful;
            // Return MIMETypedStream back to browser client
            if (dissemination.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                String location = "";
                for (Property prop: dissemination.header) {
                    if (prop.name.equalsIgnoreCase(HttpHeaders.LOCATION)) {
                        location = prop.value;
                        break;
                    }
                }

                response.sendRedirect(location);
            } else {
                int status = dissemination.getStatusCode();
                response.setStatus(status);
                if (status == HttpStatus.SC_OK) {
                    response.setContentType(dissemination.getMIMEType());
                }
                Property[] headerArray = dissemination.header;
                if (headerArray != null) {
                    for (int i = 0; i < headerArray.length; i++) {
                        if (headerArray[i].name != null
                                && !headerArray[i].name
                                        .equalsIgnoreCase("transfer-encoding")
                                && !headerArray[i].name
                                        .equalsIgnoreCase("content-type")) {
                            response.addHeader(headerArray[i].name,
                                               headerArray[i].value);
                            logger.debug(
                                    "THIS WAS ADDED TO FEDORASERVLET RESPONSE HEADER FROM ORIGINATING PROVIDER {} : {}",
                                    headerArray[i].name, headerArray[i].value);
                        }
                    }
                }
                out = response.getOutputStream();
                int byteStream = 0;
                logger.debug("Started reading dissemination stream");
                InputStream dissemResult = dissemination.getStream();
                byte[] buffer = new byte[BUF];
                while ((byteStream = dissemResult.read(buffer)) != -1) {
                    out.write(buffer, 0, byteStream);
                }
                buffer = null;
                dissemResult.close();
                dissemResult = null;
                out.flush();
                out.close();
                logger.debug("Finished reading dissemination stream");
            }
        } finally {
            dissemination.close();
        }
    }

    /**
     * <p>
     * This method calls the Fedora Access Subsystem to retrieve a MIME-typed
     * stream corresponding to the dissemination request.
     * </p>
     *
     * @param context
     *        The read only context of the request.
     * @param PID
     *        The persistent identifier of the Digital Object.
     * @param sDefPID
     *        The persistent identifier of the Service Definition object.
     * @param methodName
     *        The method name.
     * @param userParms
     *        An array of user-supplied method parameters.
     * @param asOfDateTime
     *        The version datetime stamp of the digital object.
     * @param response
     *        The servlet response.
     * @param request
     *        The servlet request.
     * @throws IOException
     *         If an error occurrs with an input or output operation.
     * @throws ServerException
     *         If an error occurs in the Access Subsystem.
     */
    public void getDissemination(Context context,
                                 String PID,
                                 String sDefPID,
                                 String methodName,
                                 Property[] userParms,
                                 Date asOfDateTime,
                                 HttpServletResponse response,
                                 HttpServletRequest request)
            throws IOException, ServerException {
        ServletOutputStream out = null;
        MIMETypedStream dissemination = null;
        dissemination =
                m_access.getDissemination(context,
                                          PID,
                                          sDefPID,
                                          methodName,
                                          userParms,
                                          asOfDateTime);
        out = response.getOutputStream();
        try {
            // testing to see what's in request header that might be of interest
            if (logger.isDebugEnabled()) {
                for (Enumeration<?> e = request.getHeaderNames(); e
                        .hasMoreElements();) {
                    String name = (String) e.nextElement();
                    Enumeration<?> headerValues = request.getHeaders(name);
                    StringBuffer sb = new StringBuffer();
                    while (headerValues.hasMoreElements()) {
                        sb.append((String) headerValues.nextElement());
                    }
                    String value = sb.toString();
                    logger.debug("FEDORASERVLET REQUEST HEADER CONTAINED: {} : {}",
                            name, value);
                }
            }

            // Dissemination was successful;
            // Return MIMETypedStream back to browser client
            if (dissemination.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                String location = "";
                for (Property prop: dissemination.header) {
                    if (prop.name.equalsIgnoreCase(HttpHeaders.LOCATION)) {
                        location = prop.value;
                        break;
                    }
                }

                response.sendRedirect(location);
            } else {
                response.setContentType(dissemination.getMIMEType());
                Property[] headerArray = dissemination.header;
                if (headerArray != null) {
                    for (int i = 0; i < headerArray.length; i++) {
                        if (headerArray[i].name != null
                                && !headerArray[i].name
                                        .equalsIgnoreCase("transfer-encoding")
                                && !headerArray[i].name
                                        .equalsIgnoreCase("content-type")) {
                            response.addHeader(headerArray[i].name,
                                               headerArray[i].value);
                            logger.debug(
                                    "THIS WAS ADDED TO FEDORASERVLET  RESPONSE HEADER FROM ORIGINATING  PROVIDER {} : {}", headerArray[i].name, headerArray[i].value);
                        }
                    }
                }
                int byteStream = 0;
                logger.debug("Started reading dissemination stream");
                InputStream dissemResult = dissemination.getStream();
                byte[] buffer = new byte[BUF];
                while ((byteStream = dissemResult.read(buffer)) != -1) {
                    out.write(buffer, 0, byteStream);
                }
                buffer = null;
                dissemResult.close();
                dissemResult = null;
                out.flush();
                out.close();
                logger.debug("Finished reading dissemination stream");
            }
        } finally {
            dissemination.close();
        }
    }

    /**
     * <p>
     * A Thread to serialize an ObjectProfile object into XML.
     * </p>
     */
    public class ProfileSerializerThread
            extends Thread {

        private PipedWriter pw = null;

        private String PID = null;

        private ObjectProfile objProfile = null;

        private Date versDateTime = null;

        /**
         * <p>
         * Constructor for ProfileSerializeThread.
         * </p>
         *
         * @param PID
         *        The persistent identifier of the specified digital object.
         * @param objProfile
         *        An object profile data structure.
         * @param versDateTime
         *        The version datetime stamp of the request.
         * @param pw
         *        A PipedWriter to which the serialization info is written.
         */
        public ProfileSerializerThread(Context context,
                                       String PID,
                                       ObjectProfile objProfile,
                                       Date versDateTime,
                                       PipedWriter pw) {
            this.pw = pw;
            this.PID = PID;
            this.objProfile = objProfile;
            this.versDateTime = versDateTime;
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
                    pw.write("<objectProfile pid=\"");
                    StreamUtility.enc(PID, pw);
                    pw.write('"');
                    if (versDateTime != null) {
                        DateUtility.convertDateToString(versDateTime);
                        pw.write(" dateTime=\""
                                + DateUtility.convertDateToString(versDateTime)
                                + "\"");
                    }
                    pw.write(" xmlns=\"" + OBJ_PROFILE1_0.namespace.uri + "\"");
                    pw.write(" xmlns:xsi=\"" + XSI.uri + "\""
                            + " xsi:schemaLocation=\""
                            + OBJ_PROFILE1_0.namespace.uri + " "
                            + OBJ_PROFILE1_0.xsdLocation + "\">");

                    // PROFILE FIELDS SERIALIZATION
                    pw.write("<objLabel>");
                    StreamUtility.enc(objProfile.objectLabel, pw);
                    pw.write("</objLabel>");
                    pw.write("<objOwnerId>");
                    StreamUtility.enc(objProfile.objectOwnerId, pw);
                    pw.write("</objOwnerId>");

                    pw.write("<objModels>\n");
                    for (String model : objProfile.objectModels) {
                        pw.write("<model>" + model + "</model>\n");
                    }
                    pw.write("</objModels>");

                    String cDate =
                            DateUtility
                                    .convertDateToString(objProfile.objectCreateDate);
                    pw.write("<objCreateDate>" + cDate + "</objCreateDate>");
                    String mDate =
                            DateUtility
                                    .convertDateToString(objProfile.objectLastModDate);
                    pw.write("<objLastModDate>" + mDate + "</objLastModDate>");;

                    pw.write("<objDissIndexViewURL>");
                    StreamUtility.enc(objProfile.dissIndexViewURL, pw);
                    pw.write("</objDissIndexViewURL>");
                    pw.write("<objItemIndexViewURL>");
                    StreamUtility.enc(objProfile.itemIndexViewURL, pw);
                    pw.write("</objItemIndexViewURL>");
                    pw.write("</objectProfile>");
                    pw.flush();
                    pw.close();
                } catch (IOException ioe) {
                    logger.error("WriteThread IOException", ioe);
                } finally {
                    try {
                        if (pw != null) {
                            pw.close();
                        }
                    } catch (IOException ioe) {
                        logger.error("WriteThread IOException", ioe);
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

}
