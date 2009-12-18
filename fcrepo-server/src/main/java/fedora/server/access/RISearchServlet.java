/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access;

import java.io.File;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.trippi.TriplestoreReader;
import org.trippi.TriplestoreWriter;
import org.trippi.server.TrippiServer;
import org.trippi.server.http.TrippiServlet;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.errors.servletExceptionExtensions.InternalError500Exception;
import fedora.server.errors.servletExceptionExtensions.RootException;
import fedora.server.resourceIndex.ResourceIndex;
import fedora.server.security.Authorization;

/**
 * RISearchServlet
 *
 * @version $Id$
 */
public class RISearchServlet
        extends TrippiServlet {

    private static final long serialVersionUID = 1L;

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(RISearchServlet.class);

    private static final String ACTION_LABEL = "Resource Index Search";

    private static final Logger logger =
            Logger.getLogger(ResourceIndex.class.getName());

    private Authorization m_authorization;

    @Override
    public TriplestoreReader getReader() throws ServletException {
        return getWriter();
    }

    @Override
    public TriplestoreWriter getWriter() throws ServletException {
        ResourceIndex writer = null;
        try {
            Server server =
                    Server.getInstance(new File(Constants.FEDORA_HOME), false);
            writer =
                    (ResourceIndex) server
                            .getModule("fedora.server.resourceIndex.ResourceIndex");
            if (m_authorization == null) {
                m_authorization =
                        (Authorization) server
                                .getModule("fedora.server.security.Authorization");
            }
        } catch (Exception e) {
            throw new ServletException("Error initting RISearchServlet.", e);
        }
        if (writer == null
                || writer.getIndexLevel() == ResourceIndex.INDEX_LEVEL_OFF) {
            throw new ServletException("The Resource Index Module is not "
                    + "enabled.");
        } else {
            return writer;
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
            LOG.error("Authorization failed for request: "
                    + request.getRequestURI() + " (actionLabel=" + ACTION_LABEL
                    + ")", e);
            throw RootException.getServletException(e,
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