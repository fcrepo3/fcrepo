/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.oai;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.fcrepo.oai.OAIProvider;
import org.fcrepo.oai.OAIProviderServlet;
import org.fcrepo.oai.OAIResponder;
import org.fcrepo.oai.RepositoryException;
import org.fcrepo.server.Server;
import org.fcrepo.server.security.Authorization;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;




/**
 * FedoraOAIProviderServlet.
 *
 * @author Chris Wilper
 */
public class FedoraOAIProviderServlet
        extends OAIProviderServlet {

    private static final long serialVersionUID = 1L;

    Server m_server;

    OAIResponder m_responder;

    @Override
    public OAIResponder getResponder() throws RepositoryException {
        if (m_responder == null) {
            try {
                OAIProvider provider = m_server
                        .getBean("org.fcrepo.oai.OAIProvider", OAIProvider.class);
                Authorization authz = m_server
                        .getBean("org.fcrepo.server.security.Authorization", Authorization.class);
                m_responder = new OAIResponder(provider, authz);
            } catch (Exception e) {
                throw new RepositoryException(e.getClass().getName() + ": "
                        + e.getMessage());
            }
        }
        return m_responder;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            WebApplicationContext appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
            m_server = (Server)appContext.getBean("org.fcrepo.server.Server");
            if (m_server == null) failStartup("Could not retrieve org.fcrepo.server.Server bean",null);
        } catch (Throwable th) {
            String msg = "Fedora startup failed";
            failStartup(msg, th);
        }
    }

    /**
     * Prints a "FEDORA STARTUP ERROR" to STDERR along with the stacktrace of
     * the Throwable (if given) and finally, throws a ServletException.
     */
    private void failStartup(String message, Throwable th)
            throws ServletException {
        System.err.println("\n**************************");
        System.err.println("** FEDORA STARTUP ERROR **");
        System.err.println("**************************\n");
        System.err.println(message);
        if (th == null) {
            System.err.println();
            throw new ServletException(message);
        } else {
            th.printStackTrace();
            System.err.println();
            throw new ServletException(message, th);
        }
    }


    public static void main(String[] args) throws Exception {
        new FedoraOAIProviderServlet().test(args);
    }

}
