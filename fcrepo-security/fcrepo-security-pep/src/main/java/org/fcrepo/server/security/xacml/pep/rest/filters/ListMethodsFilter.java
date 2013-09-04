/*
 * File: ListMethodsFilter.java
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
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.ResourceAttributes;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.ctx.RequestCtx;


/**
 * @author nishen@melcoe.mq.edu.au
 */
public class ListMethodsFilter
        extends AbstractFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(ListMethodsFilter.class);

    /**
     * Default constructor.
     *
     * @throws PEPException
     */
    public ListMethodsFilter()
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
    public RequestCtx handleRequest(HttpServletRequest request,
                                    HttpServletResponse response)
            throws IOException, ServletException {
        if (request.getPathInfo() == null) {
            logger.error("Bad request: " + request.getRequestURI());
            throw new ServletException("Bad request: "
                    + request.getRequestURI());
        }

        String[] parts = request.getPathInfo().split("/");
        if (parts.length < 2) {
            logger.info("Not enough path components on the URI.");
            throw new ServletException("Not enough path components on the URI.");
        }

        RequestCtx req = null;

        String pid = null;
        String dateTime = null;

        // Starting assumption
        pid = parts[1];

        if (parts.length > 2) {
            if (isDate(parts[2])) {
                dateTime = parts[2];
            }
        }

        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr;

        try {
            String[] parts1 = getPathParts(request);
            resAttr = ResourceAttributes.getResources(parts1);

            if (dateTime != null && !dateTime.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.AS_OF_DATETIME.getURI(),
                            DateTimeAttribute.getInstance(dateTime));
            }

            actions.put(Constants.ACTION.API.getURI(),
                        Constants.ACTION.APIA.getStringAttribute());
            actions.put(Constants.ACTION.ID.getURI(),
                        Constants.ACTION.LIST_METHODS.getStringAttribute());

            req =
                    getContextHandler().buildRequest(getSubjects(request),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(request));

            LogUtil.statLog(request.getRemoteUser(),
                            Constants.ACTION.LIST_METHODS.uri,
                            pid,
                            null);
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
