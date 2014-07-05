/*
 * File: ModifyObject.java
 *
 * Copyright 2009 2DC
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

package org.fcrepo.server.security.xacml.pep.rest.objectshandlers;

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
import org.fcrepo.server.security.xacml.pep.rest.filters.AbstractFilter;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.StringAttribute;


/**
 * Handles the ModifyObject operation.
 *
 * @author nish.naidoo@gmail.com
 */
public class ModifyObject
        extends AbstractFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(ModifyObject.class);

    /**
     * Default constructor.
     *
     * @throws PEPException
     */
    public ModifyObject()
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
        if (logger.isDebugEnabled()) {
            logger.debug("{}/handleRequest!", this.getClass().getName());
        }

        String state = request.getParameter("state");
        String ownerId = request.getParameter("ownerId");

        RequestCtx req = null;
        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr;
        try {
            String[] parts = getPathParts(request);
            resAttr = ResourceAttributes.getResources(parts);
            if (state != null && !state.isEmpty()) {
                resAttr.put(Constants.OBJECT.STATE.getURI(),
                            new StringAttribute(state));
            }
            if (ownerId != null && !ownerId.isEmpty()) {
                resAttr.put(Constants.OBJECT.OWNER.getURI(),
                            new StringAttribute(state));
            }

            if (state != null && state.equals("A")) {
                actions.put(Constants.ACTION.ID.getURI(),
                            new StringAttribute("publish"));
            } else if (state != null && state.equals("I")) {
                actions.put(Constants.ACTION.ID.getURI(),
                            new StringAttribute("unpublish"));
            } else {
                actions.put(Constants.ACTION.ID.getURI(),
                            Constants.ACTION.MODIFY_OBJECT
                                    .getStringAttribute());
            }
            actions.put(Constants.ACTION.API.getURI(),
                        Constants.ACTION.APIM.getStringAttribute());

            req =
                    getContextHandler().buildRequest(getSubjects(request),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(request));

            LogUtil.statLog(request.getRemoteUser(),
                            Constants.ACTION.MODIFY_OBJECT.uri,
                            parts[1],
                            null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
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
