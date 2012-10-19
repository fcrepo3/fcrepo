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
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;


/**
 * Handles the get operations.
 *
 * @author nishen@melcoe.mq.edu.au
 */
public class GetFilter
        extends AbstractFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(GetFilter.class);

    /**
     * Default constructor.
     *
     * @throws PEPException
     */
    public GetFilter()
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
        if (request.getPathInfo() == null) {
            logger.error("Bad request: " + request.getRequestURI());
            throw new ServletException("Bad request: "
                    + request.getRequestURI());
        }

        String[] parts = request.getPathInfo().split("/");
        if (parts.length < 2) {
            logger.warn("Not enough path components on the URI.");
            return null;
            // throw new ServletException("Not enough path components on the
            // URI.");
        }

        RequestCtx req = null;

        String pid = null;
        String dsID = null;
        String dissID = null;
        String methodName = null;
        String dateTime = null;

        if (logger.isDebugEnabled()) {
            for (String p : parts) {
                logger.debug("Parts: {}", p);
            }
        }

        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();

        String logAction = null;

        // Starting assumption is that we are doing a GetObjectProfile
        pid = parts[1];
        actions.put(Constants.ACTION.ID.getURI(),
                    Constants.ACTION.GET_OBJECT_PROFILE
                            .getStringAttribute());
        logAction =
                Constants.ACTION.GET_OBJECT_PROFILE.uri;

        if (parts.length > 2) {
            if (isDate(parts[2])) {
                dateTime = parts[2];
            } else if (isDatastream(parts[2])) {
                dsID = parts[2];
                actions.clear();
                actions
                        .put(Constants.ACTION.ID.getURI(),
                             Constants.ACTION.GET_DATASTREAM_DISSEMINATION
                                     .getStringAttribute());
                actions.put(Constants.ACTION.ID.getURI(),
                            Constants.ACTION.GET_DATASTREAM
                                    .getStringAttribute());
                logAction =
                        Constants.ACTION.GET_DATASTREAM_DISSEMINATION.getURI()
                                .toASCIIString();
            } else {
                dissID = parts[2];
                actions.clear();
                actions
                        .put(Constants.ACTION.ID.getURI(),
                             Constants.ACTION.GET_DISSEMINATION
                                     .getStringAttribute());
                logAction =
                        Constants.ACTION.GET_DISSEMINATION.uri;
            }
        }

        if (parts.length > 3) {
            if (isDate(parts[3])) {
                dateTime = parts[3];
            } else {
                methodName = parts[3];
            }
        }

        if (parts.length > 4) {
            if (isDate(parts[4])) {
                dateTime = parts[4];
            }
        }

        try {
            if (pid != null && !"".equals(pid)) {
                resAttr.put(Constants.OBJECT.PID.getURI(),
                            new StringAttribute(pid));
            // XACML 1.0 conformance. resource-id is mandatory. Remove when switching to 2.0
                resAttr
                        .put(Constants.XACML1_RESOURCE.ID.getURI(),
                             new AnyURIAttribute(new URI(pid)));
            }
            if (dsID != null && !"".equals(dsID)) {
                resAttr.put(Constants.DATASTREAM.ID.getURI(),
                            new StringAttribute(dsID));
            }
            if (dissID != null && !"".equals(dissID)) {
                resAttr.put(Constants.DISSEMINATOR.ID.getURI(),
                            new StringAttribute(dissID));
            }
            if (methodName != null && !"".equals(methodName)) {
                resAttr.put(Constants.DISSEMINATOR.METHOD.getURI(),
                            new StringAttribute(methodName));
            }
            if (dateTime != null && !"".equals(dateTime)) {
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

            LogUtil.statLog(request.getRemoteUser(), logAction, pid, dsID);
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
