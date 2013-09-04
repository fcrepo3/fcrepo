/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access.dissemination;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;

import java.sql.Timestamp;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.SpringServlet;
import org.fcrepo.server.errors.InitializationException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.authorization.AuthzOperationalException;
import org.fcrepo.server.errors.servletExceptionExtensions.RootException;
import org.fcrepo.server.security.BackendPolicies;
import org.fcrepo.server.storage.ContentManagerParams;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.ExternalContentManager;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamMediation;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.fcrepo.server.storage.types.Property;
import org.fcrepo.server.utilities.ServerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * This servlet acts as a proxy to resolve the physical location of datastreams.
 *
 * <p>It requires a single parameter named <code>id</code> that denotes the
 * temporary id of the requested datastresm. This id is in the form of a
 * DateTime stamp. The servlet will perform an in-memory hashtable lookup
 * using the temporary id to obtain the actual physical location of the
 * datastream and then return the contents of the datastream as a MIME-typed
 * stream. This servlet is invoked primarily by external mechanisms needing to
 * retrieve the contents of a datastream.
 *
 * <p>The servlet also requires that an external mechanism request a datastream
 * within a finite time interval of the tempID's creation. This is to lessen the
 * risk of unauthorized access. The time interval within which a mechanism must
 * respond is set by the Fedora configuration parameter named
 * datastreamMediationLimit and is specified in milliseconds. If this
 * parameter is not supplied it defaults to 5000 milliseconds.
 *
 * @author Ross Wayland
 */
public class DatastreamResolverServlet
        extends SpringServlet {

    private static final Logger logger =
            LoggerFactory.getLogger(DatastreamResolverServlet.class);

    private static final long serialVersionUID = 1L;

    private DOManager m_manager;

    private static int datastreamMediationLimit;

    private static final String HTML_CONTENT_TYPE = "text/html";

    private static String fedoraServerHost;

    private static String fedoraServerPort;

    private static String fedoraServerRedirectPort;

    /**
     * Initialize servlet.
     *
     * @throws ServletException
     *         If the servlet cannot be initialized.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            fedoraServerPort = m_server.getParameter("fedoraServerPort");
            fedoraServerRedirectPort =
                    m_server.getParameter("fedoraRedirectPort");
            fedoraServerHost = m_server.getParameter("fedoraServerHost");
            m_manager =
                    (DOManager) m_server
                            .getModule("org.fcrepo.server.storage.DOManager");
            String expireLimit =
                    m_server.getParameter("datastreamMediationLimit");
            if (expireLimit == null || expireLimit.equalsIgnoreCase("")) {
                logger.info("datastreamMediationLimit unspecified, using default "
                        + "of 5 seconds");
                datastreamMediationLimit = 5000;
            } else {
                datastreamMediationLimit = Integer.parseInt(expireLimit);
                logger.info("datastreamMediationLimit: {}",
                        datastreamMediationLimit);
            }
        } catch (Throwable th) {
            logger.error("Error initializing servlet", th);
        }
    }

    private static final boolean contains(String[] array, String item) {
        boolean contains = false;
        for (String element : array) {
            if (element.equals(item)) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    public static final String ACTION_LABEL = "Resolve Datastream";

    /**
     * Processes the servlet request and resolves the physical location of the
     * specified datastream.
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
        String id = null;
        String dsPhysicalLocation = null;
        String dsControlGroupType = null;
        String user = null;
        String pass = null;
        MIMETypedStream mimeTypedStream = null;
        DisseminationService ds = null;
        Timestamp keyTimestamp = null;
        Timestamp currentTimestamp = null;
        PrintWriter out = null;
        ServletOutputStream outStream = null;
        String requestURI =
                request.getRequestURL().toString() + "?"
                        + request.getQueryString();

        id = request.getParameter("id").replaceAll("T", " ");
        logger.debug("Datastream tempID={}", id);

        logger.debug("DRS doGet()");

        try {
            // Check for required id parameter.
            if (id == null || id.equalsIgnoreCase("")) {
                String message =
                        "[DatastreamResolverServlet] No datastream ID "
                                + "specified in servlet request: "
                                + request.getRequestURI();
                logger.error(message);
                response
                        .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response
                        .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                   message);
                return;
            }
            id = id.replaceAll("T", " ").replaceAll("/", "").trim();

            // Get in-memory hashtable of mappings from Fedora server.
            ds = new DisseminationService(m_server);
            DatastreamMediation dm = DisseminationService.dsRegistry.get(id);
            if (dm == null) {
                StringBuffer entries = new StringBuffer();
                Iterator eIter = DisseminationService.dsRegistry.keySet().iterator();
                while (eIter.hasNext()) {
                    entries.append("'" + (String) eIter.next() + "' ");
                }
                throw new IOException("Cannot find datastream in temp registry by key: "
                        + id + "\n" + "Reg entries: " + entries.toString());
            }
            dsPhysicalLocation = dm.dsLocation;
            dsControlGroupType = dm.dsControlGroupType;
            user = dm.callUsername;
            pass = dm.callPassword;
            if (logger.isDebugEnabled()) {
                logger.debug("**************************** DatastreamResolverServlet dm.dsLocation: {}", dm.dsLocation);
                logger.debug("**************************** DatastreamResolverServlet dm.dsControlGroupType: {}", dm.dsControlGroupType);
                logger.debug("**************************** DatastreamResolverServlet dm.callUsername: {}", dm.callUsername);
                logger.debug("**************************** DatastreamResolverServlet dm.Password: {}", dm.callPassword);
                logger.debug("**************************** DatastreamResolverServlet dm.callbackRole: {}", dm.callbackRole);
                logger.debug("**************************** DatastreamResolverServlet dm.callbackBasicAuth: {}", dm.callbackBasicAuth);
                logger.debug("**************************** DatastreamResolverServlet dm.callBasicAuth: {}", dm.callBasicAuth);
                logger.debug("**************************** DatastreamResolverServlet dm.callbackSSl: {}", dm.callbackSSL);
                logger.debug("**************************** DatastreamResolverServlet dm.callSSl: {}", dm.callSSL);
                logger.debug("**************************** DatastreamResolverServlet non ssl port: {}", fedoraServerPort);
                logger.debug("**************************** DatastreamResolverServlet ssl port: {}", fedoraServerRedirectPort);
            }

            // DatastreamResolverServlet maps to two distinct servlet mappings
            // in fedora web.xml.
            // getDS - is used when the backend service is incapable of
            // basicAuth or SSL
            // getDSAuthenticated - is used when the backend service has
            // basicAuth and SSL enabled
            // Since both the getDS and getDSAuthenticated servlet targets map
            // to the same servlet
            // code and the Context used to initialize policy enforcement is
            // based on the incoming
            // HTTPRequest, the code must provide special handling for requests
            // using the getDS
            // target. When the incoming URL to DatastreamResolverServlet
            // contains the getDS target,
            // there are several conditions that must be checked to insure that
            // the correct role is
            // assigned to the request before policy enforcement occurs.
            // 1) if the mapped dsPhysicalLocation of the request is actually a
            // callback to the
            // Fedora server itself, then assign the role as
            // BACKEND_SERVICE_CALL_UNSECURE so
            // the basicAuth and SSL constraints will match those of the getDS
            // target.
            // 2) if the mapped dsPhysicalLocation of the request is actually a
            // Managed Content
            // or Inline XML Content datastream, then assign the role as
            // BACKEND_SERVICE_CALL_UNSECURE so
            // the basicAuth and SSL constraints will match the getDS target.
            // 3) Otherwise, leave the targetrole unchanged.
            if (request.getRequestURI().endsWith("getDS")
                    && (ServerUtility.isURLFedoraServer(dsPhysicalLocation)
                            || dsControlGroupType.equals("M") || dsControlGroupType
                            .equals("X"))) {
                if (logger.isDebugEnabled()) {
                    logger.debug("*********************** Changed role from: "
                            + dm.callbackRole + "  to: "
                            + BackendPolicies.BACKEND_SERVICE_CALL_UNSECURE);
                }
                dm.callbackRole = BackendPolicies.BACKEND_SERVICE_CALL_UNSECURE;
            }

            // If callback is to fedora server itself and callback is over SSL,
            // adjust the protocol and port
            // on the URL to match settings of Fedora server. This is necessary
            // since the SSL settings for the
            // backend service may have specified basicAuth=false, but contained
            // datastreams that are callbacks
            // to the local Fedora server which requires SSL. The version of
            // HttpClient currently in use does
            // not handle autoredirecting from http to https so it is necessary
            // to set the protocol and port
            // to the appropriate secure port.
            if (dm.callbackRole.equals(BackendPolicies.FEDORA_INTERNAL_CALL)) {
                if (dm.callbackSSL) {
                    dsPhysicalLocation =
                            dsPhysicalLocation.replaceFirst("http:", "https:");
                    dsPhysicalLocation =
                            dsPhysicalLocation
                                    .replaceFirst(fedoraServerPort,
                                                  fedoraServerRedirectPort);
                    if (logger.isDebugEnabled()) {
                        logger
                                .debug("*********************** DatastreamResolverServlet -- Was Fedora-to-Fedora call -- modified dsPhysicalLocation: "
                                        + dsPhysicalLocation);
                    }
                }
            }
            keyTimestamp = Timestamp.valueOf(ds.extractTimestamp(id));
            currentTimestamp = new Timestamp(new Date().getTime());
            logger.debug("dsPhysicalLocation={} dsControlGroupType={}",
                    dsPhysicalLocation, dsControlGroupType);

            // Deny mechanism requests that fall outside the specified time
            // interval.
            // The expiration limit can be adjusted using the Fedora config
            // parameter
            // named "datastreamMediationLimit" which is in milliseconds.
            long diff = currentTimestamp.getTime() - keyTimestamp.getTime();
            logger.debug("Timestamp diff for mechanism's reponse: {} ms.",
                    diff);
            if (diff > datastreamMediationLimit) {
                out = response.getWriter();
                response.setContentType(HTML_CONTENT_TYPE);
                out
                        .println("<br><b>[DatastreamResolverServlet] Error:</b>"
                                + "<font color=\"red\"> Deployment has failed to respond "
                                + "to the DatastreamResolverServlet within the specified "
                                + "time limit of \""
                                + datastreamMediationLimit
                                + "\""
                                + "milliseconds. Datastream access denied.");
                logger.error("Deployment failed to respond to "
                        + "DatastreamResolverServlet within time limit of "
                        + datastreamMediationLimit);
                out.close();
                return;
            }

            if (dm.callbackRole == null) {
                throw new AuthzOperationalException("no callbackRole for this ticket");
            }
            String targetRole = //Authorization.FEDORA_ROLE_KEY + "=" +
                    dm.callbackRole; // restrict access to role of this
            // ticket
            String[] targetRoles = {targetRole};
            Context context =
                    ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                                               request); // , targetRoles);
            if (request.getRemoteUser() == null) {
                // non-authn: must accept target role of ticket
                logger.debug("DatastreamResolverServlet: unAuthenticated request");
            } else {
                // authn: check user roles for target role of ticket
                /*
                 * logger.debug("DatastreamResolverServlet: Authenticated request
                 * getting user"); String[] roles = null; Principal principal =
                 * request.getUserPrincipal(); if (principal == null) { // no
                 * principal to grok roles from!! } else { try { roles =
                 * ReadOnlyContext.getRoles(principal); } catch (Throwable t) { } }
                 * if (roles == null) { roles = new String[0]; }
                 */
                //XXXXXXXXXXXXXXXXXXXXXXxif (contains(roles, targetRole)) {
                logger.debug("DatastreamResolverServlet: user=={}",
                        request.getRemoteUser());
                /*
                 * if
                 * (((ExtendedHttpServletRequest)request).isUserInRole(targetRole)) {
                 * logger.debug("DatastreamResolverServlet: user has required
                 * role"); } else { logger.debug("DatastreamResolverServlet: authZ
                 * exception in validating user"); throw new
                 * AuthzDeniedException("wrong user for this ticket"); }
                 */
            }

            if (logger.isDebugEnabled()) {
                logger.debug("debugging backendService role");
                logger.debug("targetRole=" + targetRole);
                int targetRolesLength = targetRoles.length;
                logger.debug("targetRolesLength=" + targetRolesLength);
                if (targetRolesLength > 0) {
                    logger.debug("targetRoles[0]=" + targetRoles[0]);
                }
                int nSubjectValues = context.nSubjectValues(targetRole);
                logger.debug("nSubjectValues=" + nSubjectValues);
                if (nSubjectValues > 0) {
                    logger.debug("context.getSubjectValue(targetRole)="
                            + context.getSubjectValue(targetRole));
                }
                Iterator<String> subjectNames = context.subjectAttributes();
                while (subjectNames.hasNext()) {
                    String name = subjectNames.next();
                    int n = context.nSubjectValues(name);
                    switch (n) {
                        case 0:
                            logger.debug("no subject attributes for " + name);
                            break;
                        case 1:
                            String value = context.getSubjectValue(name);
                            logger.debug("single subject attributes for " + name
                                    + "=" + value);
                            break;
                        default:
                            String[] values = context.getSubjectValues(name);
                            for (String element : values) {
                                logger
                                        .debug("another subject attribute from context "
                                                + name + "=" + element);
                            }
                    }
                }
                Iterator<URI> it = context.environmentAttributes();
                while (it.hasNext()) {
                    URI name = it.next();
                    String value = context.getEnvironmentValue(name);
                    logger.debug("another environment attribute from context "
                            + name + "=" + value);
                }
            }
            /*
             * // Enforcement of Backend Security is temporarily disabled
             * pending refactoring. // logger.debug("DatastreamResolverServlet:
             * about to do final authZ check"); Authorization authorization =
             * (Authorization) s_server
             * .getModule("org.fcrepo.server.security.Authorization");
             * authorization.enforceResolveDatastream(context, keyTimestamp);
             * logger.debug("DatastreamResolverServlet: final authZ check
             * suceeded.....");
             */

            if (dsControlGroupType.equalsIgnoreCase("E")) {
                // testing to see what's in request header that might be of
                // interest
                if (logger.isDebugEnabled()) {
                    for (Enumeration e = request.getHeaderNames(); e
                            .hasMoreElements();) {
                        String name = (String) e.nextElement();
                        Enumeration headerValues = request.getHeaders(name);
                        StringBuffer sb = new StringBuffer();
                        while (headerValues.hasMoreElements()) {
                            sb.append((String) headerValues.nextElement());
                        }
                        String value = sb.toString();
                        logger
                                .debug("DATASTREAMRESOLVERSERVLET REQUEST HEADER CONTAINED: "
                                        + name + " : " + value);
                    }
                }

                // Datastream is ReferencedExternalContent so dsLocation is a
                // URL string
                ExternalContentManager externalContentManager =
                        (ExternalContentManager) m_server
                                .getModule("org.fcrepo.server.storage.ExternalContentManager");
                ContentManagerParams params = new ContentManagerParams(dsPhysicalLocation);
                params.setContext(context);
                mimeTypedStream = externalContentManager.getExternalContent(params);

                // had substituted context:
                // ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                // request));
                outStream = response.getOutputStream();
                response.setContentType(mimeTypedStream.MIMEType);
                Property[] headerArray = mimeTypedStream.header;
                if (headerArray != null) {
                    for (int i = 0; i < headerArray.length; i++) {
                        if (headerArray[i].name != null
                                && !headerArray[i].name
                                        .equalsIgnoreCase("content-type")) {
                            response.addHeader(headerArray[i].name,
                                               headerArray[i].value);
                            logger.debug("THIS WAS ADDED TO DATASTREAMRESOLVERSERVLET RESPONSE HEADER FROM ORIGINATING PROVIDER {} : {}",
                                            headerArray[i].name, headerArray[i].value);
                        }
                    }
                }
                int byteStream = 0;
                byte[] buffer = new byte[255];
                while ((byteStream = mimeTypedStream.getStream().read(buffer)) != -1) {
                    outStream.write(buffer, 0, byteStream);
                }
                buffer = null;
                outStream.flush();
                mimeTypedStream.close();
            } else if (dsControlGroupType.equalsIgnoreCase("M")
                    || dsControlGroupType.equalsIgnoreCase("X")) {
                // Datastream is either XMLMetadata or ManagedContent so
                // dsLocation
                // is in the form of an internal Fedora ID using the syntax:
                // PID+DSID+DSVersID; parse the ID and get the datastream
                // content.
                String PID = null;
                String dsVersionID = null;
                String dsID = null;
                String[] s = dsPhysicalLocation.split("\\+");
                if (s.length != 3) {
                    String message =
                            "[DatastreamResolverServlet]  The "
                                    + "internal Fedora datastream id:  \""
                                    + dsPhysicalLocation + "\"  is invalid.";
                    logger.error(message);
                    throw new ServletException(message);
                }
                PID = s[0];
                dsID = s[1];
                dsVersionID = s[2];
                logger.debug("PID={}, dsID={}, dsVersionID={}",
                        PID, dsID, dsVersionID);

                DOReader doReader =
                        m_manager.getReader(Server.USE_DEFINITIVE_STORE,
                                            context,
                                            PID);
                Datastream d =
                        doReader.getDatastream(dsID, dsVersionID);
                logger.debug("Got datastream: {}", d.DatastreamID);
                InputStream is = d.getContentStream(context);
                int bytestream = 0;
                response.setContentType(d.DSMIME);
                outStream = response.getOutputStream();
                byte[] buffer = new byte[255];
                while ((bytestream = is.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytestream);
                }
                buffer = null;
                is.close();
            } else {
                out = response.getWriter();
                response.setContentType(HTML_CONTENT_TYPE);
                out
                        .println("<br>[DatastreamResolverServlet] Unknown "
                                + "dsControlGroupType: " + dsControlGroupType
                                + "</br>");
                logger.error("Unknown dsControlGroupType: " + dsControlGroupType);
            }
        } catch (AuthzException ae) {
            logger.error("Authorization failure resolving datastream"
                    + " (actionLabel=" + ACTION_LABEL + ")", ae);
            throw RootException.getServletException(ae,
                                                    request,
                                                    ACTION_LABEL,
                                                    new String[0]);
        } catch (Throwable th) {
            logger.error("Error resolving datastream", th);
            String message =
                    "[DatastreamResolverServlet] returned an error. The "
                            + "underlying error was a  \""
                            + th.getClass().getName() + "  The message was  \""
                            + th.getMessage() + "\".  ";
            throw new ServletException(message);
        } finally {
            if (out != null) {
                out.close();
            }
            if (outStream != null) {
                outStream.close();
            }
            DisseminationService.dsRegistry.remove(id);
        }
    }

    // Clean up resources
    @Override
    public void destroy() {
    }

}
