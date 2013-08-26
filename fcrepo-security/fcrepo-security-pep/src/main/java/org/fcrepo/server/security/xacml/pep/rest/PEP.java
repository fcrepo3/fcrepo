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

package org.fcrepo.server.security.xacml.pep.rest;

import java.io.IOException;
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

import org.fcrepo.server.security.xacml.pep.AuthzDeniedException;
import org.fcrepo.server.security.xacml.pep.ContextHandler;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.rest.filters.DataResponseWrapper;
import org.fcrepo.server.security.xacml.pep.rest.filters.ObjectsFilter;
import org.fcrepo.server.security.xacml.pep.rest.filters.ObjectsRESTFilterMatcher;
import org.fcrepo.server.security.xacml.pep.rest.filters.ParameterRequestWrapper;
import org.fcrepo.server.security.xacml.pep.rest.filters.RESTFilter;
import org.fcrepo.server.security.xacml.pep.rest.filters.ResponseHandlingRESTFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;


/**
 * This is the PEP for the REST interface.
 *
 * @author nishen@melcoe.mq.edu.au
 */
public final class PEP
        implements Filter {

    private static final Logger logger =
            LoggerFactory.getLogger(PEP.class);

    private Map<String, RESTFilter> m_filters;
    private ObjectsRESTFilterMatcher m_objectsRESTFilterMatcher;

    private ContextHandler m_ctxHandler = null;

    public PEP(ObjectsRESTFilterMatcher objectsRESTFilterMatcher, Map<String, RESTFilter> filters ) throws PEPException {
        m_objectsRESTFilterMatcher = objectsRESTFilterMatcher;
        m_filters = filters;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException,
            ServletException {
        // if a response has already been committed, bypass this filter...
        if (response.isCommitted()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Response has already been committed. Bypassing PEP.");
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
            logger.error("Servlets are not HttpServlets!");
            throw new ServletException("Servlets are not HttpServlets!");
        }

        ServletOutputStream out = null;
        ParameterRequestWrapper req = null;
        HttpServletResponse res = (HttpServletResponse)response;

        // the request and response context
        RequestCtx reqCtx = null;
        ResponseCtx resCtx = null;

        String uri = ((HttpServletRequest) request).getRequestURI();
        String servletPath = ((HttpServletRequest) request).getServletPath();
        if (logger.isDebugEnabled()) {
            logger.debug("Incoming URI: {}", uri);
            logger.debug("Incoming servletPath: {}", servletPath);
        }

        // Fix-up for direct web.xml servlet mappings for:
        // /objects/nextPID and /objects/nextPID.xml
        // (so servlet path will be different in these cases)
        // FIXME: we don't support .xml any more? but included as the servlet mapping is still specified
        if (uri.endsWith("/nextPID") || uri.endsWith("/nextPID.xml")) {
            servletPath = "/objects";
        }


        // get the filter (or null if no filter)
        RESTFilter filter = m_filters.get(servletPath);

        if (filter != null && logger.isDebugEnabled())
            logger.debug("obtaining filter: {}", filter.getClass().getName());

        if(ObjectsFilter.class.isInstance(filter)) { // go find the ObjectHandler
            HttpServletRequest httpRequest = (HttpServletRequest)request;
      	  filter = this.m_objectsRESTFilterMatcher.getObjectsHandler(httpRequest);
      	  if (filter == null) {
            logger.error("No FeSL REST objects handler found for \"{}\"", httpRequest.getPathInfo());
            throw new ServletException(new PEPException("No FeSL REST objects handler found for " + servletPath));
      	  }
        }
        try {
            // handle the request if we have a filter
            if (filter != null) {
                // substitute our own request object that manages parameters
                try {
                    req = new ParameterRequestWrapper((HttpServletRequest) request);
                } catch (Exception e) {
                    throw new PEPException(e);
                }

                logger.debug("Filtering URI: [{}] with: [{}]" , req.getRequestURI(), filter.getClass().getName());

                if(ResponseHandlingRESTFilter.class.isInstance(filter)) {
               	    // substitute our own response object that captures the data
               	    res = new DataResponseWrapper(((HttpServletResponse) response));
                    // get a handle for the original OutputStream
                    out = response.getOutputStream();
                    logger.debug("Filtering will include post-processing the response");
                }

                reqCtx = filter.handleRequest(req, res);
                if (reqCtx != null) {
                    resCtx = m_ctxHandler.evaluate(reqCtx);
                    enforce(resCtx);
                }

                // pass the request along to the next chain...
                chain.doFilter(req, res);
            } else {
                // there must always be a filter, even if it is a NOOP
                logger.error("No FeSL REST filter found for \"{}\"", servletPath);
                throw new PEPException("No FeSL REST filter found for " + servletPath);
            }

            if(ResponseHandlingRESTFilter.class.isInstance(filter)) {
            	// handle the response if we have a non-null response handling filter
            	reqCtx = ((ResponseHandlingRESTFilter)filter).handleResponse(req, res);
            	if (reqCtx != null) {
            		resCtx = m_ctxHandler.evaluate(reqCtx);
            		enforce(resCtx);
            	}

            	out.write(((DataResponseWrapper)res).getData());
            	out.flush();
            	out.close();
            }
        } catch (AuthzDeniedException ae) {
            if (!res.isCommitted()
                    && (req.getRemoteUser() == null || req
                            .getRemoteUser().trim().isEmpty())) {
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
    public void init() throws ServletException {
        logger.info("Initialising Servlet Filter: {}", PEP.class);
        if (m_ctxHandler == null) {
            throw new ServletException("Error obtaining ContextHandler");
        }
    }

    @Override
    public void init(FilterConfig cfg) throws ServletException {
        init();
    }


    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
        logger.info("Destroying Servlet Filter: {}", PEP.class);
        m_filters = null;
        m_objectsRESTFilterMatcher = null;
        m_ctxHandler = null;
    }

    public void setContextHandler(ContextHandler ctxHandler) {
        m_ctxHandler = ctxHandler;
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
                logger.debug("Denying access: " + r.getDecision());
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
        logger.debug("Permitting access!");
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
        sb.append("Fedora: 403 ").append(message.toUpperCase());

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
