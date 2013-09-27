
package org.fcrepo.server.security.xacml.test;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SnoopFilter
        implements Filter {

    private static final Logger logger =
            LoggerFactory.getLogger(SnoopFilter.class);

    private FilterConfig filterConfig = null;

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException,
            ServletException {
        // Need to make sure we are dealing with HttpServlets
        if (!(request instanceof HttpServletRequest)
                || !(response instanceof HttpServletResponse)) {
            logger.error("Servlets are not HttpServlets!");
            throw new ServletException("Servlets are not HttpServlets!");
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();

        if (logger.isDebugEnabled()) {
            logger.debug("Incoming Request, URI: {}", uri);
            logger.debug("Headers: ");
            Enumeration<?> headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = (String) headerNames.nextElement();
                Enumeration<?> values = req.getHeaders(headerName);
                while (values.hasMoreElements()) {
                    String value = (String) values.nextElement();
                    logger.debug("{}: {}", headerName, value);
                }
            }
            logger.debug("userPrincipal: " + req.getUserPrincipal());
            logger.debug("remoteUser: " + req.getRemoteUser());
        }

        chain.doFilter(req, res);

        if (logger.isDebugEnabled()) {
            logger.debug("Outgoing Response, URI: " + uri);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterCfg) throws ServletException {
        logger.info("Initialising Servlet Filter: " + this.getClass().getName());
        filterConfig = filterCfg;

        // exit if no config. Should always have a config.
        if (filterConfig == null) {
            logger.error("No config found!");
            throw new ServletException("No config found for filter (filterConfig)");
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        logger.info("Destroying Servlet Filter: " + this.getClass().getName());
        filterConfig = null;
    }
}
