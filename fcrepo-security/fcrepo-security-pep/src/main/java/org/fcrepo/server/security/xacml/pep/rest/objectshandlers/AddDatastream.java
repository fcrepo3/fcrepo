/*
 * File: AddDatastream.java
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
import org.fcrepo.server.security.xacml.pdp.data.FedoraPolicyStore;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.ResourceAttributes;
import org.fcrepo.server.security.xacml.pep.rest.filters.AbstractFilter;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;


/**
 * Handles the AddDatastream operation.
 *
 * @author nish.naidoo@gmail.com
 */
public class AddDatastream
        extends AbstractFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(AddDatastream.class);

    /**
     * Default constructor.
     *
     * @throws PEPException
     */
    public AddDatastream()
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

        String[] parts = getPathParts(request);
        // -/objects/{pid}/datastreams/{dsID}
        if (parts.length < 4) {
            logger.error("Not enough path components on the URI: {}", request.getRequestURI());
            throw new ServletException("Not enough path components on the URI: " + request.getRequestURI());
        }

        String mimeType = request.getParameter("mimeType");
        String formatURI = request.getParameter("formatURI");
        String dsLocation = request.getParameter("dsLocation");
        String controlGroup = request.getParameter("controlGroup");
        String dsState = request.getParameter("dsState");
        String checksumType = request.getParameter("checksumType");
        String checksum = request.getParameter("checksum");
        // String logMessage = null;

        RequestCtx req = null;
        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr;
        try {
            resAttr = ResourceAttributes.getResources(parts);
            if (mimeType != null && !mimeType.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_MIME_TYPE.getURI(),
                            new StringAttribute(mimeType));
            }
            if (formatURI != null && !formatURI.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_FORMAT_URI.getURI(),
                            new AnyURIAttribute(new URI(formatURI)));
            }
            if (dsLocation != null && !dsLocation.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_LOCATION.getURI(),
                            new AnyURIAttribute(new URI(dsLocation)));
            }
            if (controlGroup != null && !controlGroup.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_CONTROL_GROUP.getURI(),
                            new StringAttribute(controlGroup));
            }
            if (dsState != null && !dsState.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_STATE.getURI(),
                            new StringAttribute(dsState));
            }
            if (checksumType != null && !checksumType.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_CHECKSUM_TYPE.getURI(),
                            new StringAttribute(checksumType));
            }
            if (checksum != null && !checksum.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_CHECKSUM.getURI(),
                            new StringAttribute(checksum));
            }

            actions.put(Constants.ACTION.ID.getURI(),
                        Constants.ACTION.ADD_DATASTREAM
                                .getStringAttribute());
            actions.put(Constants.ACTION.API.getURI(),
                        Constants.ACTION.APIM.getStringAttribute());
            String dsID = parts[3];
            // modifying the FeSL policy datastream requires policy management permissions
            if (dsID != null && dsID.equals(FedoraPolicyStore.FESL_POLICY_DATASTREAM)) {
                actions.put(Constants.ACTION.ID.getURI(),
                            Constants.ACTION.MANAGE_POLICIES.getStringAttribute());
            }


            req =
                    getContextHandler().buildRequest(getSubjects(request),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(request));

            LogUtil.statLog(request.getRemoteUser(),
                            Constants.ACTION.ADD_DATASTREAM.uri,
                            parts[1],
                            dsID);
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
