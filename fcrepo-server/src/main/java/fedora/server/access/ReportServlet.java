/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.InitializationException;
import fedora.server.errors.QueryParseException;
import fedora.server.errors.ServerException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.errors.servletExceptionExtensions.RootException;

/**
 * Servlet exposing reporting functionality.
 * 
 * @author Bill Niebel
 */
public class ReportServlet
        extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private Server s_server = null;

    @Override
    public void init() throws ServletException {
        try {
            s_server =
                    Server.getInstance(new File(Constants.FEDORA_HOME), false);
        } catch (InitializationException ie) {
            throw new ServletException("Error getting Fedora Server instance: "
                    + ie.getMessage());
        }
    }

    /** Exactly the same behavior as doGet. */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    public static final String ACTION_LABEL = "Report on Repository";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String sessionToken = request.getParameter("sessionToken");

        String remoteAddr = request.getRemoteAddr();

        String query = request.getParameter("query");

        //Hashtable parmshash = new Hashtable();
        String[] fieldsArray = null;
        {
            ArrayList fieldsList = new ArrayList();
            Enumeration enm = request.getParameterNames();
            while (enm.hasMoreElements()) {
                String name = (String) enm.nextElement();
                if (Report.allFields.contains(name)) {
                    fieldsList.add(name);
                    //} else if (Report.parms.contains(name)) {
                    //parmshash.put(name,request.getParameter(name));
                }
            }
            if (fieldsList.size() > 0) {
                fieldsArray = (String[]) fieldsList.toArray(new String[] {});
            }
        }

        String maxResults = request.getParameter("maxResults");

        String newBase = request.getParameter("newBase");

        String xslt = request.getParameter("xslt");

        String reportName = request.getParameter("report");

        String prefix = request.getParameter("prefix");

        String dateRange = request.getParameter("dateRange");

        Report report = null;
        try {
            Context context =
                    ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                                               request);
            report =
                    Report.getInstance(context,
                                       remoteAddr,
                                       sessionToken,
                                       reportName,
                                       fieldsArray,
                                       query,
                                       xslt,
                                       maxResults,
                                       newBase,
                                       prefix,
                                       dateRange);
        } catch (AuthzException ae) {
            throw RootException.getServletException(ae,
                                                    request,
                                                    ACTION_LABEL,
                                                    new String[0]);
        } catch (QueryParseException e1) {
            throw new ServletException("bad query parm", e1);
        } catch (ServerException e1) {
            throw new ServletException("server not available", e1);
        }

        String contentType = report.getContentType();
        response.setContentType(contentType + "; charset=UTF-8");
        OutputStream out = null; // PrintWriter
        if ("text/xml".equals(contentType)) {
            out = response.getOutputStream();
        } else if ("text/html".equals(contentType)) {
            out = response.getOutputStream(); //response.getWriter();
        }

        try {
            report.writeOut(out);
        } catch (QueryParseException e) {
            throw new ServletException(e.getMessage(), e);
        } catch (ServerException e) {
            throw new ServletException(e.getMessage(), e);
        } catch (TransformerConfigurationException e) {
            throw new ServletException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ServletException(e.getMessage(), e);
        } catch (TransformerException e) {
            throw new ServletException(e.getMessage(), e);
        }

    }

}
