/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access.dissemination;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import java.sql.Timestamp;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.InitializationException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.errors.authorization.AuthzOperationalException;
import fedora.server.errors.servletExceptionExtensions.RootException;
import fedora.server.security.BackendPolicies;
import fedora.server.storage.ContentManagerParams;
import fedora.server.storage.DOManager;
import fedora.server.storage.DOReader;
import fedora.server.storage.ExternalContentManager;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamMediation;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.Property;
import fedora.server.utilities.ServerUtility;

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
        extends HttpServlet {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DatastreamResolverServlet.class.getName());

    private static final long serialVersionUID = 1L;

    private static Server s_server;

    private static DOManager m_manager;

    private static Hashtable dsRegistry;

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
    public void init() throws ServletException {
        try {
            s_server =
                    Server.getInstance(new File(Constants.FEDORA_HOME), false);
            fedoraServerPort = s_server.getParameter("fedoraServerPort");
            fedoraServerRedirectPort =
                    s_server.getParameter("fedoraRedirectPort");
            fedoraServerHost = s_server.getParameter("fedoraServerHost");
            m_manager =
                    (DOManager) s_server
                            .getModule("fedora.server.storage.DOManager");
            String expireLimit =
                    s_server.getParameter("datastreamMediationLimit");
            if (expireLimit == null || expireLimit.equalsIgnoreCase("")) {
                LOG.info("datastreamMediationLimit unspecified, using default "
                        + "of 5 seconds");
                datastreamMediationLimit = 5000;
            } else {
                datastreamMediationLimit = new Integer(expireLimit).intValue();
                LOG.info("datastreamMediationLimit: "
                        + datastreamMediationLimit);
            }
        } catch (InitializationException ie) {
            throw new ServletException("Unable to get an instance of Fedora server "
                    + "-- " + ie.getMessage());
        } catch (Throwable th) {
            LOG.error("Error initializing servlet", th);
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
        LOG.debug("Datastream tempID=" + id);

        LOG.debug("DRS doGet()");

        try {
            // Check for required id parameter.
            if (id == null || id.equalsIgnoreCase("")) {
                String message =
                        "[DatastreamResolverServlet] No datastream ID "
                                + "specified in servlet request: "
                                + request.getRequestURI();
                LOG.error(message);
                response
                        .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response
                        .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                   message);
                return;
            }
            id = id.replaceAll("T", " ").replaceAll("/", "").trim();

            // Get in-memory hashtable of mappings from Fedora server.
            ds = new DisseminationService();
            dsRegistry = DisseminationService.dsRegistry;
            DatastreamMediation dm = (DatastreamMediation) dsRegistry.get(id);
            if (dm == null) {
                StringBuffer entries = new StringBuffer();
                Iterator eIter = dsRegistry.keySet().iterator();
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
            if (LOG.isDebugEnabled()) {
                LOG
                        .debug("**************************** DatastreamResolverServlet dm.dsLocation: "
                                + dm.dsLocation);
                LOG
                        .debug("**************************** DatastreamResolverServlet dm.dsControlGroupType: "
                                + dm.dsControlGroupType);
                LOG
                        .debug("**************************** DatastreamResolverServlet dm.callUsername: "
                                + dm.callUsername);
                LOG
                        .debug("**************************** DatastreamResolverServlet dm.Password: "
                                + dm.callPassword);
                LOG
                        .debug("**************************** DatastreamResolverServlet dm.callbackRole: "
                                + dm.callbackRole);
                LOG
                        .debug("**************************** DatastreamResolverServlet dm.callbackBasicAuth: "
                                + dm.callbackBasicAuth);
                LOG
                        .debug("**************************** DatastreamResolverServlet dm.callBasicAuth: "
                                + dm.callBasicAuth);
                LOG
                        .debug("**************************** DatastreamResolverServlet dm.callbackSSl: "
                                + dm.callbackSSL);
                LOG
                        .debug("**************************** DatastreamResolverServlet dm.callSSl: "
                                + dm.callSSL);
                LOG
                        .debug("**************************** DatastreamResolverServlet non ssl port: "
                                + fedoraServerPort);
                LOG
                        .debug("**************************** DatastreamResolverServlet ssl port: "
                                + fedoraServerRedirectPort);
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
                if (LOG.isDebugEnabled()) {
                    LOG.debug("*********************** Changed role from: "
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
                    if (LOG.isDebugEnabled()) {
                        LOG
                                .debug("*********************** DatastreamResolverServlet -- Was Fedora-to-Fedora call -- modified dsPhysicalLocation: "
                                        + dsPhysicalLocation);
                    }
                }
            }
            keyTimestamp = Timestamp.valueOf(ds.extractTimestamp(id));
            currentTimestamp = new Timestamp(new Date().getTime());
            LOG.debug("dsPhysicalLocation=" + dsPhysicalLocation
                    + "dsControlGroupType=" + dsControlGroupType);

            // Deny mechanism requests that fall outside the specified time
            // interval.
            // The expiration limit can be adjusted using the Fedora config
            // parameter
            // named "datastreamMediationLimit" which is in milliseconds.
            long diff = currentTimestamp.getTime() - keyTimestamp.getTime();
            LOG.debug("Timestamp diff for mechanism's reponse: " + diff
                    + " ms.");
            if (diff > (long) datastreamMediationLimit) {
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
                LOG.error("Deployment failed to respond to "
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
                LOG.debug("DatastreamResolverServlet: unAuthenticated request");
            } else {
                // authn: check user roles for target role of ticket
                /*
                 * LOG.debug("DatastreamResolverServlet: Authenticated request
                 * getting user"); String[] roles = null; Principal principal =
                 * request.getUserPrincipal(); if (principal == null) { // no
                 * principal to grok roles from!! } else { try { roles =
                 * ReadOnlyContext.getRoles(principal); } catch (Throwable t) { } }
                 * if (roles == null) { roles = new String[0]; }
                 */
                //XXXXXXXXXXXXXXXXXXXXXXxif (contains(roles, targetRole)) {
                LOG.debug("DatastreamResolverServlet: user=="
                        + request.getRemoteUser());
                /*
                 * if
                 * (((ExtendedHttpServletRequest)request).isUserInRole(targetRole)) {
                 * LOG.debug("DatastreamResolverServlet: user has required
                 * role"); } else { LOG.debug("DatastreamResolverServlet: authZ
                 * exception in validating user"); throw new
                 * AuthzDeniedException("wrong user for this ticket"); }
                 */
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("debugging backendService role");
                LOG.debug("targetRole=" + targetRole);
                int targetRolesLength = targetRoles.length;
                LOG.debug("targetRolesLength=" + targetRolesLength);
                if (targetRolesLength > 0) {
                    LOG.debug("targetRoles[0]=" + targetRoles[0]);
                }
                int nSubjectValues = context.nSubjectValues(targetRole);
                LOG.debug("nSubjectValues=" + nSubjectValues);
                if (nSubjectValues > 0) {
                    LOG.debug("context.getSubjectValue(targetRole)="
                            + context.getSubjectValue(targetRole));
                }
                Iterator it = context.subjectAttributes();
                while (it.hasNext()) {
                    String name = (String) it.next();
                    int n = context.nSubjectValues(name);
                    switch (n) {
                        case 0:
                            LOG.debug("no subject attributes for " + name);
                            break;
                        case 1:
                            String value = context.getSubjectValue(name);
                            LOG.debug("single subject attributes for " + name
                                    + "=" + value);
                            break;
                        default:
                            String[] values = context.getSubjectValues(name);
                            for (String element : values) {
                                LOG
                                        .debug("another subject attribute from context "
                                                + name + "=" + element);
                            }
                    }
                }
                it = context.environmentAttributes();
                while (it.hasNext()) {
                    String name = (String) it.next();
                    String value = context.getEnvironmentValue(name);
                    LOG.debug("another environment attribute from context "
                            + name + "=" + value);
                }
            }
            /*
             * // Enforcement of Backend Security is temporarily disabled
             * pending refactoring. // LOG.debug("DatastreamResolverServlet:
             * about to do final authZ check"); Authorization authorization =
             * (Authorization) s_server
             * .getModule("fedora.server.security.Authorization");
             * authorization.enforceResolveDatastream(context, keyTimestamp);
             * LOG.debug("DatastreamResolverServlet: final authZ check
             * suceeded.....");
             */

            if (dsControlGroupType.equalsIgnoreCase("E")) {
                // testing to see what's in request header that might be of
                // interest
                if (LOG.isDebugEnabled()) {
                    for (Enumeration e = request.getHeaderNames(); e
                            .hasMoreElements();) {
                        String name = (String) e.nextElement();
                        Enumeration headerValues = request.getHeaders(name);
                        StringBuffer sb = new StringBuffer();
                        while (headerValues.hasMoreElements()) {
                            sb.append((String) headerValues.nextElement());
                        }
                        String value = sb.toString();
                        LOG
                                .debug("DATASTREAMRESOLVERSERVLET REQUEST HEADER CONTAINED: "
                                        + name + " : " + value);
                    }
                }

                // Datastream is ReferencedExternalContent so dsLocation is a
                // URL string
                ExternalContentManager externalContentManager =
                        (ExternalContentManager) s_server
                                .getModule("fedora.server.storage.ExternalContentManager");
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
                            LOG
                                    .debug("THIS WAS ADDED TO DATASTREAMRESOLVERSERVLET RESPONSE HEADER FROM ORIGINATING PROVIDER "
                                            + headerArray[i].name
                                            + " : "
                                            + headerArray[i].value);
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
                    LOG.error(message);
                    throw new ServletException(message);
                }
                PID = s[0];
                dsID = s[1];
                dsVersionID = s[2];
                LOG.debug("PID=" + PID + ", dsID=" + dsID + ", dsVersionID="
                        + dsVersionID);

                DOReader doReader =
                        m_manager.getReader(Server.USE_DEFINITIVE_STORE,
                                            context,
                                            PID);
                Datastream d =
                        (Datastream) doReader.getDatastream(dsID, dsVersionID);
                LOG.debug("Got datastream: " + d.DatastreamID);
                InputStream is = d.getContentStream();
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
                LOG.error("Unknown dsControlGroupType: " + dsControlGroupType);
            }
        } catch (AuthzException ae) {
            LOG.error("Authorization failure resolving datastream"
                    + " (actionLabel=" + ACTION_LABEL + ")", ae);
            throw RootException.getServletException(ae,
                                                    request,
                                                    ACTION_LABEL,
                                                    new String[0]);
        } catch (Throwable th) {
            LOG.error("Error resolving datastream", th);
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
            dsRegistry.remove(id);
        }
    }

    // Clean up resources
    public void destroy() {
    }

}
