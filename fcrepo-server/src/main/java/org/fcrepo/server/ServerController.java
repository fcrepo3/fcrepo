/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcrepo.common.Constants;
import org.fcrepo.server.errors.DatastreamNotFoundException;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.ObjectNotFoundException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.authorization.AuthzDeniedException;
import org.fcrepo.server.errors.authorization.AuthzOperationalException;
import org.fcrepo.server.errors.authorization.AuthzPermittedException;
import org.fcrepo.server.errors.servletExceptionExtensions.BadRequest400Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.Continue100Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.Forbidden403Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.InternalError500Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.NotFound404Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.Ok200Exception;
import org.fcrepo.server.management.DefaultManagement;
import org.fcrepo.server.management.ManagementModule;
import org.fcrepo.server.security.Authorization;
import org.fcrepo.server.utilities.PIDStreamIterableWrapper;
import org.fcrepo.server.utilities.ServerUtilitySerializer;
import org.fcrepo.server.utilities.status.ServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Server Controller.
 *
 * @author Chris Wilper
 */
public class ServerController
        extends SpringServlet {

    private static final Logger logger =
        LoggerFactory.getLogger(DefaultManagement.class);

    private static final long serialVersionUID = 1L;

    private static String PROTOCOL_FILE = "file:///";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String actionLabel = "server control";
        String action = request.getParameter("action");

        if (action == null) {
            throw new BadRequest400Exception(request,
                                             actionLabel,
                                             "no action",
                                             new String[0]);
        }

        if (action.equals("status")) {
            statusAction(request, response);
        } else if (action.equals("reloadPolicies")) {
            reloadPoliciesAction(request, response);
        } else if (action.equals("modifyDatastreamControlGroup")) {
            modifyDatastreamControlGroupAction(request, response);
        } else {
            throw new BadRequest400Exception(request, actionLabel, "bad action:  "
                                             + action, new String[0]);
        }
    }

    private void statusAction(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String actionLabel = "getting server status";
            Context context =
                    ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                                               request);
            if (m_server == null) {
                throw new InternalError500Exception(request,
                                                    actionLabel,
                                                    "server not available",
                                                    new String[0]);
            }
            try {
                m_server.status(context);
            } catch (AuthzOperationalException aoe) {
                throw new Forbidden403Exception(request,
                                                actionLabel,
                                                "authorization failed",
                                                new String[0]);
            } catch (AuthzDeniedException ade) {
                throw new Forbidden403Exception(request,
                                                actionLabel,
                                                "authorization denied",
                                                new String[0]);
            } catch (AuthzPermittedException ape) {
                throw new Continue100Exception(request,
                                               actionLabel,
                                               "authorization permitted",
                                               new String[0]);
            } catch (Throwable t) {
                throw new InternalError500Exception(request,
                                                    actionLabel,
                                                    "error performing action2",
                                                    new String[0]);
            }
            throw new Ok200Exception(request,
                                     actionLabel,
                                     "server running",
                                     new String[0]);

        }

    private void reloadPoliciesAction(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String actionLabel = "reloading repository policies";
            Context context =
                    ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                                               request);
            if (m_server == null) {
                throw new InternalError500Exception(request,
                                                    actionLabel,
                                                    "server not available",
                                                    new String[0]);
            }
            Authorization authModule = null;
            authModule =
                    m_server.getBean("org.fcrepo.server.security.Authorization", Authorization.class);
            if (authModule == null) {
                throw new InternalError500Exception(request,
                                                    actionLabel,
                                                    "Required Authorization module not available",
                                                    new String[0]);
            }
            try {
                authModule.reloadPolicies(context);
            } catch (AuthzOperationalException aoe) {
                throw new Forbidden403Exception(request,
                                                actionLabel,
                                                "authorization failed",
                                                new String[0]);
            } catch (AuthzDeniedException ade) {
                throw new Forbidden403Exception(request,
                                                actionLabel,
                                                "authorization denied",
                                                new String[0]);
            } catch (AuthzPermittedException ape) {
                throw new Continue100Exception(request,
                                               actionLabel,
                                               "authorization permitted",
                                               new String[0]);
            } catch (Throwable t) {
                throw new InternalError500Exception(request,
                                                    actionLabel,
                                                    "error performing action2",
                                                    new String[0]);
            }
            throw new Ok200Exception(request,
                                     actionLabel,
                                     "server running",
                                     new String[0]);

        }


    private boolean getParameterAsBoolean(HttpServletRequest request, String name, boolean defaultValue) {

        String parameter = request.getParameter(name);
        boolean res;

        if (parameter == null || parameter.isEmpty()) {
            res = defaultValue;
        } else {
            if (parameter.toLowerCase().equals("true") || parameter.toLowerCase().equals("yes")) {
                res = true;
            } else if (parameter.toLowerCase().equals("false") || parameter.toLowerCase().equals("no")) {
                res = false;
            } else {
                throw new IllegalArgumentException("Invalid value " + parameter + " supplied for " + name + ".  Please use true or false");
    }
        }



        return res;
    }


    // FIXME: see FCREPO-765 - this should be migrated to an admin API (possibly with other methods from this class)
    private void modifyDatastreamControlGroupAction(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String actionLabel = "modifying datastream control group";

        Context context =
                ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                                           request);
        if (m_server == null) {
            throw new InternalError500Exception(request,
                                                actionLabel,
                                                "server not available",
                                                new String[0]);
        }
        // FIXME: see FCREPO-765 Admin methods are currently in DefaultManagement and carried through to ManagementModule
        ManagementModule apimDefault = (ManagementModule) m_server.getModule("org.fcrepo.server.management.Management");

        // FIXME: see FCREPO-765. tidy up output writing

        // get parameters
        String pid = request.getParameter("pid");
        String dsID = request.getParameter("dsID");
        String controlGroup = request.getParameter("controlGroup");
        boolean addXMLHeader = getParameterAsBoolean(request, "addXMLHeader", false);
        boolean reformat = getParameterAsBoolean(request, "reformat", false);
        boolean setMIMETypeCharset = getParameterAsBoolean(request, "setMIMETypeCharset", false);

        // get datastream list (single ds id is a list of one)
        String[] datastreams = dsID.split(",");

        // get iterable for pid looping
        boolean singlePID;
        Iterable<String> pids = null;
        if (pid.startsWith(PROTOCOL_FILE)) {
            File pidFile = new File(pid.substring(PROTOCOL_FILE.length()));
            pids = new PIDStreamIterableWrapper(new FileInputStream(pidFile));
            singlePID = false;

        } else { // pid list
            String[] pidList = pid.split(",");
            pids = new ArrayList<String>(Arrays.asList(pidList));
            singlePID = (pidList.length == 1);
        }

        try {

            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding("UTF-8");

            PrintWriter pw = response.getWriter();

            // if doing a single pid/datastream, simple xml output
            if (singlePID && datastreams.length == 1) {
                response.setContentType("text/xml; charset=UTF-8");
                Date[] versions = apimDefault.modifyDatastreamControlGroup(context, pid, dsID, controlGroup, addXMLHeader, reformat, setMIMETypeCharset);
                pw.write("<versions>\n");
                    for (Date version : versions) {
                        pw.write("<version>" + version.toString() + "</version>\n");
                    }
                pw.write("</versions>\n");

            } else { // logging style output
                response.setContentType("text/plain; charset=UTF-8");
                ServerUtilitySerializer ser = new ServerUtilitySerializer(pw);
                for (String curpid : pids) {
                    ser.startObject(curpid);
                    for (String curdsID : datastreams) {
                        ser.startDatastream(curdsID);
                        Date[] versions;
                        try {
                            versions = apimDefault.modifyDatastreamControlGroup(context, curpid, curdsID, controlGroup, addXMLHeader, reformat, setMIMETypeCharset);
                        } catch (DatastreamNotFoundException e) {
                            versions = null;
                        }
                        ser.writeVersions(versions);
                        ser.endDatastream();
                    }
                    ser.endObject();
                }

                ser.finish();
            }
            pw.flush();


        } catch (ObjectNotFoundException e) {
            logger.error("Object not found: " + pid + " - " + e.getMessage());
            throw new NotFound404Exception(request,
                                           actionLabel,
                                           e.getMessage(),
                                           new String[0]);
        } catch (DatastreamNotFoundException e) {
            logger.error("Datastream not found: " + pid + "/" + dsID + " - " + e.getMessage());
            throw new NotFound404Exception(request,
                                           actionLabel,
                                           e.getMessage(),
                                           new String[0]);


        } catch (GeneralException e) {
            logger.error(e.getMessage());
            throw new InternalError500Exception(request,
                                                actionLabel,
                                                e.getMessage(),
                                                new String[0]);
        } catch (AuthzOperationalException aoe) {
            throw new Forbidden403Exception(request,
                                            actionLabel,
                                            "authorization failed",
                                            new String[0]);
        } catch (AuthzDeniedException ade) {
            throw new Forbidden403Exception(request,
                                            actionLabel,
                                            "authorization denied",
                                            new String[0]);
        } catch (AuthzPermittedException ape) {
            throw new Continue100Exception(request,
                                           actionLabel,
                                           "authorization permitted",
                                           new String[0]);

        } catch (ServerException e) {
            logger.error(e.getMessage(),e);
            throw new InternalError500Exception(request,
                                                actionLabel,
                                                "Unexpected error: " + e.getMessage(),
                                                new String[0]);
        }

    }


    @Override
    public void init() throws ServletException {
        super.init();
    }

    @Override
    public void destroy() {

        if (m_server != null) {
            try {
                m_status.append(ServerState.STOPPING,
                               "Shutting down Fedora Server and modules");
                m_server.shutdown(null);
                m_status.append(ServerState.STOPPED, "Shutdown Successful");
            } catch (Throwable th) {
                try {
                    m_status.appendError(ServerState.STOPPED_WITH_ERR, th);
                } catch (Exception e) {
                }
            }
            m_server = null;
        }

        super.destroy();
    }

}
