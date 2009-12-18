/*
 * File: PEP.java
 *
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package melcoe.fedora.pep.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import melcoe.fedora.pep.AuthzDeniedException;
import melcoe.fedora.pep.ContextHandler;
import melcoe.fedora.pep.ContextHandlerImpl;
import melcoe.fedora.pep.PEPException;
import melcoe.fedora.pep.rest.filters.DataResponseWrapper;
import melcoe.fedora.pep.rest.filters.ParameterRequestWrapper;
import melcoe.fedora.pep.rest.filters.RESTFilter;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;

import fedora.common.Constants;

/**
 * This is the PEP for the REST interface.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public final class PEP
        implements Filter {

    private static Logger log = Logger.getLogger(PEP.class.getName());

    private FilterConfig filterConfig = null;

    private Map<String, RESTFilter> filters = null;

    private ContextHandler ctxHandler = null;

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException,
            ServletException {
        // if a response has already been committed, bypass this filter...
        if (response.isCommitted()) {
            if (log.isDebugEnabled()) {
                log
                        .debug("Response has already been committed. Bypassing PEP.");
            }
            // continuing the chain once auth has failed causes errors. Short
            // circuiting the path here.
            // remove this if it causes problems
            // chain.doFilter(request, response);
            return;
        }

        // Need to make sure we are dealing with HttpServlets
        if (!(request instanceof HttpServletRequest)
                || !(response instanceof HttpServletResponse)) {
            log.error("Servlets are not HttpServlets!");
            throw new ServletException("Servlets are not HttpServlets!");
        }

        ServletOutputStream out = null;
        ParameterRequestWrapper req = null;
        DataResponseWrapper res = null;

        // the request and response context
        RequestCtx reqCtx = null;
        ResponseCtx resCtx = null;

        String uri = ((HttpServletRequest) request).getRequestURI();
        String servletPath = ((HttpServletRequest) request).getServletPath();
        if (log.isDebugEnabled()) {
            log.debug("Incoming URI: " + uri);
            log.debug("Incoming servletPath: " + servletPath);
        }

        // get the filter (or null if no filter)
        RESTFilter filter = getFilter(servletPath);
        try {
            // handle the request if we have a filter
            if (filter != null) {
                // get a handle for the original OutputStream
                out = response.getOutputStream();

                // substitute our own request object that manages parameters
                try {
                    req =
                            new ParameterRequestWrapper((HttpServletRequest) request);
                } catch (Exception e) {
                    throw new PEPException(e);
                }

                // substitute our own response object that captures the data
                res = new DataResponseWrapper(((HttpServletResponse) response));

                if (log.isDebugEnabled()) {
                    log.debug("Filtering URI: [" + req.getRequestURI()
                            + "] with: [" + filter.getClass().getName() + "]");
                }

                reqCtx = filter.handleRequest(req, res);
                if (reqCtx != null) {
                    resCtx = ctxHandler.evaluate(reqCtx);
                    enforce(resCtx);
                }

                // pass the request along to the next chain...
                chain.doFilter(req, res);
            } else {
                // no filter, just use the original request/response...
                chain.doFilter(request, response);
            }

            // handle the response if we have a filter
            if (filter != null) {
                reqCtx = filter.handleResponse(req, res);
                if (reqCtx != null) {
                    resCtx = ctxHandler.evaluate(reqCtx);
                    enforce(resCtx);
                }

                out.write(res.getData());
                out.flush();
                out.close();
            }
        } catch (AuthzDeniedException ae) {
            if (!res.isCommitted()
                    && (req.getRemoteUser() == null || "".equals(req
                            .getRemoteUser().trim()))) {
                loginForm(res);
            } else {
                denyAccess((HttpServletResponse) response, ae.getMessage());
            }
        } catch (PEPException pe) {
            throw new ServletException("Error evaluating request", pe);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterCfg) throws ServletException {
        try {
            ctxHandler = ContextHandlerImpl.getInstance();
        } catch (PEPException pe) {
            log.error("Error obtaining ContextHandler", pe);
            throw new ServletException("Error obtaining ContextHandler", pe);
        }

        log.info("Initialising Servlet Filter: " + PEP.class.getName());
        filterConfig = filterCfg;

        // exit if no config. Should always have a config.
        if (filterConfig == null) {
            log.error("No config found!");
            throw new ServletException("No config found for filter (filterConfig)");
        }

        loadFilters();
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        log.info("Destroying Servlet Filter: " + PEP.class.getName());
        filterConfig = null;
        filters = null;
        ctxHandler = null;
    }

    private void loadFilters() throws ServletException {
        filters = new HashMap<String, RESTFilter>();

        try {
            // get the PEP configuration
            File configPEPFile =
                    new File(Constants.FEDORA_HOME,
                             "server/config/config-melcoe-pep.xml");
            InputStream is = new FileInputStream(configPEPFile);
            if (is == null) {
                throw new PEPException("Could not locate config file: config-melcoe-pep.xml");
            }

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(is);

            Node node = doc.getElementsByTagName("handlers-rest").item(0);
            NodeList nodes = node.getChildNodes();
            for (int x = 0; x < nodes.getLength(); x++) {
                Node n = nodes.item(x);
                if (n.getNodeType() == Node.ELEMENT_NODE
                        && "handler".equals(n.getNodeName())) {
                    String opn =
                            n.getAttributes().getNamedItem("operation")
                                    .getNodeValue();
                    String cls =
                            n.getAttributes().getNamedItem("class")
                                    .getNodeValue();

                    if (opn == null || "".equals(opn)) {
                        throw new PEPException("Cannot have a missing or empty operation attribute");
                    }

                    if (cls == null || "".equals(cls)) {
                        throw new PEPException("Cannot have a missing or empty class attribute");
                    }

                    try {
                        Class<?> filterClass = Class.forName(cls);
                        RESTFilter filter =
                                (RESTFilter) filterClass.newInstance();
                        filters.put(opn, filter);
                        if (log.isDebugEnabled()) {
                            log.debug("filter added to filter map: " + opn
                                    + "/" + cls);
                        }
                    } catch (ClassNotFoundException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("filterClass not found for: " + cls);
                        }
                    } catch (InstantiationException ie) {
                        log.error("Could not instantiate filter: " + cls);
                        throw new ServletException(ie.getMessage(), ie);
                    } catch (IllegalAccessException iae) {
                        log.error("Could not instantiate filter: " + cls);
                        throw new ServletException(iae.getMessage(), iae);
                    }
                }
            }
        } catch (Exception e) {
            log.fatal("Failed to initialse the PEP for REST");
            log.fatal(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }
    }

    /**
     * Obtains a filter from the filter map. If the filter does not exist in the
     * filter map, then an attempt to create the required filter is made and if
     * successful it is added to the filter map.
     * 
     * @param servletPath
     *        the servletPath of incoming servlet request
     * @return the filter to use
     * @throws ServletException
     */
    private RESTFilter getFilter(String servletPath) throws ServletException {
        RESTFilter filter = filters.get(servletPath);

        if (filter != null && log.isDebugEnabled())
        	log.debug("obtaining filter: " + filter.getClass().getName());

        return filter;
    }

    /**
     * Enforces a decision returned from the PDP.
     * 
     * @param res
     *        the XACML response
     * @throws AuthzDeniedException
     */
    private void enforce(ResponseCtx res) throws AuthzDeniedException {
        @SuppressWarnings("unchecked")
        Set<Result> results = res.getResults();
        for (Result r : results) {
            if (r.getDecision() != Result.DECISION_PERMIT) {
                log.debug("Denying access: " + r.getDecision());
                switch (r.getDecision()) {
                    case Result.DECISION_DENY:
                        throw new AuthzDeniedException("Deny");
                    case Result.DECISION_INDETERMINATE:
                        throw new AuthzDeniedException("Indeterminate");
                    case Result.DECISION_NOT_APPLICABLE:
                        throw new AuthzDeniedException("NotApplicable");
                    default:
                }
            }
        }
        log.debug("Permitting access!");
    }

    /**
     * Outputs an access denied message.
     * 
     * @param out
     *        the output stream to send the message to
     * @param message
     *        the message to send
     */
    private void denyAccess(HttpServletResponse response, String message)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Fedora: 403 " + message.toUpperCase());

        response.reset();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/plain");
        response.setContentLength(sb.length());
        ServletOutputStream out = response.getOutputStream();
        out.write(sb.toString().getBytes());
        out.flush();
        out.close();
    }

    /**
     * Sends a 401 error to the browser. This forces a login screen to be
     * displayed allowing the user to login.
     * 
     * @param response
     *        the response to set the headers and status
     */
    private void loginForm(HttpServletResponse response) {
        response.reset();
        response.addHeader("WWW-Authenticate",
                           "Basic realm=\"!!Fedora Repository Server\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
