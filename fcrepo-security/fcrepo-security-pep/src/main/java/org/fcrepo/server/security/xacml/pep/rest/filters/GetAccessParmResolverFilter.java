/*
 * File: GetFilter.java
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

package org.fcrepo.server.security.xacml.pep.rest.filters;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcrepo.common.Constants;
import org.fcrepo.server.security.RequestCtx;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.ResourceAttributes;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.DateTimeAttribute;
import org.jboss.security.xacml.sunxacml.attr.StringAttribute;


/**
 * Handles the get operations.
 *
 * @author nishen@melcoe.mq.edu.au
 */
public class GetAccessParmResolverFilter
        extends AbstractFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(GetAccessParmResolverFilter.class);

    /**
     * Default constructor.
     *
     * @throws PEPException
     */
    public GetAccessParmResolverFilter()
            throws PEPException {
        super();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.rest.filters.RESTFilter#handleRequest(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @SuppressWarnings("deprecation")
    public RequestCtx handleRequest(HttpServletRequest request,
                                    HttpServletResponse response)
            throws IOException, ServletException {
        if (request.getPathInfo() != null) {
            logger.error("Bad request: " + request.getRequestURI());
            throw new ServletException("Bad request: "
                    + request.getRequestURI());
        }

        RequestCtx req = null;

        String pid = request.getParameter("PID");
        String dissID = "sDefPID";
        String methodName = request.getParameter("methodName");
        String dateTime = request.getParameter("asOfDateTime");

        if (logger.isDebugEnabled()) {
            logger.debug("PID: {} sDefPID {} methodName {}", pid, dissID, methodName);
        }

        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr;

        String logAction = null;

        // Starting assumption is that we are doing a GetObjectProfile
        actions.put(Constants.ACTION.ID.getURI(),
                    Constants.ACTION.GET_DISSEMINATION
                            .getStringAttribute());
        logAction =
                Constants.ACTION.GET_DISSEMINATION.uri;

        try {
            resAttr = ResourceAttributes.getResources(pid);
            
            if (dissID != null && !dissID.isEmpty()) {
                resAttr.put(Constants.DISSEMINATOR.ID.getURI(),
                            new StringAttribute(dissID));
            }
            if (methodName != null && !methodName.isEmpty()) {
                resAttr.put(Constants.DISSEMINATOR.METHOD.getURI(),
                            new StringAttribute(methodName));
            }
            if (dateTime != null && !dateTime.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.AS_OF_DATETIME.getURI(),
                            DateTimeAttribute.getInstance(dateTime));
            }

            actions.put(Constants.ACTION.API.getURI(),
                        Constants.ACTION.APIA.getStringAttribute());

            req =
                    getContextHandler().buildRequest(getSubjects(request),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(request));

            LogUtil.statLog(request.getRemoteUser(), logAction, pid, dissID);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ServletException(e);
        }

        return req;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.rest.filters.RESTFilter#handleResponse(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public RequestCtx handleResponse(HttpServletRequest request,
                                     HttpServletResponse response)
            throws IOException, ServletException {
        return null;
    }
}
