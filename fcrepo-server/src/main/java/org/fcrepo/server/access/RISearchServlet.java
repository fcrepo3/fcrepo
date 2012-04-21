/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access;

import java.io.File;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.trippi.TriplestoreReader;
import org.trippi.TriplestoreWriter;
import org.trippi.server.TrippiServer;
import org.trippi.server.http.TrippiServlet;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.servletExceptionExtensions.InternalError500Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.RootException;
import org.fcrepo.server.resourceIndex.ResourceIndex;
import org.fcrepo.server.security.Authorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;



/**
 * RISearchServlet
 *
 * @version $Id$
 */
public class RISearchServlet
        extends TrippiServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger logger =
            LoggerFactory.getLogger(RISearchServlet.class);

    private static final String ACTION_LABEL = "Resource Index Search";

    private Authorization m_authorization;
    
    private ResourceIndex m_writer;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ApplicationContext appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
        Server server = (Server)appContext.getBean("org.fcrepo.server.Server");
        if (server == null) throw new ServletException("Could not retrieve org.fcrepo.server.Server bean");
        m_writer =
                (ResourceIndex) server
                        .getModule("org.fcrepo.server.resourceIndex.ResourceIndex"); 
        m_authorization =
                (Authorization) server
                        .getModule("org.fcrepo.server.security.Authorization");
    }

    @Override
    public TriplestoreReader getReader() throws ServletException {
        return getWriter();
    }

    @Override
    public TriplestoreWriter getWriter() throws ServletException {
        if (m_writer == null
                || m_writer.getIndexLevel() == ResourceIndex.INDEX_LEVEL_OFF) {
            throw new ServletException("The Resource Index Module is not "
                    + "enabled.");
        } else {
            return m_writer;
        }
    }

    @Override
    public void doGet(TrippiServer server,
                      HttpServletRequest request,
                      HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("doGet()\n" + "  type: "
                    + request.getParameter("type") + "\n" + "  template: "
                    + request.getParameter("template") + "\n" + "  lang: "
                    + request.getParameter("lang") + "\n" + "  query: "
                    + request.getParameter("query") + "\n" + "  limit: "
                    + request.getParameter("limit") + "\n" + "  distinct: "
                    + request.getParameter("distinct") + "\n" + "  format: "
                    + request.getParameter("format") + "\n" + "  flush: "
                    + request.getParameter("flush") + "\n" + "  dumbTypes: "
                    + request.getParameter("dumbTypes") + "\n");
        }
        try {
            Context context =
                    ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                                               request);
            m_authorization.enforceRIFindObjects(context);
            super.doGet(server, request, response);
        } catch (AuthzException e) {
            logger.error("Authorization failed for request: "
                    + request.getRequestURI() + " (actionLabel=" + ACTION_LABEL
                    + ")", e);
            throw RootException.getServletException(e,
                                                    request,
                                                    ACTION_LABEL,
                                                    new String[0]);
        } catch (Throwable th) {
            logger.error("Unexpected error servicing API-A request", th);
            throw new InternalError500Exception("",
                                                th,
                                                request,
                                                ACTION_LABEL,
                                                "",
                                                new String[0]);
        }
    }

    @Override
    public boolean closeOnDestroy() {
        return false;
    }

    @Override
    public String getIndexStylesheetLocation() {
        return "ri/index.xsl";
    }

    @Override
    public String getFormStylesheetLocation() {
        return "ri/form.xsl";
    }

    @Override
    public String getErrorStylesheetLocation() {
        return "ri/error.xsl";
    }

    @Override
    public String getContext(String origContext) {
        return "ri";
    }
}
